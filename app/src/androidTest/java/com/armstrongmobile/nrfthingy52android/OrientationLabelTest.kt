package com.armstrongmobile.nrfthingy52android

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import com.armstrongmobile.nrfthingy52android.ui.detail.labelRes
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Covers the orientation label mapping that moved out of SensorFormat when the four labels became
// localized (plan §10 item 13). Instrumented rather than JVM because resolving a string resource
// needs a Context — which is the whole point of keeping the mapping out of the pure domain enum.
@RunWith(AndroidJUnit4::class)
class OrientationLabelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun Context.localized(locale: Locale): Context =
        createConfigurationContext(Configuration(resources.configuration).apply {
            setLocale(locale)
        })

    @Test
    fun everyOrientationResolvesToADistinctLabel() {
        val labels = ThingyOrientation.entries.map { context.getString(it.labelRes) }
        assertEquals(4, labels.size)
        assertEquals("labels must be distinct", 4, labels.toSet().size)
        assertTrue("no label may be blank", labels.none { it.isBlank() })
    }

    @Test
    fun englishLabelsMatchTheIosSource() {
        val en = context.localized(Locale.ENGLISH)
        assertEquals("Portrait", en.getString(ThingyOrientation.PORTRAIT.labelRes))
        assertEquals("Landscape", en.getString(ThingyOrientation.LANDSCAPE.labelRes))
        assertEquals(
            "Portrait (upside down)",
            en.getString(ThingyOrientation.REVERSE_PORTRAIT.labelRes),
        )
        assertEquals(
            "Landscape (upside down)",
            en.getString(ThingyOrientation.REVERSE_LANDSCAPE.labelRes),
        )
    }

    // The parity-critical case: a Spanish user used to see the row label "Orientación" with the
    // English value "Portrait". These are the values the iOS app now holds for the same four keys.
    @Test
    fun spanishUsesThePlatformsOwnOrientationVocabulary() {
        val es = context.localized(Locale.forLanguageTag("es"))
        assertEquals("Vertical", es.getString(ThingyOrientation.PORTRAIT.labelRes))
        assertEquals("Horizontal", es.getString(ThingyOrientation.LANDSCAPE.labelRes))
        assertEquals(
            "Vertical (invertido)",
            es.getString(ThingyOrientation.REVERSE_PORTRAIT.labelRes),
        )
    }

    // German is the case that proves these are not literal translations of the English pair:
    // the platform's terms mean "high format" and "cross format".
    @Test
    fun germanUsesHochformatAndQuerformat() {
        val de = context.localized(Locale.GERMAN)
        assertEquals("Hochformat", de.getString(ThingyOrientation.PORTRAIT.labelRes))
        assertEquals("Querformat", de.getString(ThingyOrientation.LANDSCAPE.labelRes))
    }
}
