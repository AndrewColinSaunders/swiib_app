package com.example.warehousetet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class PackActivity : AppCompatActivity() {
    private lateinit var packAdapter: PackAdapter
    private lateinit var credentialManager: CredentialManager
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private val refreshScope = CoroutineScope(Dispatchers.IO)
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pack)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        packAdapter = PackAdapter()
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView_packs) // Ensure this ID matches your RecyclerView in activity_pack.xml
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PackActivity)
            adapter = packAdapter
        }

        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        refreshJob = refreshScope.launch {
            while (isActive) {
                try {
                    val packs = odooXmlRpcClient.fetchInternalTransfersWithProductDetails() // Adapt fetch method if necessary
                    withContext(Dispatchers.Main) {
                        packAdapter.submitFilteredPacks(packs) // Filter and display the data
                    }
                } catch (e: Exception) {
                    // Handle any potential errors here
                    withContext(Dispatchers.Main) {
                        // Log error or notify user
                    }
                }
                delay(5000) // Adjust the refresh delay as needed
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel() // Prevent memory leaks by cancelling the job when the activity is destroyed
    }
}
