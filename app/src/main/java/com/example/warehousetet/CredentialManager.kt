package com.example.warehousetet

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

//class CredentialManager(context: Context) {
//    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("OdooPrefs", Context.MODE_PRIVATE)
//
//    fun storeUserCredentials(username: String, password: String, userId: Int) {
//        with(sharedPreferences.edit()) {
//            putString("username", username)
//            putString("password", password)
//            putInt("userId", userId)
//            apply()
//        }
//        Log.d("CredentialManager", "Credentials stored: UserId: $userId")
//    }
//
////    fun getUserId(): Int = sharedPreferences.getInt("userId", -1)
//    fun getUserId(): Int {
//        val userId = sharedPreferences.getInt("userId", -1)
//        Log.d("CredentialManager", "Retrieved UserId: $userId")
//        return userId
//    }
////    fun getUsername(): String? = sharedPreferences.getString("username", null)
////    fun getPassword(): String? = sharedPreferences.getString("password", null)
//    fun getUsername(): String? {
//        val username = sharedPreferences.getString("username", null)
//        Log.d("CredentialManager", "Retrieved Username: $username")
//        return username
//    }
//
//    fun getPassword(): String? {
//        val password = sharedPreferences.getString("password", null)
//        Log.d("CredentialManager", "Retrieved Password: $password")
//        return password
//    }
//
//}

open class CredentialManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("OdooPrefs", Context.MODE_PRIVATE)

    fun storeUserCredentials(username: String, password: String, userId: Int) {
        with(sharedPreferences.edit()) {
            putString("username", username)
            putString("password", password)
            putInt("userId", userId)
            apply()
        }
        Log.d("CredentialManager", "Credentials stored: UserId: $userId, Username: $username")
    }

    fun getUserId(): Int {
        val userId = sharedPreferences.getInt("userId", -1)
        Log.d("CredentialManager", "Retrieved UserId: $userId from SharedPreferences")
        return userId
    }
    fun getUsername(): String? {
        val username = sharedPreferences.getString("username", null)
        Log.d("CredentialManager", "Retrieved Username: $username")
        return username
    }
    fun getPassword(): String? {
        val password = sharedPreferences.getString("password", null)
        Log.d("CredentialManager", "Retrieved Password: $password")
        return password
    }
}


