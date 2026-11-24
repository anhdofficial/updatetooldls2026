#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script biên dịch trực tiếp Android Native APK không cần mở Android Studio
Sử dụng Android SDK command-line tools có sẵn trên máy:
 - aapt2
 - javac (Java 17)
 - d8
 - zipalign
 - apksigner
"""

import os
import shutil
import subprocess
import sys
import zipfile

PROJECT_DIR = os.path.abspath("android_project")
BUILD_DIR = os.path.abspath("android_build")
OUTPUT_APK = os.path.abspath("AutoDLS2026.apk")

AAPT2 = r"C:\Android\build-tools\35.0.0\aapt2.exe"
D8 = r"C:\Android\build-tools\35.0.0\d8.bat"
ZIPALIGN = r"C:\Android\build-tools\35.0.0\zipalign.exe"
APKSIGNER = r"C:\Android\build-tools\35.0.0\apksigner.bat"
ANDROID_JAR = r"C:\Android\platforms\android-35\android.jar"
JAVAC = r"C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot\bin\javac.exe"
KEYTOOL = r"C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot\bin\keytool.exe"

def run_cmd(cmd, desc):
    print(f"[*] {desc}...")
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True, encoding="utf-8", errors="ignore")
    if res.returncode != 0:
        print(f"[!] Lỗi tại bước: {desc}")
        print(f"STDOUT: {res.stdout}")
        print(f"STDERR: {res.stderr}")
        sys.exit(1)
    return res.stdout

def main():
    print("=" * 60)
    print(" 🔨 BẮT ĐẦU ĐÓNG GÓI NATIVE APK: AutoDLS2026.apk")
    print("=" * 60)

    # 1. Dọn dẹp thư mục build
    if os.path.exists(BUILD_DIR):
        shutil.rmtree(BUILD_DIR)
    os.makedirs(os.path.join(BUILD_DIR, "compiled_res"), exist_ok=True)
    os.makedirs(os.path.join(BUILD_DIR, "gen"), exist_ok=True)
    os.makedirs(os.path.join(BUILD_DIR, "classes"), exist_ok=True)

    # 2. Biên dịch tài nguyên res với aapt2
    res_dir = os.path.join(PROJECT_DIR, "res")
    compiled_res_dir = os.path.join(BUILD_DIR, "compiled_res")
    run_cmd(f'"{AAPT2}" compile --dir "{res_dir}" -o "{compiled_res_dir}"', "Biên dịch tài nguyên res bằng aapt2")

    # 3. Liên kết tài nguyên và tạo R.java
    manifest_xml = os.path.join(PROJECT_DIR, "AndroidManifest.xml")
    gen_dir = os.path.join(BUILD_DIR, "gen")
    unaligned_apk = os.path.join(BUILD_DIR, "base_unaligned.apk")

    # Lấy danh sách các file .flat
    flat_files = [os.path.join(compiled_res_dir, f) for f in os.listdir(compiled_res_dir) if f.endswith(".flat")]
    flat_args = " ".join([f'"{f}"' for f in flat_files])

    run_cmd(
        f'"{AAPT2}" link -I "{ANDROID_JAR}" --manifest "{manifest_xml}" {flat_args} '
        f'--java "{gen_dir}" -o "{unaligned_apk}" --auto-add-overlay',
        "Liên kết tài nguyên và sinh mã R.java"
    )

    # 4. Tìm tất cả file .java để biên dịch
    java_files = []
    for root, _, files in os.walk(os.path.join(PROJECT_DIR, "src")):
        for f in files:
            if f.endswith(".java"):
                java_files.append(os.path.join(root, f))
    for root, _, files in os.walk(gen_dir):
        for f in files:
            if f.endswith(".java"):
                java_files.append(os.path.join(root, f))

    classes_dir = os.path.join(BUILD_DIR, "classes")
    java_args_file = os.path.join(BUILD_DIR, "sources.txt")
    with open(java_args_file, "w", encoding="utf-8") as f:
        for jf in java_files:
            f.write(f'"{jf.replace(os.sep, "/")}"\n')

    run_cmd(
        f'"{JAVAC}" -encoding UTF-8 -cp "{ANDROID_JAR}" -d "{classes_dir}" "@{java_args_file.replace(os.sep, "/")}"',
        "Biên dịch mã nguồn Java bằng javac"
    )

    # 5. Chuyển đổi mã bytecode sang classes.dex bằng d8
    class_files = []
    for root, _, files in os.walk(classes_dir):
        for f in files:
            if f.endswith(".class"):
                class_files.append(os.path.join(root, f))
    class_args_file = os.path.join(BUILD_DIR, "classes.txt")
    with open(class_args_file, "w", encoding="utf-8") as f:
        for cf in class_files:
            f.write(f'{cf.replace(os.sep, "/")}\n')

    dex_dir = os.path.join(BUILD_DIR, "dex")
    os.makedirs(dex_dir, exist_ok=True)
    run_cmd(
        f'"{D8}" --lib "{ANDROID_JAR}" --output "{dex_dir}" "@{class_args_file.replace(os.sep, "/")}"',
        "Biên dịch mã sang classes.dex bằng d8"
    )

    # 6. Nhúng classes.dex và thư mục assets vào APK
    print("[*] Đóng gói classes.dex và assets vào file APK...")
    classes_dex = os.path.join(dex_dir, "classes.dex")
    assets_dir = os.path.join(PROJECT_DIR, "assets")

    with zipfile.ZipFile(unaligned_apk, "a") as zip_apk:
        zip_apk.write(classes_dex, "classes.dex")
        if os.path.exists(assets_dir):
            for root, _, files in os.walk(assets_dir):
                for f in files:
                    full_path = os.path.join(root, f)
                    rel_path = "assets/" + os.path.relpath(full_path, assets_dir).replace("\\", "/")
                    zip_apk.write(full_path, rel_path)

    # 7. Zipalign tối ưu bộ nhớ
    aligned_apk = os.path.join(BUILD_DIR, "base_aligned.apk")
    run_cmd(f'"{ZIPALIGN}" -f -p 4 "{unaligned_apk}" "{aligned_apk}"', "Tối ưu hóa dung lượng với zipalign")

    # 8. Tạo keystore để ký APK nếu chưa có
    keystore = os.path.join(BUILD_DIR, "debug.keystore")
    if not os.path.exists(keystore):
        run_cmd(
            f'"{KEYTOOL}" -genkeypair -v -keystore "{keystore}" -storepass android -alias androiddebugkey '
            f'-keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"',
            "Tạo khóa chứng thực debug.keystore"
        )

    # 9. Ký APK bằng apksigner
    run_cmd(
        f'"{APKSIGNER}" sign --ks "{keystore}" --ks-pass pass:android --ks-key-alias androiddebugkey '
        f'--key-pass pass:android --out "{OUTPUT_APK}" "{aligned_apk}"',
        "Ký số chứng thực APK bằng apksigner"
    )

    # Xác thực APK
    verify_out = run_cmd(f'"{APKSIGNER}" verify "{OUTPUT_APK}"', "Kiểm tra tính hợp lệ của file APK")
    
    size_mb = os.path.getsize(OUTPUT_APK) / (1024 * 1024)
    print("=" * 60)
    print(f" [✓] THÀNH CÔNG! ĐÃ ĐÓNG GÓI RA FILE APK:")
    print(f" -> Đường dẫn: {OUTPUT_APK}")
    print(f" -> Dung lượng: {size_mb:.2f} MB")
    print("=" * 60)

if __name__ == "__main__":
    main()
