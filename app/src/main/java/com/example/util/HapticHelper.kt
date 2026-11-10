package com.example.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticHelper(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    }

    fun vibrateClick(enabled: Boolean = true) {
        if (!enabled || vibrator?.hasVibrator() != true) return
        try {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } catch (_: Exception) {}
    }

    fun vibrateSuccess(enabled: Boolean = true) {
        if (!enabled || vibrator?.hasVibrator() != true) return
        try {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } catch (_: Exception) {}
    }

    fun vibrateTick(enabled: Boolean = true) {
        if (!enabled || vibrator?.hasVibrator() != true) return
        try {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } catch (_: Exception) {}
    }
}
