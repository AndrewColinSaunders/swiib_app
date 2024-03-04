package com.example.warehousetet

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
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
                isPeriodicRefreshEnabled = false // Disable periodic refresh after search
                refreshJob?.cancel() // Optionally cancel the current job
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun initiateBarcodeScanning() {
        // Placeholder - Implement barcode scanning logic here
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
                        // Handle any errors
                    }
                }
                delay(5000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel()
    }
}
