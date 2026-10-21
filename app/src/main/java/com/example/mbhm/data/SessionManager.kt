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

        /**
         * Keeps a session alive across configuration changes (rotation) without writing it
         * to disk when "Remember me" is off. A cold start starts with null again.
         */
        @Volatile
        private var activeUsername: String? = null
    }

    fun saveSession(username: String, rememberMe: Boolean = true) {
        activeUsername = username
        val editor = prefs.edit()
            .putLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (rememberMe) {
            editor.putString(KEY_USERNAME, username)
        } else {
            editor.remove(KEY_USERNAME)
        }
        editor.apply()
    }

    fun getLoggedInUsername(): String? {
        return activeUsername ?: prefs.getString(KEY_USERNAME, null)
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
        activeUsername = null
        prefs.edit().clear().apply()
    }
}
