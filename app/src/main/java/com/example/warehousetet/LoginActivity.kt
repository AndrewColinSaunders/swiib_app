package com.example.warehousetet


import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

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

            coroutineScope.launch {
                val odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
                val userId = odooXmlRpcClient.login(username, password)

                withContext(Dispatchers.Main) {
                    if (userId > 0) {
                        handleLoginSuccess(username, password, userId)
                    } else {
                        handleLoginFailure("Invalid username or password.")
                    }
                }
            }
        }
    }

    private fun handleLoginSuccess(username: String, password: String, userId: Int) {
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

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}


