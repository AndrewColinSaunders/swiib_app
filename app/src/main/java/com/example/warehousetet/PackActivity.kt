package com.example.warehousetet

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

class PackActivity : AppCompatActivity() {
    private lateinit var packAdapter: PackAdapter
    private lateinit var credentialManager: CredentialManager
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private val refreshScope = CoroutineScope(Dispatchers.IO)
    private var refreshJob: Job? = null
    private var isPeriodicRefreshEnabled = true // Flag to control periodic refresh

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pack)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        packAdapter = PackAdapter()
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView_packs)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PackActivity)
            adapter = packAdapter
        }

        val btnCancelSearch: Button = findViewById(R.id.btnCancelSearch)
        btnCancelSearch.setOnClickListener {
            // Provide haptic feedback
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            // Reset the adapter with the full list
            packAdapter.resetList() // Ensure this method properly resets the list in PackAdapter

            // Hide the button
            it.visibility = View.GONE

            // Re-enable periodic refresh and restart it
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
                packAdapter.filter(searchQuery)
                findViewById<Button>(R.id.btnCancelSearch).visibility = View.VISIBLE // Show the cancel button
                isPeriodicRefreshEnabled = false
                refreshJob?.cancel()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun initiateBarcodeScanning() {
        // Placeholder for barcode scanning logic
    }

    private fun startPeriodicRefresh() {
        refreshJob = refreshScope.launch {
            while (isActive && isPeriodicRefreshEnabled) {
                try {
                    val packs = odooXmlRpcClient.fetchInternalTransfersWithProductDetails()
                    withContext(Dispatchers.Main) {
                        packAdapter.submitFilteredPacks(packs)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // Log or handle any errors
                    }
                }
                delay(5000) // Refresh delay
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel() // Cancel any ongoing jobs
    }
}
