package com.example.warehousetet

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder

class ClearStateService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        clearScannedState()
        stopSelf()
    }

    private fun clearScannedState() {
        val scannedProductsPrefs = getSharedPreferences("ScannedProductsPrefs", Context.MODE_PRIVATE).edit()
        scannedProductsPrefs.clear().apply()
    }
}
