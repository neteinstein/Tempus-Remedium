// Gradle init script for sandboxed/cloud agent containers only.
//
// Maven Central (repo.maven.apache.org) intermittently answers 404/429 through
// the agent egress proxy, and Gradle caches those misses. Google's mirror of
// Maven Central is reliable there, so try it first, and point Robolectric's own
// android-all downloader at it too. Installed into ~/.gradle/init.d by
// `scripts/setup-android-sdk.sh --maven-mirror`; never needed on developer
// machines or in CI.
val mirror = "https://maven-central.storage-download.googleapis.com/maven2/"

beforeSettings {
    pluginManagement.repositories {
        maven(mirror) { name = "MavenCentralMirror" }
    }
    dependencyResolutionManagement.repositories {
        maven(mirror) { name = "MavenCentralMirror" }
    }
}

allprojects {
    tasks.withType<Test>().configureEach {
        systemProperty("robolectric.dependency.repo.url", mirror)
    }
}
