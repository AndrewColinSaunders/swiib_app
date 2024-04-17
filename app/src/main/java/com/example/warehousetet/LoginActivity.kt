package com.example.warehousetet


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        supportActionBar?.hide()
        setContentView(R.layout.activity_login)


        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        if (!prefs.contains("DATABASE") || !prefs.contains("URL")) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        Constants.update(this)



        val loginButton = findViewById<Button>(R.id.login_button)
        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)

        val credentialManager = CredentialManager(this)

        usernameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                passwordEditText.requestFocus()
                true
            } else false
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

//            coroutineScope.launch {
//                val odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
//                val userId = odooXmlRpcClient.login(username, password)
//
//                withContext(Dispatchers.Main) {
//                    if (userId > 0) {
//                        handleLoginSuccess(username, password, userId)
//                    } else {
//                        handleLoginFailure("Invalid username or password.")
//                    }
//                }
//            }
            coroutineScope.launch {
                val odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
                val userId = odooXmlRpcClient.login(username, password)
                Log.d("OdooXmlRpcClient", "Received UserID from login attempt: $userId")

                withContext(Dispatchers.Main) {
                    if (userId > 0) {
                        Log.d("OdooXmlRpcClient", "Login successful with UserID: $userId before storing in prefs")
                        handleLoginSuccess(username, password, userId)
                        Log.d("OdooXmlRpcClient", "Stored UserId: ${credentialManager.getUserId()} after login")
                    } else {
                        handleLoginFailure("Invalid username or password.")
                    }
                }
            }

        }
    }

    private fun handleLoginSuccess(username: String, password: String, userId: Int) {
        Log.d("LoginActivity", "Login success with UserId: $userId")
        val credentialManager = CredentialManager(this)
        credentialManager.storeUserCredentials(username, password, userId)
        showGreenToast("Login successful")
//        Toast.makeText(this, "Login successful.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, HomePageActivity::class.java).apply {
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }

    private fun handleLoginFailure(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        showRedToast(errorMessage)
    }


    private fun showRedToast(message: String) {
        val toast = Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT)
        val view = toast.view

        // Get the TextView of the default Toast view
        val text = view?.findViewById<TextView>(android.R.id.message)

        // Set the background color of the Toast view
        view?.background?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)

        // Set the text color to be more visible on the red background, if needed
        text?.setTextColor(Color.WHITE)

        toast.show()
    }
    private fun showGreenToast(message: String) {
        val toast = Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT)
        val view = toast.view

        // Get the TextView of the default Toast view
        val text = view?.findViewById<TextView>(android.R.id.message)

        // Retrieve the success_green color from resources
        val successGreen = ContextCompat.getColor(this@LoginActivity, R.color.success_green)

        // Set the background color of the Toast view to success_green
        view?.background?.setColorFilter(successGreen, PorterDuff.Mode.SRC_IN)

        // Set the text color to be more visible on the green background, if needed
        text?.setTextColor(Color.WHITE)

        toast.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}


