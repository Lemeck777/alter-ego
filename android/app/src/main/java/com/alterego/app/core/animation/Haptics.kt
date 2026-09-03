package com.alterego.app.core.animation

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.alterego.app.domain.models.HapticPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The buzz that precedes the companion. Short, distinct per pattern, never alarming. */
@Singleton
class Haptics @Inject constructor(@ApplicationContext private val context: Context) {

    fun play(pattern: HapticPattern) {
        if (pattern == HapticPattern.NONE) return
        val timings: LongArray
        val amplitudes: IntArray
        when (pattern) {
            HapticPattern.TAP -> { timings = longArrayOf(0, 28); amplitudes = intArrayOf(0, 120) }
            HapticPattern.DOUBLE_TAP -> { timings = longArrayOf(0, 26, 90, 26); amplitudes = intArrayOf(0, 140, 0, 140) }
            HapticPattern.HEARTBEAT -> { timings = longArrayOf(0, 60, 110, 90); amplitudes = intArrayOf(0, 90, 0, 160) }
            HapticPattern.SOFT -> { timings = longArrayOf(0, 45); amplitudes = intArrayOf(0, 70) }
            HapticPattern.NONE -> return
        }
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java) ?: return
            manager.vibrate(CombinedVibration.createParallel(effect))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Vibrator::class.java) ?: return
            vibrator.vibrate(effect)
        }
    }
}
