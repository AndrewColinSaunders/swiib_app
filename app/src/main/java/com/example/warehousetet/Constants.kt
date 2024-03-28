//package com.example.warehousetet
//
//
//object Constants {
//
//    const val DATABASE = "db2"
//    const val URL = "https://db2.swiib.app/"
//
//}

package com.example.warehousetet

import android.content.Context

object Constants {
    var DATABASE: String = " "
    var URL: String = " "

    fun update(context: Context) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        DATABASE = prefs.getString("DATABASE", DATABASE) ?: DATABASE
        URL = prefs.getString("URL", URL) ?: URL
    }
}
