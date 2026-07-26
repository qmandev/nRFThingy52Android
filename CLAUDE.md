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

The starter has been converted to a Compose app with the Nordic theme in place and the whole project
builds; the parity app's screens (Phase 5 on) are still unwritten. What exists now:

- **Compose entry point + theme.** `MainActivity` is a `ComponentActivity` wrapping a
  `Text("Thingy52")` placeholder in **`ThingyTheme`**. The old View-based `activity_main.xml` and
  `AppCompatActivity` are gone. Phase 5 replaces the placeholder with the scanner screen + `NavHost`.
- **Theme package** (`ui/theme/`): `Color.kt` (`NordicColors` — all 12 brand colors ported verbatim
  from the iOS `UIColorExtension.swift`, verified against the source), `Theme.kt` (`ThingyTheme` with
  light/dark `ColorScheme`s, `nordicBlue` primary + `nordicRed` error in both, Material You dynamic
  color deliberately off to preserve branding), `Type.kt` (Material 3 default `Typography()` — the
  iOS app uses the system font with no custom scale). Rule for later phases: no hardcoded colors in
  screen code — pull from `MaterialTheme.colorScheme` or add a token to `NordicColors`.
- **Dependencies wired:** Compose BOM + Material 3, activity-compose, navigation-compose,
  lifecycle-viewmodel/runtime-compose, kotlinx-coroutines-android. The View-world `appcompat`/
  `constraintlayout` deps were dropped; `com.google.android.material` is kept only as the XML
  window-theme parent in `res/values/themes.xml`.
- **Product flavors `mock` / `prod`** (dimension `transport`) each expose
  `BuildConfig.USE_FAKE_TRANSPORT` (mock=true, prod=false) — the resolved answer to plan §10.6.
  They currently differ only by that flag; the composition root starts reading it in Phase 3+ once a
  transport exists. Build variants are `{mock,prod}{Debug,Release}`.
- **Domain layer** (`domain/`, Phase 2): pure-Kotlin `ThingyEnvironment`/`ThingyMotion` (UUID
  constants + `parse*`/`encode*`), `EnvironmentReading`/`MotionReading` sealed interfaces,
  `TapDirection`/`ThingyOrientation`/`RssiBucket` enums, and internal LE codec helpers — ported 1:1
  from the iOS source, zero Android deps (only `java.util.UUID` + `kotlin.math`). Unit-tested under
  `app/src/test/.../domain/` (12 tests, fixtures verbatim from the iOS `BLEModelTests.swift`).
- **Transport seam + both implementations** (`ble/`, Phase 3–4): `ThingyController` (exposing
  `events: SharedFlow<ThingyGattEvent>`) and `BleScanner`/`ThingyScanResult` interfaces, implemented
  by `ThingyGatt`/`AndroidBleScanner` (real, Nordic `ble-ktx`) and `FakeThingyController`/
  `FakeBleScanner`/`ThingyMocks` (fake). Plus `BluetoothStateObserver` for the adapter-off path.
- **Composition root** (`di/`, Phase 4): `AppContainer` reads `BuildConfig.USE_FAKE_TRANSPORT` and
  picks the fake or real scanner/repository; `ThingyRepository` resolves a MAC address to a
  controller (nav routes carry only primitives). Owned by `ThingyApplication`. Permission flow lives
  in `ui/permissions/BlePermissions.kt`.
- **Detail ViewModel** (`ui/detail/`, Phase 3): `ThingyConnectionViewModel` +
  `ThingyDetailUiState`/`ConnectionState` — the iOS `ThingyConnection` port. 16 unit tests
  (`ThingyConnectionViewModelTest` 10, `FakeThingyTransportTest` 6) run on the fake with no device.
- **Scanner screen** (`ui/scanner/`, Phase 5): `ScannerViewModel` (dedupe by address + 1 s row
  throttle, ported from iOS `handleDiscovery`), `ScannerScreen` (Nordic-blue `LargeTopAppBar`,
  scanning spinner, `ContentUnavailableView`-equivalent empty state), `ThingyRow`. `MainActivity`
  hosts the `NavHost` with `"scanner"` and `"detail/{deviceAddress}"`.
- **Detail screen** (`ui/detail/`, Phases 6–7): `ThingyDetailScreen` with all four sections — LED,
  Button, and the Environment/Motion dashboards gated on `hasEnvironmentData`/`hasMotionData` (any
  field non-null, matching iOS). `SettingsSection` is the header/Card/footer container replacing
  SwiftUI's inset-grouped `Section`; `SensorRow` renders one reading; `SensorFormat` holds the pure
  formatters. **Formatters use `Locale.ROOT`, not the device locale** — iOS's `String(format:)` is
  locale-independent, so a device-locale format would print `22,5 °C` in de-DE and diverge.
  `rememberHeavyImpactHaptic` fires on button press.
  `ThingyConnectionViewModel.factory(repository, unknownDeviceName)` reads `deviceAddress` from
  `SavedStateHandle`; an unresolvable address falls back to `UnavailableThingyController`, which
  reports disconnected rather than hanging on "Scanning...".
- **Strings + icons**: `res/values/strings.xml` holds all 29 keys from plan §8.2 with values verbatim
  from the iOS `Localizable.strings` (the other 15 locales land in Phase 8). Icons are **vendored
  vector drawables**, not a Material Icons dependency — `rssi_1..4` are hand-drawn four-tier bars;
  the rest (`ic_scanning`, `ic_lightbulb`, `ic_temperature`, …) are converted from the official
  google/material-design-icons SVGs. `androidx.compose.material.icons` is deliberately not a
  dependency (decoupled from material3; `material-icons-extended` is deprecated and huge).
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
  `ScanCallback` fire on an internal Binder thread. Rule (plan §4.1): the transport must never touch
  Compose state — it converts each callback into a plain `ThingyGattEvent` pushed onto
  `ThingyController.events`, and the `viewModelScope.launch { controller.events.collect(...) }` in
  `ThingyConnectionViewModel.init` is the **only** place `uiState` is mutated. That single collection
  point is the concurrency boundary replacing iOS's `@MainActor` isolation; there is no compiler
  enforcement, so don't mutate state anywhere else. This is why the seam is an events flow rather
  than the listener interface plan §3 names — see the §11 Phase 3 status note.

- **Fake-vs-real BLE transport seam.** The `ThingyController` + `BleScanner` interfaces are
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

Plan §11 defines 11 phases, each leaving the app building/working. **Phases 0–7 are done and
verified** (full `./gradlew build` green; 50 JVM unit tests; on the emulator the full UI works
end-to-end against the fake — scan, tap, LED round-trip, and both live dashboards with every format
checked). Remaining: Phase 8 fake-transport integration + Compose UI tests → Phase 9 hardware
verification → Phase 10 localization/docs. **No real GATT traffic has run yet** — scanning has been
exercised against the real API on the emulator's simulated adapter, but `ThingyGatt`'s
connect/notify/read pipeline is unverified until the Phase 9 hardware pass.

The mock build streams live demo readings (`ThingyMocks.startEnvironmentDemo`/`startMotionDemo`,
launched from `ThingyApplication` when `USE_FAKE_TRANSPORT` and not under instrumentation). Phase 8
tests should drive `ThingyMocks.controller` directly instead — the demos are suppressed under tests
precisely so they don't fight the test's own values.

Phase 10 calls for rewriting this CLAUDE.md once real code exists, to document the architecture
decisions actually made (especially the §10 resolutions) — replace this starter-state version at
that point rather than layering onto it.
