# Requirements & Technical Specifications: Ravalt

Dokumen ini mendefinisikan kebutuhan fungsional, non-fungsional, spesifikasi kriptografi, dan kontrak API untuk **Ravalt Password Manager**.

---

## 1. Functional Requirements (FR)

### 1.1 Autentikasi & Akun
- **FR-AUTH-01 (Pre-login / Salt Retrieval)**: Sistem menyediakan endpoint untuk mengambil `UserSalt` berdasarkan email pengguna tanpa membocorkan status apakah email tersebut terdaftar atau tidak (jika tidak terdaftar, kembalikan fake deterministik salt untuk mencegah email enumeration).
- **FR-AUTH-02 (Zero-Knowledge Key Derivation)**: Klien Android menderivasi:
  - `MasterKey = Argon2id(MasterPassword, UserSalt, Memory=64MB, Iterations=3, Parallelism=4)`
  - `AuthKey = HKDF-SHA256(MasterKey, salt=UserSalt, info="ravalt-auth")`
  - `VaultKey = HKDF-SHA256(MasterKey, salt=UserSalt, info="ravalt-vault")`
- **FR-AUTH-03 (Registrasi)**: Klien mengirimkan `email`, `auth_key`, dan `user_salt`. Server menyimpan `email`, `user_salt`, dan `hash(auth_key)` menggunakan bcrypt/Argon2id.
- **FR-AUTH-04 (Login)**: Klien mengirimkan `email` dan `auth_key`. Server memverifikasi kecocokan hash `auth_key` dan menerbitkan JWT Access Token & Refresh Token.

### 1.2 Brankas Kata Sandi (Vault)
- **FR-VAULT-01 (Enkripsi Sisi Klien)**: Seluruh atribut item (judul, username, password, url, catatan) dikemas dalam format JSON, lalu dienkripsi menggunakan `AES-256-GCM` dengan 96-bit random nonce menggunakan `VaultKey`.
- **FR-VAULT-02 (Format Penyimpanan Server)**: Server hanya menerima payload terenkripsi:
  ```json
  {
    "id": "uuid-v4",
    "encrypted_data": "base64(ciphertext + tag)",
    "nonce": "base64(96_bit_nonce)",
    "version": 1
  }
  ```
- **FR-VAULT-03 (Sinkronisasi Data)**: Klien dapat melakukan pull untuk mengambil seluruh daftar item terbaru atau item yang berubah sejak `last_sync_timestamp`.
- **FR-VAULT-04 (Hapus Item)**: Mendukung soft-delete / hard-delete item dari brankas.

### 1.3 Deteksi Kebocoran Password (Breach Checking)
- **FR-BREACH-01 (k-Anonymity SHA-1)**: Klien menghitung hash `SHA-1(plain_password)`.
- **FR-BREACH-02 (Query Prefix)**: Klien memotong 5 karakter pertama dari hash SHA-1 (contoh: `21BD1`) dan memanggil `GET https://api.pwnedpasswords.com/range/21BD1`.
- **FR-BREACH-03 (Client-side Verification)**: Klien menerima daftar hash suffix (35 karakter) beserta jumlah kemunculan breach. Klien mencocokkan secara lokal. Jika ditemukan, sistem menampilkan peringatan *"Password ini telah muncul X kali dalam kebocoran data"*.
- **FR-BREACH-04 (Privasi)**: Server backend Ravalt tidak pernah dilibatkan dalam proses pengecekan ini, dan HIBP API tidak pernah menerima password asli maupun hash lengkap.

### 1.4 Two-Factor Authenticator (TOTP)
- **FR-TOTP-01 (RFC 6238 Engine)**: Klien mengimplementasikan generator kode OTP 6-digit dengan interval rotasi 30 detik berbasis algoritma HMAC-SHA1/SHA256.
- **FR-TOTP-02 (QR Scanner)**: Klien mendukung pemindaian barcode URI format `otpauth://totp/{label}?secret={secret}&issuer={issuer}` menggunakan kamera perangkat.
- **FR-TOTP-03 (Manual Secret Key)**: Klien mendukung input manual secret key format Base32 dengan penanganan padding standar.
- **FR-TOTP-04 (Zero-Knowledge Storage)**: Seed/secret key 2FA disimpan dienkripsi secara lokal di dalam payload `encrypted_data` brankas menggunakan `VaultKey`.

### 1.5 Server & SSH Keys Management
- **FR-SSH-01 (Keypair Generation)**: Klien mendukung pembuatan pasangan kunci kriptografi SSH secara lokal langsung di perangkat:
  - `Ed25519` (Curve25519, 256-bit, format OpenSSH)
  - `RSA-4096` (PKCS#8 / OpenSSH format)
- **FR-SSH-02 (1-Click Deployment)**: Klien menyediakan tombol cepat untuk menyalin Public Key (format baris tunggal `ssh-ed25519 ... user@host`) untuk file `~/.ssh/authorized_keys`.
- **FR-SSH-03 (Terminal Command Helper)**: Klien memformat dan memungkinkan 1-klik salin perintah koneksi terminal (contoh: `ssh {user}@{host} -p {port}`).
- **FR-SSH-04 (Zero-Knowledge Private Key)**: Private key disimpan dalam ciphertext AES-256-GCM. Dekripsi hanya dilakukan secara on-demand di memori RAM ketika pengguna melakukan otentikasi.

---

## 2. Non-Functional Requirements (NFR)

- **NFR-SEC-01 (Zero-Knowledge)**: Backend dan database tidak pernah memiliki akses ke `MasterPassword` atau `VaultKey`. Kunci tidak pernah dikirimkan melalui jaringan.
- **NFR-SEC-02 (Transport Security)**: Semua komunikasi antara Android, Backend, dan Third-party API wajib melalui HTTPS/TLS 1.3.
- **NFR-PERF-01 (Responsivitas UI)**: Komputasi berat KDF (Argon2id) dan enkripsi/dekripsi AES wajib dijalankan di thread background (Kotlin Coroutines `Dispatchers.Default`) untuk mencegah freeze pada tampilan mobile.
- **NFR-PERF-02 (Latency API)**: Waktu pemrosesan API Go Backend untuk operasi CRUD dan sync vault rata-rata < 50ms.
- **NFR-RELIABILITY-01 (Offline First)**: Pengguna tetap dapat melihat dan menyalin data vault yang telah diunduh meskipun perangkat tidak memiliki koneksi internet.

---

## 3. Database Schema (PostgreSQL)

```sql
-- Tabel Pengguna
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL UNIQUE,       -- Batas RFC 5321 (max path 254 octet)
    user_salt VARCHAR(44) NOT NULL,           -- Base64 dari 32-byte CSPRNG salt
    auth_hash VARCHAR(60) NOT NULL,           -- Tepat 60 karakter format standard Bcrypt ($2b$...)
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabel Item Brankas (Zero-Knowledge Encrypted)
CREATE TABLE vault_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    encrypted_data TEXT NOT NULL,                -- Base64 payload terenkripsi AES-256-GCM
    nonce VARCHAR(16) NOT NULL,                  -- Tepat 16 karakter (12-byte / 96-bit AES-GCM nonce Base64)
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_vault_items_user_id ON vault_items(user_id);
CREATE INDEX idx_vault_items_updated_at ON vault_items(updated_at);
```

---

## 4. API Endpoints Specification

### Auth Endpoints
- `GET /api/v1/auth/prelogin?email={email}`
  - Respons: `{ "salt": "..." }`
- `POST /api/v1/auth/register`
  - Body: `{ "email": "...", "auth_hash": "...", "salt": "..." }`
  - Respons: `{ "user_id": "...", "token": "..." }`
- `POST /api/v1/auth/login`
  - Body: `{ "email": "...", "auth_hash": "..." }`
  - Respons: `{ "token": "...", "refresh_token": "..." }`

### Vault Endpoints (Protected by Bearer JWT)
- `GET /api/v1/vault/items` (Mendukung query `?since={timestamp}`)
  - Respons: `{ "items": [ { "id": "...", "encrypted_data": "...", "nonce": "...", "updated_at": "..." } ] }`
- `POST /api/v1/vault/items`
  - Body: `{ "encrypted_data": "...", "nonce": "..." }`
  - Respons: `{ "id": "...", "created_at": "..." }`
- `PUT /api/v1/vault/items/{id}`
  - Body: `{ "encrypted_data": "...", "nonce": "...", "version": 2 }`
- `DELETE /api/v1/vault/items/{id}`
  - Respons: `{ "message": "item deleted" }`
