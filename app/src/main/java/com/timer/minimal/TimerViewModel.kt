package com.timer.minimal

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED
}

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesManager = PreferencesManager(application)
    private val _soundManager = SoundManager(application)
    val soundManager: SoundManager get() = _soundManager
    
    private var countDownTimer: CountDownTimer? = null
    
    private val _timeRemainingMs = MutableLiveData(0L)
    val timeRemainingMs: LiveData<Long> = _timeRemainingMs
    
    private val _totalTimeMs = MutableLiveData(0L)
    val totalTimeMs: LiveData<Long> = _totalTimeMs
    
    private val _timerState = MutableLiveData(TimerState.IDLE)
    val timerState: LiveData<TimerState> = _timerState
    
    private val _inputMinutes = MutableLiveData(preferencesManager.getLastDuration())
    val inputMinutes: LiveData<Int> = _inputMinutes
    
    private var pausedTimeMs: Long = 0L
    
    fun setInputMinutes(minutes: Int) {
        if (minutes in 1..999) {
            _inputMinutes.value = minutes
            preferencesManager.saveLastDuration(minutes)
        }
    }
    
    fun start() {
        val currentState = _timerState.value
        
        when (currentState) {
            TimerState.IDLE -> {
                val minutes = _inputMinutes.value ?: return
                val totalMs = minutes * 60 * 1000L
                _totalTimeMs.value = totalMs
                startTimer(totalMs)
            }
            TimerState.PAUSED -> {
                startTimer(pausedTimeMs)
            }
            else -> {}
        }
    }
    
    private fun startTimer(durationMs: Long) {
        _timerState.value = TimerState.RUNNING
        
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMs, 50) {
            private var lastSecond = -1
            
            override fun onTick(millisUntilFinished: Long) {
                _timeRemainingMs.value = millisUntilFinished
                
                val currentSecond = (millisUntilFinished / 1000).toInt()
                
                if (currentSecond != lastSecond) {
                    lastSecond = currentSecond
                    handleSecondTick(currentSecond)
                }
            }
            
            override fun onFinish() {
                _timeRemainingMs.value = 0L
                _timerState.value = TimerState.IDLE
                _soundManager.playFinalBeep()
            }
        }.start()
    }
    
    private fun handleSecondTick(secondsRemaining: Int) {
        val secondsInMinute = secondsRemaining % 60
        
        // Beep corto de advertencia de 5 a 1 segundos antes del minuto
        if (secondsInMinute in 1..5 && secondsRemaining > 0) {
            _soundManager.playWarningBeep()
        }
        // Beep largo al completar cada minuto (cuando segundos = 0 y no es el final)
        else if (secondsInMinute == 0 && secondsRemaining > 0) {
            _soundManager.playMinuteBeep()
        }
    }
    
    fun pause() {
        if (_timerState.value == TimerState.RUNNING) {
            countDownTimer?.cancel()
            pausedTimeMs = _timeRemainingMs.value ?: 0L
            _timerState.value = TimerState.PAUSED
        }
    }
    
    fun stop() {
        countDownTimer?.cancel()
        _timerState.value = TimerState.IDLE
        _timeRemainingMs.value = 0L
        pausedTimeMs = 0L
    }
    
    fun reset() {
        stop()
        val minutes = _inputMinutes.value ?: 1
        _totalTimeMs.value = minutes * 60 * 1000L
        _timeRemainingMs.value = _totalTimeMs.value
    }
    
    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        _soundManager.release()
    }
    
    fun formatTime(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
