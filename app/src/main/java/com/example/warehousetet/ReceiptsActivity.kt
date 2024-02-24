package com.example.warehousetet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceiptsActivity : AppCompatActivity() {

    private lateinit var receiptsAdapter: ReceiptsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager // Add this line

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipts)
        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        initializeRecyclerView()
        fetchReceiptsAndDisplay()
    }

    private fun initializeRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.receiptsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        receiptsAdapter = ReceiptsAdapter(listOf()) { receipt ->
            Intent(this, ProductsActivity::class.java).also { intent ->
                intent.putExtra("RECEIPT_ID", receipt.id)
                startActivity(intent)
            }
        }
        recyclerView.adapter = receiptsAdapter
    }

    private fun fetchReceiptsAndDisplay() {
        coroutineScope.launch {
            try {
                val receipts = odooXmlRpcClient.fetchReceipts() // Implement this method to fetch receipts
                withContext(Dispatchers.Main) {
                    receiptsAdapter.updateReceipts(receipts)
                }
            } catch (e: Exception) {
                Log.e("ReceiptsActivity", "Error fetching receipts: ${e.localizedMessage}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
