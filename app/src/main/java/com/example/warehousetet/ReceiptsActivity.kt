//package com.example.warehousetet
//
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.drawable.ColorDrawable
//import android.os.Bundle
//import android.util.Log
//import android.view.Menu
//import android.view.MenuItem
//import android.widget.ImageButton
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.*
//
//class ReceiptsActivity : AppCompatActivity() {
//
//    private lateinit var receiptsAdapter: ReceiptsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var credentialManager: CredentialManager
//    private var refreshJob: Job? = null
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_receipts)
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//
//        credentialManager = CredentialManager(this)
//        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
//
//        initializeRecyclerView()
//        fetchReceiptsAndDisplay()
//        startPeriodicRefresh()
//    }
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }
//
//    override fun onResume() {
//        super.onResume()
//        startPeriodicRefresh()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        stopPeriodicRefresh()
//    }
//
//    private fun initializeRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.receiptsRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        receiptsAdapter = ReceiptsAdapter(listOf()) { receipt ->
//            // Launch ProductsActivity with receipt ID
//            Intent(this, ProductsActivity::class.java).also { intent ->
//                intent.putExtra("RECEIPT_ID", receipt.id)
//                intent.putExtra("RECEIPT_NAME", receipt.name)
//                intent.putExtra("RECEIPT_ORIGIN", receipt.origin) // Pass the receipt name as well
//                startActivity(intent)
//            }
//            coroutineScope.launch {
//                odooXmlRpcClient.fetchAndLogBuyerDetails(receipt.name)
//            }
//        }
//        recyclerView.adapter = receiptsAdapter
//    }
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> {
//                onBackPressed()
//                return true
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }
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
//        refreshJob?.cancel() // Cancel any existing job to avoid duplicates
//        refreshJob = coroutineScope.launch {
//            while (isActive) {
//                fetchReceiptsAndDisplay()
//                delay(5000) // Refresh every 5 seconds
//            }
//        }
//    }
//
//    private fun stopPeriodicRefresh() {
//        refreshJob?.cancel()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        refreshJob?.cancel()
//        coroutineScope.cancel()
//    }
//}


package com.example.warehousetet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class ReceiptsActivity : AppCompatActivity() {

    private lateinit var receiptsAdapter: ReceiptsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private var refreshJob: Job? = null
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipts)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        initializeRecyclerView()
        fetchReceiptsAndDisplay()
        startPeriodicRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_widget_button)
        val searchView = searchItem.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                receiptsAdapter.filter(newText ?: "")
                return true
            }
        })

        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                isSearching = true
                manageRefresh()
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                isSearching = false
                manageRefresh()
                return true
            }
        })

        return true
    }

    private fun manageRefresh() {
        if (isSearching) {
            stopPeriodicRefresh()
        } else {
            startPeriodicRefresh()
        }
    }

    private fun initializeRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.receiptsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        receiptsAdapter = ReceiptsAdapter(listOf()) { receipt ->
            Intent(this, ProductsActivity::class.java).also { intent ->
                intent.putExtra("RECEIPT_ID", receipt.id)
                intent.putExtra("RECEIPT_NAME", receipt.name)
                intent.putExtra("RECEIPT_ORIGIN", receipt.origin)
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
        refreshJob?.cancel() // Cancel any existing job to avoid duplicates
        refreshJob = coroutineScope.launch {
            while (isActive) {
                fetchReceiptsAndDisplay()
                delay(5000) // Refresh every 5 seconds
            }
        }
    }

    private fun stopPeriodicRefresh() {
        refreshJob?.cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel()
        coroutineScope.cancel()
    }
}
