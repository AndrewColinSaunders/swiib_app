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

//user inputs db name only
//package com.example.warehousetet
//
//import android.content.Context
//
//object Constants {
//    var DATABASE: String = ""
//    var URL: String = ""
//
//    fun update(context: Context) {
//        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
//        DATABASE = prefs.getString("DATABASE", DATABASE) ?: DATABASE
//        // Set URL using the updated DATABASE value
//        URL = "https://$DATABASE.swiib.app/"
//    }
//}
