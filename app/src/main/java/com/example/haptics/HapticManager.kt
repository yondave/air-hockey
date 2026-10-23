package com.example.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager(context: Context) {
  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  var isHapticsEnabled: Boolean = true

  fun vibrateLight() {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(18)
      }
    } catch (_: Exception) {}
  }

  fun vibrateMedium() {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(38, 200))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(38)
      }
    } catch (_: Exception) {}
  }

  fun vibrateGoal() {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
          VibrationEffect.createWaveform(
            longArrayOf(0, 70, 60, 140, 80, 220),
            intArrayOf(0, 180, 0, 240, 0, 255),
            -1
          )
        )
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0, 70, 60, 140, 80, 220), -1)
      }
    } catch (_: Exception) {}
  }
}
