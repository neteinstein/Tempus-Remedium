import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.security.MessageDigest

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
// so the app installed on a phone's home screen opens offline from its first launch. sw.js gets
// a version made from the files' contents: a new build changes sw.js, which makes the browser
// install it, fill a new cache and drop the old one.
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
        val digest = MessageDigest.getInstance("SHA-256")
        files.forEach { path ->
            digest.update(path.toByteArray())
            digest.update(root.resolve(path).readBytes())
        }
        val version = digest.digest().take(8).joinToString("") { "%02x".format(it) }
        val worker = root.resolve("sw.js")
        worker.writeText(worker.readText().replace("const VERSION = 'dev';", "const VERSION = '$version';"))
    }
}
