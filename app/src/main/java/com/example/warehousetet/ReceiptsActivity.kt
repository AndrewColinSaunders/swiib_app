//package com.example.warehousetet
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.CoroutineScope
//
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//// Define the ReceiptsActivity class, which extends AppCompatActivity
//class ReceiptsActivity : AppCompatActivity() {
//
//    private lateinit var receiptsAdapter: ReceiptsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var credentialManager: CredentialManager
//    private var refreshJob: Job? = null  // Declare a Job to manage the repeating task
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_receipts)
//
//        credentialManager = CredentialManager(this)
//        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
//
//        initializeRecyclerView()
//
//        // Initial fetch
//        fetchReceiptsAndDisplay()
//
//        // Start periodic refresh
//        startPeriodicRefresh()
//
//    }
//
//    private fun initializeRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.receiptsRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        receiptsAdapter = ReceiptsAdapter(listOf()) { receipt ->
//            Intent(this, ProductsActivity::class.java).also { intent ->
//                intent.putExtra("RECEIPT_ID", receipt.id)
//                startActivity(intent)
//            }
//        }
//        recyclerView.adapter = receiptsAdapter
//    }
//
//
//
//    private fun fetchReceiptsAndDisplay() {
//        coroutineScope.launch {
//            try {
//                val receipts = odooXmlRpcClient.fetchReceipts()
//                withContext(Dispatchers.Main) {
//                    receiptsAdapter.updateReceipts(receipts)
//                }
//            } catch (e: Exception) {
//                Log.e("ReceiptsActivity", "Error fetching receipts: ${e.localizedMessage}")
//            }
//        }
//    }
//
//    private fun startPeriodicRefresh() {
//        refreshJob = coroutineScope.launch {
//            while (isActive) {  // Ensures the loop runs as long as the coroutine is active
//                fetchReceiptsAndDisplay()
//                delay(5000)  // Wait for 10 seconds before the next refresh
//            }
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        refreshJob?.cancel()  // Ensure to cancel the refresh job when the activity is destroyed
//        coroutineScope.cancel()
//    }
//}


package com.example.warehousetet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class ReceiptsActivity : AppCompatActivity() {

    private lateinit var receiptsAdapter: ReceiptsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipts)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        initializeRecyclerView()
        fetchReceiptsAndDisplay()
        startPeriodicRefresh()
    }

    private fun initializeRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.receiptsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        receiptsAdapter = ReceiptsAdapter(listOf()) { receipt ->
            // Launch ProductsActivity with receipt ID
            Intent(this, ProductsActivity::class.java).also { intent ->
                intent.putExtra("RECEIPT_ID", receipt.id)
                intent.putExtra("RECEIPT_NAME", receipt.name)
                intent.putExtra("RECEIPT_ORIGIN", receipt.origin) // Pass the receipt name as well
                startActivity(intent)
            }
            coroutineScope.launch {
                odooXmlRpcClient.fetchAndLogBuyerDetails(receipt.name)
            }
        }
        recyclerView.adapter = receiptsAdapter
    }

    private fun fetchReceiptsAndDisplay() {
        coroutineScope.launch {
            try {
                val receipts = odooXmlRpcClient.fetchReceipts()
                withContext(Dispatchers.Main) {
                    receiptsAdapter.updateReceipts(receipts)
                }
            } catch (e: Exception) {
                Log.e("ReceiptsActivity", "Error fetching receipts: ${e.localizedMessage}")
            }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob = coroutineScope.launch {
            while (isActive) {
                fetchReceiptsAndDisplay()
                delay(5000) // Refresh every 5 seconds
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel()
        coroutineScope.cancel()
    }
}
