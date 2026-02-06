package com.quranmedia.player.wear.presentation.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Manages haptic feedback with different intensities for the Athkar counter.
 */
class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService<VibratorManager>()?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Light haptic feedback for regular counting.
     * Short, gentle vibration that doesn't distract.
     */
    fun lightFeedback() {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(20L)
            }
        }
    }

    /**
     * Medium haptic feedback for milestones (every 10 counts).
     */
    fun mediumFeedback() {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(40L)
            }
        }
    }

    /**
     * Strong haptic feedback for collection completion (33, 10, or 1 count collections).
     * Double pulse pattern that's clearly noticeable.
     */
    fun strongFeedback() {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Double vibration pattern for completion
                val timings = longArrayOf(0, 80, 50, 80)
                val amplitudes = intArrayOf(0, 255, 0, 200)
                vib.vibrate(
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 80, 50, 80), -1)
            }
        }
    }

    /**
     * Performs appropriate haptic feedback based on the counter state.
     * @param remainingBefore The remaining count before this action
     * @param totalCount The total repeat count for this thikr
     */
    fun performCountFeedback(remainingBefore: Int, totalCount: Int) {
        val remainingAfter = remainingBefore - 1

        when {
            // Collection completed
            remainingAfter == 0 -> strongFeedback()
            // Milestone reached (every 10)
            remainingAfter > 0 && (totalCount - remainingAfter) % 10 == 0 -> mediumFeedback()
            // Regular count
            else -> lightFeedback()
        }
    }
}
