/**
 * ==============================================================================
 * AUTO DLS 2026 - TOOL TỰ ĐÁ & CÀY CUỐC TỰ ĐỘNG THÍCH ỨNG ĐỘ PHÂN GIẢI
 * Phát triển: Antigravity x dovananh
 * Tính năng:
 *  - Tự động co dãn tọa độ theo mọi độ phân giải màn hình (%)
 *  - Cụm phím ảo bóng nổi (Floating Window) có thể kéo thả, thu nhỏ
 *  - Tự động giữ phím B (Pressing cướp bóng liên tục)
 *  - Tự động sút / phá bóng bằng phím A khi có cơ hội
 *  - Tự động bấm qua hiệp 1, hết trận, bỏ qua cutscene, nhận thưởng
 *  - Giữ màn hình luôn sáng trong lúc cày cuốc
 * ==============================================================================
 */

// Yêu cầu quyền trợ năng (Accessibility Service)
auto.waitFor();

// ------------------------------------------------------------------------------
// 1. TÍNH TOÁN TOẠ ĐỘ TỰ THÍCH ỨNG THEO ĐỘ PHÂN GIẢI (%)
// ------------------------------------------------------------------------------
// Đảm bảo game DLS luôn ở chế độ màn hình ngang (Landscape)
var rawW = device.width;
var rawH = device.height;
var SCREEN_WIDTH = Math.max(rawW, rawH);
var SCREEN_HEIGHT = Math.min(rawW, rawH);

// Hàm tính toạ độ dựa trên tỉ lệ % màn hình
function getCoord(percentX, percentY) {
    return {
        x: Math.round(SCREEN_WIDTH * (percentX / 100)),
        y: Math.round(SCREEN_HEIGHT * (percentY / 100))
    };
}

// Bảng toạ độ chuẩn của DLS 2026 (Đo đạc chính xác từ ảnh thực tế)
var POS = {
    // 1. Trong trận đấu (Ảnh 3)
    BTN_B: getCoord(78.5, 83.0),          // Nút B: Pressing cướp bóng
    BTN_A: getCoord(90.5, 83.0),          // Nút A: Sút bóng / Phá bóng
    BTN_C: getCoord(90.5, 58.5),          // Nút C: Chuyền bổng

    // 2. Nghỉ hiệp / Tạm dừng (Ảnh 4)
    BTN_RESUME_BLUE: getCoord(70.0, 83.0),// Nút RESUME màu xanh dương
    CONTINUE_BOTTOM_RIGHT: getCoord(85.0, 89.0), // Nút Tiếp tục

    // 3. Đội hình ra sân (Ảnh 2)
    BTN_PLAY_GREEN: getCoord(88.0, 94.0), // Nút PLAY màu xanh lá

    // 4. Sảnh Career Mode (Ảnh 1)
    CARD_PLAY_NOW: getCoord(25.0, 22.0),  // Thẻ trận đấu PLAY NOW
    SKIP_CUTSCENE: getCoord(50.0, 50.0),  // Chạm giữa màn hình bỏ qua replay/ăn mừng
    CLAIM_REWARD: getCoord(50.0, 75.0),   // Nút Nhận vàng / Kim cương
    CLOSE_POPUP_RIGHT: getCoord(94.5, 7.5)// Nút X góc trên phải
};

// ------------------------------------------------------------------------------
// 2. BIẾN TRẠNG THÁI & ĐIỀU KHIỂN
// ------------------------------------------------------------------------------
var isRunning = false;
var matchCount = 0;
var loopCount = 0;
var workerThread = null;
var startTime = 0;

// ------------------------------------------------------------------------------
// 3. GIAO DIỆN BÓNG NỔI (FLOATING WINDOW)
// ------------------------------------------------------------------------------
var window = floaty.window(
    <frame id="frame" bg="#00000000">
        {/* Khung điều khiển chính */}
        <card id="mainCard" w="210" h="auto" cardCornerRadius="14" cardElevation="10" bg="#E61A1C23" padding="10">
            <vertical>
                {/* Header */}
                <horizontal gravity="center_vertical">
                    <text text="⚽ DLS 2026 AUTO" textColor="#00E676" textSize="14sp" textStyle="bold" layout_weight="1"/>
                    <text id="btnMinimize" text="➖" textColor="#B0BEC5" textSize="14sp" padding="4"/>
                    <text id="btnClose" text="✖" textColor="#FF5252" textSize="14sp" padding="4"/>
                </horizontal>

                {/* Phân cách */}
                <view h="1" bg="#37474F" margin="0 6 0 6"/>

                {/* Thông số màn hình */}
                <text id="tvResolution" text="Màn hình: -- x --" textColor="#90A4AE" textSize="11sp"/>
                <text id="tvStatus" text="Trạng thái: Đang dừng" textColor="#FFD600" textSize="12sp" textStyle="bold" margin="0 2 0 2"/>
                <text id="tvStats" text="Số trận: 0 | Đã chạy: 0p" textColor="#ECEFF1" textSize="11sp"/>

                {/* Nút hành động */}
                <horizontal margin="0 8 0 0" gravity="center">
                    <button id="btnToggle" text="▶ BẮT ĐẦU" w="*" h="38" bg="#00C853" textColor="#FFFFFF" textSize="13sp" textStyle="bold"/>
                </horizontal>
            </vertical>
        </card>

        {/* Nút thu nhỏ tròn khi ẩn menu */}
        <card id="miniCard" w="46" h="46" cardCornerRadius="23" cardElevation="8" bg="#00C853" visibility="gone" gravity="center">
            <text id="miniIcon" text="⚽" textSize="22sp" gravity="center" layout_gravity="center"/>
        </card>
    </frame>
);

// Khởi tạo thông tin màn hình trên giao diện
ui.run(function() {
    window.tvResolution.setText("Màn hình: " + SCREEN_WIDTH + " x " + SCREEN_HEIGHT);
});

// Cho phép kéo thả bong bóng nổi trên màn hình
var initialTouchX, initialTouchY;
var initialWindowX, initialWindowY;
var isMoving = false;

window.frame.setOnTouchListener(function(view, event) {
    switch (event.getAction()) {
        case event.ACTION_DOWN:
            initialTouchX = event.getRawX();
            initialTouchY = event.getRawY();
            initialWindowX = window.getX();
            initialWindowY = window.getY();
            isMoving = false;
            return true;
        case event.ACTION_MOVE:
            var dx = event.getRawX() - initialTouchX;
            var dy = event.getRawY() - initialTouchY;
            if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                isMoving = true;
                window.setPosition(initialWindowX + dx, initialWindowY + dy);
            }
            return true;
        case event.ACTION_UP:
            if (!isMoving) {
                // Nếu bấm vào miniCard thì mở lại bảng to
                if (window.miniCard.getVisibility() === 0) {
                    toggleMinimize(false);
                }
            }
            return true;
    }
    return true;
});

// ------------------------------------------------------------------------------
// 4. SỰ KIỆN NÚT BẤM GIAO DIỆN
// ------------------------------------------------------------------------------
function toggleMinimize(minimize) {
    ui.run(function() {
        if (minimize) {
            window.mainCard.setVisibility(8); // GONE
            window.miniCard.setVisibility(0); // VISIBLE
        } else {
            window.mainCard.setVisibility(0); // VISIBLE
            window.miniCard.setVisibility(8); // GONE
        }
    });
}

window.btnMinimize.click(function() {
    toggleMinimize(true);
});

window.btnClose.click(function() {
    stopBot();
    device.cancelKeepingScreenOn();
    toast("Đã tắt Auto DLS 2026!");
    exit();
});

window.btnToggle.click(function() {
    if (!isRunning) {
        startBot();
    } else {
        stopBot();
    }
});

// ------------------------------------------------------------------------------
// 5. LOGIC CHẠY TỰ ĐỘNG (BOT ENGINE)
// ------------------------------------------------------------------------------
function startBot() {
    isRunning = true;
    startTime = new Date().getTime();
    device.keepScreenOn(); // Giữ sáng màn hình
    
    ui.run(function() {
        window.btnToggle.setText("⏸ TẠM DỪNG");
        window.btnToggle.attr("bg", "#D50000"); // Màu đỏ
        window.tvStatus.setText("Trạng thái: Đang tự đá ⚡");
        window.tvStatus.setTextColor(android.graphics.Color.parseColor("#00E676"));
    });

    toast("Auto DLS 2026: BẮT ĐẦU CHẠY!");

    workerThread = threads.start(function() {
        while (isRunning) {
            try {
                loopCount++;

                // 1. TRONG TRẬN (Ảnh 3): Giữ nút B để pressing cướp bóng
                press(POS.BTN_B.x, POS.BTN_B.y, 1400);
                sleep(100);

                // 2. TRONG TRẬN (Ảnh 3): Nhấp nhẹ phím A để sút/phá bóng lên trên
                click(POS.BTN_A.x, POS.BTN_A.y);
                sleep(120);

                // 3. Bỏ qua quay chậm, phạm lỗi, ăn mừng (Chạm giữa màn hình)
                click(POS.SKIP_CUTSCENE.x, POS.SKIP_CUTSCENE.y);
                sleep(120);

                // 4. NGHỈ GIỮA HIỆP / TẠM DỪNG (Ảnh 4): Bấm nút RESUME xanh dương & Tiếp tục
                click(POS.BTN_RESUME_BLUE.x, POS.BTN_RESUME_BLUE.y);
                sleep(120);
                click(POS.CONTINUE_BOTTOM_RIGHT.x, POS.CONTINUE_BOTTOM_RIGHT.y);
                sleep(120);

                // 5. ĐỘI HÌNH RA SÂN (Ảnh 2): Cứ mỗi 2 vòng, bấm nút PLAY xanh lá
                if (loopCount % 2 === 0) {
                    click(POS.BTN_PLAY_GREEN.x, POS.BTN_PLAY_GREEN.y);
                    sleep(150);
                }

                // 6. SẢNH CAREER (Ảnh 1): Cứ mỗi 4 vòng, bấm card PLAY NOW + Nhận thưởng
                if (loopCount % 4 === 0) {
                    click(POS.CARD_PLAY_NOW.x, POS.CARD_PLAY_NOW.y);
                    sleep(150);
                    click(POS.CLAIM_REWARD.x, POS.CLAIM_REWARD.y);
                    sleep(150);
                    click(POS.CLOSE_POPUP_RIGHT.x, POS.CLOSE_POPUP_RIGHT.y);
                }

                // Cập nhật thống kê thời gian chạy
                if (loopCount % 5 === 0) {
                    var elapsedMinutes = Math.floor((new Date().getTime() - startTime) / 60000);
                    ui.run(function() {
                        window.tvStats.setText("Đã chạy: " + elapsedMinutes + " phút | Vòng: " + loopCount);
                    });
                }

                sleep(200);
            } catch (err) {
                log("Lỗi trong vòng lặp bot: " + err);
                sleep(500);
            }
        }
    });
}

function stopBot() {
    isRunning = false;
    if (workerThread != null && workerThread.isAlive()) {
        workerThread.interrupt();
        workerThread = null;
    }
    device.cancelKeepingScreenOn();

    ui.run(function() {
        window.btnToggle.setText("▶ BẮT ĐẦU");
        window.btnToggle.attr("bg", "#00C853");
        window.tvStatus.setText("Trạng thái: Đã tạm dừng");
        window.tvStatus.setTextColor(android.graphics.Color.parseColor("#FFD600"));
    });

    toast("Auto DLS 2026: ĐÃ TẠM DỪNG!");
}

// Giữ cho luồng ứng dụng luôn hoạt động
setInterval(function() {}, 1000);

