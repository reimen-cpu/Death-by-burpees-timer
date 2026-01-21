package com.timer.minimal

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "timer_prefs"
        private const val KEY_LAST_DURATION = "last_duration_minutes"
        private const val DEFAULT_DURATION = 5
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun saveLastDuration(minutes: Int) {
        prefs.edit().putInt(KEY_LAST_DURATION, minutes).apply()
    }
    
    fun getLastDuration(): Int {
        return prefs.getInt(KEY_LAST_DURATION, DEFAULT_DURATION)
    }
}
