# Ravalt

[![Go](https://img.shields.io/badge/Go-1.22+-00ADD8?style=flat-square&logo=go&logoColor=white)](https://golang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android_Native-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Docker](https://img.shields.io/badge/Docker-Supported-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=flat-square&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)

Ravalt adalah sistem pengelola kata sandi (*password manager*) berarsitektur **Zero-Knowledge Encryption** yang dilengkapi fitur audit kebocoran kata sandi (**Breach Checker**) berbasis **k-Anonymity**.

---

## Tech Stack

| Komponen | Teknologi | Keterangan |
|---|---|---|
| **Backend API** | Golang (Chi v5, pgx/v5) | Layanan REST API stateless performa tinggi |
| **Database** | PostgreSQL 16 | Relational storage untuk ciphertext & auth hash |
| **Mobile Client** | Kotlin Native (Android) | Jetpack Compose, Room/SQLCipher, Argon2id, AES-256-GCM |
| **Web Presentation** | HTML5, Vanilla JS, Tailwind CSS | Landing page statis dan link distribusi aplikasi |
| **Containerization** | Docker, Docker Compose | Orkestrasi database lokal & deployment service |

---

## Struktur Monorepo

```
Ravalt/
├── docs/                 # Dokumentasi spesifikasi arsitektur & PRD
│   ├── PRD.md            # Product Requirement Document
│   ├── REQUIREMENTS.md   # Spesifikasi teknis, kriptografi, dan skema database
│   └── PROJECT_STRUCTURE.md
├── backend/              # Layanan backend API Golang & migrasi database
├── android/              # Aplikasi mobile native Android Kotlin
└── web/                  # Web landing page statis
```

---

## Prinsip Keamanan & Kriptografi

1. **Zero-Knowledge Architecture**:
   - Master Password tidak pernah ditransmisikan ke jaringan atau disimpan di server.
   - Kunci enkripsi (`VaultKey`) dan kunci autentikasi (`AuthKey`) diderivasi secara lokal di perangkat klien menggunakan **Argon2id** dan **HKDF-SHA256**.
2. **End-to-End Vault Encryption**:
   - Data kredensial dienkripsi di perangkat klien menggunakan **AES-256-GCM** dengan 96-bit random nonce sebelum dikirim ke backend.
   - Database server hanya menyimpan payload terenkripsi (*ciphertext*).
3. **Breach Checking Berbasis k-Anonymity**:
   - Klien menghitung hash `SHA-1` dari kata sandi, lalu hanya mengirimkan 5 karakter pertama (*prefix*) ke HaveIBeenPwned API.
   - Pencocokan sisa 35 karakter hash dilakukan secara lokal di perangkat. Baik server Ravalt maupun server eksternal tidak pernah mengetahui kata sandi pengguna.

---

## Memulai Pengembangan (Quick Start)

### 1. Menjalankan Database PostgreSQL
Pastikan Docker telah berjalan di sistem Anda:
```bash
cd backend
docker compose up -d
```

### 2. Menjalankan Backend Service
Pastikan environment file `.env` telah disiapkan:
```bash
cd backend
go run ./cmd/server
```
Periksa status server melalui endpoint healthcheck:
```bash
curl http://localhost:8080/healthz
```

---

## Lisensi
Proyek ini didistribusikan di bawah lisensi MIT.
