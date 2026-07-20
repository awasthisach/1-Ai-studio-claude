# VVF Smart Manager — release ProGuard/R8 rules
#
# NOTE (honesty check, 18 July 2026): Room, Hilt, Compose, WorkManager, and ML Kit AARs each
# bundle their own consumer-rules.pro that R8 merges automatically at app-build time — so most
# of the rules below are defense-in-depth, not the only thing standing between this app and a
# release-build crash. The SQLCipher and app-specific rules ARE load-bearing, since that library
# is JNI-heavy and our own domain/entity classes aren't covered by anyone else's consumer rules.
# None of this has been verified against a real R8 run yet — see docs/PENDING_WORK.md.

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# SQLCipher — net.zetetic:sqlcipher-android (NOT the deprecated net.sqlcipher.* package;
# see DatabaseModule.kt for why that distinction matters here).
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# Hilt / Dagger generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }

# Kotlin coroutines / metadata
-keepattributes *Annotation*
-keepclassmembers class kotlin.Metadata { *; }

# Keep domain models (used via reflection-free Room mapping, but safe to keep names for debugging)
-keep class com.vvf.smartmanager.domain.model.** { *; }
-keep class com.vvf.smartmanager.data.local.entity.** { *; }
