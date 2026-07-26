package com.armstrongmobile.nrfthingy52android

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armstrongmobile.nrfthingy52android.ble.fake.ThingyMocks
import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Drives the app from the scanner to the detail screen against the fake transport — the port of the
// iOS nRFThingy52UITests.testSensorDashboardsShow plus the UI-layer half of
// testLEDToggleWritesAndReadsBack and testButtonPressAndReleaseNotify (plan §9.2).
//
// Requires the `mock` flavor: run with `./gradlew connectedMockDebugAndroidTest`. On `prod` these
// skip rather than fail — the direct analogue of the iOS suite's XCTSkipUnless(isSimulator) guard,
// since the real transport has no device to discover on CI.
//
// ThingyApplication suppresses the demo loops under instrumentation, so these tests drive
// ThingyMocks.controller themselves and nothing competes with the values they push.
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SensorDashboardsUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun requireFakeTransport() {
        assumeTrue("Needs the mock flavor's fake transport", BuildConfig.USE_FAKE_TRANSPORT)
    }

    @Test
    fun sensorDashboardsShow() {
        openDetailScreen()

        // Both dashboards are still hidden: no reading has arrived yet, and the sections are gated on
        // any-field-non-null (plan §6.2).
        composeTestRule.onNodeWithText("Environment").assertDoesNotExist()
        composeTestRule.onNodeWithText("Motion").assertDoesNotExist()

        ThingyMocks.controller.simulateEnvironment(
            temperature = 23.5,
            humidity = 48,
            pressure = 1008.75,
            eco2 = 520,
            tvoc = 34,
        )

        composeTestRule.await("Temperature")
        composeTestRule.onNodeWithText("Humidity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pressure").assertIsDisplayed()
        composeTestRule.onNodeWithText("Air Quality").assertIsDisplayed()

        ThingyMocks.controller.simulateOrientation(ThingyOrientation.LANDSCAPE)
        ThingyMocks.controller.simulateStepCount(steps = 42, durationSeconds = 12.5)
        ThingyMocks.controller.simulateHeading(degrees = 90.0)
        ThingyMocks.controller.simulateTap(direction = TapDirection.Z_POSITIVE, count = 2)

        composeTestRule.await("Orientation")
        composeTestRule.onNodeWithText("Steps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Heading").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last Tap").assertIsDisplayed()
    }

    // The LED round-trip at the UI layer, mirroring iOS's testLEDToggleWritesAndReadsBack: the
    // displayed state is the fake firmware's read-back, not the optimistic write.
    @Test
    fun ledToggleWritesAndReadsBack() {
        openDetailScreen()
        composeTestRule.await("OFF")

        composeTestRule.onNode(isToggleable()).performClick()

        composeTestRule.await("ON")
        assert(ThingyMocks.controller.ledIsOn) { "the fake firmware should have recorded the write" }

        composeTestRule.onNode(isToggleable()).performClick()

        composeTestRule.await("OFF")
        assert(!ThingyMocks.controller.ledIsOn) { "the fake firmware should have recorded the write" }
    }

    // Button notifications reaching the row — the piece that can't be checked over adb, because
    // pressButton() has to be called inside the app process (Phase 6 note).
    @Test
    fun buttonPressAndReleaseUpdateTheRow() {
        openDetailScreen()
        composeTestRule.await("RELEASED")

        ThingyMocks.controller.pressButton()
        composeTestRule.await("PRESSED")

        ThingyMocks.controller.releaseButton()
        composeTestRule.await("RELEASED")
    }

    private fun openDetailScreen() {
        // The fake scanner reports the device as soon as scanning starts.
        composeTestRule.await(ThingyMocks.MOCK_NAME)
        composeTestRule.onNodeWithText(ThingyMocks.MOCK_NAME).performClick()
        composeTestRule.await("LED")
    }
}

@OptIn(ExperimentalTestApi::class)
private fun AndroidComposeTestRule<*, *>.await(text: String, timeoutMillis: Long = 10_000) =
    waitUntilExactlyOneExists(hasText(text), timeoutMillis)
