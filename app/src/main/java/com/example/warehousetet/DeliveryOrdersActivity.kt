package com.example.warehousetet

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
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

        registerBackPressHandler()
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
        val recyclerView: RecyclerView = findViewById(R.id.deliveryOrdersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        deliveryOrdersAdapter = DeliveryOrdersAdapter(listOf()) { deliveryOrders ->
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
                val deliveryOrders = odooXmlRpcClient.fetchDeliveryOrders()
                withContext(Dispatchers.Main) {
                    if (deliveryOrders.isEmpty()) {
                        findViewById<RecyclerView>(R.id.deliveryOrdersRecyclerView).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.emptyStateLayout).visibility = View.VISIBLE
                    } else {
                        deliveryOrdersAdapter.updateDeliveryOrders(deliveryOrders)
                        findViewById<RecyclerView>(R.id.deliveryOrdersRecyclerView).visibility = View.VISIBLE
                        findViewById<LinearLayout>(R.id.emptyStateLayout).visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("DeliveryOrdersActivity", "Error fetching delivery orders: ${e.localizedMessage}")
            }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = coroutineScope.launch {
            while (isActive) {
                fetchDeliveryOrdersAndDisplay()
                delay(5000)
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

    // Handle the back button press
    private fun registerBackPressHandler() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                // Back is pressed... Finishing the activity
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this /* lifecycle owner */) {
                // Back is pressed... Finishing the activity
                finish()
            }
        }
    }
}
