# ERP runbook note (copy into barabdERPVAT-JAVA)

Paste into `docs/System/hr-and-payroll/attendance-kby-ai.md` when the kiosk is stable:

---

## Open-source face kiosk (`com.barabd.facekiosk`)

The Android tablet app **Face Attendance Kiosk** uses the same public bridge:

- `GET /kby-ai/health`
- `POST /kby-ai/punch`

Commissioning:

1. Enable `ATTENDANCE_KBYAI_ENABLED=true` and set `ATTENDANCE_KBYAI_ALLOWED_CIDRS` to the office LAN
2. Register Attendance Device: vendor **`other`**, Alias = kiosk Device ID, Terminal SN = tablet serial, Active
3. Align employee `attendance_code` with enroll codes on the tablet
4. Install APK from the kiosk repo GitHub Actions artifacts (see kiosk `docs/SETUP.md`)

Face images/templates are **not** sent to ERP — only punch JSON (`device_id`, `person_id`, `time`, `punch_type`, `similarity`).

ZKTeco `/iclock` ADMS path remains unchanged.

---
