# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A Kotlin + Jetpack Compose Android app being built as a behavior-for-behavior parity companion to
the sibling iOS app at `/Users/armstrongllc/Desktop/BLE/nRFThingy52`. Same physical target — a
Nordic Thingy:52 BLE dev kit — same GATT UUIDs, same wire-format parsing, same UX: scan for devices
→ list with live RSSI icons → connect on tap → LED toggle with write-then-read-back confirmation →
button press/release with haptics → Environment (temperature/humidity/pressure/air-quality) and
Motion (orientation/steps/heading/tap) dashboards once readings arrive.

`androidImplPlan.md` (repo root) is the authoritative build spec — a ~900-line document that read
the iOS app end-to-end and maps every iOS file/type/behavior to its Kotlin/Compose equivalent, with
exact UUID/wire-format tables (§5), per-screen UI spec (§6), color palette (§7), localization plan
(§8), test-by-test mapping (§9), and an 11-phase build order (§11). **Read it before writing code**;
copy reference values (UUIDs, byte layouts, colors, string keys) from it rather than re-deriving
them from the iOS source. Treat it as authoritative except where it explicitly flags a decision as
open (§10).

## Current state: Phase 0 complete (Compose scaffolding builds)

The starter has been converted to a Compose app and the whole project builds; the parity app itself
(everything from Phase 1 on) is still unwritten. What exists now:

- **Compose entry point.** `MainActivity` is a `ComponentActivity` calling `setContent { … }` with a
  `Text("Thingy52")` placeholder. The old View-based `activity_main.xml` and `AppCompatActivity` are
  gone. Phase 1 replaces the bare `MaterialTheme` with `ThingyTheme`; Phase 5 replaces the
  placeholder with the scanner screen + `NavHost`.
- **Dependencies wired:** Compose BOM + Material 3, activity-compose, navigation-compose,
  lifecycle-viewmodel/runtime-compose, kotlinx-coroutines-android. The View-world `appcompat`/
  `constraintlayout` deps were dropped; `com.google.android.material` is kept only as the XML
  window-theme parent in `res/values/themes.xml`.
- **Product flavors `mock` / `prod`** (dimension `transport`) each expose
  `BuildConfig.USE_FAKE_TRANSPORT` (mock=true, prod=false) — the resolved answer to plan §10.6.
  They currently differ only by that flag; the composition root starts reading it in Phase 3+ once a
  transport exists. Build variants are `{mock,prod}{Debug,Release}`.
- Java/Kotlin target raised to **17**; BLE permissions + `bluetooth_le` feature added to the manifest.

Two non-obvious gotchas discovered while doing Phase 0 (both baked into the build files now):

- **AGP 9 has built-in Kotlin support** — applying the standalone `org.jetbrains.kotlin.android`
  plugin is now a hard error ("no longer required since AGP 9.0"). Only the Compose compiler plugin
  (`org.jetbrains.kotlin.plugin.compose`) is applied. Do not re-add `kotlin-android`.
- **compileSdk is 37** (bumped from the plan's pinned 36 at the maintainer's request — an intentional
  divergence from plan §2.3). The android-37 platform was not pre-installed; AGP auto-downloaded it
  on first build (there is no command-line `sdkmanager` in this SDK). The AndroidX libraries are
  still on the API-36-era line (core-ktx 1.17.0, lifecycle 2.9.4, compose-bom 2025.09.01,
  activity-compose 1.10.1, navigation-compose 2.9.4); compileSdk 37 now **unblocks** the latest
  (core-ktx 1.19, lifecycle 2.11, compose-bom 2026.x, activity-compose 1.13) whenever a bump is
  wanted — they were downgraded only for the old 36 pin.

Fixed config (do not re-decide): `minSdk = 24`, `targetSdk = 36`, `compileSdk = 37`, AGP `9.1.1`,
Gradle `9.3.1`, Kotlin `2.2.20`, namespace/applicationId `com.armstrongmobile.nrfthingy52android`.
This directory is **not a git repository** yet (plan §10.10).

## Commands

Single `:app` module, standard Gradle wrapper. Run from the repo root. Note the `mock`/`prod`
product flavors: variant-specific tasks are named `{mock,prod}{Debug,Release}`, so use `installMockDebug`
(there is no plain `installDebug`). Use the `mock` flavor for hardware-free dev.

The system JDK isn't on PATH; the build needs Java 17+. Use the Android Studio JBR:
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

```bash
./gradlew build                 # all 4 variants + lint + unit tests (the Phase 0 DoD command)
./gradlew assembleMockDebug     # build the mock/debug APK only
./gradlew test                  # JVM unit tests (app/src/test) — pure-Kotlin domain/ViewModel suite, all variants
./gradlew testMockDebugUnitTest # unit tests for the mock/debug variant only (faster)
./gradlew connectedMockDebugAndroidTest  # instrumented + Compose UI tests — needs a device/emulator
./gradlew lint                  # Android lint
./gradlew installMockDebug      # install the mock/debug build on a connected device/emulator
```

Run a single unit test class or method:

```bash
./gradlew test --tests "com.armstrongmobile.nrfthingy52android.RssiBucketTest"
./gradlew test --tests "*.ThingyEnvironmentTest.parsesTemperature"
```

Per the plan's test split: pure domain logic and ViewModel tests (with the fake transport) run as
JVM unit tests under `app/src/test/`; the fake-transport end-to-end suite and Compose UI tests
(`createAndroidComposeRule<MainActivity>()`) run as instrumented tests under `app/src/androidTest/`.

## Architecture (the big picture the plan prescribes)

Single-Activity Compose app, MVVM with `ViewModel` + `StateFlow` collected via
`collectAsStateWithLifecycle()`. The parts that span multiple files and are easy to get wrong:

- **BLE callbacks arrive off the main thread.** Unlike iOS's CoreBluetooth (which delivers on the
  main queue, letting the iOS models be `@MainActor`), Android's `BluetoothGattCallback`/
  `ScanCallback` fire on an internal Binder thread. Rule (plan §4.1): `ThingyGatt` (the
  `BluetoothGattCallback` impl) must never touch Compose state — it converts each callback into a
  plain event pushed onto a flow, and only the owning `ViewModel`'s `viewModelScope` collection
  point mutates the `StateFlow` Compose observes. This single collection point is the concurrency
  boundary that replaces iOS's `@MainActor` isolation; there is no compiler enforcement, so it must
  be respected by convention.

- **Fake-vs-real BLE transport seam.** `ThingyController` + `ThingyGattListener` interfaces are
  implemented by both a real transport and a `FakeThingyController`/`FakeBleScanner` test/demo double.
  The real implementation is backed by **Nordic's `no.nordicsemi.android:ble` / `ble-ktx`** library
  (decision resolved, plan §10.5), not raw `BluetoothGatt` — it provides the GATT operation queue,
  CCCD/notification handling, and coroutine `suspend` request semantics that the raw API makes you
  hand-roll (§4 points 7 & 9). This seam replaces the iOS app's CoreBluetoothMock and is the single
  biggest structural divergence from iOS. Build the fake **before** the real transport (Phase 3
  before Phase 4) so the scanner/detail/dashboard screens can be built and tested with no hardware.
  Selection between fake and real (plan §10.6) uses **product flavors + lightweight DI**: the
  `mock`/`prod` flavors expose `BuildConfig.USE_FAKE_TRANSPORT`, and a small composition-root DI seam
  reads it to inject the right `ThingyController`/`BleScanner` (no Hilt/Koin — manual/constructor DI
  in a single-module app).

- **Navigation passes MAC addresses, not objects.** Navigation Compose routes carry only primitive
  args, so `ThingyConnectionViewModel` receives a `deviceAddress: String` (via `SavedStateHandle`)
  and looks the peripheral up through a repository — it does not get an object reference the way the
  iOS `ThingyConnection(peripheral:)` does. The UI-layer `DiscoveredThingyUi` data class holds no
  `BluetoothDevice`/controller reference (plan §3, §6.1).

- **Domain layer is pure Kotlin, zero framework deps.** `ThingyEnvironment`/`ThingyMotion` objects
  (UUID constants + pure `parse*`/`encode*` functions), `EnvironmentReading`/`MotionReading` sealed
  interfaces, `TapDirection`/`ThingyOrientation`/`RssiBucket` enums — mirroring how the iOS
  equivalents are framework-free and unit-tested in isolation.

## Open decisions

Plan §10 flagged 10 decisions. Resolved so far: version triple (Kotlin 2.2.20 + Compose compiler
plugin on AGP 9.1.1), Java target (17), compileSdk (bumped to 37, diverging from the plan's 36 pin),
**BLE library — adopt Nordic `no.nordicsemi.android:ble`/`ble-ktx`** (not hand-rolled; wired in
Phase 3/4), and **fake/real transport selection — `mock`/`prod` flavors + lightweight manual DI**.
Still open: RSSI icon fidelity (redraw the custom bars vs. Material Symbols), Material Symbols vs.
classic Material Icons for the `aqi` glyph, and git-repo initialization. The plan states its
recommendation for each.

## Build order

Plan §11 defines 11 phases, each leaving the app building/working. **Phase 0 is done and verified**
(full `./gradlew build` green; installs and launches on the Pixel 9 emulator showing the placeholder).
Remaining:
Phase 1 theme/tokens (`ThingyTheme`/`Color.kt`) → Phase 2 pure domain layer → Phase 3 transport
interfaces + fake → Phase 4 real BLE transport → Phase 5 scanner screen → Phase 6 detail (LED/button)
→ Phase 7 sensor dashboards → Phase 8 fake-transport integration + UI tests → Phase 9 hardware
verification → Phase 10 docs. Follow this order; later phases depend on the fake-transport seam from
Phase 3.

Phase 10 calls for rewriting this CLAUDE.md once real code exists, to document the architecture
decisions actually made (especially the §10 resolutions) — replace this starter-state version at
that point rather than layering onto it.
