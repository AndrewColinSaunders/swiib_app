
//User enters db name and url
//package com.example.warehousetet
//
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.view.inputmethod.EditorInfo
//import android.widget.Button
//import android.widget.EditText
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//
//class SetupActivity : AppCompatActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_setup)
//
//        supportActionBar?.hide()
//
//        val databaseInput: EditText = findViewById(R.id.databaseInput)
//        val urlInput: EditText = findViewById(R.id.urlInput)
//        val confirmButton: Button = findViewById(R.id.confirmButton)
//
//        // Set up an OnEditorActionListener for databaseInput
//        databaseInput.setOnEditorActionListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
//                // Move focus to urlInput when the Enter key is pressed
//                urlInput.requestFocus()
//                true // Return true to consume the event
//            } else {
//                false // Return false to let the system handle the event as usual
//            }
//        }
//
//        confirmButton.setOnClickListener {
//            val database = databaseInput.text.toString().trim()
//            val url = urlInput.text.toString().trim()
//
//            if (database.isEmpty() || url.isEmpty()) {
//                // Show an error if either field is empty
//                AlertDialog.Builder(this).apply {
//                    setTitle("Error")
//                    setMessage("Both Database and URL fields must be filled out.")
//                    setPositiveButton("OK") { dialog, which ->
//                        dialog.dismiss()
//                    }
//                    show()
//                }
//            } else {
//                // Show AlertDialog to confirm or cancel
//                AlertDialog.Builder(this).apply {
//                    setTitle("Confirmation")
//                    setMessage("Please ensure the database name and URL are correct. You will not be able to edit these later.")
//                    setPositiveButton("Continue") { dialog, which ->
//                        // User confirms their choices are correct, save and proceed
//                        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE).edit()
//                        prefs.putString("DATABASE", database)
//                        prefs.putString("URL", url)
//                        prefs.apply()
//
//                        Constants.update(this@SetupActivity)
//
//                        startActivity(Intent(this@SetupActivity, LoginActivity::class.java))
//                        finish()
//                    }
//                    setNegativeButton("Cancel") { dialog, which ->
//                        // User chooses to cancel, do nothing and let them edit their inputs
//                        dialog.dismiss()
//                    }
//                    show()
//                }
//            }
//        }
//    }
//}
//
//

//user enters only db name
package com.example.warehousetet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        supportActionBar?.hide()

        val databaseInput: EditText = findViewById(R.id.databaseInput)
        val confirmButton: Button = findViewById(R.id.confirmButton)

        confirmButton.setOnClickListener {
            // Trim the database input and replace spaces with an empty string (or another character if preferred)
            val database = databaseInput.text.toString().trim().replace(" ", "")

            if (database.isEmpty()) {
                // Show an error if the database field is empty
                AlertDialog.Builder(this).apply {
                    setTitle("Error")
                    setMessage("The Database field must be filled out.")
                    setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
            } else {
                // Format the URL using the sanitized database name
                val formattedUrl = "https://$database.swiib.app/"

                // Show AlertDialog to confirm or cancel with the formatted URL
                AlertDialog.Builder(this).apply {
                    setTitle("Confirmation")
                    setMessage("Database: $database\nURL: $formattedUrl\n\nPlease confirm your settings.")
                    setPositiveButton("Continue") { dialog, _ ->
                        // User confirms their choices are correct, save and proceed
                        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE).edit()
                        prefs.putString("DATABASE", database)
                        prefs.putString("URL", formattedUrl)
                        prefs.apply()

                        Constants.DATABASE = database
                        Constants.URL = formattedUrl

                        startActivity(Intent(this@SetupActivity, LoginActivity::class.java))
                        finish()
                    }
                    setNegativeButton("Cancel") { dialog, _ ->
                        // User chooses to cancel, do nothing and let them edit their inputs
                        dialog.dismiss()
                    }
                    show()
                }
            }
        }
    }
}
