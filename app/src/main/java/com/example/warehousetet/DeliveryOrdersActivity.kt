package com.example.warehousetet

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class DeliveryOrdersActivity : AppCompatActivity(), OnInternalTransferSelectedListener {
    private lateinit var deliveryOrdersAdapter: DeliveryOrdersAdapter
    private lateinit var credentialManager: CredentialManager
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private val refreshScope = CoroutineScope(Dispatchers.IO)
    private var refreshJob: Job? = null
    private var isPeriodicRefreshEnabled = true
    private var isInstantRefreshRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_orders)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        // Instantiation of pickAdapter moved here to ensure it uses the correct context and listener
        deliveryOrdersAdapter = DeliveryOrdersAdapter(this, this)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView_delivery_orders)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = deliveryOrdersAdapter

        val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        isInstantRefreshRequested = sharedPreferences.getBoolean("InstantRefreshRequested", false)
        if (isInstantRefreshRequested) {
            sharedPreferences.edit().remove("InstantRefreshRequested").apply()
        }

        val btnCancelSearch: Button = findViewById(R.id.btnCancelSearch)
        btnCancelSearch.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            deliveryOrdersAdapter.resetList()
            it.visibility = View.GONE
            isPeriodicRefreshEnabled = true
            startPeriodicRefresh()
        }

        startPeriodicRefresh()
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_widget_button -> {
                showSearchMethodDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSearchMethodDialog() {
        val options = arrayOf("Type the source document", "Scan barcode")
        AlertDialog.Builder(this)
            .setTitle("Search for package")
            .setItems(options) { _, which ->
                window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                when (which) {
                    0 -> showTypeSearchDialog()
                    1 -> initiateBarcodeScanning()
                }
            }
            .show()
    }

    private fun showTypeSearchDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Type the source document")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val searchQuery = input.text.toString()
                deliveryOrdersAdapter.filter(searchQuery)
                findViewById<Button>(R.id.btnCancelSearch).visibility = View.VISIBLE
                isPeriodicRefreshEnabled = false
                refreshJob?.cancel()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun initiateBarcodeScanning() {
        // Placeholder - Implement your barcode scanning logic here
    }

    private fun startPeriodicRefresh() {
        refreshJob = refreshScope.launch {
            while (isActive && isPeriodicRefreshEnabled) {
                try {
                    // Fetch data and update UI
                    val deliveryOrders = odooXmlRpcClient.fetchDeliveryOrdersWithProductDetails()
                    withContext(Dispatchers.Main) {
                        deliveryOrdersAdapter.submitFilteredPicks(deliveryOrders)
                    }
                } catch (e: Exception) {
                    // Handle errors
                }

                // Skip delay if instant refresh is requested, else wait for 5 seconds
                if (isInstantRefreshRequested) {
                    isInstantRefreshRequested = false // Reset the flag immediately after use
                } else {
                    delay(5000)
                }
            }
        }
    }

    override fun onInternalTransferFinish() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel()
    }
}
