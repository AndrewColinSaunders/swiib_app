package com.example.warehousetet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class PickActivity : AppCompatActivity() {
    private lateinit var pickAdapter: PickAdapter
    private lateinit var credentialManager: CredentialManager
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private val refreshScope = CoroutineScope(Dispatchers.IO)
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        pickAdapter = PickAdapter()
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView_picks) // Make sure this ID matches your layout
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PickActivity)
            adapter = pickAdapter
        }

        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        refreshJob = refreshScope.launch {
            while (isActive) {
                try {
                    val picks = odooXmlRpcClient.fetchInternalTransfersWithProductDetails()
                    withContext(Dispatchers.Main) {
                        // Use the adapter's method to filter and submit the list
                        pickAdapter.submitFilteredPicks(picks)
                    }
                } catch (e: Exception) {
                    // Handle any errors, for example logging or displaying an error message
                    withContext(Dispatchers.Main) {
                        // Log the error, show a message to the user, etc.
                    }
                }
                delay(5000) // Adjust the delay as needed
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel() // Cancel refresh job to prevent memory leaks
    }
}
