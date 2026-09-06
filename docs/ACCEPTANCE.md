# Acceptance checklist

| Test | Pass rule |
| --- | --- |
| Health | Returns bridge identity when ERP flag enabled |
| Happy path | Enroll code → identify → Attendance Sync row + today present/late |
| Spoof | Phone/print photo → no punch |
| Unknown | Unenrolled face → no punch |
| Cooldown | Second punch within 60s suppressed |
| Device ACL | Wrong Alias/SN → HTTP 403 |
| Offline | Kill Wi‑Fi mid-punch → outbox → recovers after reconnect |
| Privacy | Proxy/log shows no image payload |
| CI APK | Push/tag produces downloadable artifact; installs without Studio |
| PIN | Settings / Enroll / People / exit kiosk require PIN |

## Sign-off

- [ ] Commissioned tablet on office Wi‑Fi
- [ ] Models vendored + MODELS.md hashes filled
- [ ] Operator completed enroll + punch smoke
- [ ] ERP CIDR + device row verified
