plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0" apply false
    // Applied conditionally in app/build.gradle.kts — only when
    // app/google-services.json exists. See the comment there.
    id("com.google.gms.google-services") version "4.5.0" apply false
}
