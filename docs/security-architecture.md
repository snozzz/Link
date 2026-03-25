# Security Architecture

This app should not invent a custom cryptographic algorithm. The secure design is a composition of standard, reviewed primitives.

## Test invite flow

1. The server generates a one-time invite token with a short TTL.
2. Only a salted password hash of the token is stored server-side.
3. The Android client submits the invite token over TLS 1.3.
4. After validation, the server binds the device public key and returns a short-lived session token.

## Device identity

1. The app creates a device key pair on first unlock.
2. The private key remains inside Android Keystore.
3. The server stores only the public key and metadata needed for revocation.

## Recommended primitives

- Transport: TLS 1.3
- Invite token storage: Argon2id or scrypt on the server
- Device key agreement: X25519
- Key derivation: HKDF-SHA-256
- Authenticated encryption: AES-256-GCM or ChaCha20-Poly1305
- Token signing: Ed25519 or P-256 ECDSA, depending on backend stack

## Message security roadmap

1. Phase 1: encrypted transport plus server-side encrypted storage.
2. Phase 2: pairwise end-to-end encrypted messages using X25519 sessions.
3. Phase 3: per-message ratcheting if we decide the extra complexity is justified.

## Notes

- Usage stats collection must always require explicit user consent.
- Invite tokens should be revocable.
- A single invite should have device count limits.
- Session secrets should never be stored in plaintext on device.
