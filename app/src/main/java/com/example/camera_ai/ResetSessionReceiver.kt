package com.example.camera_ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class ResetSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs: SharedPreferences = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putInt("session_count", 1) // Reset session count to 1
            apply()
        }
    }
}
