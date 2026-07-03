# PITO Android

Android client for [PITO](https://github.com/gmrdad82/pito) — the self-hosted,
chat-first YouTube channel manager. This app is a thin
[Hotwire Native](https://native.hotwired.dev) shell: it renders your PITO
instance's own server-rendered UI full-screen with native navigation. No UI is
reimplemented here; your server is the product, this is the window.

**The app is instance-agnostic.** PITO is self-hostable, and so is this
client: on first launch you enter the URL of *your* instance — a box in your
closet, a VPS, or a hosted instance such as `app.pitomd.com`. No backend is
baked in.

## Install

Download the latest signed APK:

**[github.com/gmrdad82/pito-android/releases/latest/download/pito.apk](https://github.com/gmrdad82/pito-android/releases/latest/download/pito.apk)**

Open the file on your phone and allow your browser to install unknown apps
when prompted (first time only). Updates install in place over the previous
version — just download and open the new APK.

Requirements: Android 9 (API 28) or newer. Your instance must be served over
**HTTPS** — plain-HTTP instances are not supported.

## Connect

On first launch, enter your instance URL (e.g. `https://pito.example.com`)
and tap connect. To point the app at a different instance later, long-press
the launcher icon and choose **Server**.

Login works exactly like the browser: `/authenticate <TOTP code>` in the
chatbox. One note: Google account (re)connection for YouTube must be done
from a regular browser — Google blocks OAuth inside embedded WebViews. The
connection is server-side, so the app picks it up immediately.

## Build from source

You need JDK 17 and the Android SDK (platform 35, build-tools 35.0.0), with
`ANDROID_HOME` and `JAVA_HOME` set. No Android Studio required.

```bash
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
adb install app/build/outputs/apk/debug/app-debug.apk
```

Release builds are signed via environment variables (`KEYSTORE_PATH`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) — see
`app/build.gradle.kts`. CI builds and attaches the signed `pito.apk` to a
GitHub Release on every `v*` tag. Android only updates an app in place when
the signing key matches, so sideload only release-signed APKs.

## License

[AGPL-3.0](LICENSE), same as PITO.
