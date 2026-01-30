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

enum class TimerPhase {
    WORK,
    REST
}

enum class TimerMode {
    ROUTINE,       // Intervalos trabajo/descanso
    DEATH_BURPEES  // Beep cada minuto con aviso 5s
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
    
    // Nuevas propiedades para intervalos
    private val _currentPhase = MutableLiveData(TimerPhase.WORK)
    val currentPhase: LiveData<TimerPhase> = _currentPhase
    
    private val _currentSet = MutableLiveData(1)
    val currentSet: LiveData<Int> = _currentSet
    
    private val _totalSets = MutableLiveData(preferencesManager.getTotalSets())
    val totalSets: LiveData<Int> = _totalSets
    
    private val _workDuration = MutableLiveData(preferencesManager.getWorkDuration())
    val workDuration: LiveData<Int> = _workDuration
    
    private val _restDuration = MutableLiveData(preferencesManager.getRestDuration())
    val restDuration: LiveData<Int> = _restDuration
    
    // Legacy - mantener compatibilidad
    private val _inputMinutes = MutableLiveData(preferencesManager.getLastDuration())
    val inputMinutes: LiveData<Int> = _inputMinutes
    
    private var pausedTimeMs: Long = 0L
    private var pausedPhase: TimerPhase = TimerPhase.WORK
    private var pausedSet: Int = 1
    
    // Modo actual del temporizador
    private var _timerMode: TimerMode = TimerMode.ROUTINE
    val timerMode: TimerMode get() = _timerMode
    
    fun setTimerMode(mode: TimerMode) {
        _timerMode = mode
    }
    
    fun setWorkDuration(seconds: Int) {
        if (seconds in 5..3600) {
            _workDuration.value = seconds
            preferencesManager.saveWorkDuration(seconds)
        }
    }
    
    fun setRestDuration(seconds: Int) {
        if (seconds in 0..3600) {
            _restDuration.value = seconds
            preferencesManager.saveRestDuration(seconds)
        }
    }
    
    fun setTotalSets(sets: Int) {
        if (sets in 1..99) {
            _totalSets.value = sets
            preferencesManager.saveTotalSets(sets)
        }
    }
    
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
                when (_timerMode) {
                    TimerMode.ROUTINE -> {
                        _currentSet.value = 1
                        _currentPhase.value = TimerPhase.WORK
                        startPhase(TimerPhase.WORK)
                    }
                    TimerMode.DEATH_BURPEES -> {
                        // Death by Burpees: usar inputMinutes como duración total
                        val minutes = _inputMinutes.value ?: 10
                        val durationMs = minutes * 60 * 1000L
                        _totalTimeMs.value = durationMs
                        _soundManager.playWorkStartBeep()
                        startTimer(durationMs)
                    }
                }
            }
            TimerState.PAUSED -> {
                startTimer(pausedTimeMs)
            }
            else -> {}
        }
    }
    
    private fun startPhase(phase: TimerPhase) {
        _currentPhase.value = phase
        
        val durationMs = when (phase) {
            TimerPhase.WORK -> (_workDuration.value ?: 60) * 1000L
            TimerPhase.REST -> (_restDuration.value ?: 180) * 1000L
        }
        
        _totalTimeMs.value = durationMs
        
        // Sonido distintivo al iniciar fase
        if (phase == TimerPhase.WORK) {
            _soundManager.playWorkStartBeep()
        } else {
            _soundManager.playRestStartBeep()
        }
        
        startTimer(durationMs)
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
                onPhaseComplete()
            }
        }.start()
    }
    
    private fun onPhaseComplete() {
        val phase = _currentPhase.value ?: TimerPhase.WORK
        val set = _currentSet.value ?: 1
        val total = _totalSets.value ?: 1
        val restSeconds = _restDuration.value ?: 0
        
        when (phase) {
            TimerPhase.WORK -> {
                if (set >= total) {
                    // Última serie completada - finalizar
                    _soundManager.playFinalBeep()
                    _timerState.value = TimerState.IDLE
                } else if (restSeconds > 0) {
                    // Iniciar descanso
                    startPhase(TimerPhase.REST)
                } else {
                    // Sin descanso, siguiente serie inmediatamente
                    _currentSet.value = set + 1
                    startPhase(TimerPhase.WORK)
                }
            }
            TimerPhase.REST -> {
                // Descanso completado, siguiente serie
                _currentSet.value = set + 1
                startPhase(TimerPhase.WORK)
            }
        }
    }
    
    private fun handleSecondTick(secondsRemaining: Int) {
        when (_timerMode) {
            TimerMode.ROUTINE -> {
                // Beep progresivo de advertencia 10 a 1 segundos antes de terminar fase
                if (secondsRemaining in 1..10 && secondsRemaining > 0) {
                    _soundManager.playWarningBeep(secondsRemaining)
                }
            }
            TimerMode.DEATH_BURPEES -> {
                val totalSeconds = (_totalTimeMs.value ?: 0L) / 1000
                val elapsedSeconds = totalSeconds - secondsRemaining
                val secondsIntoCurrentMinute = elapsedSeconds % 60
                
                // Beep al INICIO de cada minuto (cuando empieza un nuevo minuto)
                if (secondsIntoCurrentMinute == 0L && elapsedSeconds > 0) {
                    _soundManager.playMinuteBeep()
                }
                
                // Aviso 10 segundos antes de cada minuto (segundos 50-59)
                // Convertir a "segundos que faltan para el minuto" (10 a 1)
                if (secondsIntoCurrentMinute in 50..59) {
                    val secondsToMinute = (60 - secondsIntoCurrentMinute).toInt()
                    _soundManager.playWarningBeep(secondsToMinute)
                }
            }
        }
    }
    
    fun pause() {
        if (_timerState.value == TimerState.RUNNING) {
            countDownTimer?.cancel()
            pausedTimeMs = _timeRemainingMs.value ?: 0L
            pausedPhase = _currentPhase.value ?: TimerPhase.WORK
            pausedSet = _currentSet.value ?: 1
            _timerState.value = TimerState.PAUSED
        }
    }
    
    fun stop() {
        countDownTimer?.cancel()
        _timerState.value = TimerState.IDLE
        _timeRemainingMs.value = 0L
        _currentSet.value = 1
        _currentPhase.value = TimerPhase.WORK
        pausedTimeMs = 0L
    }
    
    fun reset() {
        stop()
        val workSeconds = _workDuration.value ?: 60
        _totalTimeMs.value = workSeconds * 1000L
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
