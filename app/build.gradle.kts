import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
    id("com.google.gms.google-services") apply false
}

// Firebase push wiring (build wiring only — no messaging code lands until a
// later task). Local convenience: mirror the owner's tmp/google-services.json
// (gitignored, .keep-tracked folder — see .gitignore) into app/google-services.json
// whenever it's newer, so a fresh checkout builds off the owner's file with no
// manual copy step. This has to be plain configuration-time code, not a Gradle
// task: it must finish BEFORE the conditional apply(...) below, which itself
// runs during configuration — a lazily-executed task would only take effect on
// the NEXT build. CI writes app/google-services.json itself from a secret (see
// .github/workflows/*.yml) and never touches tmp/.
val googleServicesJson = file("google-services.json")
val ownerGoogleServicesJson = rootProject.file("tmp/google-services.json")
if (ownerGoogleServicesJson.exists() &&
    (!googleServicesJson.exists() ||
        ownerGoogleServicesJson.lastModified() > googleServicesJson.lastModified())
) {
    ownerGoogleServicesJson.copyTo(googleServicesJson, overwrite = true)
}

// The google-services plugin fails the build outright when google-services.json
// is absent. Forks and a not-yet-configured checkout have no Firebase project,
// so apply it ONLY when the file exists — those builds simply ship pushless.
if (googleServicesJson.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// versionCode derives from the release tag: v1.2.3 -> 10203. CI provides
// GITHUB_REF_NAME on tag builds; local release builds pass -PversionTag=v1.2.3.
// Constraint: minor and patch must stay < 100 or codes collide.
val releaseTag: String? =
    (providers.environmentVariable("GITHUB_REF_NAME").orNull
        ?: providers.gradleProperty("versionTag").orNull)
        ?.takeIf { Regex("""^v\d+\.\d+\.\d+$""").matches(it) }
val semver = releaseTag?.removePrefix("v")

android {
    namespace = "md.pitom.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "md.pitom.android"
        minSdk = 28
        targetSdk = 35
        versionName = semver ?: "0.0.0-dev"
        versionCode = semver?.split(".")?.map(String::toInt)
            ?.let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }
            ?: 1

        // Release ships NO default backend — the app is instance-agnostic.
        buildConfigField("String", "DEFAULT_INSTANCE_URL", "\"\"")
        buildConfigField("boolean", "ALLOW_HTTP", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            System.getenv("KEYSTORE_PATH")?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Owner dev convenience only: prefills the onboarding field.
            applicationIdSuffix = ".debug"
            buildConfigField("String", "DEFAULT_INSTANCE_URL", "\"https://dev.pitomd.com\"")
            buildConfigField("boolean", "ALLOW_HTTP", "true")
        }
        release {
            isMinifyEnabled = false
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Coverage gate (parity with pito's SimpleCov / pito-tui's go test floor):
// koverVerifyDebug fails the build when line coverage of the JVM-testable
// code drops below the floor. Android lifecycle glue (Activities, custom
// Views, the fragment) is exercised by the instrumented suite, which doesn't
// feed this number — excluded so the floor measures logic, not theater.
kover {
    reports {
        filters {
            excludes {
                classes(
                    "md.pitom.android.BuildConfig",
                    "md.pitom.android.MainActivity",
                    "md.pitom.android.OnboardingActivity",
                    "md.pitom.android.PitoWebFragment",
                    "md.pitom.android.NeonLogoView*",
                    "md.pitom.android.GlassButton",
                    // Thin Android/Firebase glue — permission requests, the
                    // ActivityResultRegistry dance, and FCM's own service
                    // lifecycle all need a live Activity/Firebase app to
                    // exercise meaningfully (instrumented-test territory).
                    // Their actual logic (data -> notification mapping,
                    // token persistence) is pulled into PushNotifications /
                    // PendingPushToken, which stay covered.
                    "md.pitom.android.PushRegistrationComponent*",
                    "md.pitom.android.PitoMessagingService",
                )
            }
        }
        variant("debug") {
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}

dependencies {
    implementation("dev.hotwire:core:1.2.8")
    implementation("dev.hotwire:navigation-fragments:1.2.8")
    // Already in the runtime graph via navigation-fragments; declared so the
    // in-place pull-to-refresh override can type against SwipeRefreshLayout.
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Firebase Cloud Messaging client. The BoM pins every Firebase artifact's
    // version together; declared unconditionally so the app compiles the same
    // on forks (see the conditional google-services plugin above) — without a
    // google-services.json, Firebase just stays uninitialized at runtime and
    // push is a no-op.
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-messaging")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")

    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.7.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
}
