# Face Attendance Kiosk (`com.barabd.facekiosk`)

On-device face attendance for office tablets. Enroll staff by ERP `attendance_code`, identify with open-source ML, punch the existing ERP bridge `POST /kby-ai/punch`. Face templates stay on the tablet — never uploaded.

Product plan: [`ins.md`](ins.md)

## Operator path

1. Connect tablet to office **Wi‑Fi** (Android system settings)
2. Install APK from **GitHub Actions → Artifacts**
3. Settings → create PIN → Server Base URL (with port) → Device ID / Terminal SN
4. Test connection → Enroll → **Start attendance**

## Build APK (primary: GitHub Actions)

```text
git push / tag v*
  → Actions workflow "Android APK"
  → Artifacts → facekiosk-*.apk
```

- Triggers: push to `main`, tags `v*`, manual `workflow_dispatch`
- Without keystore secrets → **debug** APK
- With secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` → **signed release** APK

Local fallback (optional): Android Studio → Run / `./gradlew assembleDebug`

## Docs

| Doc | Purpose |
| --- | --- |
| [docs/SETUP.md](docs/SETUP.md) | Commissioning + install from Actions |
| [docs/SECURITY.md](docs/SECURITY.md) | Privacy, PIN, network |
| [docs/MODELS.md](docs/MODELS.md) | TFLite sources + hashes |
| [docs/ACCEPTANCE.md](docs/ACCEPTANCE.md) | Test checklist |
| [docs/ERP_RUNBOOK_NOTE.md](docs/ERP_RUNBOOK_NOTE.md) | Text to paste into ERP runbook |

## Stack

Kotlin · CameraX · ML Kit Face Detection · MobileFaceNet · MiniFASNet · Room · OkHttp · minSdk 26

## Models

Copy `mobile_face_net.tflite` and `minifasnet_anti_spoof.tflite` into `app/src/main/assets/models/` before production use. See [docs/MODELS.md](docs/MODELS.md).
