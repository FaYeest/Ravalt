# Struktur Proyek Monorepo: Ravalt

Proyek **Ravalt** diatur sebagai monorepo yang memisahkan antara backend service (Go), aplikasi mobile (Kotlin Android), dan landing page web (Tailwind CSS + JS).

## Pohon Direktori

```
Ravalt/
├── README.md                          # Dokumentasi utama proyek
├── docs/                              # Dokumen perancangan, arsitektur, dan spesifikasi
│   ├── PRD.md                         # Product Requirement Document
│   ├── REQUIREMENTS.md                # Spesifikasi fungsional, non-fungsional, & crypto
│   └── PROJECT_STRUCTURE.md           # Rincian struktur folder dan arsitektur kode
│
├── backend/                           # Service API Backend (Golang)
│   ├── cmd/
│   │   └── server/
│   │       └── main.go                # Inisialisasi HTTP server & database connection
│   ├── internal/
│   │   ├── config/                    # Config loader (ENV / .env)
│   │   ├── domain/                    # Model entitas (User, VaultItem) & Interface
│   │   ├── handler/                   # HTTP Controller/Handler (Gin / Chi / Fiber)
│   │   ├── service/                   # Business logic (Auth, Vault Sync)
│   │   ├── middleware/                # JWT Auth, Logger, CORS, Rate Limiter
│   │   └── repository/                # Implementasi query database PostgreSQL
│   ├── migrations/                    # SQL migration schema (001_create_tables.sql)
│   ├── docker-compose.yml             # Local deployment (PostgreSQL + Backend)
│   ├── Dockerfile                     # Multi-stage Docker build untuk Go server
│   └── go.mod                         # Go module definition
│
├── android/                           # Aplikasi Mobile Android (Kotlin Native)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/ravalt/app/
│   │   │   │   ├── core/              # Utility, extensions, base classes
│   │   │   │   ├── crypto/            # Argon2id, AES-256-GCM, HKDF, Keystore helper
│   │   │   │   ├── data/              # Retrofit API, Room Database, Repositories
│   │   │   │   ├── domain/            # Use cases (LoginUseCase, CheckBreachUseCase, etc.)
│   │   │   │   └── ui/                # Jetpack Compose Screens, ViewModels, Theme
│   │   │   ├── res/                   # Drawables, strings, colors, values
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
└── web/                               # Landing Page Web
    ├── src/
    │   ├── index.html                 # Halaman utama landing page
    │   ├── js/
    │   │   └── main.js                # Interaktivitas UI & animasi sederhana
    │   └── css/
    │       └── styles.css             # Tailwind source stylesheet
    ├── package.json                   # Build runner Tailwind CSS
    └── tailwind.config.js             # Konfigurasi Tailwind theme
```

## Pembagian Peran Komponen

### 1. `backend/` (Golang & PostgreSQL)
- **Tanggung Jawab**:
  - Menyediakan REST API untuk pre-login (salt retrieval), registrasi, login pengguna, dan sinkronisasi vault.
  - Memastikan token JWT valid.
  - Menyimpan ciphertext dan metadata di PostgreSQL.
- **Batasan**:
  - Backend sama sekali tidak memiliki logika untuk mendekripsi data vault.
  - Backend tidak mengetahui master password.

### 2. `android/` (Kotlin)
- **Tanggung Jawab**:
  - Derivasi kunci aman menggunakan Argon2id dan HKDF.
  - Enkripsi lokal item sebelum dikirim ke backend dan dekripsi data setelah diunduh.
  - Penyimpanan lokal terenkripsi (Offline-first dengan Room / SQLCipher).
  - Pengecekan password breach langsung ke HaveIBeenPwned API menggunakan k-Anonymity (5 karakter pertama SHA-1).

### 3. `web/` (HTML, JS, Tailwind CSS)
- **Tanggung Jawab**:
  - Menampilkan branding produk, value proposition (Zero-Knowledge & Breach Detection), fitur utama, dan tautan unduhan aplikasi Android (.apk).
