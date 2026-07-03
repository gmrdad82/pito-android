# CLAUDE.md — pito-android

Agent guide for **pito-android** — the Hotwire Native (Kotlin) Android shell
for [PITO](https://github.com/gmrdad82/pito), distributed as a signed APK via
GitHub Releases (no Play Store).

## The log law (non-negotiable; mechanically enforced)

The active working plan in `docs/claude/*.md` is the **single source of
truth** — every todo, bug, decision, and discussion item the owner raised.
NEVER hold work in your own memory or the harness todo list. If it isn't in
the working md, it does not exist.

A `UserPromptSubmit` hook (`.claude/hooks/capture-prompt.sh`) appends every
owner message verbatim to `docs/claude/INBOX.md` as a `## ⛔ UNPROCESSED`
block. **Every turn, before anything else:**

1. Read `docs/claude/INBOX.md`.
2. **Drain** each `⛔ UNPROCESSED` block into the active plan — turn EVERY
   item into an explicit task/line; split compound messages; lose nothing.
3. Rewrite the block heading in place to `## ✅ processed — <ts> -> <plan refs>`.
   Never delete it — the back-reference makes capture auditable.

The `Stop` hook (`.claude/hooks/check-inbox.sh`) refuses to end a turn while
any `⛔ UNPROCESSED` block remains. `docs/claude/` (INBOX + plans) is
gitignored (local-only); the hooks + this section are committed so the guard
ships with the repo.

## What this is

A **thin shell**. It renders any PITO instance's server-rendered
Turbo/Stimulus UI in a WebView with native navigation, a native instance
picker, and Custom Tabs for external links. The Rails app is the product —
**never rebuild web UI natively**, never add native features the web side
can serve. When in doubt, the fix belongs in the `pito` repo, not here.

## Stack

Kotlin + Gradle (wrapper-pinned; no system gradle), single-activity
**Hotwire Native** (`dev.hotwire:core` + `dev.hotwire:navigation-fragments`),
plain XML views (no Compose), Material3 always-dark theme (terminal
aesthetic: black background, monospace, pito-blue `#5170ff` accent,
border-radius 0). Unit tests: JUnit 4 + Robolectric + AssertJ +
MockWebServer. Instrumented: Espresso (+ espresso-web).

## Commands

Builds REQUIRE JDK 17 (`JAVA_HOME=/usr/lib/jvm/java-17-openjdk` — Gradle
does not run on the system's newer default JVM) and `ANDROID_HOME`.

```bash
./gradlew testDebugUnitTest        # unit tests (the PR gate)
./gradlew assembleDebug            # debug APK
./gradlew connectedDebugAndroidTest  # instrumented (device/emulator attached)
adb install app/build/outputs/apk/debug/app-debug.apk
emulator -avd pito                 # local AVD, if created
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
  GoogleAuthRouteDecisionHandler.kt  accounts.google.com → native notice
app/src/main/assets/json/path-configuration.json   bundled fallback config
app/src/debug/           debug-only manifest overlay (cleartext for localhost/10.0.2.2)
app/src/test/            Robolectric unit tests
app/src/androidTest/     Espresso instrumented tests
.github/workflows/       build.yml (CI) · instrumented.yml (weekly/manual) · release.yml (tags)
docs/claude/             agent working docs — GITIGNORED, local only
```

## Invariants (don't break these)

- **Instance-agnostic.** Never hardcode a backend domain. Release builds ship
  NO default instance; `app.pitomd.com` / `dev.pitomd.com` are the owner's
  instances — example text and dev prefill only. Everything derives from the
  stored instance URL (`Instance`).
- **HTTPS-only in release.** Onboarding rejects `http://` unless
  `BuildConfig.ALLOW_HTTP` (debug). The debug network-security overlay allows
  cleartext ONLY for `localhost`/`10.0.2.2`.
- **`Hotwire.registerRouteDecisionHandlers(...)` REPLACES the defaults.**
  Registration order in `PitoApplication` is the routing table: Google
  handler first, then the three built-ins (app navigation → browser tab →
  system). First match wins — re-list all of them, keep the order.
- **Path configuration**: bundled asset is the contract's fallback; remote is
  `{instance}/configurations/android_v1.json`. Any instance WITHOUT that
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

## Way of working

- **Atomic tasks**, one verb each, tracked as checkboxes in the active
  `docs/claude/` plan (`[ ]`→`[-]`→`[x]`, one edit per transition).
- **Done means verified**: `./gradlew testDebugUnitTest` green +
  `assembleDebug` builds; UI-touching changes get checked on a device or
  emulator before the task closes. New code ships with tests.
- **Commit hygiene**: plain imperative messages — no `[skipci]`, no
  co-author / "Generated with" trailers. One branch, commit per phase, push
  incrementally, CI green before merging.
- **System-level changes** (SDK, packages, shell profiles) are the owner's
  to run — produce an explained runbook, never execute them silently.

## CI / release

- `build.yml` — every push/PR: unit tests + `assembleDebug` + APK artifact.
- `instrumented.yml` — weekly + manual: emulator + `connectedDebugAndroidTest`.
- `release.yml` — on `v*` tag: green-CI gate (refuses to build on red or
  missing CI), decode keystore from secrets, `assembleRelease`, attach
  `pito.apk` to the GitHub Release.
- Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`, `SLACK_WEBHOOK`.
- Ship flow: finish work → CI green → `git tag v1.2.3 && git push origin
  v1.2.3` → gate → signed release. Distribute ONLY release-signed APKs.

## License

AGPL-3.0 (see [`LICENSE`](LICENSE)).
