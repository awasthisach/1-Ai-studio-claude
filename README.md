# VVF Smart Manager

Privacy-First, Offline-First, AI-Powered Personal File Management Platform — Vishva Vijayaa Foundation.

⚠️ **पहले [docs/PENDING_WORK.md](docs/PENDING_WORK.md) पढ़ें** — इसमें ईमानदारी से बताया गया है
कि अभी क्या untested/incomplete है।

## Scope (Master Specification v2.0)

- **File Manager** — Browse / Copy / Move / Rename / Delete / Recycle Bin
- **Secure Vault** — PIN + Biometric unlock, AES-256-GCM per-file encryption
- **Search** — Filename → Metadata → OCR layered search (Room FTS4)
- **Duplicate Cleaner** — Level 1 (Hash) + Level 2 (Metadata) + Level 3 (Visual Similarity)
- **Storage Analyzer** — Category-wise breakdown

देखें: `/mnt/project/` में **Master Specification v2.0** और **Master Roadmap v2.0** के लिए
प्रोजेक्ट के मुख्य conversation से मिले Word documents — यह repo उन्हीं का Phase 1-7 (+ आंशिक
Phase 8-9 प्लंबिंग) implementation है।

## Tech Stack (Frozen — 17 July 2026)

| Layer | Technology |
|---|---|
| Language / UI | Kotlin + Jetpack Compose (Material 3) |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Database | Room + SQLCipher (encrypted at rest) |
| OCR | ML Kit (Latin + Devanagari) |
| Background | WorkManager |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

**अस्वीकृत:** Flutter, Rust, FAISS, Gemini Nano — कारण के लिए Master Specification Section 3 देखें।

## GitLab पर APK कैसे बनाएँ

यह repo `.gitlab-ci.yml` के साथ आता है — कुछ भी अलग से सेटअप नहीं करना।

1. इस पूरे folder को एक नए GitLab repo में push करें (नीचे "फोन से Push कैसे करें" देखें)
2. GitLab अपने-आप **Build → Pipelines** में pipeline शुरू कर देगा
3. `main` branch पर push करने से `build_debug_apk` job चलती है
4. Pipeline पूरी होने पर **Job → Artifacts → Browse** से `app-debug.apk` डाउनलोड करें
5. Release/signed APK के लिए एक Git tag बनाएँ (जैसे `v0.1.0`) — तभी `build_release_apk` job चलेगी

### Signed Release के लिए (वैकल्पिक, बाद में)

GitLab → Settings → CI/CD → Variables में जोड़ें:
- `KEYSTORE_BASE64` — आपकी keystore फाइल का base64 (`base64 -w0 release.keystore`)
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

बिना इनके भी release APK बनेगा, पर unsigned रहेगा (सिर्फ internal testing के लिए ठीक)। दोनों
CI files में AGP का सही injected-signing property नाम (`android.injected.signing.store.key.*`)
इस्तेमाल होता है — यह 18 July 2026 के audit में एक गलत property नाम पकड़ने के बाद ठीक किया गया,
देखें `docs/PENDING_WORK.md` Section 10।

### Lint / Code Quality

हर pipeline run में `./gradlew lintDebug` भी चलती है (non-blocking) — findings का HTML/XML
report job artifacts में मिलेगा। Ktlint/Detekt (code-style linters) अभी शामिल नहीं हैं।

### फोन से Push कैसे करें (कोई PC नहीं)

**Option A — GitLab Web IDE:**
1. GitLab.com पर नया empty project बनाएँ
2. उसमें "Web IDE" खोलें
3. इस पूरे folder को upload करें (या फाइल-दर-फाइल editor से paste करें)
4. Commit करें — pipeline अपने आप शुरू हो जाएगी

**Option B — Termux (अगर फोन पर पहले से है):**
```bash
cd vvf-smart-manager
git init
git remote add origin https://gitlab.com/<your-username>/vvf-smart-manager.git
git add .
git commit -m "Initial commit: VVF Smart Manager core (Phase 1-7)"
git push -u origin main
```

## Local Build (अगर कभी PC मिले)

```bash
./gradlew assembleDebug
# APK यहाँ मिलेगा: app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
app/src/main/java/com/vvf/smartmanager/
  core/          di, security (Keystore/SQLCipher/AES), theme, navigation, utils
  data/          local (Room+SQLCipher DAOs/Entities), repository impl, mapper, worker
  domain/        model, repository interfaces, usecase
  presentation/  Compose screens + ViewModels (filemanager, vault, search, storage, duplicate)
```

## License

Internal VVF project — license terms TBD by project owner.
