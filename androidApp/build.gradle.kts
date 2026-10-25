plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.kover)
    alias(libs.plugins.play.publisher)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "org.mariacf.tempusremedium.android"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "org.mariacf.tempusremedium"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        // .github/workflows/release.yml sets these from the run number, so every release has a
        // unique, increasing versionCode without editing this file. Local builds use the defaults.
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION_NAME") ?: "1.0.0"
    }

    // Filled by release.yml from Action secrets (KEYSTORE_BASE64 decoded to KEYSTORE_FILE, plus
    // KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD). Unset locally: see the release fallback below.
    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 (full mode, see gradle.properties) shrinks, optimises and obfuscates, and drops
            // unused resources. Keep rules live in proguard-rules.pro; scripts/verify-obfuscation.sh
            // checks the result on every PR and release.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Real release key on CI; debug signing otherwise, so `./gradlew assembleRelease`
            // still works on a machine without the secrets.
            signingConfig =
                if (System.getenv("KEYSTORE_FILE") != null) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Robolectric's SDK 36 shadows reach into JDK internals.
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
    }
}

dependencies {
    implementation(project(":ui"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    // Glance runs every widget session as a WorkManager job but only asks for WorkManager 2.7.1
    // (2021, Room 2.2), which predates targetSdk 34+. Pin a current one.
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

// Uploads the release App Bundle to the Play Console (`publishReleaseBundle`, run by release.yml
// only when the ANDROID_PUBLISHER_CREDENTIALS secret exists). Goes to the "internal" track unless
// PLAY_TRACK says otherwise, so nothing reaches production without a promotion in the console.
play {
    track.set(System.getenv("PLAY_TRACK")?.takeIf { it.isNotBlank() } ?: "internal")
    defaultToAppBundles.set(true)
}
