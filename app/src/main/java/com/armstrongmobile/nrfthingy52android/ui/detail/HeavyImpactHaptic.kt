package com.armstrongmobile.nrfthingy52android.ui.detail

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

// A heavy-impact haptic, matching iOS's .sensoryFeedback(.impact(weight: .heavy)) on button press
// (plan §6.2). On API 29+ the predefined EFFECT_HEAVY_CLICK is the closest analogue; below that
// (minSdk is 24) the view's LONG_PRESS constant is the strongest portable feedback. The VIBRATE
// permission is normal — declared in the manifest, no runtime prompt.
@Composable
fun rememberHeavyImpactHaptic(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current

    return remember(context, view) {
        {
            val vibrator = context.vibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            Unit
        }
    }
}

private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
