# Changelog

All notable changes to the PITO Android app are documented here. The format
follows [Keep a Changelog](https://keepachangelog.com/); from 1.0.0 onward
the project follows [Semantic Versioning](https://semver.org/) — patch for
fixes and polish, minor for new shell capability, major for anything that
breaks the shell/server contract.

## [Unreleased]

### Added

- **Push build wiring** — `com.google.gms.google-services` (4.5.0) and
  `firebase-messaging` (via the 34.16.0 BoM) land in the Gradle graph, ready
  for the messaging code that follows. The plugin applies ONLY when
  `app/google-services.json` exists, so forks and any checkout without a
  configured Firebase project keep building — pushless, not broken. Locally,
  `app/build.gradle.kts` mirrors the owner's `tmp/google-services.json` into
  place automatically; CI decodes it from a `GOOGLE_SERVICES_JSON_BASE64`
  secret when one is set.
- **Push registration and receive** — a `push-registration` bridge component
  answers the web's Stimulus controller: on "register" it asks for
  POST_NOTIFICATIONS (Android 13+, only if not already granted), fetches the
  current FCM token, and replies with `{"token": "..."}`; native never talks
  to the server or touches cookies, and any snag along the way (permission
  denied, a pushless fork build with no Firebase project, a failed token
  fetch) means no reply at all — the web just doesn't hear back. A new
  `PitoMessagingService` handles the data-only payloads FCM actually delivers
  (`message` + `level` keys): builds a system notification on channel `pito`
  (small icon, pito-blue accent, tapping it resumes the warm task via
  MainActivity's `singleTask`) and stashes a rotated token from `onNewToken`
  for the next natural "register" round-trip to pick up — no network calls
  originate from native at any point.

### Fixed

- **Launcher-icon taps stop replaying the boot chrome** — MainActivity was
  `singleInstance`, and OnboardingActivity's fast-forward path always cleared
  the task on its way there, so every icon tap destroyed the warm WebView and
  reran the neon boot animation from scratch. MainActivity is now
  `singleTask`, and the fast-forward path starts it with a plain intent (an
  existing instance is brought forward, not recreated) — the task teardown
  now happens only right after connecting to a NEW instance, the one case
  where the old WebView really is stale and must die.

## [1.2.0] — 2026-07-12

### Changed

- **Pull-down does nothing now** — the native swipe-to-refresh (a cold-boot
  reload that unloaded the document into a dead black frame) is disabled
  outright: no spinner, no reload, inert glass. The chat's one refresh
  gesture is PITO's own bottom pull-up, exactly as on the web; the error
  screen's pull keeps the real reload (that document is already gone), and
  the neon logo remains what it was always meant to be: app-boot chrome.

### Fixed

- **System font scale stops inflating the charts** — the WebView's text zoom
  is pinned to 100 so braille visualizations render at the geometry the
  server laid out.

## [1.0.2] — 2026-07-05

### Changed

- **Launcher icon wears the brand** — the tile background is now pito-blue
  (`--brand-pito: #5170ff`, same token as the web app) behind the white P.
- **Status bar stays solid** — a glass under-flow was prototyped and
  deliberately shelved: the web app's top controls (Esc, list actions) must
  remain tappable, and touches inside the status-bar zone belong to the
  system. The real glass (web-side `backdrop-filter` header that insets its
  own chrome, shell injecting the inset) is logged as a cross-repo upgrade.

## [1.0.1] — 2026-07-05

First round of real-device feedback, fixed.

### Added

- **Resume where you left off** — a cold start now reopens the page you were
  on instead of the start screen (which quietly spawned a fresh "Unnamed"
  conversation every launch). Same-origin only; switching servers resets it.
- **Coverage gate** — Kover verifies the JVM-testable logic layer stays
  above 80% line coverage (89.8% at adoption) on every CI run, the same
  guardrail pito (SimpleCov) and pito-tui (go test) carry.

### Fixed

- **Status-bar collision** — web content no longer runs under the clock;
  the shell pads exactly the status-bar height (an invisible black strip)
  while the rest stays edge-to-edge.
- **Launcher icon was a plain white tile** — the icon source carries an
  opaque dark background that the white-glyph recolor painted over;
  the background is now knocked out first. White P on black, as intended.

## [1.0.0] — 2026-07-04

The first release: a thin [Hotwire Native](https://native.hotwired.dev)
shell that renders any PITO instance full-screen with native navigation.
Sideloaded APK, no Play Store.

### Added

- **Instance-agnostic onboarding** — the app ships with NO backend baked in:
  a first-launch screen (block-art PITO logo with the web start screen's
  broken-neon reveal, DejaVu Sans Mono, glass connect button) asks for YOUR
  instance URL and keeps it in app storage. Change it any time via the
  launcher long-press → **Change Server**. HTTPS only.
- **Full-screen web shell** — the instance's own Turbo/Stimulus UI renders
  edge-to-edge (no native toolbar, no reserved bars); native back stack;
  keyboard and safe-area behave like the browser.
- **Server-driven path configuration** — the shell loads
  `/configurations/android_v1.json` from your instance (bundled fallback
  when absent), so navigation behavior can evolve without an APK release.
- **Sane link routing** — same-instance links stay in-app; other http(s)
  hosts open in a Chrome Custom Tab; `mailto:`/`tel:` go to the system.
  `accounts.google.com` is intercepted with a native notice (Google blocks
  OAuth in embedded WebViews) — reconnect from any browser, the app picks
  the server-side connection up immediately.
- **Chat sounds work on arrival** — the WebView lifts Chromium's autoplay
  gate so send/receive/notify sounds play without a first tap.
- **Signed releases with a green-CI gate** — a `v*` tag builds the release
  APK only when every workflow on the tagged commit is green, then attaches
  it to the GitHub Release under the stable name `pito.apk`;
  `/releases/latest/download/pito.apk` never breaks. `versionCode` derives
  from the tag (`v1.2.3` → 10203), so in-place updates always apply.
- **CI** — Robolectric unit suite + debug APK on every push/PR; an
  emulator-based Espresso suite weekly and on demand; Slack notifications
  with pito's one-message-per-event rules.
