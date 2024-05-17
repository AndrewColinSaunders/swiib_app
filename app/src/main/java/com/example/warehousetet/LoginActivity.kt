package com.example.warehousetet


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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
    lateinit var credentialManager: CredentialManager
    lateinit var odooXmlRpcClient: OdooXmlRpcClient
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

//        loginButton.setOnClickListener {
//            val username = usernameEditText.text.toString()
//            val password = passwordEditText.text.toString()
//
//            coroutineScope.launch {
//                val odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
//                val userId = odooXmlRpcClient.login(username, password)
//                Log.d("OdooXmlRpcClient", "Received UserID from login attempt: $userId")
//
//                withContext(Dispatchers.Main) {
//                    if (userId > 0) {
//                        Log.d("OdooXmlRpcClient", "Login successful with UserID: $userId before storing in prefs")
//                        handleLoginSuccess(username, password, userId)
//                        Log.d("OdooXmlRpcClient", "Stored UserId: ${credentialManager.getUserId()} after login")
//                    } else {
//                        handleLoginFailure("Invalid username or password.")
//                    }
//                }
//            }
//
//        }
        loginButton.setOnClickListener {
            if (!isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection available.", Toast.LENGTH_LONG).show()
            } else {
                val username = usernameEditText.text.toString()
                val password = passwordEditText.text.toString()

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

    }

    private fun handleLoginSuccess(username: String, password: String, userId: Int) {
        Log.d("LoginActivity", "Login success with UserId: $userId")
        val credentialManager = CredentialManager(this)
        credentialManager.storeUserCredentials(username, password, userId)
        Toast.makeText(this, "Login successful.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, HomePageActivity::class.java).apply {
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }

    private fun handleLoginFailure(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw      = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            //for other device how are able to connect with Ethernet
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            //for check internet over Bluetooth
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}


