package com.timer.minimal

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
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
    
    private lateinit var inputMinutes: EditText
    private lateinit var timerDisplay: TextView
    private lateinit var progressBar: ProgressBar
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
        
        initViews()
        initViewModel()
        requestNotificationPermission()
    }
    
    private fun initViews() {
        inputMinutes = findViewById(R.id.inputMinutes)
        timerDisplay = findViewById(R.id.timerDisplay)
        progressBar = findViewById(R.id.progressBar)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        
        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener { onStopClicked() }
        btnReset.setOnClickListener { onResetClicked() }
        
        inputMinutes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                val minutes = text.toIntOrNull()
                if (minutes != null && minutes in 1..999) {
                    viewModel.setInputMinutes(minutes)
                }
            }
        })
    }
    
    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[TimerViewModel::class.java]
        
        viewModel.inputMinutes.observe(this) { minutes ->
            if (inputMinutes.text.toString() != minutes.toString()) {
                inputMinutes.setText(minutes.toString())
            }
        }
        
        viewModel.timeRemainingMs.observe(this) { timeMs ->
            timerDisplay.text = viewModel.formatTime(timeMs)
            
            if (serviceBound && viewModel.timerState.value == TimerState.RUNNING) {
                timerService?.updateNotification(viewModel.formatTime(timeMs))
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
    }
    
    private fun updateUI(state: TimerState) {
        when (state) {
            TimerState.IDLE -> {
                inputMinutes.isEnabled = true
                (btnStart as com.google.android.material.button.MaterialButton).setIconResource(R.drawable.ic_play)
                btnStart.visibility = View.VISIBLE
                btnStop.visibility = View.GONE
                btnReset.visibility = View.GONE
                progressBar.progress = 0
            }
            TimerState.RUNNING -> {
                inputMinutes.isEnabled = false
                (btnStart as com.google.android.material.button.MaterialButton).setIconResource(R.drawable.ic_pause)
                btnStart.visibility = View.VISIBLE
                btnStop.visibility = View.VISIBLE
                btnReset.visibility = View.GONE
            }
            TimerState.PAUSED -> {
                inputMinutes.isEnabled = false
                (btnStart as com.google.android.material.button.MaterialButton).setIconResource(R.drawable.ic_play)
                btnStart.visibility = View.VISIBLE
                btnStop.visibility = View.VISIBLE
                btnReset.visibility = View.VISIBLE
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
                timerService?.updateNotification("Pausado")
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
