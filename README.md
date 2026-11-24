# ⚽ AUTO DLS 2026 - TOOL TỰ ĐÁ & CÀY CUỐC TỰ ĐỘNG

Bộ công cụ tự động chơi và cày cuốc vàng / kim cương cho tựa game **Dream League Soccer (DLS 2026)**.

---

## 🚀 ĐÃ ĐÓNG GÓI SẴN FILE APK CÀI ĐẶT TRỰC TIẾP
File APK chính thức đã được biên dịch và ký số hoàn chỉnh ngay tại thư mục này:
- 📱 **File cài đặt:** [AutoDLS2026.apk](file:///e:/dovananh/Code_DVA/toolTuDaDLS2026/AutoDLS2026.apk)
- **Hỗ trợ:** Android 7.0 đến Android 15+ (Mọi dòng máy Samsung, Xiaomi, Oppo, Realme, Vivo, Asus...)
- **Không cần Root máy.**

### Cách cài đặt vào điện thoại:
1. Copy file `AutoDLS2026.apk` sang điện thoại (qua Zalo, Telegram, Google Drive hoặc cắm cáp USB).
2. Trên điện thoại, bấm vào file `AutoDLS2026.apk` và chọn **Cài đặt**.
3. Mở app **Auto DLS 2026** lên:
   - Bấm **CẤP QUYỀN** cho `Quyền Trợ năng` (Accessibility Service).
   - Bấm **CẤP QUYỀN** cho `Quyền Cửa sổ nổi` (Overlay Permission).
4. Bấm nút **⚽ MỞ BÓNG NỔI AUTO DLS** $\rightarrow$ Vào game DLS 2026 và bấm **▶ BẮT ĐẦU**!

---

## 🌐 TÍNH NĂNG UPDATE ONLINE (HOT-UPDATE - KHÔNG CẦN CÀI LẠI APP)

Đây là tính năng độc quyền giúp bạn **không bao giờ phải cài đi cài lại app** khi game DLS có cập nhật hoặc cần chỉnh lại toạ độ:
1. Khi có bản cập nhật DLS mới hoặc muốn tinh chỉnh thời gian giữ nút B, toạ độ nút:
   - Bạn chỉ cần sửa file [online_script.json](file:///e:/dovananh/Code_DVA/toolTuDaDLS2026/online_script.json) và đưa lên GitHub / Server.
2. Trên điện thoại:
   - Bấm nút **🔄 Update Online** ngay trên bóng nổi hoặc trong app.
   - App sẽ tự động tải cấu hình và toạ độ mới nhất về máy và áp dụng ngay lập tức!

---

## 🛠 NẾU BẠN MUỐN TỰ BUILD LẠI FILE APK:
Chỉ cần chạy dòng lệnh sau trên máy tính:
```powershell
python build_apk.py
```
Hệ thống sẽ tự động biên dịch toàn bộ mã nguồn Java Native và sinh ra file `AutoDLS2026.apk` mới chỉ trong 3 giây!

---

## 🌟 ĐẶC ĐIỂM NỔI BẬT CỦA TOOL
- **Tự thích ứng mọi độ phân giải (%):** Tự đo kích thước màn hình ngang khi khởi động, tính toạ độ theo tỉ lệ chuẩn.
- **Cơ chế cày cuốc trong trận:**
  - Giữ nút **B** liên tục để pressing cướp bóng.
  - Nhấp nút **A** sút/phá bóng lên trên.
  - Tự động bỏ qua replay, cutscene ăn mừng.
  - Tự động bấm **Tiếp tục** khi hết hiệp 1, hết trận và tự động nhận thưởng.
- **Bong bóng nổi tiện lợi:** Có thể kéo thả, thu nhỏ thành quả bóng tròn để không che màn hình.

---

## 📱 CÁCH 1: XUẤT RA FILE APK VÀ CÀI ĐẶT TRÊN ĐIỆN THOẠI (KHUYÊN DÙNG)

Bạn có thể tạo ra file `.apk` cài đặt trực tiếp trên điện thoại mà **không cần cài đặt Android Studio hay máy tính cấu hình mạnh**.

### Bước 1: Cài đặt ứng dụng AutoX.js lên điện thoại
1. Tải file APK của ứng dụng **AutoX.js** (Bản mới nhất v6.x) về điện thoại:
   - [Tải AutoX.js từ GitHub Releases](https://github.com/kkevsekk1/AutoX/releases) (Tải file có đuôi `.apk`, ví dụ: `autox-v6.x.x-universal-release.apk`).
2. Cài đặt file APK vừa tải lên điện thoại Android của bạn.

### Bước 2: Đưa mã nguồn vào AutoX.js
Có 2 cách rất đơn giản:
* **Cách A (Nhanh nhất - Copy nội dung):**
  1. Mở ứng dụng **AutoX.js** trên điện thoại.
  2. Bấm vào dấu `+` ở góc dưới $\rightarrow$ Chọn **Tạo tệp mới** $\rightarrow$ Đặt tên là `main.js`.
  3. Mở file [main.js](file:///e:/dovananh/Code_DVA/toolTuDaDLS2026/main.js) trên máy tính, copy toàn bộ nội dung và dán vào điện thoại $\rightarrow$ Bấm nút **Lưu (Save)**.
* **Cách B (Chép cả thư mục):**
  - Chép toàn bộ thư mục `toolTuDaDLS2026` (gồm `project.json` và `main.js`) vào bộ nhớ điện thoại (vào thư mục `/sdcard/Scripts/`).

### Bước 3: Cấp quyền hoạt động
Mở AutoX.js và bật 2 quyền cơ bản:
1. **Dịch vụ Trợ năng (Accessibility Service):** Cho phép tool tự động bấm và giữ nút.
2. **Hiển thị trên ứng dụng khác (Floating Window):** Cho phép hiện bóng nổi điều khiển.

### Bước 4: Xuất thành file APK độc lập (1 cú nhấp chuột)
1. Trong danh sách của AutoX.js, nhấn giữ vào file hoặc project `Auto DLS 2026`.
2. Chọn mục **"Đóng gói thành APK" (Build APK / 打包应用)**.
3. Nhập tên ứng dụng bạn muốn: `Auto DLS 2026` $\rightarrow$ Nhấn **Đóng gói (Build)**.
4. Quá trình đóng gói sẽ tạo ra file **`Auto_DLS_2026.apk`** ngay trên điện thoại!
5. Bạn bấm cài đặt file APK này là xong. Từ nay bạn có thể mở thẳng app này mà không cần mở AutoX.js nữa.

---

## 🎮 HƯỚNG DẪN SỬ DỤNG KHI VÀO GAME DLS 2026

1. Mở ứng dụng **Auto DLS 2026** vừa cài đặt.
2. Màn hình sẽ xuất hiện một **Bong bóng nổi màu xanh lá** có chữ `⚽ DLS 2026 AUTO`.
3. Mở game **Dream League Soccer 2026**.
4. Vào sảnh **Career Mode** hoặc bắt đầu trận đấu.
5. Trên bong bóng nổi, bấm nút **▶ BẮT ĐẦU**:
   - Tool sẽ tự động lấy kích thước màn hình máy bạn.
   - Tự động thực hiện chuỗi thao tác: Giữ B cướp bóng $\rightarrow$ Sút A $\rightarrow$ Qua hiệp $\rightarrow$ Nhận thưởng.
6. Bấm nút `➖` để thu nhỏ menu thành 1 icon quả bóng tròn nhỏ ở góc màn hình.
7. Muốn dừng lại, chỉ cần bấm vào bóng nổi và chọn **⏸ TẠM DỪNG** hoặc **✖** để tắt.

---

## 💻 CÁCH 2: CHẠY TRỰC TIẾP TỪ MÁY TÍNH QUA ADB (DÙNG ĐIỆN THOẠI HOẶC GIẢ LẬP)

Nếu bạn chơi DLS trên giả lập (như **LDPlayer**, **BlueStacks**, **NoxPlayer**) hoặc cắm cáp điện thoại vào PC:

1. Mở cửa sổ dòng lệnh (Terminal / PowerShell / CMD) tại thư mục này:
   ```powershell
   cd e:\dovananh\Code_DVA\toolTuDaDLS2026
   python bot_pc_adb.py
   ```
2. Menu điều khiển sẽ hiện ra:
   ```text
   ---------------- MENU CHỨC NĂNG ----------------
    [1] Bắt đầu Auto Cày Trận DLS 2026
    [2] Chụp ảnh màn hình điện thoại (Lưu screen_test.png)
    [3] Thử nghiệm chạm nút B (Pressing)
    [4] Thoát
   ------------------------------------------------
   ```
3. Nhập `1` và nhấn Enter để bắt đầu auto. Nhấn `Ctrl + C` để dừng lại bất kỳ lúc nào.

---

## 💡 MẸO CÀY VÀNG & KIM CƯƠNG HIỆU QUẢ NHẤT TRONG DLS

- **Nâng cấp Cơ sở Thương mại (Commercial Facility):** Giúp tăng phần trăm số vàng nhận thêm sau mỗi trận đấu lên tới +50% - +100%.
- **Nâng cấp Sân vận động (Stadium Capacity):** Sân vận động càng lớn thì mỗi trận đá sân nhà (Home Match) trong Career Mode sẽ nhận được càng nhiều vàng (tối đa ~100-140 vàng/trận).
- **Cấu hình đội hình:** Hãy chọn sơ đồ có hàng thủ đông người (như `4-4-2` hoặc `5-3-2`) để khi bot giữ nút B áp sát, các cầu thủ luôn ở cự ly gần để bọc lót và cướp bóng dễ dàng hơn.

