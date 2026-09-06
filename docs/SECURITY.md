# Security

## Privacy

- Face **embeddings** and optional small thumbnails are stored **only on the tablet** (Room / encrypted prefs).
- Punch JSON to ERP contains **no** face bitmap or base64 — only `device_id`, `person_id`, `time`, `punch_type`, `similarity`.
- Inform staff that templates are for attendance on this device only.
- On tablet wipe/replace: re-enroll staff (or restore a controlled encrypted backup if you add one later).

## Admin PIN

- Required for Settings, Enroll, People delete, and exiting kiosk mode.
- Stored as SHA-256 hash in EncryptedSharedPreferences (not plaintext).
- Operator chooses the PIN on first Settings open.

## Anti-spoof and match

- MiniFASNet liveness gate before identify (when model is present).
- Cosine similarity threshold default **0.80** (not exposed in simple Settings UI).
- Per-person punch cooldown default **60s**.
- Outbox dedupe by person + minute.

## Transport

- Network security config permits cleartext for office LAN HTTP ERP.
- Prefer HTTPS when the ERP is TLS-terminated.
- Never expose the punch bridge to the public internet without reverse-proxy + CIDR controls.

## ERP controls

- Active device registration (Alias / Terminal SN).
- `ATTENDANCE_KBYAI_ALLOWED_CIDRS` allowlist on the punch bridge.

## Supply chain

- Vendored `.tflite` models only — document URL, license, SHA-256 in MODELS.md.
- Do not download models from arbitrary URLs at runtime.
- Keystore files are never committed; CI uses GitHub Secrets.
