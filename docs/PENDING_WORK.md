# Pending Work / अधूरे काम — ईमानदार सूची

यह फाइल जानबूझकर सबसे ऊपर रखी गई है। इसमें वह सब कुछ है जो अभी तक **research/verify नहीं हुआ**,
**test नहीं हुआ**, या जो मैंने Android SDK/Gradle के बिना, सिर्फ code review से बनाया — यानी
"Zero Assumption Rule" और "Build Verification" के अपने ही नियम के हिसाब से, यह Phase Completion
Checklist अभी पास नहीं करता। इसे build करने से पहले पढ़ें।

## 1. यह repo अभी तक कभी compile नहीं हुआ

इस environment में Android SDK नहीं है, इसलिए `./gradlew assembleDebug` कभी नहीं चलाया जा सका।
पूरा code Kotlin/Gradle/Room/Hilt की जानकारी के आधार पर हाथ से लिखा गया है, syntax सावधानी से
जाँचा गया है, और तीन असली bugs code-review में पकड़कर ठीक किए गए (नीचे Section 3 देखें) — लेकिन
**असली compiler ने इसे अभी तक नहीं देखा।** पहला GitLab CI run ही असली सच बताएगा।

**आपको क्या करना है:** Repo push करने के बाद पहले pipeline को ध्यान से देखें। संभावित समस्याएं:
- KSP/Hilt annotation processing में version mismatch
- Room schema export path (`app/schemas/`) की ज़रूरत हो सकती है
- SQLCipher native library लोड होने में डिवाइस/emulator-विशेष दिक्कत
- `.gitlab-ci.yml` में `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` का पथ अनुमानित है (सामान्य
  Docker images में यह standard path है), पर `mingc/android-build-box` में असली path अलग हो सकता
  है — अगर pipeline में "JAVA_HOME तय नहीं मिला" जैसी error आए तो `before_script` में यह लाइन
  हटा दें (image का default JDK शायद पहले से ठीक हो) या सही path के लिए `ls /usr/lib/jvm/` चलाएँ।

## 2. Level 4 Duplicate Detection (Semantic AI) — लागू नहीं है

Master Specification Section 9 में Level 4 (Semantic AI similarity) का ज़िक्र है। यह repo सिर्फ
Level 1-3 (Hash, Metadata, Perceptual Hash) देता है। Level 4 के लिए एक असली TFLite embedding
model चाहिए (Phase 9-10) — वह model चुनना, बंडल करना, और उसका JNI/TFLite wiring खुद एक बड़ा
काम है जिसे "जल्दी में" ठीक से नहीं किया जा सकता। Roadmap के Phase 9-10 में यह अलग से होना चाहिए।

## 3. Code review में मिले और ठीक किए गए bugs (transparency के लिए)

- `moveToTrash()` में एक bug था जो trash में डाली गई फाइल का entity पुराने (अब न रहे) path से
  फिर से पढ़ने की कोशिश कर रहा था — ठीक कर दिया गया, अब पहले से मौजूद domain data इस्तेमाल होता है।
- Vault का "Biometric unlock" गलती से PIN-verification code को दोबारा (गलत तरीके से) कॉल कर
  रहा था — अब एक अलग, सही `unlockWithBiometric()` path है जो BiometricPrompt के अपने-आप में
  प्रमाणीकरण होने पर भरोसा करता है।
- `OcrRepositoryImpl` में एक placeholder/broken expression था जहाँ Context injection होना
  चाहिए था — ठीक कर दिया गया (`@ApplicationContext` अब सही से inject होता है)।
- **सबसे गंभीर:** `DatabaseModule.kt` शुरू में `net.zetetic:sqlcipher-android` (नया, maintained
  artifact) को dependency में इस्तेमाल कर रहा था, पर कोड `net.sqlcipher.database.SupportFactory`
  (पुराने, अब deprecated `android-database-sqlcipher` artifact की class) import कर रहा था — यह
  दो अलग-अलग libraries हैं, अलग Java package namespace के साथ। यह build को शुरू से ही तोड़ देता
  (`unresolved reference`)। Web search से verify करने के बाद सही किया गया — अब सही class
  `net.zetetic.database.sqlcipher.SupportOpenHelperFactory` इस्तेमाल होती है, जो असली माने में
  installed dependency से मेल खाती है।

**ईमानदार बात:** अगर मैंने इतनी सावधानी से review न किया होता, तो ये तीनों bugs सीधे repo में
चले जाते और सिर्फ runtime crash से पता चलते। CI compile-check यह नहीं पकड़ेगा (ये logic bugs
हैं, syntax bugs नहीं) — इसलिए **manual testing ज़रूरी है**, सिर्फ green pipeline पर भरोसा न करें।

## 4. Testing — बहुत कम कवरेज

सिर्फ 3 pure-logic unit test files हैं (FileSizeFormatter, PerceptualHash math, FileCategory) —
कुल ~15 test cases। यह Roadmap Phase 16 ("Unit Test, Integration Test, UI Test, Security Test,
All Tests Pass") से बहुत दूर है। Repository/ViewModel/DAO लेयर का **कोई automated test नहीं है**।
Room in-memory DB tests, Hilt test modules, और Compose UI tests — यह सब अभी बाकी है।

## 5. Release Signing — कॉन्फ़िगर नहीं है

CI release job में keystore secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`) की जगह है, पर कोई keystore generate नहीं किया गया — क्योंकि keystore को कभी
repo या chat में generate नहीं करना चाहिए (वह production सुरक्षा का आधार है)। जब तक ये
GitLab CI/CD Variables में नहीं डाले जाते, release job अपने-आप unsigned APK बनाएगा, जो सिर्फ
internal testing के लिए ठीक है, Play Store के लिए नहीं।

## 6. Play Store "All Files Access" Declaration — नहीं भरा गया

`MANAGE_EXTERNAL_STORAGE` permission (AndroidManifest.xml) एक file manager app के लिए approved
use-case है, पर Play Console में इसके लिए एक अलग "All Files Access" declaration form भरना
अनिवार्य होता है, वरना app submission पर reject होगा। यह repo के दायरे से बाहर है — Phase 18
(Production Audit) में करना होगा।

## 7. Cloud Sync (Phase 11) — बिल्कुल शुरू नहीं हुआ

इस repo में कोई Google Drive/OneDrive/Dropbox/NextCloud/S3/NAS code नहीं है। Master Roadmap के
अनुसार यह Phase 11 है — File Manager (Phase 3-7) और Vault (Phase 6) के base ready होने के बाद
अगला काम। जानबूझकर इसे अभी शामिल नहीं किया, ताकि core पहले ठोस बने।

## 8. OCR Plugin — कोड मौजूद है, पर feature फिलहाल core में wired नहीं

`OcrRepositoryImpl` और उसका ML Kit binding बना है, पर UI में कहीं से call नहीं होता (कोई
"Extract Text" button नहीं है)। Roadmap के हिसाब से यह सही है (Phase 8 = plugin, अभी core-phase
में हैं), पर अगर आप जल्दी OCR टेस्ट करना चाहें तो अभी manual wiring करनी होगी।

## 9. Similarity slider की सीमाएं

Duplicate Cleaner का 70%-95% slider सिर्फ perceptual-hash (Level 3) पर लागू है — असली AI-आधारित
"semantic similarity" (Level 4, जैसे burst-photo detection) नहीं। बड़े photo sets (10k+ images)
पर वर्तमान O(n²) pairwise comparison धीमा पड़ सकता है — यह अभी सिर्फ correctness के लिए लिखा गया
है, बड़े scale पर optimize नहीं (जैसे locality-sensitive hashing bucket करना)।

## 10. User audit (18 July 2026) — 3 और असली bugs मिले और ठीक किए गए

आपने CI/CD, Gradle, और Architecture का सीधा निरीक्षण किया और 3 चीज़ें flag कीं। तीनों verify करने
पर सही निकलीं:

- **Signing property नाम गलत था (गंभीर):** `.gitlab-ci.yml` और GitHub workflow दोनों में
  `-Pandroid.injected.signing.key.alias` लिखा था, जबकि AGP का असली property नाम
  `android.injected.signing.store.key.alias` है (बीच में `.store.` चाहिए)। नतीजा: भले ही आप
  `KEYSTORE_BASE64` जैसे secrets सही से configure कर देते, AGP इन गलत-नाम properties को चुपचाप
  नज़रअंदाज़ कर देता — release APK बिना किसी error के unsigned बन जाता। Web search से AGP का
  सही property नाम verify करके दोनों CI files में ठीक किया गया।
- **GitHub Actions में explicit Android SDK setup नहीं था:** पहले भरोसा था कि `ubuntu-latest`
  runner में SDK pre-installed मिलेगा। जनवरी 2026 में GitHub ने अपनी runner images से पुराने
  SDK platforms/build-tools और sdkmanager binary खुद हटाने शुरू कर दिए (disk space बचाने के
  लिए) — यह Zero Assumption Rule के हिसाब से एक वाजिब जोखिम था। अब `android-actions/setup-android@v4`
  explicit step जोड़ा गया है, साथ ही `actions/setup-java@v4` का built-in `cache: gradle` (पहले
  GitHub workflow में कोई cache नहीं था, सिर्फ GitLab में था)।
- **Lint/Static Analysis पूरी तरह गायब था:** दोनों CI files में सिर्फ unit tests चलते थे — कोई
  lint step नहीं, जबकि आपके अपने Master AI Agent के नियम में "Lint, Static Analysis" स्पष्ट रूप
  से Build Verification का हिस्सा है। अब दोनों में `./gradlew lintDebug` जोड़ा गया है
  (non-blocking, ताकि शुरुआती builds findings की वजह से block न हों, पर report artifact में
  हमेशा दिखेगी)। **Ktlint/Detekt (code-style linters) अभी भी नहीं जोड़े गए** — इनके लिए अलग
  Gradle plugin setup चाहिए; अगर चाहें तो अगली बार जोड़ सकता हूँ।
- **इस जांच से एक चौथा bug भी मिला:** SQLCipher fix (ऊपर Section 3) के दौरान मैंने
  `DatabaseModule.kt` में package तो सही कर दिया था (`net.zetetic.database.sqlcipher.*`), पर
  `proguard-rules.pro` में पुराना (अब गलत) `net.sqlcipher.**` keep rule छूट गया था। Release
  build में SQLCipher की classes R8 द्वारा strip हो सकती थीं। अब ठीक किया गया।

**ProGuard rules पर एक साफ़ बात:** Room, Hilt, Compose, WorkManager, ML Kit जैसी libraries अपनी
`consumer-rules.pro` खुद अपने AAR में bundle करके लाती हैं, जो R8 अपने-आप merge करता है — तो
मेरी manual proguard-rules.pro ज़्यादातर defense-in-depth है, अकेला भरोसा नहीं। असली load-bearing
हिस्सा सिर्फ SQLCipher (JNI-heavy library) और अपने domain/entity classes के लिए है। यह अब भी
किसी असली R8 run से verify नहीं हुआ — सिर्फ code-review-level तर्क है।

## 11. दूसरा (गहरा, file-by-file) audit — 18 July 2026 — सब कुछ confirmed निकला, ज़्यादातर ठीक किया गया

आपने 8 files का forensic, evidence-based audit किया (PassphraseProvider, VaultCryptoManager,
VaultPinManager, VaultRepositoryImpl, DatabaseModule, AppDatabase, FileEntryDao,
DuplicateRepositoryImpl)। हर एक finding को actual source code से मिलाकर verify किया गया —
**कोई false positive नहीं निकला।** जो ठीक किया गया:

- **VaultRepositoryImpl — दो HIGH-severity ordering bugs (सबसे गंभीर पूरे audit में):**
  `addToVault()` में पहले source file delete होती थी, फिर DB insert — insert fail होने पर source
  हमेशा के लिए गायब, कोई record नहीं (data loss)। अब DB insert पहले, फिर delete — insert fail होने
  पर source + encrypted copy दोनों बचे रहते हैं (orphan, पर recoverable)। इसी तरह `removeFromVault()`
  में DB delete अब encrypted-file delete से पहले होता है, ताकि DB-delete fail होने पर एक broken
  reference (encrypted file गायब पर DB record अभी भी उसे point करे) बनने की बजाय एक recoverable
  orphan बने।
- **VaultCryptoManager — atomic writes:** पहले encryption/decryption सीधे final destination file
  पर लिखते थे; crash होने पर partial/corrupt file बन सकती थी। अब temp file पर लिखा जाता है,
  `fsync` किया जाता है, फिर atomically `destFile` पर rename होता है। साथ ही file format में अब
  1-byte version header भी है (future format बदलने पर backward-compat detect करने के लिए)।
- **VaultPinManager — दो security gaps:** `contentEquals()` (non-constant-time) की जगह अब
  `MessageDigest.isEqual()` इस्तेमाल होता है। साथ ही failed-attempt lockout जोड़ा गया — 5 गलत
  प्रयासों के बाद exponential-backoff lockout (30 सेकंड से शुरू, 30 मिनट तक cap) शुरू हो जाता है,
  जो पहले बिल्कुल मौजूद नहीं था (unlimited brute-force संभव था)।
- **PassphraseProvider — one-time secret persistence:** database passphrase और vault key की पहली
  बार generation अब `apply()` (async) की बजाय `commit()` (sync) से persist होती है — यह एक
  one-time, non-hot-path operation है तो synchronous cost नगण्य है, और crash-right-after-generation
  का data-loss window बंद हो जाता है।
- **DatabaseModule — silent data loss:** `fallbackToDestructiveMigration()` अब एक logging callback
  के साथ है, ताकि अगर यह कभी trigger हो (कोई future schema बदलाव बिना migration के), तो यह loudly
  log हो, चुपचाप नहीं। **असली fix अब भी नहीं है** (कोई v2 migration नहीं लिखी गई, क्योंकि अभी कोई
  v2 schema ही नहीं है) — यह procedural discipline पर निर्भर रहेगा: version कभी भी बिना migration
  के मत बढ़ाना।
- **DuplicateRepositoryImpl — O(n²) visual comparison पर एक hard safety cap:** पूरा fix (LSH
  bucketing / BK-tree) अभी implement नहीं किया — वह एक बड़ा, अलग piece of work है। इसकी जगह एक
  conservative (untested) 5000-images की सीमा जोड़ी गई, ताकि बहुत बड़ी gallery पर ऐप घंटों तक hang
  होने की बजाय तुरंत एक साफ़ error दे।

**जो जानबूझकर नहीं ठीक किया गया (scope से बाहर, honestly documented):**
- `FileEntryDao.listAllFiles()` / `observeAllNonTrashFiles()` का पूरी table memory में लोड होना
  — असली fix (Paging3) एक बड़ा architecture बदलाव है, अभी नहीं किया।
- `DuplicateRepositoryImpl` में Kotlin-level filtering (SQL WHERE clause की जगह) — मामूली
  inefficiency, functionality को प्रभावित नहीं करती।
- Encrypted key bytes / PIN CharArray को इस्तेमाल के बाद zero-out करना — hardening सुझाव है,
  JVM/Kotlin में guaranteed नहीं (GC/String pool की वजह से), इस पास में नहीं किया।

## 12. तीसरा audit round (Phase 2 — 18 July 2026) — SmartManagerApp, MainActivity, DI, Repository, ViewModel, UI

आपके अपने Phase-2 scope (SmartManagerApp, MainActivity, Hilt/DI Modules, Repository, ViewModels,
FileManager, Search, Vault) के मुताबिक बाकी files audit कीं। **एक ज़रूरी नोट:** आपने जो
`compress_for_audit.zip` भेजा वह मेरे पहले दो fix-rounds से पहले का पुराना snapshot था — तो
यह raund मैंने अपने current (latest) repo पर किया, ताकि पहले से ठीक हो चुके बग दोबारा न गिनाऊं।

**सबसे बड़ा confirmed finding (इस पूरे audit का सबसे गंभीर):**
`IndexingScheduler.schedulePeriodicIndexing()` — जो पूरे background-indexing सिस्टम (Phase 12)
का entry point है — **पूरे codebase में कहीं से भी call नहीं होता था।** इसका मतलब: पहले से मौजूद
(install से पहले की) कोई भी file कभी index नहीं होती थी — सिर्फ वो files जिन्हें File Manager से
छुआ जाए (copy/move/rename) incrementally index होती थीं। **Search और Duplicate Cleaner किसी भी
वास्तविक user के real-world photos/files के लिए लगभग खाली परिणाम देते।** साथ ही, `WorkManager`
का कोई भी Hilt binding मौजूद नहीं था — तो जिस दिन कोई इस dead code को wire करने की कोशिश करता,
build तुरंत `[Dagger/MissingBinding]` error से fail हो जाता। दोनों ठीक किए गए:
- `AppModule.kt` में `WorkManager` का एक Hilt provider जोड़ा गया।
- `SmartManagerApp.onCreate()` में (आधिकारिक Hilt docs से verify करने के बाद कि field injection
  `super.onCreate()` में होता है, `attachBaseContext()` में नहीं) `indexingScheduler.schedulePeriodicIndexing()`
  को सुरक्षित तरीके से call किया गया।
- `IndexingScheduler` में अब periodic (6-घंटे) के साथ-साथ एक one-time immediate index request भी
  जुड़ा है, ताकि fresh install पर user को पहले search result के लिए 6 घंटे इंतज़ार न करना पड़े।

**दूसरा गंभीर confirmed finding:** `FileEntryEntity.path` column पर कोई index/unique constraint
नहीं थी। नतीजा: (1) हर `findByPath`/`deleteByPath` query full table scan करती, (2) चूंकि नई
entities हमेशा `id=0` (autogenerate) से बनती हैं, एक ही physical file अलग-अलग indexing calls से
duplicate rows के रूप में जमा हो सकती थी — जिससे Search में एक ही file दो बार दिखती, और Duplicate
Cleaner उसे "खुद अपने से duplicate" के रूप में गलत तरीके से flag कर सकता था। अब `path` पर
`unique = true` index जोड़ा गया है, जिससे Room का REPLACE conflict resolution fresh (id=0)
inserts पर भी सही तरीके से एक ही row में collapse होता है।

**तीसरा finding (behavioral, real पर scope से बाहर — नया feature-building, bug-fix नहीं):**
File Manager में Rename/Copy/Move — तीनों का business logic (Repository + UseCase स्तर पर) पूरी
तरह सही और काम करने लायक है, पर **UI से इन तीनों में से किसी तक पहुंचना संभव नहीं है।**
`FileManagerScreen` में long-press सीधे `requestDelete()` से जुड़ा है — कोई context-menu/action-sheet
नहीं जो Rename/Copy/Move/Delete में चुनने का विकल्प दे। साथ ही Copy/Move के लिए एक destination-folder
picker UI की भी ज़रूरत होगी, जो अभी कहीं मौजूद नहीं है। यह असल में एक नया, ठीक-ठाक बड़ा UI feature
बनाने का काम है — इसलिए इस audit-fix session में शामिल नहीं किया, अगली प्राथमिकता के तौर पर दर्ज
कर रहा हूँ।

**अन्य confirmed व ठीक किए गए issues:**
- `StorageRepositoryImpl` में hardcoded `"/storage/emulated/0"` path था, जो secondary Android
  user/work-profiles पर गलत हो सकता है (`Environment.getExternalStorageDirectory()` से ठीक किया,
  जो बाकी codebase में पहले से इस्तेमाल हो रहा तरीका है)।
- `FileRepositoryImpl.moveToTrash()` में एक redundant/dead `dao.deleteByPath()` call थी जो
  REPLACE-based upsert के बाद कभी कोई row delete नहीं करती (हटाई गई, सिर्फ confusing code था,
  functional bug नहीं)।

**Verify करके confirm किया (bug नहीं निकला):**
- `@HiltAndroidApp` सही से `SmartManagerApp` पर लगा है।
- `PassphraseProvider`/`VaultCryptoManager` में कोई hardcoded key नहीं (`SecureRandom` से generate
  होती है) — Devil's Advocate audit के अपने संदेह की तरह हमने इसे दोबारा verify किया।
- ML Kit Text Recognition dependency (`com.google.mlkit:text-recognition-devanagari`) सही "bundled"
  variant है, "unbundled" (Play Services से download होने वाला) नहीं — तो OCR वाकई 100% offline है,
  जैसा दावा किया गया था।
- `StoragePermissionGate` — Settings से वापस आने पर permission state सही तरीके से refresh होता है।

**एक architecture-level ईमानदार बात (bug नहीं, पर design-gap):** Master Specification का
"Core vs Plugin" विभाजन (OCR, Semantic AI, non-Drive cloud providers = plugin) अभी सिर्फ
package-structure के स्तर पर logical है — असली Android Dynamic Feature Modules (Play Feature
Delivery) के ज़रिए build-level अलगाव नहीं है। इसका मतलब ML Kit के bundled OCR models (~8MB, दो
scripts के लिए) अभी हमेशा base APK में शामिल रहेंगे, चाहे उन्हें "plugin" कहा जाए। असली on-demand
delivery के लिए अलग Gradle dynamic-feature modules चाहिए होंगे — यह अपने आप में एक बड़ा, अलग
architecture-level काम है।

## 13. Branding update + चौथा audit round (19 July 2026) — Navigation, Theme, Auto Lock, Edge-to-Edge

**Branding बदलाव (आपके निर्देश पर):**
- Launcher icon अब golden leaf image है (legacy + adaptive दोनों), VVF badge logo नहीं।
- VVF logo अब ऐप के अंदर एक Splash Screen (androidx.core:core-splashscreen, verified current
  version 1.2.0 — पहले गलती से पुराना 1.0.1 लिख दिया था, official Android docs से ठीक किया) पर
  दिखता है, cold-start पर।
- नोट: नए plan/skill zip के तीनों project docs पहले जैसे ही थे — कोई नया निर्णय नहीं मिला।

**गंभीर confirmed finding: Auto Lock पूरी तरह गायब था।** Master Roadmap का स्पष्ट Phase 6
requirement ("PIN, Biometric, **Auto Lock**") — पर कोड में सिर्फ manual "Lock करें" button था।
कोई background/inactivity-आधारित auto-lock नहीं था, यानी एक बार unlock होने के बाद Vault तब तक
unlocked रहता जब तक कोई manually lock न करे — चाहे app background में चला जाए, screen off हो
जाए, कुछ भी हो। `ProcessLifecycleOwner` (पूरे app का foreground/background state, single
Activity का नहीं — ताकि screen rotation जैसी चीज़ें गलती से lock न कर दें) से एक
`VaultAutoLockObserver` बनाया और `SmartManagerApp.onCreate()` में register किया — अब app
background जाते ही Vault अपने आप lock हो जाता है।

**दूसरा confirmed finding: Edge-to-Edge / Window Insets समस्याएं (targetSdk 35 से सीधे जुड़ी)।**
- `Theme.kt` में `Window.setStatusBarColor()` इस्तेमाल हो रहा था, जो official Android docs के
  अनुसार API 35+ पर **deprecated और no-op दोनों है** — यानी targetSdk 35 पर यह कोड कुछ करता ही
  नहीं था, पुराने Android पर `enableEdgeToEdge()` के मॉडल से टकराता था। हटाया गया (सिर्फ
  status-bar icon light/dark वाला हिस्सा रखा, जो अब भी valid API है)।
- 5 में से 4 screens (Search/Vault/Storage/Duplicate) का अपना कोई `Scaffold` नहीं था — सिर्फ
  बाहरी `VvfNavHost` का Scaffold insets handle करता था। पर `FileManagerScreen` का अपना nested
  Scaffold था, जो बाहरी Scaffold के insets के ऊपर **दोबारा** insets जोड़ देता — यानी
  FileManagerScreen पर status bar के नीचे ज़रूरत से ज़्यादा खाली जगह दिखती, बाकी 4 screens से
  अलग। ठीक किया: बाहरी Scaffold अब `contentWindowInsets = WindowInsets(0)` से insets खुद नहीं
  लेता (सिर्फ अपने bottomBar की height reserve करता है), और बाकी 4 screens में सीधे
  `.safeDrawingPadding()` जोड़ा गया।

**Verify करके confirm किया (bug नहीं निकला):**
- Navigation (`VvfNavHost`/`VvfDestinations`) की structure सही है, कोई crash-risk नहीं।
- Material3 components (Scaffold, NavigationBar) खुद-ब-खुद insets handle करते हैं, यह
  आधिकारिक Android docs से verify किया — सिर्फ ऊपर बताई गई double-counting वाली जगह गलत थी।

## 14. Tests folder audit + locale-sensitivity sweep (20 July 2026) — पूरा audit अब complete

तीनों test files (`FileSizeFormatterTest`, `PerceptualHashMathTest`, `FileCategoryTest`) की
assertions manually हाथ से गणित करके verify कीं — सभी सही हैं। पर इस दौरान एक असली, systemic
bug class मिला:

**Locale-sensitive string operations — 4 जगह मिलीं, सभी ठीक कीं:**
`String.format()` और `String.lowercase()`/`.uppercase()` बिना explicit `Locale` के, device की
**default locale** इस्तेमाल करते हैं — सिर्फ "style" का मसला नहीं, असली functional bug:
- `FileSizeFormatter.format()` — comma-decimal locales (कई European locales) पर "512.0 B" की
  जगह "512,0 B" दिखाता, **और `testDebugUnitTest` को CI पर locale-निर्भर तरीके से fail भी करा
  सकता था** (यह इसे सिर्फ cosmetic नहीं, CI-reliability का मसला भी बनाता है)।
- `FileCategory.fromExtension()` — Turkish locale पर `"AVI".lowercase()` "avi" नहीं बल्कि "avı"
  (dotless ı) बनाता है, यानी **`.avi` video files गलत तरीके से category detect ना होतीं** — यह
  असल में `FileCategoryTest` (`extension matching is case insensitive`) test इसे कभी नहीं
  पकड़ता क्योंकि test सिर्फ "JPG"→"jpg" जैसे "I" रहित examples इस्तेमाल करता है।
- `MimeTypeResolver.resolve()` — वही समस्या MIME-type lookup से पहले extension lowercase करने में।
- `FileRepositoryImpl` की file-listing sort order — कम गंभीर (सिर्फ sort क्रम बदलता, functionality
  नहीं टूटती), पर उसी वजह से फिर भी ठीक किया।

सभी को `Locale.ROOT` (technical/ASCII string-matching के लिए भाषा-निष्पक्ष locale, Kotlin/Java
docs की recommended practice) से explicit किया गया।

**निष्कर्ष: पूरा audit (5 rounds — CI/CD, Vault Security, SmartManagerApp/DI/Repository/ViewModel,
Navigation/Theme/Auto-Lock, Tests/Locale) अब complete है।** बचे मुद्दे Section 11 और नीचे की
सूची में documented हैं (Rename/Copy/Move UI, Semantic AI, Cloud Sync, Paging3, इत्यादि)।

## अगला सुझाया गया कदम

1. Repo को GitLab पर push करें, पहला pipeline run देखें, यहाँ जो errors मिलें वो share करें।
2. Debug APK को असली फोन पर install करके File Manager + Vault का manual test करें।
3. **नई प्राथमिकता (Section 12 से):** File Manager में Rename/Copy/Move के लिए UI बनाना —
   long-press context menu + destination-folder picker। Business logic पहले से तैयार है,
   सिर्फ UI जोड़ना बाकी है।
4. उसके बाद ही Phase 8 (OCR UI wiring), Phase 9-10 (Semantic AI), Phase 11 (Cloud) पर बढ़ें —
   Golden Rule के अनुसार "कोई भी चरण Skip नहीं होगा।"
