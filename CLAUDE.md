# CLAUDE.md

Guidance for Claude Code (claude.ai/code) working in this repository.

Start with **`README.md`** for what the app is, how to build it, and the architecture with diagrams.
This file covers what an agent needs *beyond* that: the reference documents, the invariants that are
easy to violate, and the traps that cost real debugging time to find.

## The two reference documents

- **`androidImplPlan.md`** (repo root, ~1450 lines) is the authoritative build spec. It read the iOS
  app end-to-end and maps every iOS file/type/behavior to its Kotlin/Compose equivalent, with exact
  UUID/wire-format tables (§5), per-screen UI spec (§6), color palette (§7), localization plan (§8),
  test-by-test mapping (§9), the **decision log and open items (§10)**, and the phased build order
  with per-phase status callouts (§11). **Copy reference values from it** — UUIDs, byte layouts,
  colors, string keys — rather than re-deriving them from the iOS source.
- **The iOS app itself**, at `/Users/armstrongllc/Desktop/BLE/nRFThingy52`. It is the parity target,
  and for anything the plan doesn't cover it is the tiebreaker.

Two cautions about the plan. It was **regenerated on 2026-07-24** after the original was deleted, and
the reconstruction silently dropped content — §10's orientation-label item and the entire Phase 10
section were both lost and had to be re-recorded. If the plan seems to be missing something you
remember, that's a plausible explanation. And §8.1's claim that the iOS app was fully localized was
simply wrong (it verified key *count*, not values); §10 item 12 supersedes it.

## Status

**Phases 0–10 are complete.** `./gradlew build` green: 62 JVM unit tests, lint 0 errors, all four
variants. `./gradlew connectedMockDebugAndroidTest` green: 4 instrumented tests.

**The one substantive gap: the real BLE transport has never touched hardware.** `ThingyGatt`'s
connect/discover/notify/read pipeline is written but entirely unproven — verifying it needs a
physical Thingy:52 *and* a physical Android device, since the emulator has no BLE radio. Everything
else is proven against the fake transport. Do not describe the `prod` flavor as working.

Smaller open items live in plan §10: orientation labels hardcoded English on both platforms (item
13), two untranslated accessibility strings, and the launcher icon still being the Android Studio
template.

## Invariants — violating these breaks things quietly

**Never mutate `uiState` outside the ViewModel's `events` collection.** Android's
`BluetoothGattCallback`/`ScanCallback` fire on a Binder thread, unlike iOS's main-queue
CoreBluetooth. The transport converts each callback to an immutable `ThingyGattEvent` on a
`SharedFlow`; the single `viewModelScope.launch { controller.events.collect(…) }` is the concurrency
boundary that replaces iOS's `@MainActor`. **There is no compiler enforcement.** Never call into
Compose from the transport. (README has the sequence diagram; plan §4.1 has the rationale.)

**`domain/` stays free of framework dependencies.** Only `java.util.UUID` and `kotlin.math`. This is
what lets the wire-format tests run as plain JVM tests with fixtures copied verbatim from iOS. When a
domain type needs a UI resource, do the lookup in the UI layer — the way `RssiBucket` maps to a
drawable in `ThingyRow`, not in the enum. (This is the crux of plan §10 item 13.)

**`ui/detail/SensorFormat.kt` is a cross-platform contract — don't change one side alone.**
Locale-aware separators, grouping suppressed everywhere, HALF_EVEN rounding. Its header comment says
*"DO NOT 'fix' the locale handling here"*; believe it. If the formatting genuinely must change, it
changes on both platforms in the same pass.

**No hardcoded colors in screen code.** Pull from `MaterialTheme.colorScheme` or add a token to
`NordicColors`. Dynamic color is deliberately off to preserve branding.

**Localized values are transcribed, never machine-translated.** `tools/gen_locales.py` copies
verbatim from the iOS `.lproj` files. `values/strings.xml` is *not* generated — the English source
and the Android-only `cd_*` accessibility strings are hand-maintained.

**Parity divergences are decisions, not accidents.** If iOS and Android must differ, record it in
plan §10 with the reasoning, and prefer landing the change on both platforms together. Several items
there exist precisely because a unilateral "fix" would have made the two apps disagree.

## Traps that cost real time

**`String.format` rounds HALF_UP; `NumberFormat` rounds HALF_EVEN.** `java.util.Formatter` renders
`-5.25` as `-5.3` where iOS gives `-5.2`. This is why `SensorFormat` uses `NumberFormat` and why
`SensorFormatTest` asserts exactly `-5.25` and `271.5` — the only values where the modes diverge.
A hand-off document once asserted the opposite; it was wrong, and only an empirical check caught it.

**AGP 9 has built-in Kotlin support.** Applying `org.jetbrains.kotlin.android` is a hard error ("no
longer required since AGP 9.0"). Only the Compose compiler plugin is applied. Do not re-add it.

**`BleManager` resists subclassing from Kotlin.** `isConnected()` is a Java method that doesn't
satisfy a Kotlin `val`, and `disconnect()` is `final` returning a request object. `ThingyGatt`
therefore **wraps** a private `ThingyBleManager` rather than extending it.

**`uiautomator` reports semantic text, not rendered text.** It cannot detect a visual ellipsis, so
truncation must be checked from screenshots. Locale switches also need settle time — a stale dump
once made `ja` appear to render Simplified Chinese.

**Green build, broken pixels.** Two vendored icons rendered as solid black squares because the
official Material SVGs carry a leading `M0,0h24v24H0V0z` bounding-box path. Build and tests were
green; only viewing a screenshot found it. For anything visual, look at it.

**Mirroring an SVG arc means reversing its endpoints, not negating x.** An elliptical-arc command
has four candidate arcs for a given endpoint pair and radius; the large-arc and sweep flags pick
one. The launcher icon's left arcs initially selected a wrong centre and bulged across the device.
Nothing catches this but rendering — the build is green either way. (Plan §10 item 15.)

**Grep translated terms, not English ones.** When correcting a translated string, searching for the
English phrase finds nothing in 17 of 18 locales. Regenerating wholesale and diffing is more reliable
than patching matched lines.

**There is no plain `installDebug`.** Flavors make every variant task `{mock,prod}{Debug,Release}`.
Use `mock` for anything hardware-free.

## Working agreements

- **The maintainer stages, commits, and pushes.** Draft the commit message; don't run `git commit`
  unless asked.
- **Prefer the Edit tool over shell scripts for source edits.** Scripts are fine for genuinely bulk
  mechanical transforms — `tools/gen_locales.py` is the sanctioned example.
- **Phases start on explicit instruction** ("start Phase N"), and each leaves the app building.
- **Verify claims before reporting them.** Several defects here were found only because a plausible
  result was re-checked rather than accepted. Report what the command actually printed.

## Fixed configuration — do not re-decide

`minSdk 24` · `targetSdk 36` · `compileSdk 37` · AGP `9.1.1` · Gradle `9.3.1` · Kotlin `2.2.20` ·
namespace/applicationId `com.armstrongmobile.nrfthingy52android`.

compileSdk 37 is an intentional divergence from the plan's pinned 36, made at the maintainer's
request; it is what allows the current AndroidX line. There is no command-line `sdkmanager` in this
SDK — AGP auto-downloads platforms on first build.

The system JDK isn't on `PATH`; export `JAVA_HOME` to the Android Studio JBR before building
(see README).
