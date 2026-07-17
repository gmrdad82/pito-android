# pito-android — project guide (for Claude / agents)

Agent guide for **pito-android** — the Hotwire Native (Kotlin) Android shell
for [PITO](https://github.com/gmrdad82/pito), distributed as a signed APK via
GitHub Releases (no Play Store).

> The global working agreement (`~/.claude/CLAUDE.md`) applies here; this
> file carries only pito-android specifics. This repo is deliberately small —
> a thin shell, not a product of its own. Deeper PITO product architecture
> lives in the `pito` repo (`docs/architecture.md`, `docs/design.md` for the
> visual tokens this shell's theme tracks); this repo's own `README.md`
> covers install/build/connect — read the relevant one before writing code,
> don't work from memory.

## Working here

- **System-level changes** (SDK, packages, shell profiles, keystores) are
  the owner's to run — produce an explained runbook, never execute them
  silently.

**Done means verified**, against this repo's real gates:

- `./gradlew testDebugUnitTest koverVerifyDebug` green — unit tests (JUnit 4
  + Robolectric + AssertJ + MockWebServer) plus the 80% line-coverage floor
  over the JVM-testable logic layer (`BuildConfig`, Activities, the
  fragment, and custom Views are excluded from the floor — the instrumented
  suite exercises those instead). This is the exact `build.yml` CI gate.
- `./gradlew assembleDebug` builds clean — also gated in CI, uploaded as an
  artifact.
- UI-touching changes get checked on a device or emulator before the task
  closes. A change touching the WebView shell, onboarding, or routing gets a
  pass through `./gradlew connectedDebugAndroidTest` (Espresso +
  espresso-web) even though that suite runs in CI only weekly / on manual
  dispatch (`instrumented.yml`) — a red run on the exact SHA you then tag
  WILL block the release gate.
- New code ships with tests: unit at minimum, instrumented for flows
  Robolectric can't reach (real WebView content, real navigation).

---

# pito-android architecture (map + invariants)

A **thin shell**. It renders any PITO instance's server-rendered
Turbo/Stimulus UI in a WebView with native navigation, a native instance
picker, and Custom Tabs for external links. The Rails app is the product —
**never rebuild web UI natively**, never add native features the web side
can serve. When in doubt, the fix belongs in the `pito` repo, not here.

## Invariants (don't break these)

- **Instance-agnostic.** Never hardcode a backend domain. Release builds
  ship NO default instance; `app.pitomd.com` / `dev.pitomd.com` are the
  owner's instances — example text and dev prefill only. Everything derives
  from the stored instance URL (`Instance`).
- **HTTPS-only in release.** Onboarding rejects `http://` unless
  `BuildConfig.ALLOW_HTTP` (debug). The debug network-security overlay
  allows cleartext ONLY for `localhost`/`10.0.2.2`.
- **`Hotwire.registerRouteDecisionHandlers(...)` REPLACES the defaults.**
  Registration order in `PitoApplication` is the routing table: Google
  handler first, then the three built-ins (app navigation → browser tab →
  system). First match wins — re-list all of them, keep the order.
- **Path configuration**: bundled asset is the contract's fallback; remote
  is `{instance}/configurations/android_v1.json`. Any instance WITHOUT that
  endpoint must keep working (404 is ignored by design).
- **Caching**: WebView `LOAD_DEFAULT` + persistent cookies + the library's
  path-config cache — that's the v1 design. Never flip to
  `LOAD_CACHE_ELSE_NETWORK`; never adopt the Experimental
  `offlineRequestHandler`.
- **Signing/versioning**: ONE release keystore, forever, never in git —
  signing reads env vars only. `versionCode` derives from the tag
  (`v1.2.3 → 10203`); minor/patch must stay < 100. The release asset name is
  **`pito.apk`**, stable forever — the `/releases/latest/download/pito.apk`
  URL must never break.
- **UA contract**: `applicationUserAgentPrefix` starts with `PITO;` and the
  library appends `Hotwire Native` — the Rails side detects the app via
  `request.user_agent =~ /Hotwire Native/`. Don't remove either half.
- **Visual tokens track `pito`'s design system, not the other way around.**
  Always-dark Material3 theme, DejaVu Sans Mono, black background, pito-blue
  `#5170ff` accent, border-radius 0 — these mirror the web app's
  `docs/design.md` tokens. Check there before changing colors, spacing, or
  typography; don't invent new visual language here.

---

# Stack & commands

Kotlin + Gradle (wrapper-pinned to 9.2.0; no system gradle), single-activity
**Hotwire Native** (`dev.hotwire:core` + `dev.hotwire:navigation-fragments`,
pinned `1.2.8`), plain XML views (no Compose), Material3 always-dark theme.
Unit tests: JUnit 4 + Robolectric + AssertJ + MockWebServer, with Kover
enforcing the coverage floor. Instrumented: Espresso (+ espresso-web).

Builds REQUIRE JDK 17 (`JAVA_HOME=/usr/lib/jvm/java-17-openjdk` — Gradle
does not run on the system's newer default JVM) and `ANDROID_HOME`
(`compileSdk`/`targetSdk` 35, `minSdk` 28).

```bash
./gradlew testDebugUnitTest koverVerifyDebug  # unit tests + coverage floor (the PR gate)
./gradlew assembleDebug                       # debug APK (also gated in CI)
./gradlew connectedDebugAndroidTest           # instrumented (device/emulator attached)
adb install app/build/outputs/apk/debug/app-debug.apk
emulator -avd pito                            # local AVD, if created
```

Debug builds use applicationId `md.pitom.android.debug` (coexists with the
release app) and prefill the owner's dev instance in onboarding.

## Structure

```
app/src/main/kotlin/md/pitom/android/
  PitoApplication.kt     Hotwire config: UA prefix, destinations, route handlers
  Instance.kt            instance-URL store (SharedPreferences) + path-config wiring
  OnboardingActivity.kt  LAUNCHER; first-run / settings screen (instance URL)
  MainActivity.kt        HotwireActivity + NavigatorHost (the whole app UI)
  PitoWebFragment.kt     stock web fragment, native toolbar hidden
  GoogleAuthRouteDecisionHandler.kt  accounts.google.com → native notice
  NeonLogoView.kt, GlassButton.kt, AsciiLogo.kt   boot chrome / onboarding UI
app/src/main/assets/json/path-configuration.json   bundled fallback config
app/src/debug/           debug-only manifest overlay (cleartext for localhost/10.0.2.2)
app/src/test/            Robolectric unit tests
app/src/androidTest/     Espresso instrumented tests
.github/workflows/       build.yml (CI) · instrumented.yml (weekly/manual) · release.yml (tags)
<local notes dir>/       agent working docs — outside the repo (per-person, optional)
```

## CI / release

- `build.yml` — every push/PR to `main`: `testDebugUnitTest` +
  `koverVerifyDebug` + `assembleDebug` + APK artifact. Skippable per-commit
  with the literal `[skipci]` token in the commit message or PR title.
- `instrumented.yml` — weekly (Mondays 05:00 UTC) + manual dispatch:
  KVM-accelerated emulator (API 35) running `connectedDebugAndroidTest`.
  Deliberately OUT of the push/PR loop (slow, occasionally flaky) — but a
  red run on the SHA you then tag blocks the release gate.
- `release.yml` — on `v*` tag: waits for and requires ALL non-release
  workflow runs on the tagged SHA to be green (fails closed on timeout or no
  CI evidence), decodes the keystore from secrets, `assembleRelease`, stages
  `pito.apk`, attaches it to the GitHub Release.
- Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`, `SLACK_WEBHOOK`.
- Ship flow: finish work → CI green → owner runs `git tag v1.2.3 && git
  push origin v1.2.3` → gate → signed release. Distribute ONLY
  release-signed APKs.

## License

AGPL-3.0 (see [`LICENSE`](LICENSE)).
