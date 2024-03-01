package com.example.warehousetet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.warehousetet.CredentialManager
import com.example.warehousetet.InternalTransfers
import com.example.warehousetet.InternalTransfersAdapter
import com.example.warehousetet.OdooXmlRpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                val internalTransfers = odooXmlRpcClient.fetchInternalTransfersWithProductDetails()
                withContext(Dispatchers.Main) {
                    internalTransfersAdapter.submitList(internalTransfers)
                }
                delay(10_000) // 10 second delay
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel() // Cancel refresh job to prevent memory leaks
    }
}
