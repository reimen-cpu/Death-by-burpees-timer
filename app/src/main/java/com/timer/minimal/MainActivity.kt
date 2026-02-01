package com.timer.minimal

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: TimerViewModel
    
    private lateinit var inputWorkDuration: EditText
    private lateinit var inputRestDuration: EditText
    private lateinit var inputTotalSets: EditText
    private lateinit var timerDisplay: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var phaseIndicator: TextView
    private lateinit var setCounter: TextView
    private lateinit var workDurationLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var restDurationLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var toggleWorkUnit: com.google.android.material.button.MaterialButtonToggleGroup
    private lateinit var toggleRestUnit: com.google.android.material.button.MaterialButtonToggleGroup
    
    // Multiplicadores actuales
    private var workMultiplier = 1
    private var restMultiplier = 1
    
    // Modo de temporizador
    private var currentMode: TimerMode = TimerMode.ROUTINE
    
    // Flags para evitar bucles de actualización
    private var isUpdatingFromViewModel = false
    
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button
    
    private var timerService: TimerService? = null
    private var serviceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            serviceBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            serviceBound = false
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Leer modo desde Intent
        val modeExtra = intent.getStringExtra(ModeSelectionActivity.EXTRA_MODE)
        currentMode = when (modeExtra) {
            ModeSelectionActivity.MODE_DEATH_BURPEES -> TimerMode.DEATH_BURPEES
            else -> TimerMode.ROUTINE
        }
        
        initViews()
        initViewModel()
        setupModeUI()
        requestNotificationPermission()
    }
    
    private fun setupModeUI() {
        // Configurar modo en ViewModel
        viewModel.setTimerMode(currentMode)
        
        // Ocultar/mostrar controles según modo
        val configCard = findViewById<View>(R.id.configCard)
        
        when (currentMode) {
            TimerMode.ROUTINE -> {
                // Mostrar configuración completa de intervalos
                configCard.visibility = View.VISIBLE
                phaseIndicator.visibility = View.VISIBLE
                setCounter.visibility = View.VISIBLE
            }
            TimerMode.DEATH_BURPEES -> {
                // Ocultar configuración de intervalos, solo tiempo total
                configCard.visibility = View.GONE
                phaseIndicator.text = getString(R.string.mode_death_burpees)
                setCounter.visibility = View.GONE
            }
        }
    }
    
    private fun initViews() {
        inputWorkDuration = findViewById(R.id.inputWorkDuration)
        inputRestDuration = findViewById(R.id.inputRestDuration)
        inputTotalSets = findViewById(R.id.inputTotalSets)
        workDurationLayout = findViewById(R.id.workDurationLayout)
        restDurationLayout = findViewById(R.id.restDurationLayout)
        toggleWorkUnit = findViewById(R.id.toggleWorkUnit)
        toggleRestUnit = findViewById(R.id.toggleRestUnit)
        
        timerDisplay = findViewById(R.id.timerDisplay)
        progressBar = findViewById(R.id.progressBar)
        phaseIndicator = findViewById(R.id.phaseIndicator)
        setCounter = findViewById(R.id.setCounter)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        
        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener { onStopClicked() }
        btnReset.setOnClickListener { onResetClicked() }
        
        setupInputs()
        setupToggles()
    }
    
    private fun setupInputs() {
        // Listener para duración de trabajo
        inputWorkDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromViewModel) return
                updateWorkDurationFromInput()
            }
        })
        
        // Listener para duración de descanso
        inputRestDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromViewModel) return
                updateRestDurationFromInput()
            }
        })
        
        // Listener para número de series
        inputTotalSets.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromViewModel) return
                val text = s?.toString() ?: return
                val sets = text.toIntOrNull()
                if (sets != null && sets in 1..99) {
                    viewModel.setTotalSets(sets)
                }
            }
        })
    }
    
    private fun setupToggles() {
        toggleWorkUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                workMultiplier = if (checkedId == R.id.btnWorkMin) 60 else 1
                // Actualizar hint dinámicamente
                workDurationLayout.hint = getString(
                    if (checkedId == R.id.btnWorkMin) R.string.hint_work_duration_min
                    else R.string.hint_work_duration_sec
                )
                if (!isUpdatingFromViewModel) updateWorkDurationFromInput()
            }
        }
        
        toggleRestUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                restMultiplier = if (checkedId == R.id.btnRestMin) 60 else 1
                // Actualizar hint dinámicamente
                restDurationLayout.hint = getString(
                    if (checkedId == R.id.btnRestMin) R.string.hint_rest_duration_min
                    else R.string.hint_rest_duration_sec
                )
                if (!isUpdatingFromViewModel) updateRestDurationFromInput()
            }
        }
    }
    
    private fun updateWorkDurationFromInput() {
        val text = inputWorkDuration.text?.toString() ?: return
        val value = text.toIntOrNull()
        if (value != null) {
            val totalSeconds = value * workMultiplier
            // Validar rango (5s a 60m)
            if (totalSeconds in 5..3600) {
                viewModel.setWorkDuration(totalSeconds)
            }
        }
    }
    
    private fun updateRestDurationFromInput() {
        val text = inputRestDuration.text?.toString() ?: return
        val value = text.toIntOrNull()
        if (value != null) {
            val totalSeconds = value * restMultiplier
            // Validar rango (0s a 60m)
            if (totalSeconds in 0..3600) {
                viewModel.setRestDuration(totalSeconds)
            }
        }
    }
    
    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[TimerViewModel::class.java]
        
        // Cargar valores guardados
        viewModel.workDuration.observe(this) { seconds ->
            if (!inputWorkDuration.hasFocus()) {
                isUpdatingFromViewModel = true
                if (seconds % 60 == 0 && seconds >= 60) {
                    toggleWorkUnit.check(R.id.btnWorkMin)
                    inputWorkDuration.setText((seconds / 60).toString())
                } else {
                    toggleWorkUnit.check(R.id.btnWorkSec)
                    inputWorkDuration.setText(seconds.toString())
                }
                isUpdatingFromViewModel = false
            }
        }
        
        viewModel.restDuration.observe(this) { seconds ->
            if (!inputRestDuration.hasFocus()) {
                isUpdatingFromViewModel = true
                if (seconds % 60 == 0 && seconds >= 60) {
                    toggleRestUnit.check(R.id.btnRestMin)
                    inputRestDuration.setText((seconds / 60).toString())
                } else {
                    toggleRestUnit.check(R.id.btnRestSec)
                    inputRestDuration.setText(seconds.toString())
                }
                isUpdatingFromViewModel = false
            }
        }
        
        viewModel.totalSets.observe(this) { sets ->
            if (!inputTotalSets.hasFocus()) {
                isUpdatingFromViewModel = true
                inputTotalSets.setText(sets.toString())
                isUpdatingFromViewModel = false
            }
        }
        
        viewModel.timeRemainingMs.observe(this) { timeMs ->
            timerDisplay.text = viewModel.formatTime(timeMs)
            
            if (serviceBound && viewModel.timerState.value == TimerState.RUNNING) {
                val phase = if (viewModel.currentPhase.value == TimerPhase.WORK) "💪" else "😮‍💨"
                timerService?.updateNotification("$phase ${viewModel.formatTime(timeMs)}")
            }
            
            val totalMs = viewModel.totalTimeMs.value ?: 1L
            if (totalMs > 0) {
                val progress = ((timeMs.toFloat() / totalMs.toFloat()) * 100).toInt()
                progressBar.progress = progress
            }
        }
        
        viewModel.timerState.observe(this) { state ->
            updateUI(state)
        }
        
        viewModel.currentPhase.observe(this) { phase ->
            updatePhaseIndicator(phase)
        }
        
        viewModel.currentSet.observe(this) { currentSet ->
            val total = viewModel.totalSets.value ?: 1
            setCounter.text = getString(R.string.set_counter, currentSet, total)
        }
    }
    
    private fun updatePhaseIndicator(phase: TimerPhase) {
        when (phase) {
            TimerPhase.WORK -> {
                phaseIndicator.text = getString(R.string.phase_work)
                phaseIndicator.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))
            }
            TimerPhase.REST -> {
                phaseIndicator.text = getString(R.string.phase_rest)
                phaseIndicator.setTextColor(Color.parseColor("#4FC3F7")) // Azul claro para descanso
            }
        }
    }
    
    private fun updateUI(state: TimerState) {
        when (state) {
            TimerState.IDLE -> {
                inputWorkDuration.isEnabled = true
                inputRestDuration.isEnabled = true
                inputTotalSets.isEnabled = true
                (btnStart as com.google.android.material.button.MaterialButton).setIconResource(R.drawable.ic_play)
                btnStart.visibility = View.VISIBLE
                btnStop.visibility = View.GONE
                btnReset.visibility = View.GONE
                phaseIndicator.visibility = View.INVISIBLE
                setCounter.visibility = View.INVISIBLE
                progressBar.progress = 0
            }
            TimerState.RUNNING -> {
                inputWorkDuration.isEnabled = false
                inputRestDuration.isEnabled = false
                inputTotalSets.isEnabled = false
                (btnStart as com.google.android.material.button.MaterialButton).setIconResource(R.drawable.ic_pause)
                btnStart.visibility = View.VISIBLE
                btnStop.visibility = View.VISIBLE
                btnReset.visibility = View.GONE
                phaseIndicator.visibility = View.VISIBLE
                setCounter.visibility = View.VISIBLE
            }
            TimerState.PAUSED -> {
                inputWorkDuration.isEnabled = false
                inputRestDuration.isEnabled = false
                inputTotalSets.isEnabled = false
                (btnStart as com.google.android.material.button.MaterialButton).setIconResource(R.drawable.ic_play)
                btnStart.visibility = View.VISIBLE
                btnStop.visibility = View.VISIBLE
                btnReset.visibility = View.VISIBLE
                phaseIndicator.visibility = View.VISIBLE
                setCounter.visibility = View.VISIBLE
            }
        }
    }
    
    private fun onStartClicked() {
        when (viewModel.timerState.value) {
            TimerState.IDLE, TimerState.PAUSED -> {
                viewModel.start()
                startTimerService()
            }
            TimerState.RUNNING -> {
                viewModel.pause()
                timerService?.updateNotification("⏸️ Pausado")
            }
            else -> {}
        }
    }
    
    private fun onStopClicked() {
        viewModel.stop()
        stopTimerService()
    }
    
    private fun onResetClicked() {
        viewModel.reset()
    }
    
    private fun startTimerService() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME_REMAINING, timerDisplay.text.toString())
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun stopTimerService() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        
        val intent = Intent(this, TimerService::class.java)
        stopService(intent)
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}
