plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .customRuleSets(listOf("io.nlopez.compose.rules:ktlint:${libs.versions.compose.rules.get()}"))
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
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
                packages("io.github.neteinstein.remedioemdia.ui.resources")
            }
        }
    }
}
