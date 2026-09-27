package com.example.mbhm.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mbhm_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "logged_in_username"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
        const val TIMEOUT_MILLIS = 10 * 60 * 1000L // 10 minutes
    }

    fun saveSession(username: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putLong(KEY_LAST_BACKGROUND_TIME, 0L)
            .apply()
    }

    fun getLoggedInUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun recordBackgroundTime() {
        if (getLoggedInUsername() != null) {
            prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis()).apply()
        }
    }

    fun isSessionExpired(): Boolean {
        val username = getLoggedInUsername() ?: return false
        val lastBg = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBg <= 0L) return false
        val elapsed = System.currentTimeMillis() - lastBg
        return elapsed >= TIMEOUT_MILLIS
    }

    fun clearBackgroundTime() {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, 0L).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
