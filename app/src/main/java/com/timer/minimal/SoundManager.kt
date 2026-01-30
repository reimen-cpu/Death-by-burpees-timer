package com.timer.minimal

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * SoundManager con audio sintetizado premium.
 * Genera tonos distintivos y agradables usando AudioTrack.
 */
class SoundManager(private val context: Context) {
    
    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    
    /**
     * Advertencia progresiva: beeps cortos que suben en frecuencia e intensidad
     * @param secondsToMinute segundos que faltan para el minuto (10 a 1)
     */
    fun playWarningBeep(secondsToMinute: Int = 5) {
        scope.launch {
            // Calcular progresión (10s=0.0, 1s=1.0)
            val progress = 1f - (secondsToMinute - 1) / 9f
            
            // Frecuencia: 400Hz → 1000Hz
            val frequency = 400.0 + (600.0 * progress)
            
            // Duración: 100ms → 50ms
            val duration = (100 - (50 * progress)).toInt()
            
            // Amplitud: 0.5 → 0.9 (más audible)
            val amplitude = 0.5f + (0.4f * progress)
            
            playShortBeep(frequency, duration, amplitude)
        }
    }
    
    /**
     * Genera un beep corto con parámetros específicos
     */
    private fun playShortBeep(frequency: Double, duration: Int, amplitude: Float) {
        val numSamples = duration * sampleRate / 1000
        val buffer = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Envelope rápido
            val envelope = when {
                i < numSamples * 0.1 -> i / (numSamples * 0.1)
                i > numSamples * 0.7 -> (numSamples - i) / (numSamples * 0.3)
                else -> 1.0
            }
            val sample = sin(2 * PI * frequency * t) * amplitude * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        playBuffer(buffer)
    }
    
    /**
     * Minuto cumplido (Death by Burpees): beep largo igual que trabajo
     */
    fun playMinuteBeep() {
        scope.launch {
            playLongToneRich(
                baseFrequency = 880.0, // A5 - tono claro
                duration = 900,        // 900ms largo
                amplitude = 0.65f
            )
            vibrateShort(120)
        }
    }
    
    /**
     * Final: resolución clara y larga (acorde mayor descendente)
     */
    fun playFinalBeep() {
        scope.launch {
            // Acorde C-E-G descendente
            playTone(
                frequencies = listOf(523.25, 659.25, 783.99, 523.25),
                durations = listOf(200, 200, 200, 400),
                amplitude = 0.6f
            )
            vibratePattern(longArrayOf(0, 150, 100, 150))
        }
    }
    
    /**
     * Trabajo / Minuto completado: "beeeeep" largo tipo inicio de carrera
     * Tono sostenido claro y distintivo con armónicos para mejor calidad
     */
    fun playWorkStartBeep() {
        scope.launch {
            playLongToneRich(
                baseFrequency = 880.0, // A5 - tono claro
                duration = 900,        // 900ms largo
                amplitude = 0.65f
            )
            vibrateShort(120)
        }
    }
    
    /**
     * Descanso: tono descendente suave y relajante
     */
    fun playRestStartBeep() {
        scope.launch {
            playTone(
                frequencies = listOf(659.25, 523.25, 392.0),
                durations = listOf(150, 150, 200),
                amplitude = 0.5f
            )
        }
    }
    
    /**
     * Genera un tono largo sostenido con armónicos para mayor riqueza sonora
     */
    private fun playLongToneRich(baseFrequency: Double, duration: Int, amplitude: Float) {
        val numSamples = duration * sampleRate / 1000
        val buffer = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Envelope suave: fade in rápido, sustain largo, fade out suave
            val envelope = when {
                i < numSamples * 0.03 -> i / (numSamples * 0.03) // Fade in 3%
                i > numSamples * 0.85 -> (numSamples - i) / (numSamples * 0.15) // Fade out 15%
                else -> 1.0 // Sustain
            }
            // Fundamental + armónicos para tono más rico
            val fundamental = sin(2 * PI * baseFrequency * t)
            val harmonic2 = sin(2 * PI * baseFrequency * 2 * t) * 0.3
            val harmonic3 = sin(2 * PI * baseFrequency * 3 * t) * 0.15
            val sample = (fundamental + harmonic2 + harmonic3) / 1.45 * amplitude * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        playBuffer(buffer)
    }
    
    /**
     * Genera y reproduce una secuencia de tonos
     */
    private fun playTone(frequencies: List<Double>, durations: List<Int>, amplitude: Float) {
        val totalSamples = durations.sum() * sampleRate / 1000
        val buffer = ShortArray(totalSamples)
        
        var sampleIndex = 0
        for (i in frequencies.indices) {
            val freq = frequencies[i]
            val durationSamples = durations[i] * sampleRate / 1000
            
            for (j in 0 until durationSamples) {
                val t = j.toDouble() / sampleRate
                // Envelope suave (fade in/out)
                val envelope = when {
                    j < durationSamples * 0.1 -> j / (durationSamples * 0.1)
                    j > durationSamples * 0.8 -> (durationSamples - j) / (durationSamples * 0.2)
                    else -> 1.0
                }
                val sample = sin(2 * PI * freq * t) * amplitude * envelope
                buffer[sampleIndex++] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
        }
        
        playBuffer(buffer)
    }
    
    /**
     * Sonido percusivo con decay exponencial
     */
    private fun playPercussive(frequency: Double, duration: Int, amplitude: Float) {
        val numSamples = duration * sampleRate / 1000
        val buffer = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Decay exponencial rápido
            val decay = kotlin.math.exp(-t * 15)
            val sample = sin(2 * PI * frequency * t) * amplitude * decay
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        
        playBuffer(buffer)
    }
    
    private fun playBuffer(buffer: ShortArray) {
        try {
            val bufferSize = buffer.size * 2 // Short = 2 bytes
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            
            // Liberar después de reproducir
            scope.launch {
                delay(buffer.size.toLong() * 1000 / sampleRate + 100)
                audioTrack.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun vibrateShort(durationMs: Long) {
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
    
    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        scope.cancel()
    }
}
