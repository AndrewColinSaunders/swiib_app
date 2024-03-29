package com.example.warehousetet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class InternalTransfersActivity : AppCompatActivity() {
    private lateinit var credentialManager: CredentialManager
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var internalTransfersAdapter: InternalTransfersAdapter
    private val refreshScope = CoroutineScope(Dispatchers.IO)
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internal_transfers)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        internalTransfersAdapter = InternalTransfersAdapter()
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView_internal_transfers)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@InternalTransfersActivity)
            adapter = internalTransfersAdapter
        }

        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        refreshJob = refreshScope.launch {
            while (isActive) {
                try {
                    val internalTransfers = odooXmlRpcClient.fetchInternalTransfersWithProductDetails()
                    withContext(Dispatchers.Main) {
                        // Use the adapter's method to submit and filter the list
                        internalTransfersAdapter.submitFilteredInternalTransfers(internalTransfers)
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
