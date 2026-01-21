package com.timer.minimal

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*

class SoundManager(private val context: Context) {
    
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        initToneGenerator()
    }
    
    private fun initToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Beep corto de advertencia (200ms @ 650Hz aproximado)
     * Se reproduce cada segundo de 5 a 1 antes del minuto
     */
    fun playWarningBeep() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                vibrate(100)
            } catch (e: Exception) {
                e.printStackTrace()
                initToneGenerator()
            }
        }
    }
    
    /**
     * Beep largo de minuto (1000ms @ 900Hz aproximado)
     * Se reproduce cuando se completa cada minuto
     */
    fun playMinuteBeep() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
                vibrate(500)
            } catch (e: Exception) {
                e.printStackTrace()
                initToneGenerator()
            }
        }
    }
    
    /**
     * Beep final largo (2000ms @ 900Hz aproximado)
     * Se reproduce cuando el temporizador llega a 00:00
     */
    fun playFinalBeep() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000)
                vibrate(1000)
            } catch (e: Exception) {
                e.printStackTrace()
                initToneGenerator()
            }
        }
    }
    
    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        scope.cancel()
        toneGenerator?.release()
        toneGenerator = null
    }
}
