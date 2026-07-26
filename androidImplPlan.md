# Android Implementation Plan — nRFThingy52Android (Kotlin / Jetpack Compose)

*Regenerated 2026-07-24 (a prior version of this file was accidentally deleted and is being
reconstructed). Drafted by reading the complete iOS SwiftUI codebase at
`/Users/armstrongllc/Desktop/BLE/nRFThingy52` (main branch, iOS 17+, Swift 6 strict concurrency,
CoreBluetoothMock-integrated) and directly inspecting the target directory
`/Users/armstrongllc/Desktop/BLE/nRFThingy52Android`. This document is a planning artifact only —
it contains no app source code — mirroring the role `SwiftUIMigrationPlan.md` played for the iOS
SwiftUI migration. The implementing agent should treat every section as authoritative unless a
section explicitly flags a decision as open (§10).*

**Correction vs. an earlier draft of this plan:** the Android target directory is **not empty**.
It is a real Android Studio "Empty Views Activity" starter project (traditional XML/View-based,
not Compose) with working Gradle configuration, a launcher `MainActivity`, resources, and template
tests. §2 below documents its exact contents. Critically, **the starter project's Gradle config is
currently missing the Kotlin Android Gradle plugin** — only `com.android.application` is applied —
even though `MainActivity.kt` is a Kotlin file. As configured, `./gradlew build` will fail to
compile Kotlin sources. Fixing this is the first concrete action in Phase 0, not an open question.

---

## 1. Executive Summary

**What's being built:** a Kotlin + Jetpack Compose Android app that is a pixel-for-pixel-intent,
behavior-for-behavior parity companion to the existing iOS nRFThingy52 app, for the same physical
hardware target: a Nordic Thingy:52 Bluetooth LE development kit. The Android app scans for
nearby Thingy:52 devices, lists them with live RSSI signal-strength icons, connects on tap,
exposes an LED on/off toggle with write-then-read-back confirmation, streams the physical button's
PRESSED/RELEASED state with haptic feedback, and — once first readings arrive — shows an
Environment sensor dashboard (temperature/humidity/pressure/air-quality) and a Motion sensor
dashboard (orientation/steps/heading/tap), exactly as the iOS app does today.

**Why:** the iOS app is mature — mock-tested (28+ automated tests against a simulated Thingy:52),
documented, and partially hardware-verified — and the Android app should be its direct sibling:
same GATT profile, same UUIDs, same wire-format parsing, same UX flow, same visual brand, so a
user or reviewer moving between platforms sees the same product. It also becomes the natural home
for parity BLE mocking/testing infrastructure analogous to Nordic's CoreBluetoothMock, which the
iOS app already integrated as an SPM dependency.

**Target stack:** Kotlin, Jetpack Compose (Material 3), Jetpack Navigation Compose, `ViewModel` +
`StateFlow`, Android's native `BluetoothGatt`/`BluetoothGattCallback` BLE stack (no third-party BLE
library required for the core path — see §10.5 for the one open question on this). No Compose
Multiplatform, no KMP — a plain single-module Android app, matching the iOS app's "minimal
dependencies" philosophy as closely as Android's stricter permission/threading model allows (the
iOS app itself has exactly one dependency: CoreBluetoothMock, added deliberately for testing).

**Because the target directory has a real (if minimal) starter project**, Phase 0 is migration —
adding the missing Kotlin plugin, adding Compose, replacing the View-based `MainActivity`/
`activity_main.xml` — not greenfield scaffolding from a truly blank directory.

---

## 2. Source-of-Truth Inventory

### 2.1 iOS codebase (read in full)

- `nRFThingy52/CLAUDE.md`, `README.md`, `nRFThingy52BLEStatus.md`, `SwiftUIMigrationPlan.md`,
  `CoreBluetoothMockFeasibility.md`, `nordicSemi.md`
- `nRFThingy52/ThingyApp.swift`
- `nRFThingy52/Views/ScannerView.swift`, `ThingyRowView.swift`, `ThingyDetailView.swift`
- `nRFThingy52/Models/ScannerModel.swift`, `ThingyConnection.swift`, `ThingyPeripheral.swift`,
  `ThingyEnvironment.swift`, `ThingyMotion.swift`
- `nRFThingy52/CoreBluetoothTypeAliases.swift`, `MockThingy52.swift`
- `nRFThingy52/Utilities/StringExtension.swift`, `UIColorExtension.swift`, `ColorExtension.swift`
- `nRFThingy52/Utilities/en.lproj/Localizable.strings` (all 32 keys; confirmed all 16 locale
  directories present)
- `nRFThingy52/Assets.xcassets/*` (full asset inventory, §7.2)
- `nRFThingy52Tests/BLEModelTests.swift`, `ThingyIntegrationTests.swift`
- `nRFThingy52UITests/nRFThingy52UITests.swift`

### 2.2 Android target directory (actual current contents)

```
nRFThingy52Android/
├── build.gradle.kts                  # root: only com.android.application plugin declared
├── settings.gradle.kts               # rootProject.name = "nRFThingy52Android"; include(":app")
├── gradle.properties
├── gradlew, gradlew.bat
├── gradle/
│   ├── libs.versions.toml            # version catalog — see §2.3
│   └── wrapper/gradle-wrapper.properties   # Gradle 9.3.1, JVM toolchain 21
└── app/
    ├── build.gradle.kts              # see §2.3 for exact config
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml   # no permissions declared yet; single MainActivity/LAUNCHER
        │   ├── java/com/armstrongmobile/nrfthingy52android/MainActivity.kt
        │   └── res/
        │       ├── layout/activity_main.xml       # ConstraintLayout, View-based
        │       ├── values/{colors,strings,themes}.xml
        │       ├── values-night/themes.xml
        │       ├── mipmap-{h,m,x,xx,xxx}dpi/ic_launcher{,_round}.webp
        │       ├── mipmap-anydpi-v26/ic_launcher{,_round}.xml   # adaptive icon
        │       ├── drawable/ic_launcher_{background,foreground}.xml
        │       └── xml/{backup_rules,data_extraction_rules}.xml
        ├── test/java/.../ExampleUnitTest.kt         # template stub, 1 trivial test
        └── androidTest/java/.../ExampleInstrumentedTest.kt   # template stub, 1 trivial test
```

No `.git` repository exists yet at `nRFThingy52Android/` (confirmed via `git status` — "not a git
repository"). This plan file and all future source should still be organized as if the directory
will become its own repo; do not assume it will be nested inside the iOS repo.

### 2.3 Exact current Gradle configuration (ground truth — use these values, do not re-derive)

```kotlin
// gradle/libs.versions.toml [versions]
agp = "9.1.1"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"          // androidx.test.ext:junit
espressoCore = "3.5.1"
appcompat = "1.6.1"
material = "1.10.0"             // com.google.android.material (View-based Material, not Compose)
activity = "1.8.0"
constraintlayout = "2.1.4"

// [plugins]
android-application = "com.android.application" (agp)
// NOTE: no Kotlin plugin entry exists. Must be added — see Phase 0.

// app/build.gradle.kts (defaultConfig)
namespace = "com.armstrongmobile.nrfthingy52android"
applicationId = "com.armstrongmobile.nrfthingy52android"
compileSdk = 36 (release 36, minorApiLevel 1)
minSdk = 24
targetSdk = 36
versionCode = 1
versionName = "1.0"
compileOptions: sourceCompatibility/targetCompatibility = JavaVersion.VERSION_11
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

// gradle/wrapper/gradle-wrapper.properties
distributionUrl = gradle-9.3.1-bin.zip
toolchainVersion = 21   // JVM toolchain the wrapper resolves via foojay
```

**Use these exact values** (minSdk 24, targetSdk/compileSdk 36, AGP 9.1.1, Gradle 9.3.1) rather
than re-deciding them — they are already fixed by the starter project and there is no reason to
diverge. minSdk 24 (Android 7.0, 2016+) comfortably covers the BLE APIs this app needs (scan
filters, `BluetoothGattCallback`, runtime permissions groundwork) — see §4.4 for the two
permission-model branches minSdk 24 must still support (pre-Android 12 vs. Android 12+).

> **Superseded during Phase 0:** `compileSdk` was bumped **36 → 37** (and the AndroidX dependencies
> restored to their current releases, which require `minCompileSdk 37`). `minSdk 24` and
> `targetSdk 36` are unchanged. See the §10 Decision log entry "§10.4 / §2.3" for the full rationale.

**Two concrete config corrections needed in Phase 0** (not open questions — just fix these):
1. Add the Kotlin Android Gradle plugin (`org.jetbrains.kotlin.android`) to both the root and
   `app` `build.gradle.kts`, pinned to a version compatible with AGP 9.1.1 and the Compose
   compiler (Kotlin 2.0+ recommended for the Compose Compiler Gradle plugin path — verify current
   compatible triple via the Kotlin/Compose compatibility map at implementation time, since exact
   patch versions age quickly).
2. `sourceCompatibility`/`targetCompatibility` are set to Java 11 while the Gradle wrapper's JVM
   toolchain is 21 — raise `compileOptions` (and add `kotlinOptions { jvmTarget = "17" }` or newer)
   to at least Java 17 to match modern AGP/Compose expectations; Java 11 is unnecessarily
   conservative for a from-scratch 2026 app and may cause friction with newer AndroidX releases.

---

## 3. Architecture Mapping Table

| iOS (Swift/SwiftUI) | Android/Kotlin equivalent | Justification |
|---|---|---|
| `ThingyApp.swift` (`@main App`, `WindowGroup { ScannerView() }.tint(.nordicBlue)`) | `ThingyApplication : Application` (minimal — process init, seeds the fake transport in debug builds) + `MainActivity : ComponentActivity` calling `setContent { ThingyTheme { ScannerScreen(navController) } } }` | Compose apps have no `App`-protocol equivalent; `ComponentActivity.setContent` is the standard single-activity entry point. `.tint()` maps to the Material 3 `colorScheme.primary` set in `ThingyTheme`. |
| `ScannerView.swift` (`View` struct) | `@Composable fun ScannerScreen(viewModel: ScannerViewModel, onDeviceSelected: (String) -> Unit)` | Direct SwiftUI `View` → `@Composable` function mapping; both are declarative, state-driven, side-effect-free render functions. |
| `ThingyRowView.swift` | `@Composable fun ThingyRow(thingy: DiscoveredThingyUi, onClick: () -> Unit)` | Same. |
| `ThingyDetailView.swift` | `@Composable fun ThingyDetailScreen(uiState: ThingyDetailUiState, onLedToggle: (Boolean) -> Unit)` | Same; SwiftUI `Toggle`/`List`/`Section` → Compose `Switch`/`LazyColumn`/grouped `Card`s (Material 3 has no built-in "inset grouped list with header/footer" primitive — see §6.2). |
| `ScannerModel` (`@MainActor @Observable`, owns `CBCentralManager`, sole central delegate, dedupe+throttle) | `ScannerViewModel : ViewModel()`, exposes `StateFlow<ScannerUiState>`; owns a `BleScanner` wrapper around `BluetoothLeScanner` | `@Observable` → `StateFlow` collected via `collectAsStateWithLifecycle()` is the idiomatic Compose replacement for "observable model read by the view." `ViewModel` survives configuration changes the way a `@State`-held `@Observable` object survives SwiftUI view identity. |
| `ThingyConnection` (`@MainActor @Observable`, wraps `ThingyControlling`, republishes `ThingyDelegate` callbacks as observable state) | `ThingyConnectionViewModel : ViewModel()`, constructed with a `deviceAddress: String` (via `SavedStateHandle` from nav args) and a `ThingyRepository`; exposes `StateFlow<ThingyDetailUiState>` | Same republish-callbacks-as-state role. Key difference: Compose Navigation routes pass **primitive arguments** (String/Int), not object references, so the ViewModel looks the peripheral up by MAC address from a repository rather than receiving it as an init parameter the way `ThingyConnection(peripheral:)` does — see §6.1 "passing the selected device" callout. |
| `ThingyPeripheral` (`NSObject`, `CBPeripheralDelegate` + `CBCentralManagerDelegate`, connect→discover→notify→read state machine) | `ThingyGatt` class implementing `BluetoothGattCallback`, holding one `BluetoothGatt` connection; drives the same discover→notify→read pipeline | Both are "the BLE state machine for one physical connection." Android's `BluetoothGattCallback` is delivered per-`BluetoothGatt` instance (no shared delegate to fight over — contrast with §4 point 6), simpler than CoreBluetooth's single shared `CBCentralManagerDelegate`. |
| `ThingyDelegate` protocol (`thingyDidConnect`, `thingyDidDisconnect`, `buttonStateChanged`, `ledStateChanged`, `environmentDidUpdate`, `motionDidUpdate`; default no-op via protocol extension) | `interface ThingyGattListener` with default (no-op) method bodies directly in the interface (Kotlin interfaces support default implementations — no extension trick needed): `fun onThingyConnected(ledSupported: Boolean, buttonSupported: Boolean) {}`, `fun onThingyDisconnected() {}`, `fun onButtonStateChanged(isPressed: Boolean) {}`, `fun onLedStateChanged(isOn: Boolean) {}`, `fun onEnvironmentUpdate(reading: EnvironmentReading) {}`, `fun onMotionUpdate(reading: MotionReading) {}` | Kotlin interfaces with default methods are the exact language-level equivalent of a Swift protocol + protocol extension providing defaults. |
| `ThingyControlling` protocol (seam so `ThingyConnection` is unit-testable without a real `CBPeripheral`) | `interface ThingyController` with `val advertisedName: String?`, `val isConnected: Boolean`, `var listener: ThingyGattListener?`, `fun connect()`, `fun disconnect()`, `fun turnOnLed()`, `fun turnOffLed()`; implemented by both `ThingyGatt` (real) and `FakeThingyGatt` (test double) | Same seam, same reason: `BluetoothGatt`/`BluetoothDevice` cannot be meaningfully constructed or driven in a local JVM unit test, exactly like `CBPeripheral`. |
| `ScannerModel` + `ThingyPeripheral` split of "who owns the central manager" | `ScannerViewModel` owns the `BluetoothLeScanner`; each `ThingyGatt` owns its own `BluetoothGatt` returned from `device.connectGatt(...)` | See §4 point 6 — Android's per-connection callback object removes the need for iOS's "sole delegate + forwarding" workaround, but the plan preserves equivalent *behavior* (only one active connection attempt at a time, coordinated by `ScannerViewModel`). |
| `RSSIBucket` enum (`weakest`/`weak`/`medium`/`strong`, thresholds `<-80`/`<-60`/`<-40`/else) | `enum class RssiBucket { WEAKEST, WEAK, MEDIUM, STRONG }` with a `fun of(rssi: Int): RssiBucket` companion factory, identical thresholds | Direct port; pure function, trivially unit-testable in both languages. |
| `DiscoveredThingy` struct (`Identifiable`) | `data class DiscoveredThingyUi(val address: String, val name: String, val rssiBucket: RssiBucket, val lastUpdated: Long)` | Compose `LazyColumn` items need a stable `key = { it.address }` the way SwiftUI `List`/`ForEach` need `Identifiable`; the Bluetooth MAC address (or a scan-session-local device ID on Android 8+ if MAC randomization is in play) is the natural key, mirroring `CBPeripheral.identifier`. Note the UI-layer struct holds no direct reference to the `BluetoothDevice`/controller — see §6.1. |
| `ThingyEnvironment.swift` (UUIDs + pure parse/encode) | `object ThingyEnvironment` with the same UUID constants and pure `fun parseTemperature(data: ByteArray): EnvironmentReading?` etc. | 1:1 port; exact byte layouts in §5. |
| `ThingyMotion.swift` | `object ThingyMotion`, same pattern | Same. |
| `EnvironmentReading` enum (`.temperature`, `.humidity`, `.pressure`, `.airQuality`) | `sealed interface EnvironmentReading` with `data class Temperature(val celsius: Double)`, `data class Humidity(val percent: Int)`, `data class Pressure(val hPa: Double)`, `data class AirQuality(val eco2: Int, val tvoc: Int)` | Swift `enum` with associated values → Kotlin `sealed interface` is the standard mapping; both give exhaustive `when`/`switch`. |
| `MotionReading` enum (`.tap`, `.orientation`, `.stepCount`, `.heading`) | `sealed interface MotionReading` with `Tap(direction: TapDirection, count: Int)`, `Orientation(value: ThingyOrientation)`, `StepCount(steps: Int, durationSeconds: Double)`, `Heading(degrees: Double)` data classes | Same. |
| `TapDirection` / `ThingyOrientation` (`UInt8`-backed enums with `.label`) | `enum class TapDirection(val rawValue: Int) { X_POSITIVE(1), X_NEGATIVE(2), Y_POSITIVE(3), Y_NEGATIVE(4), Z_POSITIVE(5), Z_NEGATIVE(6) }` / `enum class ThingyOrientation(val rawValue: Int) { PORTRAIT(0), LANDSCAPE(1), REVERSE_PORTRAIT(2), REVERSE_LANDSCAPE(3) }`, each with a `val label: String` (or a `@StringRes` lookup — see §8) | Direct port; Kotlin enums carry a backing raw value and computed property identically to Swift's `RawRepresentable`. |
| `CoreBluetoothTypeAliases.swift` + `MockThingy52.swift` (CoreBluetoothMock integration: `CBCentralManagerFactory.instance` returns native on device / mock on simulator, transparently) | No direct 1:1 — there is no Android library offering "same public API surface, swap the manager based on environment" the way CoreBluetoothMock does for CoreBluetooth. Replaced by a hand-rolled `ThingyController`/`BleScanner` interface seam (§3 above) with a real implementation and a `FakeThingyController`/`FakeBleScanner` test/demo implementation selected by build variant or DI — see §9 Testing Strategy | This is the single biggest structural divergence between the two codebases; the *outcome* (build and test the full app without hardware) is preserved, but the *mechanism* is necessarily different since Android has no equivalent drop-in mock framework at the `BluetoothGatt` layer. |
| `.localized` (`String` extension over `NSLocalizedString`, used at non-`Text` call sites like inside `ThingyPeripheral`/`ScannerModel`) | Not needed as a Kotlin extension inside Composables — `stringResource(R.string.key)` resolves the active locale automatically, same as SwiftUI `Text("KEY")` auto-resolving through `Localizable.strings`. For non-Composable call sites (e.g., a fallback name built inside `ThingyGatt`, which has no `Context`), pass a `Context`/`Application` reference or a small resolver interface into the class at construction, since Android has no global `Bundle.main`-style locale accessor | Both frameworks build locale resolution into the UI-text primitive; the gap is only for code that resolves a string *outside* the Compose tree. |
| `UIColorExtension.swift` (Nordic `UIColor` palette + `dynamicColor(light:dark:)` + hex helpers) | `object NordicColors` in `Color.kt` with the same named `androidx.compose.ui.graphics.Color` constants; light/dark handled by two `ColorScheme`s (`LightColorScheme`/`DarkColorScheme`) selected via `isSystemInDarkTheme()` in `ThingyTheme.kt` | Compose's `MaterialTheme` already has a first-class light/dark-scheme mechanism; no need to port `dynamicColor(light:dark:)` as a general helper. Keep a hex-parsing helper only if a non-Compose code path needs one (unlikely — flag as YAGNI unless a requirement surfaces). |
| `ColorExtension.swift` (SwiftUI `Color` bridge over the same palette) | Folded into `NordicColors` directly — Android has no "UIKit vs SwiftUI color type" split, so no second bridging file is needed | Simplification, not a loss of fidelity. |
| `Localizable.strings` × 16 locales | `res/values/strings.xml` + 15 `res/values-<qualifier>/strings.xml` | See §8 for the exact qualifier mapping per locale. |
| `Assets.xcassets` (`rssi_1`..`rssi_4`, `ic_lightbulb_outline_48pt`, `ic_radio_button_checked`, `scanning`, `splashscreen`, `AppIcon`) | `res/drawable/rssi_1.xml`..`rssi_4.xml` (vector drawables) + Material Icons for the lightbulb/radio-button/scanning/thermometer/humidity/gauge/aqi/rotate/walk/compass/tap glyphs (§7.3 substitution table) + the existing `mipmap-anydpi-v26` adaptive launcher icon (already present in the starter — restyle to Nordic branding, don't recreate the plumbing) + `androidx.core:core-splashscreen` for the splash | See §7.3 for the exact icon substitution table (SF Symbol / custom asset → Material Icons name). |
| `nRFThingy52Tests/BLEModelTests.swift`, `ThingyIntegrationTests.swift` | JVM unit tests under `app/src/test/java/...`: `RssiBucketTest`, `ScannerViewModelHelperTest`, `ThingyEnvironmentTest`, `ThingyMotionTest`, `ThingyConnectionViewModelTest` + instrumented tests under `app/src/androidTest/java/...` for the fake-transport end-to-end suite | See §9.2 for a full test-by-test mapping. |
| `nRFThingy52UITests/nRFThingy52UITests.swift` (`testSensorDashboardsShow`, `testLaunchPerformance`) | Compose UI tests under `app/src/androidTest/java/...` using `createAndroidComposeRule<MainActivity>()` | See §9.2. |

---

## 4. BLE Layer Plan

Android's Bluetooth LE API (`android.bluetooth.le.*`, `android.bluetooth.BluetoothGatt*`) differs
from CoreBluetooth in ways that are **not** cosmetic — they change where threading, permission, and
lifecycle bugs can hide. Each iOS behavior the app relies on is mapped below to its Android
treatment.

1. **Threading.** CoreBluetooth delegate callbacks land on the queue passed at
   `CBCentralManager(delegate:queue:)` creation — the iOS app passes `queue: nil` (main queue),
   which is why `ThingyPeripheral`/`ScannerModel` can be `@MainActor` and treat every delegate
   callback as already on the right thread (documented explicitly in `CLAUDE.md`'s "Concurrency
   pattern" section). Android's `BluetoothGattCallback` and `ScanCallback` methods are invoked on
   an **internal Binder thread**, not the main thread, with no equivalent "deliver on this queue"
   constructor parameter pre-API 33 (`BluetoothGatt.connectGatt` gained a `Handler` overload in
   API 33; below that, callbacks always land off-main). **Every callback body must dispatch to the
   main thread** (e.g., wrap `ViewModel` state mutation in `withContext(Dispatchers.Main.immediate)`
   or use a `MutableStateFlow` updated via `viewModelScope.launch(Dispatchers.Main)`), or use
   Kotlin coroutines' `callbackFlow` to bridge the callback into a cold `Flow` that collectors
   consume with proper dispatch. Do not assume main-thread delivery the way the iOS code does.

2. **MTU negotiation.** CoreBluetooth negotiates MTU transparently; iOS code never touches it. On
   Android, the default ATT MTU (23 bytes) is often too small once multiple services/characteristics
   are involved (though every individual characteristic payload here is ≤8 bytes, so MTU 23 is
   actually sufficient for this specific app — the LED/button/environment/motion payloads are all
   1–8 bytes). Still, call `gatt.requestMtu(185)` (or similar) after connecting as a defensive
   default, and only proceed with service discovery after `onMtuChanged` fires (or after a timeout
   if the peripheral doesn't support the request) to match real-world robustness expectations
   noted in `nRFThingy52BLEStatus.md`.

3. **Connection priority.** CoreBluetooth has no direct equivalent exposed to app code. Call
   `gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)` after connecting;
   `CONNECTION_PRIORITY_HIGH` is unnecessary for this app's low-frequency notification traffic
   (button/tap events, environment readings every few seconds) and would waste battery.

4. **Runtime permissions.** iOS's only gate is the `NSBluetoothAlwaysUsageDescription` Info.plist
   entry plus a one-time system permission prompt, requested implicitly on first
   `CBCentralManager` creation (the iOS app deliberately creates the manager lazily in
   `ScannerModel.startScan()` so the prompt's timing matches first user interaction — see
   `CLAUDE.md`). Android's model is materially different across the minSdk 24–targetSdk 36 range
   this app supports:
   - **API 24–30:** scanning for BLE devices requires `ACCESS_FINE_LOCATION` (a **runtime**
     dangerous permission) plus `BLUETOOTH`/`BLUETOOTH_ADMIN` (normal, manifest-only,
     `maxSdkVersion="30"`).
   - **API 31+ (Android 12, "S"):** new runtime permissions `BLUETOOTH_SCAN` and
     `BLUETOOTH_CONNECT` replace the location requirement for BLE-only apps *if* the manifest
     declares `<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
     android:usesPermissionFlags="neverForLocation" />` (this app does not derive physical
     location from scan results, so `neverForLocation` is correct and avoids also requiring
     location permission/services to be enabled).
   - Manifest must declare **both** permission sets, each scoped with `android:maxSdkVersion`/
     omitted as appropriate, and the app must runtime-request the correct set based on
     `Build.VERSION.SDK_INT` at the point equivalent to iOS's lazy-manager-creation trick: request
     permissions when the user first opens the scanner screen (`LaunchedEffect` + Activity Result
     API `rememberLauncherForActivityResult(RequestMultiplePermissions())`), not at app launch,
     matching the iOS timing intent even though the mechanism differs completely.
   - `VIBRATE` permission is normal/manifest-only, needed for the button-press haptic (§6.2).

5. **Foreground service for background scanning.** iOS's background BLE story (documented in
   `nordicSemi.md`) relies on `bluetooth-central` background mode + service-UUID-filtered scans.
   Android has no direct equivalent "background mode" flag — scanning from a background process is
   heavily throttled/restricted starting Android 8 (Oreo) and effectively requires a foreground
   service with a persistent notification to scan/stay connected reliably while the app is
   backgrounded. **This app's current feature set does not need background operation** (the iOS
   app is foreground-only in its documented behavior — connect on `.onAppear`, disconnect on
   `.onDisappear`), so **do not build a foreground service in the initial port** — flag it as a
   deliberately deferred feature (§10.9), matching the current iOS scope exactly.

6. **Central-manager ownership vs. per-connection callbacks.** iOS's `ScannerModel` must remain
   the *sole* `CBCentralManagerDelegate` for the app's lifetime and manually forward
   connect/disconnect/failure events to whichever `ThingyPeripheral` is currently selected,
   because CoreBluetooth only allows one delegate per central manager instance (documented at
   length in `CLAUDE.md`/`nRFThingy52BLEStatus.md` as a deliberate architectural choice). Android's
   `BluetoothDevice.connectGatt(context, autoConnect, callback)` gives **each connection its own
   callback object** — there is no shared-delegate constraint to route around. This means
   `ThingyGatt` can simply own its own `BluetoothGattCallback` instance directly; `ScannerViewModel`
   does **not** need an iOS-style forwarding mechanism. This is a genuine simplification versus the
   iOS architecture, not a gap — call it out explicitly in code comments so a future reader doesn't
   assume a forwarding layer is missing.

7. **GATT operation serialization.** CoreBluetooth silently queues sequential operations
   (`discoverServices` → `discoverCharacteristics` → `setNotifyValue` → `readValue`) issued from app
   code. **Android's `BluetoothGatt` does not queue** — issuing a second GATT operation before the
   first one's callback fires silently drops or corrupts the first operation on many OEM BLE
   stacks. `ThingyGatt` **must** implement its own operation queue: a `Channel`/`ArrayDeque` of
   pending operations, draining one at a time only after the prior operation's corresponding
   callback (`onServicesDiscovered`, `onCharacteristicWrite`, `onDescriptorWrite` for CCCD
   enables, `onCharacteristicRead`) fires. This is the single most common source of flaky Android
   BLE bugs and has no iOS counterpart to copy from directly — implement it from Android BLE
   best-practice guidance (e.g., the pattern used by Nordic's own `no.nordicsemi.android:ble`
   library, which exists specifically to solve this — see §10.5).

8. **Scanning and RSSI/dedup/throttle.** iOS scans with
   `scanForPeripherals(withServices:[nordicThingyServiceUUID], options:[CBCentralManagerScanOptionAllowDuplicatesKey: true])`
   and `ScannerModel` dedupes by `CBPeripheral.identifier`, throttling visible row updates to once
   per second (`ScannerModel.shouldRefreshRow`, unit-tested). Android equivalent: a
   `ScanFilter.Builder().setServiceUuid(ParcelUuid(THINGY_UI_SERVICE_UUID)).build()` passed to
   `BluetoothLeScanner.startScan(filters, settings, callback)` with
   `ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY).build()`; dedupe by
   `result.device.address` (the Bluetooth MAC, Android's `CBPeripheral.identifier` analogue) with
   the identical 1-second throttle logic ported verbatim as a pure function (`shouldRefreshRow`,
   directly portable, no Android API dependency — see §9.2 unit test mapping).

9. **Connect → discover → notify → read pipeline.** Port `ThingyPeripheral`'s exact sequence
   (see §5 for the literal UUID/byte-format reference) as `ThingyGatt`:
   - `connectGatt()` → `onConnectionStateChange(STATE_CONNECTED)` → `gatt.discoverServices()`
   - `onServicesDiscovered()` → for the UI service, enumerate LED (write/read) and Button
     (notify/read) characteristics; for Environment/Motion services, enumerate their four
     characteristics each (all notify-only on iOS's model — port identically)
   - For each notify characteristic: `gatt.setCharacteristicNotification(char, true)` **and**
     separately write `BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE` to its Client
     Characteristic Configuration Descriptor (CCCD, UUID `00002902-0000-1000-8000-00805f9b34fb`)
     via `gatt.writeDescriptor(descriptor)` — **this two-step dance has no CoreBluetooth
     equivalent**; iOS's `setNotifyValue(true, for:)` does both steps internally. Missing the CCCD
     write is the single most common reason Android BLE notifications silently never arrive.
   - Route `onCharacteristicChanged`/`onCharacteristicRead` values through the same UUID-switch
     parsing dispatch `ThingyPeripheral.environmentReading(for:data:)`/`motionReading(for:data:)`
     use, calling the appropriate `ThingyEnvironment.parse*`/`ThingyMotion.parse*` Kotlin port.

10. **Optimistic LED write + read-back confirmation.** iOS's `ThingyConnection.setLED(on:)` sets
    `ledIsOn` immediately (optimistic UI update) then calls
    `peripheral.turnOnLED()/turnOffLED()`; `ThingyPeripheral.writeLEDCharacteristic` writes with
    `.withResponse` when available, and `peripheral(_:didWriteValueFor:error:)` triggers a
    `readLEDValue()` to fetch the authoritative state, which flows back through
    `ledStateChanged(isOn:)` and may *correct* the optimistic value if the device rejected/altered
    it. Port identically: `ThingyConnectionViewModel.setLed(on: Boolean)` optimistically updates
    `StateFlow`, `ThingyGatt.writeLedCharacteristic()` calls
    `gatt.writeCharacteristic(char, byteArrayOf(if (on) 1 else 0), WRITE_TYPE_DEFAULT)` (the
    Android analogue of `.withResponse`), and `onCharacteristicWrite` callback triggers a
    `gatt.readCharacteristic(ledChar)` read-back, whose `onCharacteristicRead` result is the
    corrected/confirmed value published to `StateFlow`.

11. **Disconnect / connection-failure / power-off handling.** iOS surfaces three distinct paths
    into the same `thingyDidDisconnect()` → `ThingyConnection.state = .disconnected` outcome:
    on-demand disconnect (`ThingyDetailView.onDisappear`), `centralManager(_:didFailToConnect:)`,
    and `centralManagerDidUpdateState` reporting `!= .poweredOn` (Bluetooth turned off). Port all
    three: `ThingyDetailScreen`'s `DisposableEffect`/`onDispose` calls `viewModel.disconnect()`
    (→ `gatt.disconnect()` + `gatt.close()` — Android additionally **requires** `close()` to
    release the underlying GATT client resource, which CoreBluetooth handles automatically and
    has no iOS-side call to port); `onConnectionStateChange` with a non-success `status` maps to
    the failure path; a registered `BroadcastReceiver` for
    `BluetoothAdapter.ACTION_STATE_CHANGED` (Android's equivalent of
    `centralManagerDidUpdateState`) maps power-off to the same disconnected state.

---

## 5. Exact UUIDs and Wire Formats (reference table — copy verbatim into Kotlin `object`s)

All UUIDs are the Thingy:52 base UUID family `EF68xxxx-9B35-4933-9B10-52FFA9740042`. Values below
are extracted verbatim from `ThingyPeripheral.swift`, `ThingyEnvironment.swift`, and
`ThingyMotion.swift` as they exist in the iOS codebase today (including the Motion dashboard added
2026-07-22).

### 5.1 User Interface Service (`ThingyPeripheral.swift`)

```
Service:              EF680300-9B35-4933-9B10-52FFA9740042
LED characteristic:    EF680301-9B35-4933-9B10-52FFA9740042   properties: write (or write-without-response), read
Button characteristic: EF680302-9B35-4933-9B10-52FFA9740042   properties: notify, read
```

- **LED wire format**: 1 byte. `0x00` = off, `0x01` = on. Write path: `turnOnLED()` writes
  `Data([0x1])`, `turnOffLED()` writes `Data([0x0])`. Read/notify path: `value[0] == 0x1` → on.
- **Button wire format**: 1 byte. `0x00` = released, `0x01` = pressed. `value[0] == 0x1` → pressed.

### 5.2 Environment Service (`ThingyEnvironment.swift`)

```
Service:                    EF680200-9B35-4933-9B10-52FFA9740042
Temperature characteristic: EF680201-9B35-4933-9B10-52FFA9740042   notify
Pressure characteristic:    EF680202-9B35-4933-9B10-52FFA9740042   notify
Humidity characteristic:    EF680203-9B35-4933-9B10-52FFA9740042   notify
Air Quality characteristic: EF680204-9B35-4933-9B10-52FFA9740042   notify
```

| Characteristic | Byte layout | Parse formula | Kotlin port notes |
|---|---|---|---|
| Temperature | byte 0: **int8** integer part (signed); byte 1: **uint8** decimal hundredths (0–99) | `integer = Int8(byte0)`; `decimal = UInt8(byte1)`; `sign = integer < 0 ? -1 : 1`; `celsius = integer + sign * decimal / 100.0`. Min 2 bytes, else `null`. | Kotlin `Byte` is signed exactly like Swift `Int8` — `val integer = data[0].toInt()` needs no bit-pattern dance. `val decimal = data[1].toInt() and 0xFF`. Same sign logic. |
| Pressure | bytes 0–3: **int32 little-endian** integer hPa; byte 4: **uint8** decimal hundredths | `integer = Int32(littleEndian: bytes[0..<4])`; `decimal = bytes[4]`; `hPa = integer + decimal / 100.0`. Min 5 bytes. | `ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int`, then `data[4].toInt() and 0xFF`. |
| Humidity | byte 0: **uint8** %RH | `percent = Int(byte0)`. Min 1 byte. | `data[0].toInt() and 0xFF`. |
| Air Quality | bytes 0–1: **uint16 little-endian** eCO2 (ppm); bytes 2–3: **uint16 little-endian** TVOC (ppb) | `eco2 = bytes[0] \| (bytes[1] << 8)`; `tvoc = bytes[2] \| (bytes[3] << 8)`. Min 4 bytes. | `val eco2 = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)`; same for tvoc. |

Encoding (mirror `encodeTemperature`/`encodePressure`/`encodeHumidity`/`encodeAirQuality` — needed
for the fake transport, §9.1):
- `encodeTemperature(celsius)`: `integer = celsius.toInt()` (round toward zero, clamped to Byte
  range); `decimal = ((abs(celsius) * 100).roundToInt() % 100)` clamped to 0–255; bytes =
  `byteArrayOf(integer.toByte(), decimal.toByte())`.
- `encodePressure(hPa)`: `integer = hPa.toInt()` (round toward zero) as little-endian Int32 (4
  bytes) + `decimal = ((hPa * 100).roundToInt() % 100)` as 1 trailing byte (5 bytes total).
- `encodeHumidity(percent)`: 1 byte, clamped 0–255.
- `encodeAirQuality(eco2, tvoc)`: 4 bytes, little-endian uint16 pair.

### 5.3 Motion Service (`ThingyMotion.swift`)

```
Service:                     EF680400-9B35-4933-9B10-52FFA9740042
Tap characteristic:          EF680402-9B35-4933-9B10-52FFA9740042   notify
Orientation characteristic:  EF680403-9B35-4933-9B10-52FFA9740042   notify
Step Counter characteristic: EF680405-9B35-4933-9B10-52FFA9740042   notify
Heading characteristic:      EF680409-9B35-4933-9B10-52FFA9740042   notify
```

*(Note the UUID suffix gap — `0402`, `0403`, `0405`, `0409` — matches the iOS source exactly.
`0401`/`0404`/`0406`–`0408` correspond to Quaternion/Euler/rotation-matrix/raw-accelerometer
characteristics the iOS app deliberately deferred as multi-field float structs of lower dashboard
value relative to parsing complexity. Port the same deferral — do not implement those in the
initial Android pass; flag as future scope in §10.9.)*

| Characteristic | Byte layout | Parse formula | Kotlin port notes |
|---|---|---|---|
| Tap | byte 0: direction enum `0x01`–`0x06`; byte 1: **uint8** tap count | `direction = TapDirection(rawValue: byte0)` (nil if 0 or >6); `count = Int(byte1)`. Min 2 bytes. | `TapDirection.entries.firstOrNull { it.rawValue == (data[0].toInt() and 0xFF) }`. |
| Orientation | byte 0: enum `0x00`–`0x03` | `orientation = ThingyOrientation(rawValue: byte0)`. Min 1 byte. | Same enum lookup pattern. |
| Step Counter | bytes 0–3: **uint32 little-endian** step count; bytes 4–7: **uint32 little-endian** duration in milliseconds | `steps = UInt32(littleEndian: bytes[0..<4])`; `millis = UInt32(littleEndian: bytes[4..<8])`; `duration = millis / 1000.0` seconds. Min 8 bytes. | `ByteBuffer.wrap(data,0,4).order(LITTLE_ENDIAN).int` reinterpreted as unsigned (`.toLong() and 0xFFFFFFFFL` if values could exceed `Int.MAX_VALUE`, unlikely for a step counter but be exact); `duration = millisLong / 1000.0`. |
| Heading | bytes 0–3: **int32 little-endian**, **16Q16 fixed-point** degrees | `raw = Int32(littleEndian: bytes[0..<4])`; `degrees = Double(raw) / 65536.0`. Min 4 bytes. | `ByteBuffer.wrap(data,0,4).order(LITTLE_ENDIAN).int`, then `raw / 65536.0`. |

Enum raw values (port verbatim):

```
TapDirection:      xPositive=1, xNegative=2, yPositive=3, yNegative=4, zPositive=5, zNegative=6
                   labels: "X+", "X−", "Y+", "Y−", "Z+", "Z−"  (minus sign is U+2212, not ASCII hyphen — copy exactly)
ThingyOrientation: portrait=0, landscape=1, reversePortrait=2, reverseLandscape=3
                   labels: "Portrait", "Landscape", "Portrait (upside down)", "Landscape (upside down)"
```

Encoding (mirror `encodeTap`/`encodeOrientation`/`encodeStepCount`/`encodeHeading` for the fake
transport, §9.1):
- `encodeTap(direction, count)`: `byteArrayOf(direction.rawValue.toByte(), count.coerceIn(0,255).toByte())`.
- `encodeOrientation(orientation)`: `byteArrayOf(orientation.rawValue.toByte())`.
- `encodeStepCount(steps, durationSeconds)`: little-endian uint32 steps + little-endian uint32
  `(durationSeconds * 1000).toLong()` millis (8 bytes).
- `encodeHeading(degrees)`: little-endian int32 of `(degrees * 65536.0).roundToInt()` (4 bytes).

---

## 6. Screen-by-Screen UI Spec

### 6.1 Scanner screen (`ScannerScreen`, replaces `ScannerView`)

- **Structure**: `Scaffold` with a `TopAppBar` (title `"Thingy52"`, large-title style via
  `LargeTopAppBar`/`MediumTopAppBar` from Material 3 to echo SwiftUI's `.navigationTitle` default
  large-title behavior) containing a trailing `CircularProgressIndicator` shown only while
  `uiState.isScanning` is true (iOS's `ProgressView()` in `ToolbarItem(placement: .topBarTrailing)`).
- **Body**: a `LazyColumn` with a single section labeled `"Nearby Devices"` (localized,
  `stringResource(R.string.nearby_devices)`) shown only when `uiState.discovered.isNotEmpty()` —
  mirrors iOS's `if !scanner.discovered.isEmpty { Section("Nearby Devices") { ... } }`. Each row is
  a clickable `ThingyRow` composable (name + RSSI icon, §3 mapping) with `onClick` navigating to
  `"detail/{deviceAddress}"`.
- **Empty state**: when `discovered.isEmpty()`, show a centered column matching iOS's
  `ContentUnavailableView`: a Bluetooth-scanning icon (§7.3), the localized title
  `"CAN'T SEE YOUR THINGY?"`, then the two-step instructional body text (both primary lines and
  their smaller caption sub-lines) exactly as iOS's `emptyState` composes them — copy the four
  text blocks verbatim from §8's string table, do not paraphrase.
- **iOS 26 nav-bar note, translated for Android**: the iOS app deliberately does *not* use an
  opaque colored nav bar — `ThingyApp.swift`'s comment explains that on iOS 26, an opaque bar
  background hides SwiftUI's large title, so the app uses the native Liquid Glass bar with
  `.tint(.nordicBlue)` instead of a solid Nordic-blue bar. **This specific constraint does not
  apply to Android** — Material 3's `TopAppBar`/`LargeTopAppBar` has no equivalent glass-vs-opaque
  conflict. Recreate the *design intent* (a Nordic-branded top bar with a bold title) directly:
  set `TopAppBarDefaults.topAppBarColors(containerColor = NordicColors.nordicBlue, titleContentColor = Color.White)`
  for a solid Nordic-blue bar with a white title — the more literal, straightforward translation
  of the brand, unconstrained by the iOS-specific rendering bug that forced the workaround there.
- **Passing the selected device**: iOS passes the `ThingyPeripheral` object reference directly into
  `ThingyConnection(peripheral:)`. Compose Navigation routes carry only primitive arguments
  (`"detail/{deviceAddress}"`), so `ThingyConnectionViewModel` must be constructed with just the
  MAC address (from `SavedStateHandle`) and independently obtain a `BluetoothDevice` via
  `bluetoothAdapter.getRemoteDevice(address)` (valid even without a prior scan result — Android
  allows constructing a `BluetoothDevice` from a known address) to call `connectGatt()` on. This is
  a structural difference worth a code comment at the call site so a future maintainer doesn't
  wonder why the object isn't just passed through like the row's `DiscoveredThingyUi.address` is.
- **Scan lifecycle**: `LaunchedEffect(Unit) { viewModel.clearDiscovered(); viewModel.startScan() }`
  on first composition, mirroring iOS's `.onAppear`. Request Bluetooth runtime permissions (§4.4)
  before `startScan()` if not already granted, showing a rationale UI if denied.

### 6.2 Detail screen (`ThingyDetailScreen`, replaces `ThingyDetailView`)

- **Structure**: `Scaffold` with `TopAppBar` (title = `uiState.name`, small/inline style — iOS uses
  `.navigationBarTitleDisplayMode(.inline)` for this screen specifically, unlike the scanner's
  large title), same Nordic-blue branding as §6.1.
- **Body**: a `LazyColumn` of Material 3 `Card`-wrapped "sections," each with a header `Text` and
  footer `Text` styled as `MaterialTheme.typography.labelMedium`/`bodySmall` in
  `colorScheme.onSurfaceVariant`, approximating SwiftUI `List`'s inset-grouped `Section { } header:
  { } footer: { }` (Compose/Material 3 has no built-in equivalent primitive — build a small reusable
  `SettingsSection(header, footer) { content }` composable used by all four sections below).
- **LED section** (always shown): a `Row` with a lightbulb icon (§7.3), the localized state text
  (`"Scanning..."` while connecting, `"DISCONNECTED"` while disconnected, `"ON"`/`"OFF"` while
  connected per `ledIsOn`), and a trailing `Switch` bound to `uiState.ledIsOn`, `enabled =
  uiState.state == Connected && uiState.ledSupported`, `onCheckedChange = viewModel::setLed`. When
  `uiState.state == Disconnected`, tint the switch's checked-thumb/track color with
  `NordicColors.nordicRed` (iOS: `.tint(connection.state == .disconnected ? Color.nordicRed :
  nil)`). Footer text: `"Toggling the switch will cause the LED on the Thingy to turn ON or OFF."`
- **Button section** (always shown): a `Row` with a radio-button-checked icon (§7.3) and the
  localized state text (`"Scanning..."`/`"DISCONNECTED"`/`"PRESSED"`/`"RELEASED"`). Footer:
  `"Pressing and releasing the button on the Thingy will update the state here."`
- **Environment section** (shown only when `uiState.hasEnvironmentData`, i.e. at least one of
  temperature/humidity/pressure/eco2 is non-null — port `hasEnvironmentData` as a computed
  property on the UI-state data class exactly as iOS computes it on `ThingyConnection`): four rows
  — Temperature (`"%.1f °C".format(celsius)`), Humidity (`"$percent %"`), Pressure
  (`"%.1f hPa".format(hPa)`), Air Quality (`"$eco2 ppm · $tvoc ppb"`, `null` until both eco2 and
  tvoc have arrived) — each row is icon + localized label + trailing value text in
  `colorScheme.onSurfaceVariant` with tabular/monospaced digit styling (`fontFeatureSettings =
  "tnum"` is Compose's approximation of SwiftUI's `.monospacedDigit()`). Header: `"Environment"`.
  Footer: `"Live sensor readings streamed from the Thingy."`
- **Motion section** (shown only when `uiState.hasMotionData`, same conditional pattern): four
  rows — Orientation (`orientation?.label`), Steps (`stepCount?.toString()`), Heading
  (`"%.0f°".format(degrees)`), Last Tap (`"${direction.label} · ×$count"`, `null` until a tap has
  been observed). Header: `"Motion"`. Footer: `"Orientation, steps, heading, and taps reported by
  the Thingy's motion sensors."`
- **Haptics**: on `buttonPressed` transitioning to `true`, trigger a heavy-impact-equivalent haptic
  — `HapticFeedbackConstants.CONFIRM` or `CONTEXT_CLICK` via `view.performHapticFeedback(...)`, or
  (API 30+) `VibratorManager`/`VibrationEffect.createPredefined(EFFECT_HEAVY_CLICK)` for a closer
  match to iOS's `.sensoryFeedback(.impact(weight: .heavy), ...)`. Requires the `VIBRATE` manifest
  permission (normal, no runtime prompt).
- **Lifecycle**: `DisposableEffect(Unit) { viewModel.connect(); onDispose { viewModel.disconnect()
  } }` mirrors iOS's `.onAppear { connection.connect() } .onDisappear { connection.disconnect() }`.
  Note Compose's `onDispose` timing relative to back-navigation is analogous to SwiftUI's
  `.onDisappear` firing *after* a navigation transition completes, not before — the iOS status doc
  flags this as worth checking for race conditions on quick back-then-reselect; port the same
  caution to the Android testing checklist (§9.2).

### 6.3 Empty-state / row text — copy verbatim (do not paraphrase)

See §8 for the full source string table; every string used in §6.1/§6.2 above is listed there with
its exact English source text pulled directly from `Localizable.strings`.

---

## 7. Visual Assets

### 7.1 Nordic Brand Palette (for `Theme.kt`/`Color.kt`)

Hex values below are computed directly from the `#colorLiteral` RGB floats in
`UIColorExtension.swift` (×255, rounded to nearest integer).

```kotlin
object NordicColors {
    val nordicBlue        = Color(0xFF00B7D7)  // primary brand color, nav bar / accent
    val nordicSky         = Color(0xFF7AD9E9)
    val nordicLake         = Color(0xFF008CD2)
    val nordicLakeDark    = Color(0xFF0079B7)
    val nordicBlueslate   = Color(0xFF0049B0)
    val nordicLightGray   = Color(0xFFE0E7E8)
    val nordicMediumGray  = Color(0xFF8998A3)
    val nordicDarkGray    = Color(0xFF42505A)
    val nordicGrass       = Color(0xFFD8E200)
    val nordicSun         = Color(0xFFFFD400)
    val nordicRed         = Color(0xFFF44960)  // error / disconnected-state tint
    val nordicFall        = Color(0xFFF99529)
}
```

Only `nordicBlue` (nav bar/accent) and `nordicRed` (disconnected-state tint) are actually consumed
by the current iOS UI (`ThingyApp.swift`'s `.tint(.nordicBlue)`, `ThingyDetailView`'s red switch
tint when disconnected) — the rest of the palette exists in `UIColorExtension.swift` as a general
brand kit but is currently unused by any view. Port the full palette for parity/future use, but
only `nordicBlue`/`nordicRed` are load-bearing for the current feature set.

Light/dark: use these as `colorScheme.primary`/`error` inside both `lightColorScheme(...)` and
`darkColorScheme(...)` passed to Compose's `MaterialTheme` in `ThingyTheme.kt`, selected via
`isSystemInDarkTheme()`. No separate dark-variant hex values exist in the iOS source (Nordic blue
is used unmodified in both appearances there too via
`UIColor.dynamicColor(light: .nordicBlue, dark: .black)` for the nav-bar-background use, which
Android's `TopAppBar` coloring should mirror: Nordic blue bar in light mode, and either the same
blue or `colorScheme.surface`/black in dark mode — match whichever look is chosen at
implementation time by testing both against the actual palette; not specified precisely enough in
the iOS source to mandate one exact dark-mode bar treatment beyond "matches system dark theme
conventions").

### 7.2 iOS asset inventory (`Assets.xcassets`)

```
rssi_1, rssi_2, rssi_3, rssi_4          — signal-strength icons (weakest → strongest)
ic_lightbulb_outline_48pt               — LED row icon
ic_radio_button_checked                 — Button row icon
scanning                                — empty-state Bluetooth icon
splashscreen                            — launch image
AppIcon / AppIcon-1                     — app icon (two icon sets, legacy + current)
AccentColor                             — system accent color set (verify unused/used at impl time)
```

### 7.3 Icon substitution table (SF Symbol / custom asset → Android)

| iOS source | Used for | Android replacement |
|---|---|---|
| `rssi_1`..`rssi_4` (custom PNG/PDF assets) | Scanner row signal strength | Port as `res/drawable/rssi_1.xml`..`rssi_4.xml` vector drawables — either recreate the same bar-chart glyph style as VectorDrawables, or substitute Material Symbols `signal_cellular_alt`-family icons at 1/2/3/4-bar fill levels if recreating the exact custom art is out of scope. Either is acceptable; **keep the four-tier visual distinction**, since `RssiBucket`'s four buckets are unit-tested and user-visible. |
| `ic_lightbulb_outline_48pt` (custom) | LED row icon | Material Icons `Icons.Outlined.Lightbulb` (or `Icons.Filled.Lightbulb` when LED is on, for a nice touch beyond strict parity — optional enhancement, not required for parity). |
| `ic_radio_button_checked` (custom) | Button row icon | Material Icons `Icons.Filled.RadioButtonChecked`. |
| `scanning` (custom) | Empty-state icon | Material Icons `Icons.Filled.BluetoothSearching` (closest semantic match to a "Bluetooth scanning" glyph). |
| SF Symbol `thermometer.medium` | Environment: Temperature row | Material Icons `Icons.Filled.Thermostat` or `Icons.Filled.DeviceThermostat`. |
| SF Symbol `humidity` | Environment: Humidity row | Material Icons `Icons.Filled.WaterDrop` (no exact "humidity" glyph in the standard Material set; water-drop is the closest common substitute used by weather apps). |
| SF Symbol `gauge.with.dots.needle.bottom.50percent` | Environment: Pressure row | Material Icons `Icons.Filled.Speed` (closest gauge-style glyph in Material Symbols). |
| SF Symbol `aqi.medium` | Environment: Air Quality row | Material Icons `Icons.Filled.Air` or `Icons.Filled.Co2` (Material Symbols has an explicit `aqi` icon in the extended "Symbols" set, not the classic Material Icons set — use `Icons.Filled.Air` if staying within `androidx.compose.material.icons.Icons`, or pull the extended Material Symbols font/icon pack if exact AQI iconography matters). |
| SF Symbol `rotate.3d` | Motion: Orientation row | Material Icons `Icons.Filled.ScreenRotation` or `Icons.Filled.ThreeDRotation`. |
| SF Symbol `figure.walk` | Motion: Steps row | Material Icons `Icons.Filled.DirectionsWalk`. |
| SF Symbol `location.north.circle` | Motion: Heading row | Material Icons `Icons.Filled.Explore` (compass-style, closest semantic match) or `Icons.Filled.Navigation`. |
| SF Symbol `hand.tap` | Motion: Last Tap row | Material Icons `Icons.Filled.TouchApp` or `Icons.Filled.PanTool`. |
| App icon / launch screen | App identity | The starter project **already has** working adaptive-icon plumbing (`mipmap-anydpi-v26/ic_launcher.xml` + foreground/background vector drawables + all density-specific `.webp` fallbacks) — restyle the existing foreground/background drawables with Nordic branding rather than rebuilding the icon infrastructure from scratch. |

---

## 8. Localization Plan

### 8.1 Locale list (confirmed from the 16 `*.lproj` directories under `nRFThingy52/`)

```
de   (German)              it   (Italian)              ro     (Romanian)
en   (English, base)       ko   (Korean)                ru     (Russian)
es   (Spanish)             mr   (Marathi)                uk     (Ukrainian)
fi   (Finnish)              nb   (Norwegian Bokmål)       vi     (Vietnamese)
fr   (French)               pl   (Polish)                 zh-Hans (Simplified Chinese)
                            pt-BR (Brazilian Portuguese)
```

Android resource-qualifier directory mapping (`res/values-<qualifier>/strings.xml`):

```
de → values-de          it → values-it          ro → values-ro
en → values (default)   ko → values-ko          ru → values-ru
es → values-es          mr → values-mr          uk → values-uk
fi → values-fi          nb → values-nb          vi → values-vi
fr → values-fr          pl → values-pl          zh-Hans → values-zh-rCN (or values-b+zh+Hans for explicit script tag; verify against current Android locale-qualifier docs at implementation time — script-based BCP-47 qualifiers like `b+zh+Hans` are the more precise modern form vs. the legacy region-based `zh-rCN`)
                        pt-BR → values-pt-rBR
```

**Note:** every non-English `.lproj` directory in the current iOS project (verified by directly
reading `de.lproj/Localizable.strings`) contains a translated value for each key, matching English
1:1 in key count — i.e., the iOS app is fully localized (translations present, not English
placeholders), so the Android `strings.xml` files should be populated with the corresponding
translated text pulled from each locale's `Localizable.strings`, not left as English-only stubs.

> **⚠️ CORRECTION (2026-07-26, during Phase 9).** The paragraph above is **wrong**, and it is worth
> understanding why so the same mistake isn't repeated: it verified *key count* parity, not value
> content. Every `.lproj` does have all 30 keys, but **only 6 of the 30 values are actually
> translated** (4 in `fr`). The other 24 are English strings sitting in the translated files. The
> translated ones are the UIKit-era originals — `ON`, `OFF`, `PRESSED`, `RELEASED`, `Reading...`,
> `Unknown Device` — while **everything added during the SwiftUI rewrite was never sent for
> translation**: the whole scanner empty state, and every LED/Button/Environment/Motion label and
> footer. Measured across all 15 locales, not inferred. See §10 item 12.

### 8.2 Exact string keys (source: `en.lproj/Localizable.strings`, all 32 current keys)

Copy these into `res/values/strings.xml` with Android string-resource names (snake_case, since
`.strings` keys are the literal English display text rather than symbolic identifiers — invent
Android resource names but preserve the exact English value):

```xml
<string name="on">ON</string>
<string name="off">OFF</string>
<string name="pressed">PRESSED</string>
<string name="released">RELEASED</string>
<string name="reading">Reading...</string>
<string name="unknown_device">Unknown Device</string>
<string name="nearby_devices">Nearby Devices</string>
<string name="scanning">Scanning...</string>
<string name="disconnected">DISCONNECTED</string>
<string name="led">LED</string>
<string name="button">Button</string>
<string name="cant_see_your_thingy">CAN\'T SEE YOUR THINGY?</string>
<string name="empty_state_step_1">1. Make sure it\'s switched on.</string>
<string name="empty_state_step_1_detail">Toggle the switch next to the micro USB port to switch it on.</string>
<string name="empty_state_step_2">2. Make sure the coin cell battery has power.</string>
<string name="empty_state_step_2_detail">If not, connect it to a PC or a charger using a micro USB cable. Coin cell battery is on the bottom side of the dev kit.</string>
<string name="led_section_footer">Toggling the switch will cause the LED on the Thingy to turn ON or OFF.</string>
<string name="button_section_footer">Pressing and releasing the button on the Thingy will update the state here.</string>
<string name="environment">Environment</string>
<string name="temperature">Temperature</string>
<string name="humidity">Humidity</string>
<string name="pressure">Pressure</string>
<string name="air_quality">Air Quality</string>
<string name="environment_section_footer">Live sensor readings streamed from the Thingy.</string>
<string name="motion">Motion</string>
<string name="orientation">Orientation</string>
<string name="steps">Steps</string>
<string name="heading">Heading</string>
<string name="last_tap">Last Tap</string>
<string name="motion_section_footer">Orientation, steps, heading, and taps reported by the Thingy\'s motion sensors.</string>
```

(`"Reading..."` from the iOS strings file is present in the source but not currently referenced by
any of the four active iOS views read during this inventory — port it for parity/completeness but
don't be surprised if it's unused in the initial Android UI either; verify against the very latest
iOS source at implementation time in case a newer iOS commit started using it.)

Each of the other 15 locale `strings.xml` files should contain the same 29 `<string>` elements with
translated `name`/value pairs sourced from the corresponding `<lang>.lproj/Localizable.strings`
file — read each one directly rather than machine-translating, to guarantee parity with what's
already shipped on iOS.

---

## 9. Testing Strategy

### 9.1 Fake BLE transport (replaces CoreBluetoothMock)

There is no Android library offering CoreBluetoothMock's specific trick (same public API surface,
transparently swapped for a mock based on environment). The recommended approach is a hand-rolled
seam, directly analogous to the `ThingyControlling` protocol iOS already uses for its *unit* tests
— just extended to also power *integration*-level testing and (optionally) an in-app interactive
demo mode the way `ThingyMocks.startEnvironmentDemo()`/`startMotionDemo()` currently drive the iOS
simulator's live dashboard:

- `ThingyController` interface (§3) with two implementations:
  - `ThingyGatt` (real `BluetoothGattCallback`-backed, §4 points 6–11)
  - `FakeThingyController` (in-memory state machine: holds `ledOn: Boolean`,
    `environmentState`/`motionState`, exposes `simulateEnvironment(...)`,
    `simulateTap(...)`, `simulateOrientation(...)`, `simulateStepCount(...)`,
    `simulateHeading(...)`, `simulatePowerOff()`, `simulateDisconnection()` test-control methods
    mirroring `ThingyMocks`'s exact method names/signatures for easy cross-reference against the
    iOS test suite)
- `BleScanner` interface similarly gets a `FakeBleScanner` that emits one canned
  `DiscoveredThingyUi("AA:BB:CC:DD:EE:FF", "Thingy52 Mock", ...)` — reuse the literal name
  `"Thingy52 Mock"` from `ThingyMocks.mockName` for direct parity with the iOS UI test's
  `app.staticTexts["Thingy52 Mock"]` assertion.
- Selection mechanism between fake and real: a debug build flavor/build type
  (`debugFake`/`debugReal` product flavors, or a single `DEBUG` `BuildConfig` boolean toggled per
  test target) — resolve exactly how at implementation time (§10.6 flags this as open); JVM unit
  tests always use the fake (no Android framework dependency), and instrumented/UI tests should
  default to the fake too so they run on CI without real hardware, mirroring how the iOS simulator
  always resolves to the CoreBluetoothMock implementation.

### 9.2 Test-by-test mapping (iOS → Android)

**From `nRFThingy52Tests/BLEModelTests.swift`** → JVM unit tests (`app/src/test/...`):

| iOS test | Android equivalent |
|---|---|
| `RSSIBucketTests.testBucketsAreMonotonicAcrossThresholds` | `RssiBucketTest.bucketsAreMonotonicAcrossThresholds` — same 8 threshold assertions (`-100→WEAKEST`, `-81→WEAKEST`, `-80→WEAK`, `-61→WEAK`, `-60→MEDIUM`, `-41→MEDIUM`, `-40→STRONG`, `-20→STRONG`) |
| `RSSIBucketTests.testImageNamesMatchAssets` | Adapt: assert each `RssiBucket` maps to its intended drawable resource ID (or icon-tier index), since Android has no string `imageName` the way iOS does |
| `ScannerModelHelperTests.testAdvertisedNameUsesLocalNameKey` / `testAdvertisedNameFallsBackForMissingName` | `ScannerViewModelHelperTest` — port `advertisedName(from: ScanResult)` fallback logic verbatim |
| `ScannerModelHelperTests.testRowRefreshThrottle` | `ScannerViewModelHelperTest.shouldRefreshRowThrottle` — identical 1-second-threshold pure-function test |
| `ThingyEnvironmentTests` (5 tests: temperature ±, pressure, humidity, air quality, temperature round-trip) | `ThingyEnvironmentTest` — port each byte-array fixture verbatim per §5.2 |
| `ThingyMotionTests` (5 tests: tap parsing valid/invalid, orientation, step count round-trip, heading round-trip, tap labels) | `ThingyMotionTest` — port each fixture verbatim per §5.3 |
| `ThingyConnectionTests` (10 tests: init/delegate wiring, connect guards, support-flag publishing, auto-disconnect-if-unsupported, disconnect state, LED/button state publishing, optimistic LED set, name fallback) | `ThingyConnectionViewModelTest` — same 10 assertions against `ThingyConnectionViewModel` + `FakeThingyController`, using the `makeSUT()` factory pattern the iOS suite already uses |

**From `nRFThingy52Tests/ThingyIntegrationTests.swift`** → instrumented or Robolectric tests
(`app/src/androidTest/...` or `app/src/test/...` with Robolectric, whichever the team prefers —
these don't touch real Android framework APIs beyond what a fake transport needs, so pure JVM +
Robolectric may suffice without a device/emulator):

| iOS test | Android equivalent |
|---|---|
| `testDiscoveryFindsAdvertisingThingy` | `ThingyPipelineTest.discoveryFindsAdvertisingThingy` |
| `testConnectDiscoversLEDAndButton` | `ThingyPipelineTest.connectDiscoversLedAndButton` |
| `testLEDToggleWritesAndReadsBack` | `ThingyPipelineTest.ledToggleWritesAndReadsBack` |
| `testButtonPressAndReleaseNotify` | `ThingyPipelineTest.buttonPressAndReleaseNotify` |
| `testDisconnectOnDemand` | `ThingyPipelineTest.disconnectOnDemand` |
| `testPowerOffDisconnects` | `ThingyPipelineTest.powerOffDisconnects` |
| `testEnvironmentReadingsStreamToConnection` | `ThingyPipelineTest.environmentReadingsStreamToConnection` |
| `testMotionReadingsStreamToConnection` | `ThingyPipelineTest.motionReadingsStreamToConnection` |
| `testPeripheralInitiatedDisconnect` | `ThingyPipelineTest.peripheralInitiatedDisconnect` |

**From `nRFThingy52UITests/nRFThingy52UITests.swift`** → Compose UI tests
(`app/src/androidTest/...`):

| iOS test | Android equivalent |
|---|---|
| `testSensorDashboardsShow` (taps the mock row, asserts all 4 Environment + 3 Motion row labels appear via accessibility) | `SensorDashboardsUiTest` using `createAndroidComposeRule<MainActivity>()`: `composeTestRule.onNodeWithText("Thingy52 Mock").performClick()`, then assert `onNodeWithText("Temperature")`/`"Humidity"`/`"Pressure"`/`"Air Quality"`/`"Orientation"`/`"Steps"`/`"Heading"` all exist (with `waitUntil`/`Espresso`-style polling since readings stream in asynchronously, exactly mirroring the iOS test's `waitForExistence(timeout: 10)`) |
| `testLaunchPerformance` | `LaunchTimeTest` using Macrobenchmark's `StartupTimingMetric`, or a simpler `androidx.benchmark` measurement — not a strict requirement for parity, lower priority than the functional tests above |

### 9.3 Known simulator/tooling flakiness to expect (carried over as a heads-up, not an Android bug)

The iOS project's status doc records two occasions where **CoreSimulatorService**/accessibility-daemon
stalls caused UI test runs to hang or fail with unrelated-to-code errors, resolved by a full
simulator service reset. Android's parallel risk is **emulator/instrumentation flakiness**
(similarly unrelated to app code) — if `androidTest` runs hang or fail with infra-flavored errors
(ADB disconnects, instrumentation timeouts), try a fresh emulator cold-boot or physical-device run
before assuming a code regression, mirroring the diagnostic lesson already learned on the iOS side.

---

## 10. Open Questions / Risks

Flagged here rather than silently decided, because each has a real trade-off the implementing
agent (or a human reviewer) should weigh:

1. **Kotlin/AGP/Compose-compiler version triple.** AGP is pinned at 9.1.1 by the existing starter;
   pick a Kotlin version and Compose Compiler Gradle plugin version known-compatible with AGP 9.1.1
   at implementation time (compatibility tables shift with each release — verify current guidance
   rather than trusting a version number baked into this document, which will age).
2. **Java toolchain bump.** Recommend raising `sourceCompatibility`/`targetCompatibility` from 11
   to 17+ (§2.3) — confirm this doesn't conflict with any constraint not visible in the currently
   minimal starter project.
3. **Whether to keep `minSdk = 24`.** It's already set and works fine for this app's BLE needs: no
   reason to change it, but confirm no other unstated business requirement (e.g., "match iOS 17+
   spirit by dropping older Android too") should raise it.
4. **`compileSdk`/`targetSdk = 36`.** Already set to the latest release at starter-creation time;
   revalidate this is still the current stable SDK level when implementation actually begins,
   since Android SDK levels increment yearly.
5. **Nordic Android BLE library adoption (`no.nordicsemi.android:ble`/`ble-ktx`).** This plan
   specifies hand-rolling `ThingyGatt` directly against `BluetoothGatt`/`BluetoothGattCallback` to
   mirror the iOS app's "build directly on the platform framework" philosophy (CoreBluetoothMock is
   the one deliberate exception, added specifically for testing, not for simplifying the real BLE
   path). An alternative is adopting Nordic's own `no.nordicsemi.android:ble` library, which already
   solves the GATT-operation-queueing problem (§4 point 7) and is maintained by the same
   organization that makes the Thingy:52 hardware and the iOS CoreBluetoothMock library — arguably
   a more direct "sibling project" choice than hand-rolling. **Recommend resolving this before
   Phase 3** by weighing hand-rolled-parity-with-iOS-architecture against
   less-code-more-battle-tested-queueing.
6. **Fake-vs-real transport selection mechanism.** Product flavors vs. a single `BuildConfig`
   boolean vs. Hilt/Koin DI module swapping — pick one; all three are viable, this plan doesn't
   mandate which.
7. **RSSI icon fidelity.** Recreate the exact custom `rssi_1`..`rssi_4` bar-chart artwork as vector
   drawables, or substitute Material Symbols signal-strength icons (§7.3)? Either satisfies the
   four-tier requirement the unit tests care about; purely a visual-polish decision.
8. **Extended Material Symbols vs. classic Material Icons for Air Quality/other glyphs.** Some
   icon substitutions in §7.3 (e.g., `aqi`) exist only in the newer, larger "Material Symbols"
   icon set, which may require a different Gradle dependency/font-based icon approach than the
   classic `androidx.compose.material.icons.Icons` extended set. Confirm which is acceptable/
   available at implementation time.
9. **Background scanning/foreground service.** Deliberately deferred (§4 point 5) to match current
   iOS scope (foreground-only). Flag explicitly if a future requirement adds background operation —
   it's a substantial addition (foreground service, persistent notification, battery/Doze
   interaction) that this plan does not size.
10. **Git repository initialization.** The Android directory has no `.git` yet. Decide whether it
    becomes its own repository (recommended, matching the iOS project's independence) or a
    submodule/subdirectory of something larger — outside this plan's scope to decide, but worth
    resolving before the first commit so history isn't later rewritten to relocate it.

11. **✅ RESOLVED (2026-07-26) — sensor readings are now locale-aware on both platforms.**
    iOS landed the fix (`nRFThingy52/Utilities/SensorFormat.swift`, a `FormatStyle` replacing every
    `String(format:)` in the reading path; 45/45 iOS tests green) and Android mirrored it. The
    agreed cross-platform contract, now enforced by tests on both sides:

    - **Locale-aware numbers.** Readings format through the caller's locale — `22,5 °C` in de-DE.
      Each function takes an explicit `locale` parameter defaulting to the device locale, so tests
      pin behavior without mutating global state.
    - **All seven readings** go through the shared formatter, including the integer ones that were
      correct by accident, so the next reading added inherits the right behavior.
    - **Grouping suppressed everywhere** (`isGroupingUsed = false`): `1450 ppm`, `1234` steps —
      never `1,450`/`1.450`. These are instrument values in a monospaced-digit column, and in de-DE
      a grouping dot reads as a decimal point to exactly the users the fix was for. Flipping this is
      a cross-platform change.
    - **Rounding is HALF_EVEN**, matching Swift's `FormatStyle`.
    - Separators stay fixed characters: `·` U+00B7, `×` U+00D7, tap minus U+2212.

    **Android implementation note — a real trap.** The iOS hand-off stated that Kotlin's
    `String.format` also defaults to HALF_EVEN. It does not: `java.util.Formatter` rounds **HALF_UP**,
    so `"%.1f"` renders `-5.25` as `-5.3` where iOS renders `-5.2`. Verified empirically before
    porting. `SensorFormat` therefore formats via `NumberFormat` (whose default is HALF_EVEN) rather
    than `String.format`, and `SensorFormatTest.halfWayValuesRoundHalfEven` asserts `-5.25` and
    `271.5` explicitly, since those are exactly where the two modes diverge. This also means Phase 7's
    original `%.1f` implementation had a latent half-way divergence from iOS's old `printf` behavior
    (C `printf` is round-half-to-even), which the old test had enshrined as `-5.3 °C`.

    Verified on device: with a de-DE app locale the dashboard renders `21,8 °C`, `1014,1 hPa`,
    `454 ppm · 14 ppb`, `356°`, `Z+ · ×1`.

    *Historical context (raised during Phase 7, 2026-07-25):* The iOS `ThingyDetailView` formats every numeric reading
    with `String(format: "%.1f °C", …)`. `String(format:)` **without an explicit `locale:` argument
    is locale-independent** — it always emits a "." decimal separator and never applies digit
    grouping. So on a German device the iOS app, which is otherwise fully localized into 16
    languages, still prints `22.5 °C` where a German user expects `22,5 °C`. The same applies to
    Pressure (`%.1f hPa`) and Heading (`%.0f°`).

    **This is a defect in the iOS app, not an Android porting question.** The correct fix is on the
    iOS side — pass `locale: Locale.current` to `String(format:)`, or better, use a
    `FormatStyle`/`NumberFormatter` so the separator, grouping, and numbering system all follow the
    user's locale. It is deliberately **not** fixed unilaterally on Android, because the whole point
    of this port is behavior-for-behavior parity: a divergence here would make the two apps print
    different text from identical sensor bytes, which is exactly what the parity build is meant to
    avoid.

    **Android therefore mirrors the current iOS behavior on purpose**: `SensorFormat` formats with
    `Locale.ROOT`, and `SensorFormatTest.decimalSeparatorIsLocaleIndependent` pins it by setting the
    default locale to Germany and asserting `22.5 °C` / `1013.3 hPa` still come out. That test is a
    **parity lock, not an endorsement** — it exists so the behavior can't drift silently on Android
    while iOS is still unfixed.

    **When iOS is fixed**, mirror it here: switch `SensorFormat` to the device locale (`Locale`
    default rather than `ROOT`) and invert that test to assert the localized separator. Note the row
    *labels* are already localized normally via `stringResource`; only the numeric values are
    affected. Humidity (`"$percent %"`), Steps, Orientation, and Last Tap interpolate integers and
    enum labels rather than formatting decimals, so they are unaffected by the separator issue —
    though Steps would gain digit grouping (`1,234` / `1.234`) under a locale-aware formatter.

12. **✅ RESOLVED (2026-07-26) — the 24 English placeholders are now translated on both platforms.**

    *Found during Phase 9 while populating the locale files: 24 of 30 values were English
    placeholders in the iOS `.lproj` files themselves — everything added during the SwiftUI rewrite
    had never been sent for translation, while the 6 UIKit-era strings (`ON`, `OFF`, `PRESSED`,
    `RELEASED`, `Reading...`, `Unknown Device`) were translated. This superseded §8.1's claim that the
    app was fully localized; see the correction note there.*

    iOS commissioned and landed all 24 the same day, and Android re-transcribed. Current state,
    measured across every `.lproj` rather than sampled: **29–30 of 30 values differ from English per
    locale**. The handful that still match are genuine translations that coincide with the English
    word — `LED` is LED in 11 of 17 locales, and `fr` legitimately keeps `ON`/`OFF`/`Orientation` —
    not placeholders.

    **Two locales were added upstream in the same pass: `ja` and `zh-Hant`**, which are not in §8.1's
    original list of 16. Android now ships **17** non-English files, `values-ja` and
    `values-b+zh+Hant` included. Anyone re-running the transcription should enumerate the `.lproj`
    directories rather than trusting §8.1's list.

    **Qualifier forms, verified by resolution on device rather than by convention:**
    `zh-CN → values-b+zh+Hans`, and `zh-TW`/`zh-Hant-TW`/`zh-Hant-HK → values-b+zh+Hant` (Hong Kong
    correctly reaching Traditional was the point of adding it), `ja-JP → values-ja`,
    `pt-BR → values-pt-rBR`. The iOS hand-off suggested `values-b+pt+BR`; the legacy `pt-rBR` form is
    kept because Brazilian Portuguese is region-only — there is no script to express, which is the
    single thing the legacy form cannot do — and `pt-rBR` is the conventional Android spelling. BCP-47
    `b+` is used only where it earns its keep, i.e. the two Chinese scripts.

    *(Method note: when sweeping locales over adb, allow the app several seconds to restart after
    `cmd locale set-app-locales`. A 4-second settle produced one stale `uiautomator` dump that showed
    the previous locale's text — briefly looking like Japanese was falling back to Simplified Chinese,
    which Android would never do. Re-checking in isolation showed `ja` resolving correctly.)*

    Verified on device across `es`, `uk`, `de`, `fi`, `ja`: every string renders translated, with
    **zero text truncation** — checked programmatically for ellipsis-clipped nodes, not just by eye —
    despite expansions up to 4.5× English (`ON` → `ENCENDIDO` / `ВВІМКНЕНО`). The `SettingsSection`
    and `SensorRow` layouts absorb it: long state text sits beside the `Switch` on one line and
    footers wrap to two.

    Still English-only, deliberately: `app_name` and `scanner_title` are `translatable="false"`
    (brand names), and the two Android-only accessibility strings (`cd_signal_strength`,
    `cd_scanning`) have no iOS counterpart to transcribe, so they ship under an explicit
    `tools:ignore="MissingTranslation"`. Those two should be translated whenever a11y copy is next
    reviewed. **See also item 13, which this pass did *not* close** — the four `ThingyOrientation`
    labels remain hardcoded English in both codebases.

13. **⚠️ OPEN — the four orientation labels are hardcoded English, not localized, on both platforms.**
    *(Surfaced 2026-07-26 while verifying the new translations on device: with a Spanish locale the
    dashboard correctly reads "Orientación", but its **value** reads "Portrait".)*

    `ThingyOrientation.label` returns `"Portrait"` / `"Landscape"` / `"Portrait (upside down)"` /
    `"Landscape (upside down)"` as string literals in the enum — `ThingyMotion.swift` on iOS,
    `ThingyOrientation.kt` here. They are not in `Localizable.strings`, so the 2026-07-26 translation
    pass did not reach them, and no locale renders them translated.

    The sibling `TapDirection.label` (`X+`, `X−`, …) needs no translation — those are symbols, and
    locale-neutral by nature. Only the four orientation labels are affected.

    **Is "Portrait" acceptable to Android users, or is "Vertical" the native term?** Answered from
    Android's own `framework-res.apk` (pulled off the device; resource
    `string/mediasize_unknown_portrait` / `_landscape`), not from opinion:

    | Locale | Android's own "landscape" | Android's own "portrait" |
    |---|---|---|
    | en | Unknown landscape | Unknown portrait |
    | es | Cualquier tamaño **horizontal** | Cualquier tamaño **vertical** |
    | de | Unbekannt – **Querformat** | Unbekannt – **Hochformat** |
    | it | **Orizzontale** sconosciuto | **Verticale** sconosciuto |
    | pt | **Paisagem** desconhecido | **Retrato** desconhecido |
    | fr | Taille inconnue au format **paysage** | Taille inconnue au format **portrait** |

    So **"Vertical" is genuinely Android's Spanish term** — it is the platform's own vocabulary, not
    an invented wording. Conversely, leaving the English "Portrait" is *not* generally acceptable: in
    `es`, `de`, `it`, and `pt` it is simply a foreign word in an otherwise translated screen. The one
    exception is `fr`, where "portrait" is the native term and the English string happens to read
    correctly — which is probably why the gap survived unnoticed.

    Note the platform's terms are *not* a literal translation of the English pair (German uses
    Hochformat/Querformat, "high/cross format"; Portuguese uses Retrato/Paisagem). Whoever
    commissions these should use each platform's established vocabulary rather than translating
    "portrait" word-for-word — and iOS should land the matching strings at the same time.

    Fixing it means moving them out of the enums into `Localizable.strings` / `strings.xml`, which
    changes the enum from returning a `String` to returning a resource key or `@StringRes` id on the
    Android side — a small but cross-cutting change to a **pure-domain type that currently has zero
    framework dependencies** (plan §2, a deliberate property worth preserving). The clean approach is
    to keep the enum framework-free and do the label lookup in the UI layer, the same way
    `RssiBucket.assetName` maps to a drawable in `ThingyRow` rather than in the domain enum.

    *This is deliberately not fixed unilaterally on Android* — same reasoning as item 11. Both apps
    should localize these together, or a Spanish user sees "Vertical" on one platform and "Portrait"
    on the other. Worth bundling with the `cd_*` accessibility strings into one small follow-up pass.

    **Historical note:** the pre-regeneration version of this plan tracked this as its own §10 item;
    the entry was lost when the file was reconstructed on 2026-07-24, which is why it resurfaced only
    on visual inspection. Re-recorded here.

### Decision log (resolved during Phase 0 — 2026-07-24)

Decisions taken against the open questions above, recorded here so the rationale isn't lost. Items
not listed remain open.

#### §10.5 — BLE implementation library → **RESOLVED: adopt Nordic `no.nordicsemi.android:ble` / `ble-ktx`**

The seam itself (the `ThingyController`/`ThingyGattListener` interfaces + the `FakeThingyController`
test double) is unchanged; this decision is only about what backs the *real* implementation.

Options weighed:

- **Hand-rolled against raw `BluetoothGatt`/`BluetoothGattCallback`** (the plan's original default).
  Pros: zero third-party runtime dependency, maximal architectural symmetry with the iOS app's
  "build directly on the platform framework" philosophy. Cons: you must implement — correctly, and
  defensively against OEM-stack quirks — the GATT operation-serialization queue (§4 point 7), the
  two-step `setCharacteristicNotification` + CCCD-descriptor-write notification enable (§4 point 9),
  off-Binder-thread dispatch, and explicit `gatt.close()` on disconnect. These are the single most
  common sources of flaky Android BLE bugs and represent meaningfully more code and risk.
- **Nordic `no.nordicsemi.android:ble` / `ble-ktx`** *(chosen)*. Pros: a first-party library from
  the same organization that makes the Thingy:52 hardware and the iOS CoreBluetoothMock library —
  arguably a more faithful "sibling project" choice than hand-rolling. It solves exactly the pain
  points above out of the box (built-in operation queue, automatic CCCD/notification handling,
  coroutine `suspend` request/await semantics via `ble-ktx`, reconnection logic). Cons: a real
  runtime dependency (a departure from the iOS "no deps for the core path" norm — though iOS already
  set the precedent of one deliberate Nordic-first-party dependency, CoreBluetoothMock) and its own
  abstractions to learn.

Rationale: for a parity port that must be robust on real hardware, the queue/CCCD problems `ble-ktx`
solves are precisely where hand-rolled Android BLE goes wrong; a Nordic-first-party library that
eliminates them is the pragmatic choice. Wired in Phase 3/4; the queue-related notes in §4 points 7
and 9 become "use the library's equivalent" rather than "implement from scratch."

#### §10.6 — Fake-vs-real transport selection → **RESOLVED: product flavors (`mock`/`prod`) + lightweight manual DI**

Options weighed:

- **Product flavors `mock`/`prod`** (dimension `transport`). The choice is made at build time by
  which variant is assembled, surfaced as `BuildConfig.USE_FAKE_TRANSPORT`. Most explicit; closest
  to the iOS app's compile-time `#if targetEnvironment(simulator)` seam. Cost: doubles the variant
  matrix (`{mock,prod}{Debug,Release}`) and adds `src/mock`/`src/prod` source sets; can't flip
  fake↔real without rebuilding.
- **Single `BuildConfig` boolean** keyed off build type. Simpler (no flavor explosion), but tying
  "use fake" to `DEBUG` conflates two axes you sometimes need apart — e.g. a debug build running
  against *real* hardware during Phase 9 — so you end up needing an override anyway.
- **DI framework (Hilt/Koin) module swap.** Most flexible (runtime switching, cleanest for tests),
  but the most infrastructure; heavier than a single-module app warrants.
- **Chosen: flavors + lightweight manual DI.** The `mock`/`prod` flavor provides the explicit
  build-time default via `BuildConfig.USE_FAKE_TRANSPORT`; a small composition-root DI seam (manual/
  constructor injection, no Hilt/Koin) reads that flag and injects the appropriate
  `ThingyController`/`BleScanner`. This keeps the explicit build-time selection while leaving tests
  free to inject the fake directly without any build-variant machinery.

Rationale: flavors and DI are not mutually exclusive; combining a flavor default with a manual-DI
wiring point gives the explicitness of a build-time choice and the testability of injection, without
pulling in a DI framework a single-module app doesn't need.

#### §10.4 / §2.3 — compileSdk & AndroidX versions → **RESOLVED: bump `compileSdk` 36 → 37 and restore latest AndroidX**

Phase 0 initially pinned `compileSdk = 36` (§2.3) and, as a consequence, had to hold the AndroidX
dependencies back to their API-36-era line, because the current AndroidX releases (core-ktx 1.19,
lifecycle 2.11, compose-bom 2026.x, activity-compose 1.13) declare a `minCompileSdk` of 37 and fail
the build against 36. The maintainer chose to bump `compileSdk` to 37 (AGP auto-downloads the
android-37 platform on first build; there is no command-line `sdkmanager` in this SDK) and restore
the latest AndroidX. `targetSdk` stays 36 and `minSdk` stays 24 — only the compile-against level moved.

Reasoning for restoring the latest AndroidX once on `compileSdk 37`:

1. **Coherence.** The library downgrade existed *only* to satisfy the old 36 pin; with that gone,
   compiling against SDK 37 while holding a year-old compose-bom is a confusing, self-inconsistent
   mismatch.
2. **The `ble-ktx` decision (§10.5) makes it load-bearing.** Nordic's library depends transitively
   on AndroidX (lifecycle, coroutines, annotation). When it is added in Phase 4, Gradle resolves its
   transitive requirements against the project's and silently upgrades individual artifacts to the
   higher version — leaving a mismatched mix (new lifecycle, old everything-else) that surfaces as
   duplicate-class or `@Composable`-ABI warnings. Being deliberately current avoids surprise partial
   bumps and version-conflict debugging mid-phase.
3. **Fixes and APIs.** Phases 1 and 5–7 lean on Material 3 components, lifecycle, and the Compose
   runtime; newer versions carry bug fixes, performance, and `collectAsStateWithLifecycle`/component
   improvements the app would otherwise be a year behind on.
4. **Cheapest to do now.** While the app is a one-line placeholder, a bad bump is a trivial
   rebuild-and-relaunch to catch, versus bisecting it out of real code after Phases 1–7 exist. All
   the restored versions are *stable* releases (navigation-compose stays at 2.9.8, since 2.10 is
   still alpha), so the risk is low regardless.

The only argument against was minimizing this session's diff; it was outweighed because Phase 4 would
force the AndroidX question anyway and the placeholder app is the safest possible moment to absorb it.

---

## 11. Phased Implementation Plan

Each phase leaves the app building and (from Phase 4 onward) functional end-to-end against the
fake transport, mirroring the disciplined incremental style of `SwiftUIMigrationPlan.md`. Resolve
§10 items 1–6 before or during Phase 0.

### Phase 0 — Fix the starter project & make decisions

> **✅ COMPLETE & VERIFIED (2026-07-24).** `./gradlew build` green across all four variants + lint +
> unit tests; installs and launches the Compose placeholder on a Pixel 9 (API 35) emulator.
> Deviations from the bullets below, forced by the toolchain: (1) the standalone Kotlin Android
> Gradle plugin is **not** applied — AGP 9 has built-in Kotlin support and rejects it; only the
> Compose compiler plugin is applied. (2) `compileSdk` was bumped 36 → 37 and the AndroidX
> dependencies moved to their current releases. See the §10 Decision log for the resolved decisions
> (Kotlin 2.2.20 / Java 17 / Nordic `ble-ktx` / `mock`+`prod` flavors + lightweight DI / compileSdk 37).

- Add the Kotlin Android Gradle plugin to root and `app` `build.gradle.kts` (the concrete,
  non-optional fix identified in §2.3 — the project does not currently compile Kotlin).
- Raise `compileOptions`/add `kotlinOptions.jvmTarget` to 17+ (§2.3).
- Resolve §10 items 1, 2, 5, 6 (version triple, Java target, BLE library choice, fake-transport
  selection mechanism).
- Add Compose BOM, Material 3, Navigation Compose, `androidx.lifecycle:lifecycle-viewmodel-compose`,
  `kotlinx-coroutines-android` dependencies to `app/build.gradle.kts`; enable the Compose compiler
  plugin.
- Update `AndroidManifest.xml` with the full permission set from §4.4 (version-bounded via
  `maxSdkVersion`/`android:usesPermissionFlags="neverForLocation"`), `VIBRATE`, and the
  `<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />`
  declaration — additive to the existing manifest, not a rewrite.
- **Definition of done:** `./gradlew build` succeeds (Kotlin compiles); a placeholder
  `@Composable fun MainActivity` content (`Text("Thingy52")`) replaces the existing
  `activity_main.xml`/View-based content and installs/launches on a device or emulator.

### Phase 1 — Theme & design tokens

> **✅ COMPLETE & VERIFIED (2026-07-25).** Added `ui/theme/{Color,Theme,Type}.kt`. `NordicColors`
> holds all 12 brand colors, cross-checked against the iOS `UIColorExtension.swift` `#colorLiteral`
> floats (all 12 match). `ThingyTheme` uses light/dark `ColorScheme`s with `nordicBlue` primary +
> `nordicRed` error in both; Material You dynamic color is off to preserve branding. `Type.kt` is the
> Material 3 default `Typography()`. `./gradlew build` green; placeholder renders in nordicBlue on the
> Pixel 9 emulator in both light and dark (visually confirmed). No hardcoded colors outside `Color.kt`.

- `Color.kt` (`NordicColors`, §7.1, all 12 colors), `Theme.kt` (`ThingyTheme`, light/dark
  `ColorScheme`), `Type.kt` (Material 3 typography — no specific iOS font mapping required, since
  the iOS app uses system SF fonts throughout, matched by Android's default Roboto/system font via
  `Typography()` defaults).
- **Definition of done:** `ThingyTheme` wraps the Phase 0 placeholder, renders correctly in both
  light and dark system themes (toggle emulator/device theme and visually confirm), no hardcoded
  colors anywhere outside `Color.kt`.

### Phase 2 — Domain layer: UUIDs, wire-format parsing, pure models

> **✅ COMPLETE & VERIFIED (2026-07-25).** Added the pure-Kotlin `domain/` package: `ThingyEnvironment`
> and `ThingyMotion` (`object`s with `java.util.UUID` constants + parse/encode ported 1:1 from the iOS
> source), `EnvironmentReading`/`MotionReading` sealed interfaces, `TapDirection`/`ThingyOrientation`
> enums (labels verbatim, including the U+2212 minus), `RssiBucket` with `of(rssi)`, and internal
> little-endian codec helpers (`BinaryFormat.kt`). Test fixtures ported verbatim from
> `BLEModelTests.swift`: `ThingyEnvironmentTest` (5), `ThingyMotionTest` (5), `RssiBucketTest` (2) —
> **12/12 green**. Verified no `android.`/`androidx.` imports in `domain/` (only `java.util.UUID` +
> `kotlin.math`); full `./gradlew build` green. Note: `RssiBucket` exposes `assetName`
> ("rssi_1".."rssi_4", mirroring the iOS `imageName`) + a `tier` index instead of an Android drawable
> id, to keep the layer framework-free (§9.2's adaptation of `testImageNamesMatchAssets`).

- `ThingyEnvironment.kt`, `ThingyMotion.kt` (§5 tables, `object`s with UUID constants + pure
  parse/encode functions), `EnvironmentReading`/`MotionReading` sealed interfaces,
  `TapDirection`/`ThingyOrientation` enums with `label`, `RssiBucket` enum with `of(rssi)`.
- Unit tests: every row of the §9.2 `ThingyEnvironmentTest`/`ThingyMotionTest`/`RssiBucketTest`
  table.
- **Definition of done:** 100% of the ported parser/encoder/bucket unit tests pass; zero Android
  framework dependencies in this module/package (pure Kotlin, matching how
  `ThingyEnvironment.swift`/`ThingyMotion.swift` are framework-free on iOS).

### Phase 3 — BLE transport abstraction & fake implementation

> **✅ COMPLETE & VERIFIED (2026-07-25).** Added `ble/` (`ThingyController`, `ThingyGattEvent`,
> `BleScanner`, `ThingyScanResult`), `ble/fake/` (`FakeThingyController`, `FakeBleScanner`,
> `ThingyMocks`), and `ui/detail/` (`ThingyConnectionViewModel`, `ThingyDetailUiState`,
> `ConnectionState`). **29 JVM unit tests green, no instrumentation**: all 10 rows of §9.2's
> `ThingyConnectionViewModelTest` table (ported with the iOS `makeSUT()` pattern) + 6
> `FakeThingyTransportTest` + the 12 Phase 2 domain tests + the starter's 1 example test. Full
> `./gradlew build` green.
>
> **Deviation — events flow instead of a `ThingyGattListener` interface.** §3 specifies a listener
> protocol mirroring iOS's `ThingyDelegate`; §4.1 separately requires callbacks to be converted into
> plain data events pushed onto a flow, with the ViewModel's single collection point as the
> concurrency boundary. Rather than ship both (a listener plus a redundant flow), `ThingyController`
> exposes `events: SharedFlow<ThingyGattEvent>` and the listener interface was **not** created — the
> sealed `ThingyGattEvent` carries exactly the six `ThingyDelegate` cases. This is the §4.1 design,
> is thread-safe from a Binder thread by construction, and makes the ViewModel tests deterministic.
>
> Other notes: `FakeThingyController` takes an `autoConnect` flag so one class serves both roles iOS
> splits across two doubles — `false` (default) is the dumb recorder the iOS `ThingyConnectionTests`
> use, `true` behaves like real firmware for integration/demo. Its `simulate*`/`pressButton`/
> `releaseButton`/`powerOff`/`disconnectThingy`/`ledIsOn` names are kept from the iOS `ThingyMocks`
> facade for 1:1 cross-reference, and each `simulate*` encodes then re-parses through the Phase 2
> codecs so simulated readings exercise the real wire formats. `ThingyConnectionViewModel` takes the
> controller by constructor (like iOS's `ThingyConnection(peripheral:)`); the
> `SavedStateHandle`+repository construction arrives with navigation in Phase 5/6. The
> "Unknown Device" fallback is an injected string, per §3's note that non-Composable call sites have
> no `Bundle.main` equivalent — Phase 8 wires the localized resource at the construction site.

- `ThingyController`/`ThingyGattListener` interfaces (§3).
- `FakeThingyController`, `FakeBleScanner`, a `ThingyMocks`-equivalent facade (§9.1) — build this
  **before** the real `ThingyGatt`, mirroring how the iOS project's CoreBluetoothMock integration
  let later phases (detail screen, sensor dashboards) be built and verified against the mock
  before hardware was available.
- Unit tests: every row of the §9.2 `ThingyConnectionViewModelTest` table (written against
  `ThingyConnectionViewModel`, buildable in this phase since the fake is ready even before
  `ScannerViewModel`/screens exist).
- **Definition of done:** the fake transport's `ThingyConnectionViewModelTest` suite passes as
  pure JVM unit tests, no Android instrumentation required.

### Phase 4 — Real BLE transport

> **✅ COMPLETE & VERIFIED (2026-07-25).** Added `ThingyGatt` (Nordic-`BleManager`-backed, the §4
> points 9–11 pipeline), `AndroidBleScanner` (`BluetoothLeScanner` + `ScanFilter` on the UI service,
> `SCAN_MODE_LOW_LATENCY`), `BluetoothStateObserver` (`ACTION_STATE_CHANGED` receiver as a Flow),
> `BlePermissions`/`rememberBlePermissionState` (the §4.4 version-branched runtime flow, requested at
> first scanner appearance), `ThingyRepository` (address → controller, real + fake), `AppContainer`
> (the composition root reading `BuildConfig.USE_FAKE_TRANSPORT`), and `ThingyApplication`. Also added
> `domain/ThingyUserInterface` (§5.1 UI-service UUIDs + LED/button 1-byte codec), which Phase 2 had
> not covered. `./gradlew build` green (29 unit tests, lint 0 errors); **both flavors verified on the
> Pixel 9 emulator** — `prod` renders "real transport", `mock` renders "fake transport" (read back via
> uiautomator), neither crashes. Confirmed the real transport files contain zero references to the
> fake types (the only `ble.fake` imports are in the DI seam, by design).
>
> **Nordic `ble-ktx` 2.11.0 added — no AndroidX version nudges.** Its transitive requests all resolved
> *downward* to versions already in the project: `androidx.core:core` 1.12.0 → 1.19.0,
> `androidx.annotation` 1.9.1 → 1.10.0, `kotlinx-coroutines-android` 1.10.2 → 1.11.0, and
> `kotlin-stdlib` 2.2.20 matched exactly. A before/after diff of the resolved `mockDebugRuntimeClasspath`
> showed the ble-ktx subtree as the *only* change — so the earlier compileSdk-37 + latest-AndroidX bump
> did exactly what it was meant to.
>
> **Implementation notes.** (1) `BleManager` is **wrapped, not subclassed**: its `isConnected()` is a
> Java method that doesn't satisfy a Kotlin `val`, and `disconnect()` is `final` returning a
> `DisconnectRequest`, so both collide with `ThingyController`. `ThingyGatt` therefore delegates to a
> private `ThingyBleManager` that exposes narrow wrappers over the library's *protected* request
> builders. (2) `isRequiredServiceSupported()` returns true only when the LED or Button characteristic
> is present, which makes the library abort the connection — the same outcome as iOS's
> "supports neither, disconnect" rule. (3) No `requestMtu` call: every payload is ≤ 8 bytes so the
> default 23-byte MTU suffices (§4.2) — this deliberately diverges from §4 point 2's "call
> requestMtu(185) as a defensive default", since the parenthetical there concedes MTU 23 is sufficient
> for this characteristic set, and an unnecessary negotiation adds a failure mode. (4) The fake
> transport lives in `src/main`, not `src/mock`, so `./gradlew test` compiles the fake for **all**
> variants; the cost is that `prod` APKs also contain the (unreachable) fake classes until R8 is
> enabled for release. (5) Hardware verification against a physical Thingy:52 is Phase 9 — the
> emulator has no BLE radio, so only the wiring, not real GATT traffic, is verified here.

- `ThingyGatt` (real `BluetoothGattCallback` implementation, §4 points 9–11 pipeline), `BleScanner`
  (real `BluetoothLeScanner` wrapper, §4 point 8), a `BluetoothAdapter.ACTION_STATE_CHANGED`
  `BroadcastReceiver` (§4 point 11's power-off path), the GATT operation-serialization queue
  (§4 point 7) — or the chosen library's equivalent if §10 item 5 resolved toward adopting
  `no.nordicsemi.android:ble`.
- Runtime permission flow (§4.4): permission-check composable/launcher, rationale UI.
- **Definition of done:** builds against the real Android BLE APIs with zero use of the fake types;
  `ThingyGatt`/`BleScanner` implement the same `ThingyController`/`BleScanner` interfaces the fakes
  implement, so `ScannerViewModel`/`ThingyConnectionViewModel` code added in Phase 5 needs no
  changes to work with either. Hardware verification against a physical Thingy:52 happens in
  Phase 9, matching the iOS project's pattern of building against the mock first and confirming on
  hardware later.

### Phase 5 — Scanner screen

> **✅ COMPLETE & VERIFIED (2026-07-25).** Added `ScannerViewModel` (+`DiscoveredThingyUi`/
> `ScannerUiState`), `ScannerScreen`, `ThingyRow`, the base `res/values/strings.xml` (all 29 keys from
> §8.2, values verbatim), the four `rssi_1..4` vector drawables, and the `NavHost` with the `"scanner"`
> and `"detail/{deviceAddress}"` routes. 36 unit tests green (7 new `ScannerViewModelHelperTest`);
> `./gradlew build` green, lint 0 errors.
>
> **Verified on the Pixel 9 emulator.** `mock`: Nordic-blue `LargeTopAppBar` with white title and
> white scanning spinner, "Nearby Devices" header, the `"Thingy52 Mock"` row showing a 3-of-4-bar RSSI
> icon (correct for the fake's −45 dBm → MEDIUM); tapping the row navigates to the placeholder detail
> route showing `AA:BB:CC:DD:EE:FF`, and Back returns to the populated scanner. `prod`: the empty state
> renders with the official bluetooth_searching glyph and all four instruction text blocks verbatim.
> A useful side-effect of that `prod` run — the scanning spinner was active, so
> `AndroidBleScanner.startScan()` genuinely succeeded against the real `BluetoothLeScanner` on the
> emulator's simulated adapter (it just found no Thingy), which exercises more of the Phase 4 transport
> than "wiring only".
>
> **§10.7 and §10.8 resolved.** (1) RSSI icons: the four-tier bar-chart glyph is **recreated as vector
> drawables** (`rssi_1..4`), ascending bars with inactive bars at 25% `fillAlpha` so a Compose `Icon`
> tint still colors them — not a Material Symbols substitute. (2) Icon source: `androidx.compose.
> material.icons` is **not** on the classpath (Material Icons is decoupled from material3, and
> `material-icons-extended` is deprecated and multi-megabyte). Instead the needed glyphs are **vendored
> as vector drawables converted from the official google/material-design-icons SVGs** — exact Material
> artwork, no dependency, ~1 KB each. Phase 5 needs only `ic_scanning`; the Phase 6/7 glyphs
> (`ic_lightbulb`, `ic_radio_button`, `ic_temperature`, `ic_humidity`, `ic_pressure`, `ic_air_quality`,
> `ic_orientation`, `ic_steps`, `ic_heading`, `ic_tap`) were generated in the same pass and are ready.
>
> Other notes: `ScannerViewModel.advertisedName`/`shouldRefreshRow` are `companion object` functions so
> they port the iOS `ScannerModel` statics 1:1 and stay directly unit-testable; `advertisedName` also
> treats an **empty** name as missing, since Android can report `""` where iOS reports nil. The
> permission request is skipped entirely on the `mock` flavor (the fake needs none), and `onStartScan`
> is gated on the grant so a denial can't start a scan that would silently fail.
- `ScannerViewModel` (owns `BleScanner`, dedupe/throttle per §4 point 8, exposes
  `StateFlow<ScannerUiState>`).
- `ScannerScreen`, `ThingyRow` composables (§6.1), empty state, RSSI icon assets (§10 item 7
  flags the fidelity question), toolbar scanning spinner, `NavHost` setup with `"scanner"` and
  `"detail/{deviceAddress}"` routes (§6.1's object-reference-vs-route-argument callout).
- **Definition of done:** running against the Phase 3 fake transport (per the §10 item 6
  build-variant decision), the scanner screen shows the mock device, the empty state renders
  correctly when nothing is discovered, and tapping a row navigates to a still-placeholder detail
  route.

### Phase 6 — Detail screen: LED & Button

> **✅ COMPLETE & VERIFIED (2026-07-25).** Added `ThingyDetailScreen` (inline Nordic-blue `TopAppBar`,
> LED and Button sections), `SettingsSection` (the reusable header/Card/footer container standing in
> for SwiftUI's inset-grouped `Section`), `rememberHeavyImpactHaptic`, and
> `UnavailableThingyController`. `ThingyConnectionViewModel` gained a `factory(repository,
> unknownDeviceName)` that reads `deviceAddress` from `SavedStateHandle` and resolves it through
> `ThingyRepository`, so the nav route's MAC address becomes a controller; the detail route in
> `MainActivity` now hosts the real screen instead of the placeholder. 42 unit tests green (6 new
> `DetailStateTextTest`); `./gradlew build` green, lint 0 errors.
>
> **Verified on the Pixel 9 emulator (mock flavor).** Tapping the scanner row opens the detail screen
> titled "Thingy52 Mock" showing LED **OFF** and Button **RELEASED** — i.e. the fake auto-connected and
> the state is CONNECTED, not stuck on "Scanning...". Toggling the switch round-trips the
> optimistic-write-then-read-back path: OFF → ON (switch checked, value confirmed by the fake
> firmware's `LedStateChanged` read-back) → OFF. Back-navigation disposes the screen and re-entering
> reconnects cleanly (OFF/RELEASED, never DISCONNECTED).
>
> **On the §6.2 back-navigation race caution.** A scripted back-then-immediately-tap sequence produced
> no navigation: the tap was dispatched during the back transition and swallowed before the scanner
> was interactive. The app stayed in a valid state (scanner, list populated, no crash, no hang), and
> the same sequence with a 1 s settle reconnects normally — so this was a test-harness artifact, not a
> defect. It does **not** clear §9's checklist item 8: with real GATT the disconnect/reconnect timing
> can genuinely race, so it stays on the Phase 9 hardware checklist.
>
> **Not verified on device:** (1) the `nordicRed` disconnected switch tint — the mock flavor has no UI
> affordance to force a mid-session disconnect; it's covered by a Compose preview and by
> `DetailStateTextTest`, and gets real coverage in the Phase 9 Bluetooth-off check. (2) The button
> press/release row update and its haptic — the fake's `pressButton()` can't be driven from outside
> the app process. Both are covered at the unit-test layer today
> (`ThingyConnectionViewModelTest.buttonStateChangesArePublished`) and belong in the Phase 8 UI suite,
> which runs in-process and can call `ThingyMocks.controller.pressButton()` directly.
>
> Note: `ledStateRes`/`buttonStateRes` are pure `@StringRes` functions rather than logic inside the
> composable, so the iOS `ledStateText`/`buttonStateText` derivation is unit-testable without a Compose
> or Android context (`R.string` ids are plain int constants on the JVM).
- `ThingyConnectionViewModel` (§3, §4 point 10), `ThingyDetailScreen`'s LED and Button sections
  only (§6.2, deferring Environment/Motion sections to Phase 7), disconnected-state red tint,
  connect-on-launch/disconnect-on-dispose lifecycle (§6.2), heavy-impact haptics on button press.
- **Definition of done:** against the fake transport, the LED toggle round-trips through the
  optimistic-write-then-read-back path (assert via the fake's `ledOn` state in a test, mirroring
  `ThingyMocks.ledIsOn` on iOS); button press/release updates the row and triggers a haptic.

### Phase 7 — Environment & Motion dashboards

> **✅ COMPLETE & VERIFIED (2026-07-25).** Added the Environment and Motion sections to
> `ThingyDetailScreen` (both reusing `SettingsSection`), `SensorRow` (icon + label + trailing value
> with `fontFeatureSettings = "tnum"`, Compose's `.monospacedDigit()` equivalent, falling back to an
> EM DASH), and `SensorFormat` — pure formatters matching the §6.2 format strings exactly.
> `ThingyApplication` now starts the demo loops in the mock build, mirroring iOS's `ThingyApp.init`
> (simulator-only, skipped under tests so the Phase 8 suite drives values itself; the Android analogue
> of iOS's `XCTestConfigurationFilePath` check is whether the instrumentation classes loaded into the
> process). 50 unit tests green (8 new `SensorFormatTest`); `./gradlew build` green, lint 0 errors.
>
> **All eight formats verified on-device** (mock flavor, Pixel 9): `22.4 °C`, `43 %`, `1011.0 hPa`,
> `474 ppm · 15 ppb`, `Landscape`, `17`, `346°`, `Z− · ×1` — the last carrying the U+2212 minus and
> U+00D7 multiplication sign, matching iOS byte-for-byte.
>
> **Locale note.** The numeric formats use `Locale.ROOT`, not the device locale. iOS's `String(format:)`
> without an explicit locale is locale-independent and always emits a "." decimal separator, so
> formatting with the device locale would diverge (e.g. `22,5 °C` in de-DE). `SensorFormatTest` pins
> this with a test that sets the default locale to Germany. Only the numbers are locale-independent —
> the row labels localize normally through `stringResource`.
>
> **Two bugs found and fixed during on-device verification.** (1) `ic_temperature` and `ic_air_quality`
> rendered as solid filled squares: the official SVGs carry a leading `M0,0h24v24H0V0z` bounding-box
> path, and the Phase 5 conversion filter only matched the space-separated spelling, so the box was
> being painted. Both were corrected and every vendored icon re-audited (all now have exactly one path;
> the four `rssi_*` legitimately have four bars each). (2) Orientation and Last Tap permanently showed
> the placeholder, because iOS's `startMotionDemo` emits orientation once at loop start and never taps
> — and readings are not replayed to late subscribers, so a screen opened afterwards never sees them.
> The Android demo now re-emits orientation each tick and taps periodically. **This is a deliberate,
> demo-content-only divergence** that makes all four Motion rows usable interactively; no parsing,
> gating, or formatting behavior differs from iOS.
>
> **Not verifiable on device:** the "dashboards appear only after the *first* reading" transition. The
> demo loops tick every 2–3 s, so a section populates within a second or two of opening the screen. The
> gating itself is unit-tested (`FakeThingyTransportTest` asserts `hasEnvironmentData`/`hasMotionData`
> are false before any `simulate*` call and true after), and the Phase 8 UI suite — which controls the
> fake directly — is where the on-screen transition belongs.
- `ThingyDetailScreen`'s Environment and Motion sections (§6.2), the `hasEnvironmentData`/
  `hasMotionData` conditional-visibility logic, all eight sensor rows with their icons (§7.3) and
  formatted value text.
- **Definition of done:** against the fake transport, pushing simulated environment/motion
  readings makes each section appear and populate correctly; the `SensorDashboardsUiTest` (§9.2)
  passes.

### Phase 8 — Fake-transport integration & UI test suite

> **✅ COMPLETE & VERIFIED (2026-07-26).** Added `ThingyPipelineTest` (all 9 §9.2 rows) and
> `SensorDashboardsUiTest` (3 Compose UI tests). **59 JVM unit tests** and **4 instrumented tests**
> green; `./gradlew build` clean, lint 0 errors.
>
> **The pipeline suite runs as plain JVM unit tests — no emulator, no Robolectric.** §9.2 left the
> venue open ("instrumented or Robolectric… whichever the team prefers"); because the fake transport
> and both ViewModels are pure Kotlin, the 9 tests need neither, which satisfies this phase's DoD
> ("CI runs them green without requiring an emulator with Bluetooth hardware access") outright. They
> also need none of the iOS suite's `waitUntil(...)` polling: the fake emits synchronously and
> `MainDispatcherRule` installs an unconfined dispatcher, so a simulated event is observable on the
> ViewModel immediately. That removes the timing flakiness the iOS tests have to defend against.
>
> **`SensorDashboardsUiTest`** (`app/src/androidTest/`) ports the XCUITest plus the UI-layer halves of
> `testLEDToggleWritesAndReadsBack` and `testButtonPressAndReleaseNotify` — the latter being the piece
> Phase 6 could not check over adb, since `pressButton()` must be called inside the app process. It
> asserts on row *labels* like the iOS test, not values. Run with `./gradlew connectedMockDebugAndroidTest`.
>
> **Flavor guard.** The three UI tests are `assumeTrue(BuildConfig.USE_FAKE_TRANSPORT)`-guarded, the
> direct analogue of the iOS suite's `XCTSkipUnless(isSimulator)`. Verified on both flavors: **mock →
> 4 tests, 0 skipped, 0 failed; prod → 3 skipped, 0 failed.** (Gradle's console progress counter
> misreports totals for assumption-skips — "Finished 7 tests" for a 4-test run — but the JUnit XML is
> unambiguous and is what the numbers above come from.)
>
> Two things this suite pins that nothing else could: the demo loops really are suppressed under
> instrumentation (the "Environment"/"Motion" `assertDoesNotExist` checks would fail otherwise, since
> a demo tick would populate them), and the dashboards genuinely appear only *after* the first
> reading — the on-screen transition Phase 7 could not observe on device because the demos tick too
> quickly.
>
> Note: `createAndroidComposeRule` is deprecated in Compose 1.11 in favor of the
> `androidx.compose.ui.test.junit4.v2` variant, which uses `StandardTestDispatcher` instead of
> `UnconfinedTestDispatcher`. Migrated; the v2 factory returns the same `AndroidComposeTestRule` type,
> so it was an import swap, and all 4 tests still pass.
- Write the full `ThingyPipelineTest` suite (§9.2, 9 tests) and `SensorDashboardsUiTest` against
  whichever fake-vs-real selection mechanism §10 item 6 resolved to.
- **Definition of done:** all tests listed in §9.2 exist and pass; CI (if configured) runs them
  green without requiring an emulator with Bluetooth hardware access.

### Phase 9 — Real-hardware verification & polish

> **🟡 PARTIALLY COMPLETE (2026-07-26).**
>
> **✅ Localization done.** All 15 non-English `values-*/strings.xml` files exist, transcribed verbatim
> from the corresponding iOS `.lproj/Localizable.strings` (never machine-translated). Qualifiers per
> §8.1, with `zh-Hans` using the modern BCP-47 form `values-b+zh+Hans` rather than the legacy
> `values-zh-rCN`, so it also matches zh-Hans-SG. `./gradlew build` green, **lint 0 errors**.
> Verified on device with a de-DE app locale: `AUS`, `LOSGELASSEN`, `23,4 °C`.
>
> Two lint findings resolved rather than suppressed wholesale: `app_name`/`scanner_title` are
> `translatable="false"` (brand names), and `TypographyEllipsis` is disabled project-wide with a
> comment — `Scanning...`/`Reading...` must keep three periods because the iOS source does, so taking
> lint's U+2026 advice would render different text from the same key.
>
> **Cross-checked against the iOS hand-off's §6 (`IOS_TASK_localize_readings_REPLY.md`):**
> - §6.1 locale set of 18 → matched; ships 17 non-English files. Qualifier forms verified by
>   on-device resolution (see §10 item 12).
> - §6.2 conventions → matched, because the transcription was re-run *after* their pass: Finnish
>   reads `PÄÄLLÄ`/`POIS` (not the withdrawn `SYTYTÄ`/`SAMMUTA`), and `LED` is spelled out only in
>   non-Latin scripts (`Светодиод`, `Світлодіод`, `एलईडी`, `LED 灯`, `LED 燈`, `Đèn LED`).
> - §6.3 the empty-state truncation bug → **does not reproduce on Android.** The exact strings they
>   cite render in full: de "2. Stellen Sie sicher, dass die Knopfzelle geladen ist." and ru
>   "2. Убедитесь, что батарейка-таблетка заряжена." Compose's `Text` defaults to unbounded
>   `maxLines` and the empty state's `Column` imposes no height constraint, so the trap they hit does
>   not exist here. Verified **visually from screenshots**, because `uiautomator` reports the semantic
>   string rather than the rendered one and would not reveal a visual ellipsis.
> - §6.4 `ja` must show a period → confirmed: `21.8 °C`, `1013.1 hPa`.
> - §6.5 scope → matched: no multi-density or font-scale testing done, deferred on both platforms.
>
> **⛔ Hardware verification pending — no hardware available (2026-07-26).** Needs a physical Thingy:52
> **and** a physical Android device; the emulator has no BLE radio, so `ThingyGatt`'s
> connect/notify/read pipeline remains entirely unproven. Logged as pending rather than done, exactly
> as the iOS project's own checklist stayed partially open for the same reason. Everything below the
> localization bullet is still outstanding.

- Run the full app against a real Thingy:52 on a physical Android device: discovery/RSSI, connect,
  LED toggle with read-back, button press/haptic, environment/motion dashboards populating,
  disconnect/reconnect, Bluetooth-off handling, a failed-connection scenario — mirroring the iOS
  project's `nRFThingy52BLEStatus.md` §9/§13/§14 hardware checklists item-for-item.
- Full 16-locale `strings.xml` population (§8.2) with real translated text pulled from each iOS
  `.lproj` file, not machine translation.
- Restyle the existing adaptive launcher icon (§7.3) with Nordic branding.
- **Definition of done:** feature-complete parity with the iOS app as of 2026-07-22 (through the
  Motion dashboard), verified on real hardware, localized in all 16 languages, matching the
  visual brand.
