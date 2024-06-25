//package com.example.warehousetet
//
//import android.content.Intent
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.widget.LinearLayout
//import androidx.appcompat.widget.SearchView
//import android.window.OnBackInvokedDispatcher
//import androidx.activity.addCallback
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.*
//class DeliveryOrdersActivity : AppCompatActivity() {
//
//    private lateinit var deliveryOrdersAdapter: DeliveryOrdersAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var credentialManager: CredentialManager
//    private var refreshJob: Job? = null
//    private var isSearching = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_delivery_orders)
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//
//        credentialManager = CredentialManager(this)
//        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
//
//        initializeRecyclerView()
//        fetchDeliveryOrdersAndDisplay()
//        startPeriodicRefresh()
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
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_main, menu)
//        val searchItem = menu.findItem(R.id.action_widget_button)
//        val searchView = searchItem?.actionView as? SearchView
//
//        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                deliveryOrdersAdapter.filter(newText ?: "")
//                return true
//            }
//        })
//
//        searchView?.setOnQueryTextFocusChangeListener { _, hasFocus ->
//            isSearching = hasFocus
//            manageRefresh()
//        }
//
//        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
//            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
//                isSearching = true
//                manageRefresh()  // Pause refresh when search is active
//                return true
//            }
//
//            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
//                isSearching = false
//                manageRefresh()  // Resume refresh when search is not active
//                return true
//            }
//        })
//
//        return true
//    }
//
//    private fun manageRefresh() {
//        if (isSearching) {
//            stopPeriodicRefresh()
//        } else {
//            startPeriodicRefresh()
//        }
//    }
//
//    private fun initializeRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.deliveryOrdersRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        deliveryOrdersAdapter = DeliveryOrdersAdapter(listOf()) { deliveryOrders ->
//            Intent(this, DeliveryOrdersProductsActivity::class.java).also { intent ->
//                Log.d("DeliveryOrdersActivity", "Sending data to DeliveryOrdersProductsActivity: ID=${deliveryOrders.id}, Name=${deliveryOrders.name}, Origin=${deliveryOrders.origin}, Location=${deliveryOrders.locationId}, DestLocation=${deliveryOrders.locationDestId}")
//                intent.putExtra("DELIVERY_ORDERS_ID", deliveryOrders.id)
//                intent.putExtra("DELIVERY_ORDERS_NAME", deliveryOrders.name)
//                intent.putExtra("DELIVERY_ORDERS_ORIGIN", deliveryOrders.origin)
//                intent.putExtra("LOCATION", deliveryOrders.locationId)
//                intent.putExtra("DEST_LOCATION", deliveryOrders.locationDestId)
//                startActivity(intent)
//            }
//        }
//        recyclerView.adapter = deliveryOrdersAdapter
//    }
//
//    private fun fetchDeliveryOrdersAndDisplay() {
//        coroutineScope.launch {
//            try {
//                val deliveryOrders = odooXmlRpcClient.fetchDeliveryOrders()
//                withContext(Dispatchers.Main) {
//                    if (deliveryOrders.isEmpty()) {
//                        findViewById<RecyclerView>(R.id.deliveryOrdersRecyclerView).visibility = View.GONE
//                        findViewById<LinearLayout>(R.id.emptyStateLayout).visibility = View.VISIBLE
//                    } else {
//                        deliveryOrdersAdapter.updateDeliveryOrders(deliveryOrders)
//                        findViewById<RecyclerView>(R.id.deliveryOrdersRecyclerView).visibility = View.VISIBLE
//                        findViewById<LinearLayout>(R.id.emptyStateLayout).visibility = View.GONE
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("DeliveryOrdersActivity", "Error fetching delivery orders: ${e.localizedMessage}")
//            }
//        }
//    }
//
//    private fun startPeriodicRefresh() {
//        refreshJob?.cancel()
//        refreshJob = coroutineScope.launch {
//            while (isActive) {
//                fetchDeliveryOrdersAndDisplay()
//                delay(5000)
//            }
//        }
//    }
//
//    private fun stopPeriodicRefresh() {
//        refreshJob?.cancel()
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> {
//                onBackPressedDispatcher.onBackPressed()
//                return true
//            }
//        }
//        return super.onOptionsItemSelected(item)
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
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class DeliveryOrdersActivity : AppCompatActivity() {

    private lateinit var deliveryOrdersAdapter: DeliveryOrdersAdapter
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private var isSearching = false
    private lateinit var emptyStateLayout: LinearLayout
    private val viewModel: DeliveryOrdersViewModel by viewModels { DeliveryOrdersViewModelFactory(odooXmlRpcClient) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_orders)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        emptyStateLayout = findViewById(R.id.emptyStateLayout)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_widget_button)
        val searchView = searchItem.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                deliveryOrdersAdapter.filter(newText ?: "")
                toggleEmptyView()
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
        val recyclerView: RecyclerView = findViewById(R.id.deliveryOrdersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        deliveryOrdersAdapter = DeliveryOrdersAdapter(listOf()) { deliveryOrder ->
            Intent(this, DeliveryOrdersProductsActivity::class.java).also { intent ->
                Log.d("DeliveryOrdersActivity", "Sending data to DeliveryOrdersProductsActivity: ID=${deliveryOrder.id}, Name=${deliveryOrder.name}, Origin=${deliveryOrder.origin}, Location=${deliveryOrder.locationId}, DestLocation=${deliveryOrder.locationDestId}")
                intent.putExtra("DELIVERY_ORDERS_ID", deliveryOrder.id)
                intent.putExtra("DELIVERY_ORDERS_NAME", deliveryOrder.name)
                intent.putExtra("DELIVERY_ORDERS_ORIGIN", deliveryOrder.origin)
                intent.putExtra("LOCATION", deliveryOrder.locationId)
                intent.putExtra("DEST_LOCATION", deliveryOrder.locationDestId)
                startActivity(intent)
            }
        }
        recyclerView.adapter = deliveryOrdersAdapter
    }

    private fun fetchDeliveryOrdersAndDisplay() {
        viewModel.fetchDeliveryOrdersAndDisplay {
            deliveryOrdersAdapter.updateDeliveryOrders(it)
            toggleEmptyView()
        }
    }

    private fun startPeriodicRefresh() {
        viewModel.startPeriodicRefresh {
            fetchDeliveryOrdersAndDisplay()
        }
    }

    private fun stopPeriodicRefresh() {
        viewModel.stopPeriodicRefresh()
    }

    private fun toggleEmptyView() {
        if (deliveryOrdersAdapter.itemCount == 0) {
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            emptyStateLayout.visibility = View.GONE
        }
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
        viewModel.clear()
    }
}
