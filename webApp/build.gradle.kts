import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "tempusremedium"
        browser {
            commonWebpackConfig {
                outputFileName = "tempusremedium.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":ui"))
            implementation(libs.kotlinx.browser)
        }
    }
}

// precache.json: every file of the web build, for the service worker (sw.js) to keep on install,
// so the app installed on a phone's home screen opens offline from its first launch.
tasks.named<Sync>("wasmJsBrowserDistribution") {
    doLast {
        val root = (this as Sync).destinationDir
        val files =
            root
                .walkTopDown()
                .filter { it.isFile }
                .map { it.relativeTo(root).invariantSeparatorsPath }
                .filterNot { it == "sw.js" || it == "precache.json" || it.endsWith(".map") }
                .sorted()
                .toList()
        root.resolve("precache.json").writeText(files.joinToString(",\n  ", "[\n  ", "\n]\n") { "\"$it\"" })
    }
}
