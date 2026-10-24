import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.play.publisher) apply false
    alias(libs.plugins.ktlint) apply false
    // Applied for real: the root project is Kover's merging module for core + androidApp.
    alias(libs.plugins.kover)
}

// ktlint (official style + Compose rules) on every project, including this build script.
// `./gradlew ktlintCheck` is the CI gate, `./gradlew ktlintFormat` fixes what it can.
val ktlintVersion = libs.versions.ktlint.get()
val composeRules = libs.compose.rules.ktlint
allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<KtlintExtension> {
        version.set(ktlintVersion)
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
        filter {
            // Compose resource accessors and other generated sources are not ours to style.
            exclude { entry -> entry.file.path.contains("${File.separatorChar}generated${File.separatorChar}") }
            exclude { entry -> entry.file.path.contains("${File.separatorChar}build${File.separatorChar}") }
        }
    }
    dependencies {
        "ktlintRuleset"(composeRules)
    }
}

dependencies {
    kover(project(":core"))
    kover(project(":androidApp"))
}

kover {
    reports {
        verify {
            // Merged core + Android coverage; core has its own, stricter rule.
            rule {
                minBound(70)
            }
        }
        filters {
            excludes {
                // Generated Compose Multiplatform resource accessors.
                packages("org.mariacf.tempusremedium.ui.resources")
            }
        }
    }
}
