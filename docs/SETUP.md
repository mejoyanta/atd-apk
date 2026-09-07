# Setup — Face Attendance Kiosk

## Prerequisites

- Android tablet (front camera preferred), Android 8.0+ (API 26)
- Tablet and ERP on the same office LAN (or routed)
- ERP: `ATTENDANCE_KBYAI_ENABLED=true`, device row Active (vendor `other`)
- Employee `attendance_code` values ready for enroll

## Download APK from GitHub Actions

1. Push to `main` (or create tag `v1.0.0`) or run **Actions → Android APK → Run workflow**
2. Open the successful run → **Artifacts**
3. Download the artifact (GitHub always gives a **ZIP**, even if the name ends in `.apk`)
4. On the phone/PC: **unzip / extract** the ZIP first — do **not** rename the ZIP to `.apk`
5. Open the extracted `facekiosk-debug.apk` (or `facekiosk-v*.apk`)
6. Allow install from unknown sources / Files, then Install
7. Confirm the app appears as **Face Attendance** under Settings → Apps

If Settings → Apps has no **Face Attendance**, install did not succeed — extract the real `.apk` and try again (or copy `facekiosk-debug.apk` from a PC build via USB/Drive).

You do **not** need Android Studio on the tablet PC for day-to-day installs.

## Commissioning

```text
1. Tablet → Android Wi‑Fi → office network
2. Install APK from Actions
3. Open app → Settings (create PIN)
4. Server Base URL with port, e.g. http://10.111.47.233:8080
5. Device ID = ERP Attendance Device Alias
6. Terminal SN = ERP Terminal SN (tablet serial)
7. Test connection → expect health OK (punch may 403 until device registered)
8. Enroll employees (attendance_code + name + 3 face shots)
9. Start attendance (kiosk)
```

## ERP readiness (ops)

1. Set `ATTENDANCE_KBYAI_ENABLED=true` and restart backend
2. Prefer `ATTENDANCE_KBYAI_ALLOWED_CIDRS` = office LAN
3. Attendance Devices → Alias + Terminal SN, vendor `other`, Active
4. Smoke from PC: `GET /kby-ai/health` and sample `POST /kby-ai/punch`

## Hardware SOP

- Fixed wall/desk mount; avoid strong backlight behind the user
- Keep front camera lens clean
- Use Android screen lock + app admin PIN
- One tablet ↔ one ERP device row (do not reuse Alias across sites)

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Health fail | ERP up? correct IP/port? flag enabled? |
| 403 punch | Device Alias/SN Active? CIDR allowlist? |
| Punch OK, no today row | `attendance_code` mismatch; rollup |
| Always unknown | Re-enroll lighting; one face only |
| Always spoof | Avoid screen glare; real face distance |
| Models missing | Add TFLite files per MODELS.md and rebuild |
| **App not installed** (esp. Xiaomi/MIUI) | 1) Unzip GitHub artifact — do not install the ZIP. 2) Copy `facekiosk-release.apk` via **USB/Drive** (not WhatsApp). 3) Uninstall any old Face Attendance / `com.barabd.facekiosk` / `.debug`. 4) Settings → Play Protect → turn off scan. 5) Install from **Files** → Downloads. 6) If still blocked: Developer options → disable **MIUI optimization**, reboot, retry. |

## Signed release (optional)

Add GitHub Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Workflow then builds `assembleRelease`.
