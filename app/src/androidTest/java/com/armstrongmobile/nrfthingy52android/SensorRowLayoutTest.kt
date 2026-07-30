package com.armstrongmobile.nrfthingy52android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.armstrongmobile.nrfthingy52android.ui.detail.SensorRow
import com.armstrongmobile.nrfthingy52android.ui.theme.ThingyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Regression guard for the two SensorRow layout faults the localized orientation labels exposed.
//
// Both were invisible in English, because until those labels were translated no reading was long
// enough to compete with its own label for width. The Russian value below is the longest of the 68
// (`Альбомная (перевёрнутая)`), which is what surfaced them on a real screen.
//
// Asserted rather than screenshotted deliberately: a screenshot proves it once, an assertion keeps
// proving it. Uses a pure compose rule, so it needs no Activity, no navigation and no scanner tap.
class SensorRowLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    private val label = "Ориентация"
    private val longestValue = "Альбомная (перевёрнутая)"

    private fun render(width: Int) {
        rule.setContent {
            ThingyTheme {
                Box(Modifier.width(width.dp)) {
                    SensorRow(
                        icon = R.drawable.ic_orientation,
                        label = label,
                        value = longestValue,
                    )
                }
            }
        }
    }

    // The weight(1f) spacer collapses to zero once the value claims the rest of the row, which left
    // the value glued to its label with no gap at all.
    @Test
    fun longValueKeepsAGapFromItsLabel() {
        render(width = 360)
        val labelBounds = rule.onNodeWithText(label).getUnclippedBoundsInRoot()
        val valueBounds = rule.onNodeWithText(longestValue).getUnclippedBoundsInRoot()

        val gap = valueBounds.left - labelBounds.right
        assertTrue(
            "value must keep >= 12dp from the label, was $gap (label ends ${labelBounds.right}, " +
                "value starts ${valueBounds.left})",
            gap.value >= 12f,
        )
    }

    // Neither piece of text may be pushed out of the row or clipped to nothing.
    @Test
    fun neitherLabelNorValueIsSqueezedOut() {
        render(width = 360)
        val labelBounds = rule.onNodeWithText(label).getUnclippedBoundsInRoot()
        val valueBounds = rule.onNodeWithText(longestValue).getUnclippedBoundsInRoot()

        // Computed from the edges rather than DpRect.width: the Modifier.width import above shadows
        // that extension property.
        assertTrue("label has zero width", (labelBounds.right - labelBounds.left).value > 0f)
        assertTrue("value has zero width", (valueBounds.right - valueBounds.left).value > 0f)
        assertTrue("value overflows the row", valueBounds.right.value <= 360f)
    }

    // Even at a narrow width the row must lay out rather than collapse — the value wraps instead.
    @Test
    fun narrowRowWrapsRatherThanCollapsing() {
        render(width = 280)
        val valueBounds = rule.onNodeWithText(longestValue).getUnclippedBoundsInRoot()
        assertTrue("value has zero height", (valueBounds.bottom - valueBounds.top).value > 0f)
        assertTrue("value overflows the narrow row", valueBounds.right.value <= 280f)
    }
}
