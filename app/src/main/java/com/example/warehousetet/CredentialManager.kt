package com.example.warehousetet

import android.content.Context
import android.content.SharedPreferences

class CredentialManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("OdooPrefs", Context.MODE_PRIVATE)

    fun storeUserCredentials(username: String, password: String, userId: Int) {
        with(sharedPreferences.edit()) {
            putString("username", username)
            putString("password", password)
            putInt("userId", userId)
            apply()
        }
    }

    fun getUserId(): Int = sharedPreferences.getInt("userId", -1)
    fun getUsername(): String? = sharedPreferences.getString("username", null)
    fun getPassword(): String? = sharedPreferences.getString("password", null)

    fun storeSessionId(sessionId: String) {
        with(sharedPreferences.edit()) {
            putString("sessionId", sessionId)
            apply()
        }
    }

    fun getSessionId(): String? = sharedPreferences.getString("sessionId", null)

    fun clearSession() {
        with(sharedPreferences.edit()) {
            remove("sessionId")
            // Consider also clearing other user credentials if logging out
            remove("username")
            remove("password")
            remove("userId")
            apply()
        }
    }
}
