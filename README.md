# Ravalt - Zero-Knowledge Password Manager with Breach Checker

Ravalt adalah aplikasi pengelola kata sandi (*password manager*) berstandar keamanan tinggi dengan arsitektur **Zero-Knowledge Encryption** dan deteksi kebocoran kata sandi (**Breach Checker**) berbasis **k-Anonymity**.

---

## 🚀 Tech Stack

- **Backend**: Golang, PostgreSQL, Docker
- **Mobile Client**: Native Android (Kotlin, Jetpack Compose, Coroutines, Room/SQLCipher)
- **Web Landing Page**: HTML5, Vanilla JavaScript, Tailwind CSS

---

## 📁 Struktur Monorepo

- [`/docs`](docs/): Dokumen perencanaan, PRD, spesifikasi teknis dan kriptografi.
  - [`PRD.md`](docs/PRD.md): Product Requirement Document & Roadmap.
  - [`REQUIREMENTS.md`](docs/REQUIREMENTS.md): Kebutuhan Fungsional, Non-Fungsional, dan Kriptografi.
  - [`PROJECT_STRUCTURE.md`](docs/PROJECT_STRUCTURE.md): Rincian struktur direktori monorepo.
- [`/backend`](backend/): Layanan REST API Golang & schema database PostgreSQL.
- [`/android`](android/): Aplikasi native Android Kotlin.
- [`/web`](web/): Landing page statis untuk perkenalan produk & unduhan.

---

## 🔒 Prinsip Keamanan Utama (Zero-Knowledge)

1. **Master Password Tidak Pernah Terkirim**: Klien menderivasi `MasterKey`, `AuthKey`, dan `VaultKey` menggunakan algoritma Argon2id dan HKDF-SHA256 langsung di perangkat Android.
2. **Server Hanya Menyimpan Ciphertext**: Server backend hanya menerima data yang telah terenkripsi menggunakan AES-256-GCM.
3. **Pemeriksaan Kebocoran Tanpa Bocor (k-Anonymity)**: Klien memeriksa kebocoran kata sandi dengan hanya mengirimkan 5 karakter pertama dari SHA-1 hash ke HaveIBeenPwned API. Server luar maupun server Ravalt tidak pernah mengetahui kata sandi asli pengguna.
