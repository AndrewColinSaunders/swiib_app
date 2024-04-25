package com.example.warehousetet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class DeliveryOrdersActivity : AppCompatActivity() {

    private lateinit var deliveryOrdersAdapter: DeliveryOrdersAdapter
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
        fetchDeliveryOrdersAndDisplay()
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
        deliveryOrdersAdapter = DeliveryOrdersAdapter(listOf()) { deliveryOrders ->
            // Launch ProductsActivity with pick ID, similar to how it's done with receipts
            Intent(this, DeliveryOrdersProductsActivity::class.java).also { intent ->
                Log.d("DeliveryOrdersActivity", "Sending data to DeliveryOrdersProductsActivity: ID=${deliveryOrders.id}, Name=${deliveryOrders.name}, Origin=${deliveryOrders.origin}, Location=${deliveryOrders.locationId}, DestLocation=${deliveryOrders.locationDestId}")
                intent.putExtra("DELIVERY_ORDERS_ID", deliveryOrders.id)
                intent.putExtra("DELIVERY_ORDERS_NAME", deliveryOrders.name)
                intent.putExtra("DELIVERY_ORDERS_ORIGIN", deliveryOrders.origin)
                intent.putExtra("LOCATION", deliveryOrders.locationId)
                intent.putExtra("DEST_LOCATION", deliveryOrders.locationDestId)
                startActivity(intent)
            }
        }
        recyclerView.adapter = deliveryOrdersAdapter
    }

    private fun fetchDeliveryOrdersAndDisplay() {
        coroutineScope.launch {
            try {
                val deliveryOrders = odooXmlRpcClient.fetchDeliveryOrders() // Implement this method in OdooXmlRpcClient
                withContext(Dispatchers.Main) {
                    deliveryOrdersAdapter.updateDeliveryOrders(deliveryOrders)
                }
            } catch (e: Exception) {
                Log.e("PickActivity", "Error fetching picks: ${e.localizedMessage}")
            }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel() // Cancel any existing job to avoid duplicates
        refreshJob = coroutineScope.launch {
            while (isActive) {
                fetchDeliveryOrdersAndDisplay()
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
