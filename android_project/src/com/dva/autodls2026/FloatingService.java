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
    private TextView tvStats;
    private Button btnToggle;
    private Button btnMode;
    private Button btnUpdate;

    private static final int MODE_ADS_RESET = 0;
    private static final int MODE_AUTO_MATCH = 1;
    private int currentMode = MODE_ADS_RESET;
    private int adsWatchedCount = 0;

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
        tvStats = (TextView) floatingView.findViewById(R.id.tv_float_stats);
        btnToggle = (Button) floatingView.findViewById(R.id.btn_float_toggle);
        btnMode = (Button) floatingView.findViewById(R.id.btn_float_mode);
        btnUpdate = (Button) floatingView.findViewById(R.id.btn_float_update);

        tvRes.setText("Màn hình: " + screenWidth + " x " + screenHeight);

        btnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBotRunning) {
                    Toast.makeText(FloatingService.this, "Hãy tạm dừng bot trước khi đổi chế độ", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentMode == MODE_ADS_RESET) {
                    currentMode = MODE_AUTO_MATCH;
                    btnMode.setText("Chế độ: ⚽ Tự Đá Trận");
                } else {
                    currentMode = MODE_ADS_RESET;
                    btnMode.setText("Chế độ: 🎬 Cày Ads Reset");
                }
            }
        });

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

    private void setStatus(final String text) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tvStatus != null) tvStatus.setText(text);
            }
        });
    }

    private void updateStats() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tvStats != null) {
                    tvStats.setText("Đã xem: " + adsWatchedCount + " video (~" + (adsWatchedCount * 40) + " vàng)");
                }
            }
        });
    }

    private void sleepSafe(long ms) {
        long end = System.currentTimeMillis() + ms;
        while (isBotRunning && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(Math.min(200, end - System.currentTimeMillis()));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void startBot() {
        if (!AutoAccessibilityService.isRunning()) {
            Toast.makeText(this, "Chưa bật quyền Trợ năng! Vui lòng vào Cài đặt để bật.", Toast.LENGTH_LONG).show();
            return;
        }

        updateScreenResolution();
        isBotRunning = true;
        btnToggle.setText("⏸ TẠM DỪNG");
        tvStatus.setTextColor(Color.parseColor("#00E676"));

        // Giữ sáng màn hình
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "AutoDLS:GrindLock");
        wakeLock.acquire();

        botThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (currentMode == MODE_ADS_RESET) {
                    runAdsResetLoop();
                } else {
                    runAutoMatchLoop();
                }
            }
        });
        botThread.start();
        Toast.makeText(this, "Auto DLS 2026: BẮT ĐẦU CHẠY!", Toast.LENGTH_SHORT).show();
    }

    private void runAdsResetLoop() {
        while (isBotRunning) {
            try {
                AutoAccessibilityService service = AutoAccessibilityService.getInstance();
                if (service == null) break;

                // 1. Bấm nút FREE COINS ở sảnh (Ảnh 1: 21.0%, 4.5%)
                setStatus("1. Bấm Free Coins...");
                service.tap(getPxX(21.0f), getPxY(4.5f));
                sleepSafe(1200);

                // 2. Chờ video quảng cáo chạy (~32 giây)
                for (int s = 32; s > 0 && isBotRunning; s--) {
                    setStatus("2. Đang xem video: " + s + "s...");
                    sleepSafe(1000);
                }
                if (!isBotRunning) break;

                // 3. Tắt quảng cáo (bấm góc trên phải, trên trái, skip)
                setStatus("3. Đang đóng quảng cáo...");
                service.tap(getPxX(95.0f), getPxY(5.0f));
                sleepSafe(400);
                service.tap(getPxX(5.0f), getPxY(5.0f));
                sleepSafe(400);
                service.tap(getPxX(93.0f), getPxY(12.0f));
                sleepSafe(400);
                service.goBack();
                sleepSafe(1500);

                adsWatchedCount++;
                updateStats();

                // 4. Mở đa nhiệm & vuốt tắt game DLS
                setStatus("4. Đang tắt & reset DLS...");
                service.openRecents();
                sleepSafe(1000);
                service.swipe(getPxX(50.0f), getPxY(65.0f), getPxX(50.0f), getPxY(15.0f), 300);
                sleepSafe(800);
                service.goHome();
                sleepSafe(600);

                // 5. Mở lại game DLS từ đầu
                setStatus("5. Đang mở lại DLS...");
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.firsttouchgames.dls7");
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(launchIntent);
                }

                // 6. Chờ game load màn hình chờ và sảnh chính
                for (int w = 7; w > 0 && isBotRunning; w--) {
                    setStatus("6. Game đang load: " + w + "s...");
                    sleepSafe(1000);
                }
                if (!isBotRunning) break;

                // Bấm giữa màn hình để qua màn hình splash/chờ
                service.tap(getPxX(50.0f), getPxY(50.0f));
                sleepSafe(3000);

            } catch (Exception e) {
                e.printStackTrace();
                sleepSafe(1000);
            }
        }
    }

    private void runAutoMatchLoop() {
        int loop = 0;
        setStatus("Trạng thái: Đang tự đá ⚡");
        while (isBotRunning) {
            try {
                loop++;
                AutoAccessibilityService service = AutoAccessibilityService.getInstance();
                if (service == null) break;

                JSONObject config = OnlineUpdateManager.loadConfig(FloatingService.this);
                JSONObject coords = config.optJSONObject("coords");
                long pressB = config.optLong("pressB_duration_ms", 1400);

                // 1. TRONG TRẬN (Ảnh 3): Giữ nút B (Pressing cướp bóng ở 78.5%, 83.0%)
                if (coords != null && coords.has("btn_b")) {
                    JSONArray b = coords.getJSONArray("btn_b");
                    service.press(getPxX((float) b.getDouble(0)), getPxY((float) b.getDouble(1)), pressB);
                }
                sleepSafe(120);

                // 2. TRONG TRẬN (Ảnh 3): Nhấp nút A (Sút / Phá bóng ở 90.5%, 83.0%)
                if (coords != null && coords.has("btn_a")) {
                    JSONArray a = coords.getJSONArray("btn_a");
                    service.tap(getPxX((float) a.getDouble(0)), getPxY((float) a.getDouble(1)));
                }
                sleepSafe(120);

                // 3. Bỏ qua quay chậm / ăn mừng bàn thắng (Chạm giữa màn hình 50%, 50%)
                if (coords != null && coords.has("skip_cutscene")) {
                    JSONArray s = coords.getJSONArray("skip_cutscene");
                    service.tap(getPxX((float) s.getDouble(0)), getPxY((float) s.getDouble(1)));
                }
                sleepSafe(120);

                // 4. NGHỈ GIỮA HIỆP / TẠM DỪNG (Ảnh 4): Bấm nút RESUME màu xanh dương (70.0%, 83.0%) & Nút Tiếp tục (85.0%, 89.0%)
                if (coords != null) {
                    if (coords.has("btn_resume_blue")) {
                        JSONArray res = coords.getJSONArray("btn_resume_blue");
                        service.tap(getPxX((float) res.getDouble(0)), getPxY((float) res.getDouble(1)));
                    }
                    sleepSafe(120);
                    if (coords.has("continue_br")) {
                        JSONArray cbr = coords.getJSONArray("continue_br");
                        service.tap(getPxX((float) cbr.getDouble(0)), getPxY((float) cbr.getDouble(1)));
                    }
                }
                sleepSafe(120);

                // 5. ĐỘI HÌNH RA SÂN (Ảnh 2): Bấm nút PLAY xanh lá góc dưới phải (88.0%, 94.0%)
                if (loop % 2 == 0 && coords != null && coords.has("btn_play_green")) {
                    JSONArray p = coords.getJSONArray("btn_play_green");
                    service.tap(getPxX((float) p.getDouble(0)), getPxY((float) p.getDouble(1)));
                    sleepSafe(150);
                }

                // 6. SẢNH CAREER (Ảnh 1): Bấm card PLAY NOW (25.0%, 22.0%) + Nhận thưởng + Đóng popup
                if (loop % 4 == 0 && coords != null) {
                    if (coords.has("card_play_now")) {
                        JSONArray cpn = coords.getJSONArray("card_play_now");
                        service.tap(getPxX((float) cpn.getDouble(0)), getPxY((float) cpn.getDouble(1)));
                    }
                    sleepSafe(150);
                    if (coords.has("claim_reward")) {
                        JSONArray cr = coords.getJSONArray("claim_reward");
                        service.tap(getPxX((float) cr.getDouble(0)), getPxY((float) cr.getDouble(1)));
                    }
                    sleepSafe(150);
                    if (coords.has("close_ad")) {
                        JSONArray ca = coords.getJSONArray("close_ad");
                        service.tap(getPxX((float) ca.getDouble(0)), getPxY((float) ca.getDouble(1)));
                    }
                }

                sleepSafe(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

