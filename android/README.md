# Android Client (Ravalt)

Aplikasi mobile native Android untuk Ravalt Password Manager ditulis menggunakan **Kotlin** dan **Jetpack Compose**.

## Tanggung Jawab Utama Klien
1. **Zero-Knowledge Key Derivation**: Menggunakan Argon2id dan HKDF-SHA256 untuk menderivasi `MasterKey`, `AuthKey`, dan `VaultKey`.
2. **Kriptografi Lokal**: Enkripsi dan dekripsi payload data brankas menggunakan AES-256-GCM sebelum dikirim ke backend Go.
3. **Penyimpanan Lokal Terenkripsi**: Menggunakan Room / SQLCipher untuk menyimpan brankas secara offline-first.
4. **Breach Checker (k-Anonymity)**: Memeriksa SHA-1 prefix (5 karakter pertama) ke HaveIBeenPwned API tanpa membocorkan kata sandi asli.

## Rencana Struktur Paket
```
com.ravalt.app/
├── core/
│   ├── crypto/            # Argon2id, AES-GCM, HKDF, Keystore
│   └── network/           # Retrofit & OkHttp client
├── data/
│   ├── local/             # Room Database & DAO
│   ├── remote/            # API Endpoints (Auth, Vault)
│   └── repository/        # Implementasi Repository
├── domain/
│   ├── model/             # Domain Model (VaultItem, User)
│   └── usecase/           # Business logic & Use cases
└── ui/
    ├── auth/              # Screen Login & Register Master Password
    ├── vault/             # Screen Daftar & Detail Brankas
    ├── breach/            # Screen Hasil Audit Kebocoran
    └── theme/             # Material 3 Compose Theme
```
