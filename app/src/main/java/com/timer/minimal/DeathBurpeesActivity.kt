package com.timer.minimal

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton

class DeathBurpeesActivity : AppCompatActivity() {
    
    private lateinit var viewModel: TimerViewModel
    
    private lateinit var inputMinutes: EditText
    private lateinit var timerDisplay: TextView
    private lateinit var burpeeCounter: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    
    // Colores para gradiente
    private val colorStart = Color.parseColor("#00BCD4") // Cyan/Azul
    private val colorEnd = Color.parseColor("#FF5252")   // Rojo
    
    private var hasLoadedInitialValue = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_death_burpees)
        
        initViews()
        initViewModel()
    }
    
    private fun initViews() {
        inputMinutes = findViewById(R.id.inputMinutes)
        timerDisplay = findViewById(R.id.timerDisplay)
        burpeeCounter = findViewById(R.id.burpeeCounter)
        progressBar = findViewById(R.id.progressBar)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        
        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener { onStopClicked() }
        
        // Guardar minutos cuando cambian - permitir vacío
        inputMinutes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                if (text.isNotEmpty()) {
                    val minutes = text.toIntOrNull()
                    if (minutes != null && minutes in 1..999) {
                        viewModel.setInputMinutes(minutes)
                    }
                }
                // Si está vacío, no hacer nada (permitir que quede vacío)
            }
        })
    }
    
    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[TimerViewModel::class.java]
        viewModel.setTimerMode(TimerMode.DEATH_BURPEES)
        
        // Cargar último valor guardado solo si el input está vacío
        viewModel.inputMinutes.observe(this) { minutes ->
            // Solo actualizar si el campo está vacío al inicio
            if (inputMinutes.text.isNullOrEmpty() && !hasLoadedInitialValue) {
                inputMinutes.setText(minutes.toString())
                hasLoadedInitialValue = true
            }
        }
        
        // Observar tiempo restante
        viewModel.timeRemainingMs.observe(this) { timeMs ->
            timerDisplay.text = viewModel.formatTime(timeMs)
            
            val totalMs = viewModel.totalTimeMs.value ?: 1L
            val elapsedMs = totalMs - timeMs
            
            // Calcular burpees a hacer = minuto actual + 1
            // Empieza en 1, después del minuto 1 son 2 burpees, etc.
            val currentMinute = (elapsedMs / 60000).toInt()
            val burpeesToDo = currentMinute + 1
            burpeeCounter.text = burpeesToDo.toString()
            
            // Calcular progreso TOTAL del ciclo para color (0.0 a 1.0)
            val totalProgress = if (totalMs > 0) elapsedMs.toFloat() / totalMs.toFloat() else 0f
            
            // Aplicar color gradiente basado en progreso total
            val color = ColorUtils.blendARGB(colorStart, colorEnd, totalProgress)
            applyColorToProgress(color)
            burpeeCounter.setTextColor(color)
            
            // Actualizar progreso circular
            if (totalMs > 0) {
                val progress = ((timeMs.toFloat() / totalMs) * 100).toInt()
                progressBar.progress = progress
            }
        }
        
        // Observar estado del timer
        viewModel.timerState.observe(this) { state ->
            when (state) {
                TimerState.IDLE -> {
                    btnStart.setIconResource(R.drawable.ic_play)
                    inputMinutes.isEnabled = true
                    // Resetear colores
                    applyColorToProgress(colorStart)
                    burpeeCounter.setTextColor(colorStart)
                    burpeeCounter.text = "0"
                }
                TimerState.RUNNING -> {
                    btnStart.setIconResource(R.drawable.ic_pause)
                    inputMinutes.isEnabled = false
                }
                TimerState.PAUSED -> {
                    btnStart.setIconResource(R.drawable.ic_play)
                    inputMinutes.isEnabled = false
                }
                null -> {}
            }
        }
    }
    
    private fun applyColorToProgress(color: Int) {
        try {
            val drawable = progressBar.progressDrawable
            if (drawable is LayerDrawable) {
                // Cambiar color del progress (capa 1 o 2 típicamente)
                drawable.getDrawable(1)?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            } else {
                drawable?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun onStartClicked() {
        when (viewModel.timerState.value) {
            TimerState.IDLE, TimerState.PAUSED -> viewModel.start()
            TimerState.RUNNING -> viewModel.pause()
            null -> {}
        }
    }
    
    private fun onStopClicked() {
        viewModel.stop()
        burpeeCounter.text = "0"
        applyColorToProgress(colorStart)
        burpeeCounter.setTextColor(colorStart)
    }
    
    override fun onStart() {
        super.onStart()
        // Iniciar servicio si el timer está corriendo
        if (viewModel.timerState.value == TimerState.RUNNING) {
            startTimerService()
        }
    }
    
    private fun startTimerService() {
        val intent = android.content.Intent(this, TimerService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
