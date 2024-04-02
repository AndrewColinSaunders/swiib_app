package com.example.warehousetet

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class ClearStateService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.example.warehousetet.CLEAR_SCANNED_STATE") {
            clearScannedState()
        }
        return START_NOT_STICKY
    }

    private fun clearScannedState() {
        val scannedProductsPrefs = getSharedPreferences("ScannedProductsPrefs", Context.MODE_PRIVATE).edit()
        scannedProductsPrefs.clear().apply()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        clearScannedState()
        stopSelf()
    }
}
