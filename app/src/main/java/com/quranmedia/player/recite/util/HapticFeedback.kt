package com.quranmedia.player.recite.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/**
 * Utility for providing haptic feedback during recitation
 */
object HapticFeedback {

    /**
     * Provide haptic feedback for a mistake (error pattern)
     * Pattern: SHORT - PAUSE - SHORT - PAUSE - SHORT (triple vibration)
     */
    fun vibrateOnMistake(context: Context) {
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+: Use VibrationEffect
            val pattern = longArrayOf(
                0,     // Start immediately
                100,   // Vibrate 100ms
                50,    // Pause 50ms
                100,   // Vibrate 100ms
                50,    // Pause 50ms
                100    // Vibrate 100ms
            )
            val effect = VibrationEffect.createWaveform(pattern, -1) // -1 = don't repeat
            vibrator.vibrate(effect)
        } else {
            // API < 26: Use deprecated vibrate() method
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(
                0,     // Start immediately
                100,   // Vibrate 100ms
                50,    // Pause 50ms
                100,   // Vibrate 100ms
                50,    // Pause 50ms
                100    // Vibrate 100ms
            )
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1) // -1 = don't repeat
        }
    }

    /**
     * Provide haptic feedback for success (single short vibration)
     */
    fun vibrateOnSuccess(context: Context) {
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+: Use VibrationEffect
            val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            // API < 26: Use deprecated vibrate() method
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    /**
     * Provide haptic feedback for completion (long single vibration)
     */
    fun vibrateOnCompletion(context: Context) {
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+: Use VibrationEffect
            val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            // API < 26: Use deprecated vibrate() method
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    /**
     * Get the Vibrator service (compatible with API 31+)
     */
    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: Use VibratorManager
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            // API < 31: Use deprecated getSystemService
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
