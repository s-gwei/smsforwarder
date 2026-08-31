package com.example.smsforwarder;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private EditText etWebhook, etAlias1, etAlias2;
    private Switch switchEnable;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("config", MODE_PRIVATE);
        etWebhook = findViewById(R.id.et_webhook);
        etAlias1 = findViewById(R.id.et_alias_sim1);
        etAlias2 = findViewById(R.id.et_alias_sim2);
        switchEnable = findViewById(R.id.switch_enable);
        Button btnTest = findViewById(R.id.btn_test);

        // 加载保存的数据
        etWebhook.setText(prefs.getString("webhook_url", ""));
        etAlias1.setText(prefs.getString("alias_sim1", ""));
        etAlias2.setText(prefs.getString("alias_sim2", ""));
        switchEnable.setChecked(prefs.getBoolean("service_enabled", false));

        // 申请运行时权限
        String[] perms = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.POST_NOTIFICATIONS
        };
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, perms, 1001);
                break;
            }
        }

        // 引导用户关闭电池优化（保活）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("优化建议")
                        .setMessage("为保证服务不被系统杀死，请允许关闭电池优化")
                        .setPositiveButton("去设置", (d, w) ->
                                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)))
                        .setNegativeButton("稍后", null)
                        .show();
            }
        }

        // 开关监听
        switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startService(new Intent(this, SmsForwardService.class));
            } else {
                stopService(new Intent(this, SmsForwardService.class));
            }
            prefs.edit().putBoolean("service_enabled", isChecked).apply();
        });

        // 自动保存输入内容（失去焦点时保存）
        etWebhook.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) prefs.edit().putString("webhook_url", etWebhook.getText().toString().trim()).apply();
        });
        etAlias1.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) prefs.edit().putString("alias_sim1", etAlias1.getText().toString().trim()).apply();
        });
        etAlias2.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) prefs.edit().putString("alias_sim2", etAlias2.getText().toString().trim()).apply();
        });

        // 测试按钮
        btnTest.setOnClickListener(v -> {
            String webhook = etWebhook.getText().toString().trim();
            if (webhook.isEmpty()) {
                Toast.makeText(this, "请先输入Webhook地址", Toast.LENGTH_SHORT).show();
                return;
            }
            SmsData testData = new SmsData(
                    "测试系统",
                    "这是一条测试消息，配置成功！",
                    "2026-08-31 12:00:00",
                    1,
                    "测试卡"
            );
            Intent intent = new Intent(this, SmsForwardService.class);
            intent.putExtra("sms_data", testData);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(this, "测试消息已发送，请查看企微群", Toast.LENGTH_SHORT).show();
        });
    }
}