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
        // 如果依然拿不到，默认当作卡1（降级方案）
        if (subscriptionId == -1) {
            subscriptionId = 1;
        }

        String sender = "";
        String body = "";

        for (Object pdu : pdus) {
            SmsMessage sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, intent.getExtras().getString("format"));
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (sms != null) {
                sender = sms.getDisplayOriginatingAddress();
                body = sms.getMessageBody();
            }
        }

        if (body != null && !body.isEmpty()) {
            Log.d("SmsReceiver", "收到短信，卡槽ID: " + subscriptionId);

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