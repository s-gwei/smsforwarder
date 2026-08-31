package com.example.smsforwarder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import okhttp3.*;
import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
public class SmsForwardService extends Service {
    private Handler handler = new Handler(Looper.getMainLooper());
    private OkHttpClient client;
    private Gson gson = new Gson();
    @Override
    public void onCreate() {
        super.onCreate();
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        startForeground(1001, createNotification());
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("sms_data")) {
            SmsData data = (SmsData) intent.getSerializableExtra("sms_data");
            SharedPreferences prefs = getSharedPreferences("config", Context.MODE_PRIVATE);
            String webhook = prefs.getString("webhook_url", "");
            if (webhook.isEmpty()) { stopSelf(); return START_NOT_STICKY; }
            String content = "【手机号：" + data.displayNumber + "】\n发送人：" + data.sender +
                    "\n时间：" + data.timestamp + "\n内容：" + data.body;
            sendWithRetry(webhook, content, 0);
        }
        return START_STICKY;
    }
    private void sendWithRetry(String url, String content, int attempt) {
        if (attempt >= 3) {
            stopSelf();
            return;
        }

        // 使用 Map 构建 JSON，完全不用定义多余的类
        java.util.Map<String, Object> jsonMap = new java.util.HashMap<>();
        jsonMap.put("msgtype", "text");
        java.util.Map<String, String> textMap = new java.util.HashMap<>();
        textMap.put("content", content);
        jsonMap.put("text", textMap);

        String json = gson.toJson(jsonMap);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.postDelayed(() -> sendWithRetry(url, content, attempt + 1), 5000);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    stopSelf();
                } else {
                    handler.postDelayed(() -> sendWithRetry(url, content, attempt + 1), 5000);
                }
                response.close();
            }
        });
    }
    private Notification createNotification() {
        String channelId = "sms_forward_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "短信转发", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持服务运行");
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("短信转发助手")
                .setContentText("服务运行中...")
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .build();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}