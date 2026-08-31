package com.example.smsforwarder;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
public class SubscriptionHelper {
    public static String getDisplayNumber(Context context, int subscriptionId, SharedPreferences prefs) {
        int slotIndex = getSlotIndex(context, subscriptionId);
        String aliasKey = (slotIndex == 1) ? "alias_sim1" : "alias_sim2";
        String alias = prefs.getString(aliasKey, "");
        if (!alias.isEmpty()) return alias;
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && subscriptionId >= 0) {
                TelephonyManager specific = tm.createForSubscriptionId(subscriptionId);
                String num = specific.getLine1Number();
                if (num != null && !num.isEmpty()) return num;
            }
        } catch (Exception ignored) {}
        return "卡" + slotIndex;
    }
    public static int getSlotIndex(Context context, int subscriptionId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            android.telephony.SubscriptionInfo info = sm.getActiveSubscriptionInfo(subscriptionId);
            if (info != null) return info.getSimSlotIndex() + 1;
        }
        return (subscriptionId == 1) ? 1 : 2;
    }
}