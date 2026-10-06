# Product Requirement Document (PRD): Ravalt

## 1. Overview & Vision
**Ravalt** adalah aplikasi pengelola kata sandi (*password manager*) mandiri dan aman yang mengadopsi arsitektur **Zero-Knowledge Encryption**. Terinspirasi dari sistem keamanan Bitwarden dan 1Password, Ravalt memposisikan server hanya sebagai tempat penyimpanan data terenkripsi (ciphertext). Kunci enkripsi dan plaintext password tidak pernah keluar dari perangkat pengguna.

Salah satu fokus utama Ravalt adalah fitur **Breach Checker** proaktif yang memungkinkan pengguna memeriksa apakah kata sandi yang mereka gunakan telah terkompromi dalam insiden kebocoran data publik di seluruh dunia, dengan menggunakan protokol privasi **k-Anonymity**.

---

## 2. Target Persona & User Stories

### Target Persona
- **Individual Privacy-Conscious User**: Pengguna smartphone yang menginginkan kontrol penuh atas data kredensial mereka, menyadari risiko kebocoran data massal, dan membutuhkan aplikasi yang aman, cepat, dan transparan.

### User Stories (Fase 1 / MVP)
1. **Pendaftaran & Pembuatan Kunci**:
   - Sebagai pengguna baru, saya ingin mendaftar dengan email dan membuat satu Master Password yang kuat sehingga saya hanya perlu mengingat satu kata sandi.
2. **Penyimpanan Kredensial (Vault)**:
   - Sebagai pengguna, saya ingin menyimpan nama akun/aplikasi, username, password, URL website, dan catatan tambahan ke dalam brankas saya secara terenkripsi.
3. **Pencarian & Salin Kredensial**:
   - Sebagai pengguna, saya ingin dengan cepat mencari akun dan menyalin username/password ke clipboard dengan proteksi pembersihan otomatis.
4. **Pemeriksaan Kebocoran Password (Breach Checking)**:
   - Sebagai pengguna, saya ingin langsung mengetahui apakah password yang baru saya masukkan atau simpan pernah bocor di database publik tanpa harus mengunggah password saya ke internet.
5. **Generator Kata Sandi**:
   - Sebagai pengguna, saya ingin membuat password acak yang kuat dengan kombinasi panjang, karakter khusus, dan angka sesuai kebutuhan.

---

## 3. Product Scope & Roadmap

### Fase 1: MVP (Fokus Saat Ini)
- **Zero-Knowledge Auth**: Registrasi, Login, Salt Generation, Key Derivation (Argon2id + HKDF).
- **Brankas Kredensial**: CRUD item login (Title, Username, Password, URL, Notes) dengan enkripsi AES-256-GCM di Android.
- **Sinkronisasi Cloud**: Go Backend + PostgreSQL untuk sync encrypted data antar perangkat.
- **Client-Side Breach Checker**: Integrasi HaveIBeenPwned API dengan k-Anonymity (5-char SHA-1 prefix).
- **Web Landing Page**: Website perkenalan aplikasi dengan HTML + JS + Tailwind CSS.

### Fase 2: Peningkatan Platform & Keamanan
- **Android Biometric Unlock**: Akses cepat dengan sidik jari / face recognition memanfaatkan Android Keystore.
- **Android Autofill Framework**: Pengisian otomatis di browser dan aplikasi Android lain.
- **Two-Factor Authentication (2FA/TOTP)**: Generator kode OTP 6-digit di dalam item vault.
- **Audit Keamanan Brankas**: Ringkasan kesehatan password (reused passwords, weak passwords, breached passwords).

---

## 4. Success Metrics
- **Zero-Knowledge Integrity**: 0 byte plaintext data pengguna yang pernah dikirimkan atau disimpan di server backend.
- **Kecepatan Sinkronisasi**: Sinkronisasi vault memakan waktu < 500ms pada koneksi jaringan standar.
- **Performa Mobile**: Derivasi kunci Argon2id selesai di bawah 1 detik pada perangkat Android modern tanpa lag/ANR pada UI.
