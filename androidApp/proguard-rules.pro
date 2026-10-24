# R8 configuration for the minified, obfuscated `release` build (androidApp/build.gradle.kts pairs
# this file with AGP's proguard-android-optimize.txt).
#
# Rule of thumb: only keep what is reached *by name at runtime*. Everything called through normal
# Kotlin code (the repository, view models, Compose UI, Glance content) is renamed consistently at
# the definition and the call site, so keeping it only makes the APK bigger.
#
# Needs no rule here:
# - Manifest components (TempusRemediumApplication, MainActivity, the reminder and widget receivers):
#   AGP generates keep rules from the merged manifest.
# - kotlinx.serialization: the library ships R8 rules that keep each @Serializable class's
#   generated serializer. JSON keys come from the serial descriptors, not from field names, so
#   renaming fields never changes the stored document. scripts/verify-obfuscation.sh checks that
#   AppData's serializer survived.
# - Compose Multiplatform resources: read from assets by path, never through reflection.
# - Glance, WorkManager, coroutines: their consumer rules ship with the libraries.

# ── Crash readability ────────────────────────────────────────────────────────
# Keep line numbers but hide the original file names; traces are decoded with the release's
# mapping.txt (attached to every GitHub Release by .github/workflows/release.yml).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Obfuscation strength / size ──────────────────────────────────────────────
# Nothing resolves a class through its package, so flatten everything into the root package and
# let R8 widen visibility to inline across package boundaries.
-repackageclasses ''
-allowaccessmodification

# ── Room (WorkManager's database) ────────────────────────────────────────────
# Glance brings in WorkManager, whose WorkManagerInitializer runs at startup and opens WorkDatabase.
# Room finds the generated WorkDatabase_Impl by name and calls its no-arg constructor reflectively.
# Room's own consumer rule (`-keep class * extends androidx.room.RoomDatabase`) keeps the class but,
# in R8 full mode, not that constructor, so the release build crashed on launch with
# NoSuchMethodException. scripts/verify-obfuscation.sh checks the constructor survives.
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
