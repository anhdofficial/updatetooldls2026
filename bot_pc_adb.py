#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
==============================================================================
BOT AUTO DLS 2026 - CHẠY TRÊN PC QUA ADB (ĐIỆN THOẠI HOẶC GIẢ LẬP)
Tác giả: dovananh
Tính năng:
  - Tự động nhận diện thiết bị kết nối qua ADB
  - Tự động đo độ phân giải và tính toạ độ theo tỉ lệ %
  - Tự động giữ nút B pressing cướp bóng, sút A phá bóng
  - Tự động bấm tiếp tục qua hiệp 1, hiệp 2, nhận thưởng
  - Tính năng chụp màn hình để test toạ độ
==============================================================================
"""

import subprocess
import time
import sys
import re

ADB_PATH = "adb"

def run_adb(cmd):
    """Thực thi lệnh ADB và trả về kết quả"""
    try:
        full_cmd = f"{ADB_PATH} {cmd}"
        result = subprocess.run(full_cmd, shell=True, capture_output=True, text=True, encoding="utf-8", errors="ignore")
        return result.stdout.strip()
    except Exception as e:
        print(f"[!] Lỗi gọi ADB: {e}")
        return ""

def get_devices():
    """Lấy danh sách thiết bị ADB đang kết nối"""
    out = run_adb("devices")
    lines = out.split("\n")[1:]
    devices = []
    for line in lines:
        parts = line.strip().split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices

def get_screen_size():
    """Lấy kích thước màn hình ngang của thiết bị"""
    out = run_adb("shell wm size")
    # Output mẫu: Physical size: 1080x2400 hoặc Override size: ...
    match = re.search(r"(\d+)x(\d+)", out)
    if match:
        w = int(match.group(1))
        h = int(match.group(2))
        # Chuẩn hóa về chế độ ngang (Landscape) cho DLS
        screen_w = max(w, h)
        screen_h = min(w, h)
        return screen_w, screen_h
    return 1920, 1080

def coord(percent_x, percent_y, screen_w, screen_h):
    """Tính toạ độ pixel theo % màn hình"""
    x = int(screen_w * (percent_x / 100.0))
    y = int(screen_h * (percent_y / 100.0))
    return x, y

def tap(x, y):
    """Chạm vào 1 điểm trên màn hình"""
    run_adb(f"shell input tap {x} {y}")

def long_press(x, y, duration_ms=1400):
    """Giữ nút tại 1 điểm (dùng lệnh swipe tại chỗ)"""
    run_adb(f"shell input swipe {x} {y} {x} {y} {duration_ms}")

def capture_screen(filename="screen_test.png"):
    """Chụp ảnh màn hình lưu vào máy tính"""
    print(f"[*] Đang chụp ảnh màn hình -> {filename} ...")
    subprocess.run(f"{ADB_PATH} exec-out screencap -p > {filename}", shell=True)
    print(f"[✓] Đã lưu ảnh thành công: {filename}")

def auto_play_loop(screen_w, screen_h):
    """Vòng lặp tự động cày trận DLS 2026"""
    # Tính toạ độ các nút
    btn_b = coord(88.5, 84.5, screen_w, screen_h)       # Nút B: Pressing
    btn_a = coord(95.5, 69.0, screen_w, screen_h)       # Nút A: Sút/Phá
    btn_c = coord(82.0, 67.0, screen_w, screen_h)       # Nút C: Chuyền bổng
    skip_cutscene = coord(50.0, 50.0, screen_w, screen_h) # Bỏ qua replay
    continue_br = coord(86.0, 89.0, screen_w, screen_h)   # Tiếp tục góc phải
    continue_c  = coord(50.0, 85.0, screen_w, screen_h)   # Tiếp tục giữa
    play_match  = coord(82.0, 82.0, screen_w, screen_h)   # Vào trận Career
    claim_reward = coord(50.0, 72.0, screen_w, screen_h)  # Nhận thưởng
    close_ad    = coord(94.5, 7.5, screen_w, screen_h)    # Đóng quảng cáo

    print("\n" + "="*55)
    print(" ⚽ BẮT ĐẦU AUTO CÀY CUỐC DLS 2026")
    print(" Nhấn tổ hợp phím [Ctrl + C] bất kỳ lúc nào để DỪNG")
    print("="*55)

    loop_count = 0
    start_time = time.time()

    try:
        while True:
            loop_count += 1
            elapsed_min = int((time.time() - start_time) / 60)
            sys.stdout.write(f"\r[*] Đang chạy... Vòng: {loop_count} | Thời gian: {elapsed_min} phút ")
            sys.stdout.flush()

            # 1. Giữ nút B (Pressing cướp bóng)
            long_press(btn_b[0], btn_b[1], 1300)
            time.sleep(0.1)

            # 2. Nhấp nút A (Sút / Phá bóng)
            tap(btn_a[0], btn_a[1])
            time.sleep(0.15)

            # 3. Chạm giữa màn hình bỏ qua ăn mừng / quay chậm
            tap(skip_cutscene[0], skip_cutscene[1])
            time.sleep(0.15)

            # 4. Định kỳ thử bấm nút Tiếp tục (hết hiệp 1 / hết trận)
            if loop_count % 3 == 0:
                tap(continue_br[0], continue_br[1])
                time.sleep(0.2)
                tap(continue_c[0], continue_c[1])
                time.sleep(0.2)

            # 5. Định kỳ kiểm tra nút vào trận mới hoặc nhận thưởng
            if loop_count % 10 == 0:
                tap(play_match[0], play_match[1])
                time.sleep(0.2)
                tap(claim_reward[0], claim_reward[1])
                time.sleep(0.2)
                tap(close_ad[0], close_ad[1])

            time.sleep(0.2)

    except KeyboardInterrupt:
        print("\n\n[✓] Đã dừng Bot Auto DLS 2026!")

def main():
    print("="*55)
    print("      TOOL AUTO DLS 2026 - ADB CONTROLLER")
    print("="*55)
    
    # Kiểm tra thiết bị
    devices = get_devices()
    if not devices:
        print("[!] Không tìm thấy thiết bị ADB nào kết nối.")
        print("[i] Hãy bật Gỡ lỗi USB trên điện thoại hoặc mở Giả lập (LDPlayer/BlueStacks).")
        input("\nNhấn Enter để thoát...")
        return

    device_id = devices[0]
    print(f"[✓] Đã kết nối thiết bị: {device_id}")

    # Lấy kích thước màn hình
    w, h = get_screen_size()
    print(f"[✓] Độ phân giải game (Landscape): {w} x {h} pixel")

    while True:
        print("\n---------------- MENU CHỨC NĂNG ----------------")
        print(" [1] Bắt đầu Auto Cày Trận DLS 2026")
        print(" [2] Chụp ảnh màn hình điện thoại (Lưu screen_test.png)")
        print(" [3] Thử nghiệm chạm nút B (Pressing)")
        print(" [4] Thoát")
        print("------------------------------------------------")
        choice = input("Nhập lựa chọn của bạn (1-4): ").strip()

        if choice == "1":
            auto_play_loop(w, h)
        elif choice == "2":
            capture_screen()
        elif choice == "3":
            bx, by = coord(88.5, 84.5, w, h)
            print(f"[*] Thử giữ nút B tại tọa độ ({bx}, {by})...")
            long_press(bx, by, 1500)
            print("[✓] Hoàn thành chạm thử!")
        elif choice == "4":
            print("Tạm biệt!")
            break
        else:
            print("[!] Lựa chọn không hợp lệ.")

if __name__ == "__main__":
    main()

