package com.example.warehousetet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class PickActivity : AppCompatActivity() {

    private lateinit var pickAdapter: PickAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_orders)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        initializeRecyclerView()
        fetchPicksAndDisplay()
        startPeriodicRefresh()
    }

    override fun onResume() {
        super.onResume()
        startPeriodicRefresh()
    }

    override fun onPause() {
        super.onPause()
        stopPeriodicRefresh()
    }

    private fun initializeRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.deliveryOrdersRecyclerView) // Make sure your layout file for PickActivity includes a RecyclerView with this ID
        recyclerView.layoutManager = LinearLayoutManager(this)
        pickAdapter = PickAdapter(listOf()) { deliveryOrder ->
            // Launch ProductsActivity with delivery order ID, similar to how it's done with receipts
            Intent(this, PickProductsActivity::class.java).also { intent ->
                intent.putExtra("DELIVERY_ORDER_ID", deliveryOrder.id)
                intent.putExtra("DELIVERY_ORDER_NAME", deliveryOrder.name)
                intent.putExtra("DELIVERY_ORDER_ORIGIN", deliveryOrder.origin) // Assuming Pick class includes similar fields as Receipt
                startActivity(intent)
            }
        }
        recyclerView.adapter = pickAdapter
    }

    private fun fetchPicksAndDisplay() {
        coroutineScope.launch {
            try {
                val deliveryOrders = odooXmlRpcClient.fetchPicks() // Implement this method in OdooXmlRpcClient
                withContext(Dispatchers.Main) {
                    pickAdapter.updateDeliveryOrders(deliveryOrders)
                }
            } catch (e: Exception) {
                Log.e("PickActivity", "Error fetching delivery orders: ${e.localizedMessage}")
            }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel() // Cancel any existing job to avoid duplicates
        refreshJob = coroutineScope.launch {
            while (isActive) {
                fetchPicksAndDisplay()
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
                onBackPressed()
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
