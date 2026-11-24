package com.dva.autodls2026;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private View layoutExpanded;
    private View layoutCollapsed;
    private TextView tvStatus;
    private TextView tvRes;
    private Button btnToggle;
    private Button btnUpdate;

    private boolean isBotRunning = false;
    private Thread botThread = null;
    private PowerManager.WakeLock wakeLock;
    private Handler mainHandler;

    private int screenWidth = 1920;
    private int screenHeight = 1080;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        updateScreenResolution();

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 80;
        params.y = 120;

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null);

        layoutExpanded = floatingView.findViewById(R.id.layout_expanded);
        layoutCollapsed = floatingView.findViewById(R.id.layout_collapsed);
        tvStatus = (TextView) floatingView.findViewById(R.id.tv_float_status);
        tvRes = (TextView) floatingView.findViewById(R.id.tv_float_res);
        btnToggle = (Button) floatingView.findViewById(R.id.btn_float_toggle);
        btnUpdate = (Button) floatingView.findViewById(R.id.btn_float_update);

        tvRes.setText("Màn hình: " + screenWidth + " x " + screenHeight);

        // Kéo thả view
        setupDrag();

        // Thu nhỏ / mở rộng
        floatingView.findViewById(R.id.btn_mini).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutExpanded.setVisibility(View.GONE);
                layoutCollapsed.setVisibility(View.VISIBLE);
            }
        });

        layoutCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutExpanded.setVisibility(View.VISIBLE);
                layoutCollapsed.setVisibility(View.GONE);
            }
        });

        floatingView.findViewById(R.id.btn_close_float).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopBot();
                stopSelf();
            }
        });

        // Bắt đầu / Tạm dừng
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isBotRunning) {
                    startBot();
                } else {
                    stopBot();
                }
            }
        });

        // Cập nhật online
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FloatingService.this, "Đang kiểm tra cập nhật online...", Toast.LENGTH_SHORT).show();
                OnlineUpdateManager.checkAndUpdate(FloatingService.this, new OnlineUpdateManager.UpdateCallback() {
                    @Override
                    public void onSuccess(final String version, final boolean isNewer) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(FloatingService.this, "Cập nhật thành công! Phiên bản: v" + version, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(FloatingService.this, "Cập nhật thất bại: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        windowManager.addView(floatingView, params);
    }

    private void updateScreenResolution() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int rawW = dm.widthPixels;
        int rawH = dm.heightPixels;
        // Luôn chuẩn hóa ngang cho DLS
        screenWidth = Math.max(rawW, rawH);
        screenHeight = Math.min(rawW, rawH);
    }

    private float getPxX(float percentX) {
        return screenWidth * (percentX / 100.0f);
    }

    private float getPxY(float percentY) {
        return screenHeight * (percentY / 100.0f);
    }

    private void startBot() {
        if (!AutoAccessibilityService.isRunning()) {
            Toast.makeText(this, "Chưa bật quyền Trợ năng! Vui lòng vào Cài đặt để bật.", Toast.LENGTH_LONG).show();
            return;
        }

        updateScreenResolution();
        isBotRunning = true;
        btnToggle.setText("⏸ TẠM DỪNG");
        tvStatus.setText("Trạng thái: Đang tự đá ⚡");
        tvStatus.setTextColor(Color.parseColor("#00E676"));

        // Giữ sáng màn hình
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "AutoDLS:GrindLock");
        wakeLock.acquire();

        botThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int loop = 0;
                while (isBotRunning) {
                    try {
                        loop++;
                        AutoAccessibilityService service = AutoAccessibilityService.getInstance();
                        if (service == null) break;

                        // Đọc cấu hình mới nhất (đã cập nhật online)
                        JSONObject config = OnlineUpdateManager.loadConfig(FloatingService.this);
                        JSONObject coords = config.optJSONObject("coords");
                        long pressB = config.optLong("pressB_duration_ms", 1400);

                        // 1. Giữ nút B (Pressing cướp bóng)
                        if (coords != null && coords.has("btn_b")) {
                            JSONArray b = coords.getJSONArray("btn_b");
                            service.press(getPxX((float) b.getDouble(0)), getPxY((float) b.getDouble(1)), pressB);
                        }
                        Thread.sleep(150);

                        // 2. Nhấp nút A (Sút / Phá bóng)
                        if (coords != null && coords.has("btn_a")) {
                            JSONArray a = coords.getJSONArray("btn_a");
                            service.tap(getPxX((float) a.getDouble(0)), getPxY((float) a.getDouble(1)));
                        }
                        Thread.sleep(150);

                        // 3. Chạm giữa màn hình bỏ qua replay
                        if (coords != null && coords.has("skip_cutscene")) {
                            JSONArray s = coords.getJSONArray("skip_cutscene");
                            service.tap(getPxX((float) s.getDouble(0)), getPxY((float) s.getDouble(1)));
                        }
                        Thread.sleep(150);

                        // 4. Bấm Tiếp tục qua hiệp 1 / hết trận
                        if (loop % 3 == 0 && coords != null) {
                            if (coords.has("continue_br")) {
                                JSONArray cbr = coords.getJSONArray("continue_br");
                                service.tap(getPxX((float) cbr.getDouble(0)), getPxY((float) cbr.getDouble(1)));
                            }
                            Thread.sleep(200);
                            if (coords.has("continue_c")) {
                                JSONArray cc = coords.getJSONArray("continue_c");
                                service.tap(getPxX((float) cc.getDouble(0)), getPxY((float) cc.getDouble(1)));
                            }
                        }

                        // 5. Kiểm tra vào trận mới / nhận thưởng
                        if (loop % 10 == 0 && coords != null) {
                            if (coords.has("play_match")) {
                                JSONArray p = coords.getJSONArray("play_match");
                                service.tap(getPxX((float) p.getDouble(0)), getPxY((float) p.getDouble(1)));
                            }
                            Thread.sleep(200);
                            if (coords.has("claim_reward")) {
                                JSONArray cr = coords.getJSONArray("claim_reward");
                                service.tap(getPxX((float) cr.getDouble(0)), getPxY((float) cr.getDouble(1)));
                            }
                            Thread.sleep(200);
                            if (coords.has("close_ad")) {
                                JSONArray ca = coords.getJSONArray("close_ad");
                                service.tap(getPxX((float) ca.getDouble(0)), getPxY((float) ca.getDouble(1)));
                            }
                        }

                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        botThread.start();
        Toast.makeText(this, "Auto DLS 2026: BẮT ĐẦU CHẠY!", Toast.LENGTH_SHORT).show();
    }

    private void stopBot() {
        isBotRunning = false;
        if (botThread != null) {
            botThread.interrupt();
            botThread = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        btnToggle.setText("▶ BẮT ĐẦU");
        tvStatus.setText("Trạng thái: Đã tạm dừng");
        tvStatus.setTextColor(Color.parseColor("#FFD600"));
    }

    private void setupDrag() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            params.x = initialX + dx;
                            params.y = initialY + dy;
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return false;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBot();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}

