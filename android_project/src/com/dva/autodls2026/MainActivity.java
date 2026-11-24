package com.dva.autodls2026;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private TextView tvAccStatus;
    private TextView tvOverlayStatus;
    private TextView tvScriptVersion;
    private android.widget.EditText etUpdateUrl;
    private Button btnGrantAcc;
    private Button btnGrantOverlay;
    private Button btnCheckUpdate;
    private Button btnStartFloating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAccStatus = (TextView) findViewById(R.id.tv_acc_status);
        tvOverlayStatus = (TextView) findViewById(R.id.tv_overlay_status);
        tvScriptVersion = (TextView) findViewById(R.id.tv_script_version);
        etUpdateUrl = (android.widget.EditText) findViewById(R.id.et_update_url);
        btnGrantAcc = (Button) findViewById(R.id.btn_grant_acc);
        btnGrantOverlay = (Button) findViewById(R.id.btn_grant_overlay);
        btnCheckUpdate = (Button) findViewById(R.id.btn_check_update);
        btnStartFloating = (Button) findViewById(R.id.btn_start_floating);

        etUpdateUrl.setText(OnlineUpdateManager.getUpdateUrl(this));

        // Nút mở cài đặt Trợ năng
        btnGrantAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Hãy tìm 'Auto DLS 2026' và BẬT nó lên", Toast.LENGTH_LONG).show();
            }
        });

        // Nút mở cài đặt Cửa sổ nổi
        btnGrantOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Máy của bạn đã tự động hỗ trợ quyền này!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Nút kiểm tra cập nhật Online
        btnCheckUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputUrl = etUpdateUrl.getText().toString().trim();
                if (!inputUrl.isEmpty()) {
                    OnlineUpdateManager.setUpdateUrl(MainActivity.this, inputUrl);
                }
                btnCheckUpdate.setEnabled(false);
                btnCheckUpdate.setText("Đang kiểm tra kết nối...");
                OnlineUpdateManager.checkAndUpdate(MainActivity.this, new OnlineUpdateManager.UpdateCallback() {
                    @Override
                    public void onSuccess(final String version, final boolean isNewer) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnCheckUpdate.setEnabled(true);
                                btnCheckUpdate.setText("🔄 KIỂM TRA & CẬP NHẬT ONLINE NGAY");
                                tvScriptVersion.setText("Phiên bản kịch bản hiện tại: v" + version + " (Mới nhất ✓)");
                                Toast.makeText(MainActivity.this, "Kịch bản đã được cập nhật online thành công (v" + version + ")!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnCheckUpdate.setEnabled(true);
                                btnCheckUpdate.setText("🔄 KIỂM TRA & CẬP NHẬT ONLINE NGAY");
                                Toast.makeText(MainActivity.this, "Cập nhật online thất bại: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        // Nút mở bóng nổi
        btnStartFloating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Vui lòng cấp quyền Cửa sổ nổi trước!", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!AutoAccessibilityService.isRunning()) {
                    Toast.makeText(MainActivity.this, "Vui lòng cấp quyền Trợ năng trước!", Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, FloatingService.class);
                startService(intent);
                Toast.makeText(MainActivity.this, "Đã mở bóng nổi! Hãy vào game DLS 2026.", Toast.LENGTH_LONG).show();
                finish(); // Đóng giao diện chính, để bóng nổi hiển thị
            }
        });

        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        // Kiểm tra quyền Trợ năng
        if (AutoAccessibilityService.isRunning()) {
            tvAccStatus.setText("1. Quyền Trợ năng: Đã cấp ✓");
            tvAccStatus.setTextColor(Color.parseColor("#00E676"));
            btnGrantAcc.setEnabled(false);
            btnGrantAcc.setText("ĐÃ BẬT");
        } else {
            tvAccStatus.setText("1. Quyền Trợ năng: Chưa cấp ✗");
            tvAccStatus.setTextColor(Color.parseColor("#FF5252"));
            btnGrantAcc.setEnabled(true);
            btnGrantAcc.setText("CẤP QUYỀN");
        }

        // Kiểm tra quyền Cửa sổ nổi
        boolean canDrawOverlay = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canDrawOverlay = Settings.canDrawOverlays(this);
        }
        if (canDrawOverlay) {
            tvOverlayStatus.setText("2. Quyền Cửa sổ nổi: Đã cấp ✓");
            tvOverlayStatus.setTextColor(Color.parseColor("#00E676"));
            btnGrantOverlay.setEnabled(false);
            btnGrantOverlay.setText("ĐÃ BẬT");
        } else {
            tvOverlayStatus.setText("2. Quyền Cửa sổ nổi: Chưa cấp ✗");
            tvOverlayStatus.setTextColor(Color.parseColor("#FF5252"));
            btnGrantOverlay.setEnabled(true);
            btnGrantOverlay.setText("CẤP QUYỀN");
        }

        // Cập nhật phiên bản script
        JSONObject config = OnlineUpdateManager.loadConfig(this);
        tvScriptVersion.setText("Phiên bản kịch bản hiện tại: v" + config.optString("version", "1.0.0"));
    }
}

