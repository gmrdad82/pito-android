# PITO Android

[![CI](https://github.com/gmrdad82/pito-android/actions/workflows/build.yml/badge.svg)](https://github.com/gmrdad82/pito-android/actions/workflows/build.yml)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Sponsor](https://img.shields.io/badge/Sponsor-%E2%9D%A4-ff69b4?logo=githubsponsors)](https://github.com/sponsors/gmrdad82)

Android client for [PITO](https://github.com/gmrdad82/pito) — the self-hosted,
chat-first YouTube channel manager. This app is a thin
[Hotwire Native](https://native.hotwired.dev) shell: it renders your PITO
instance's own server-rendered UI full-screen with native navigation. No UI is
reimplemented here; your server is the product, this is the window. That's
also why the app is this small — and why it can never lag behind the web app.

**The app is instance-agnostic.** PITO is self-hostable, and so is this
client: on first launch you enter the URL of *your* instance — a box in your
closet, a VPS, or a hosted instance such as `app.pitomd.com`. No backend is
baked in. Your server, your data, your phone.

More of a terminal person? PITO also speaks ANSI:
[**`pito-tui`**](https://github.com/gmrdad82/pito-tui) is the same chatbox —
same server-side grammar, same live scrollback — living in your terminal.
And for the full tour before you commit to anything, the showcase lives at
[**pitomd.com**](https://pitomd.com) ([source](https://github.com/gmrdad82/pitomd)).

## Install

One file, straight from GitHub — no store, no account.

### Get the APK

On your phone, open:

**[github.com/gmrdad82/pito-android/releases/latest/download/pito.apk](https://github.com/gmrdad82/pito-android/releases/latest/download/pito.apk)**

Open the downloaded file with **Package installer**. The first time, Android
asks you to allow your browser to install unknown apps — that's the normal
sideload flow, and the only permission this ever needs. (If PITO's web app
already knows you're on Android, it shows you this same link as a banner.)

### Requirements

- Android 9 (API 28) or newer.
- A PITO instance served over **HTTPS** — plain-HTTP instances are not
  supported. Self-hosting on a LAN? Put TLS in front (a reverse proxy or the
  tunnel the pito installer sets up).

### Updates

Same link, newer file: download, open, install — it updates **in place**,
data and login intact. Android verifies every update against the same
signing key, so only releases from this repo can update your install. Watch
the [Releases](https://github.com/gmrdad82/pito-android/releases) page (or
the CHANGELOG) to see what changed; there is no auto-updater and none is
needed — everything inside the app is your server's UI and updates itself
the moment your instance deploys.

### Verify the download (optional, for the trust-nothing crowd)

The release certificate never changes. If you want to check an APK before
installing:

```bash
apksigner verify --print-certs pito.apk | grep SHA-256
# 914624907d963c3a27303e86a742d9fdf8f301276216fbd5216914bc0ed02f3d
```

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
