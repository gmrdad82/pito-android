# Changelog

All notable changes to the PITO Android app are documented here. The format
follows [Keep a Changelog](https://keepachangelog.com/); from 1.0.0 onward
the project follows [Semantic Versioning](https://semver.org/) — patch for
fixes and polish, minor for new shell capability, major for anything that
breaks the shell/server contract.

## [Unreleased]

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
