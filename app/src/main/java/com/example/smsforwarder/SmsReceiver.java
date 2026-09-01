package com.example.smsforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) return;

        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        if (pdus == null) return;

        // 从 Intent 中读取卡槽 ID（标准做法）
        int subscriptionId = intent.getIntExtra("subscription", -1);
        if (subscriptionId == -1) {
            subscriptionId = intent.getIntExtra(TelephonyManager.EXTRA_SUBSCRIPTION_ID, -1);
        }
        if (subscriptionId == -1) {
            subscriptionId = 1; // 降级默认卡1
        }

        String sender = "";
        // ============================================================
        // 【核心修复】使用 StringBuilder 拼接长短信的所有片段
        // 之前直接用 = 赋值，导致后面的片段覆盖了前面的！
        // ============================================================
        StringBuilder fullBody = new StringBuilder();

        for (Object pdu : pdus) {
            SmsMessage sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, intent.getExtras().getString("format"));
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (sms != null) {
                // 发送方号码只取第一次的值（防止重复）
                if (sender.isEmpty()) {
                    sender = sms.getDisplayOriginatingAddress();
                }
                // 累加短信正文（而不是覆盖！）
                fullBody.append(sms.getMessageBody());
            }
        }

        String body = fullBody.toString();

        if (body != null && !body.isEmpty()) {
            Log.d("SmsReceiver", "收到完整短信，长度：" + body.length() + "，卡槽ID: " + subscriptionId);

            SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
            SmsData data = new SmsData(
                    sender,
                    body,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()),
                    SubscriptionHelper.getSlotIndex(context, subscriptionId),
                    SubscriptionHelper.getDisplayNumber(context, subscriptionId, prefs)
            );

            Intent serviceIntent = new Intent(context, SmsForwardService.class);
            serviceIntent.putExtra("sms_data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}