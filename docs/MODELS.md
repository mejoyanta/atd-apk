# Models

Place these files under `app/src/main/assets/models/` (already vendored for v1):

| File | Role |
| --- | --- |
| `mobile_face_net.tflite` | Face embedding (MobileFaceNet lineage) |
| `minifasnet_anti_spoof.tflite` | Passive liveness / anti-spoof |

## Vendored sources (v1)

| File | Source URL | License note | SHA-256 |
| --- | --- | --- | --- |
| `mobile_face_net.tflite` | https://github.com/MCarlomagno/FaceRecognitionAuth/raw/master/assets/mobilefacenet.tflite | Derived from MobileFaceNet open models (verify upstream for your compliance review) | `BE4BC7CFC53F7BC336D0F28B1AB92535F618C913A422B683210750F6B5354854` |
| `minifasnet_anti_spoof.tflite` | https://github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing/raw/master/app/src/main/assets/FaceAntiSpoofing.tflite | Anti-spoof TFLite from open Android demo (CVPR2019 ZeroShot FAS lineage) | `A980757D55E2835AFF69F2FA9B04914E21D426DD8A43588C2982D50E8038EC02` |

```powershell
Get-FileHash app\src\main\assets\models\mobile_face_net.tflite -Algorithm SHA256
Get-FileHash app\src\main\assets\models\minifasnet_anti_spoof.tflite -Algorithm SHA256
```

## Runtime expectations in this app

- MobileFaceNet: **112×112**, RGB float `[-1, 1]`, L2-normalized embedding
- Anti-spoof file: treated as **attack score** (higher = more spoof-like); app converts to live score as `1 - attack`
- If a true MiniFASNet 2-class export is substituted later, `LivenessChecker` already accepts a 2-float softmax (index 1 = live)

## Rules

- Do **not** download models at runtime from unknown mirrors
- Re-hash and update this table whenever you replace a file
- Without models the APK still builds; Enroll/Identify show “models missing”
