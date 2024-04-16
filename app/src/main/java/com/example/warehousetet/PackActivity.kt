package com.example.warehousetet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class PackActivity : AppCompatActivity() {

    private lateinit var packAdapter: PackAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pack)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        initializeRecyclerView()
        fetchPacksAndDisplay()
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
        val recyclerView: RecyclerView = findViewById(R.id.packRecyclerView) // Make sure your layout file for PickActivity includes a RecyclerView with this ID
        recyclerView.layoutManager = LinearLayoutManager(this)
        packAdapter = PackAdapter(listOf()) { pack ->
            // Launch ProductsActivity with pick ID, similar to how it's done with receipts
            Intent(this, PackProductsActivity::class.java).also { intent ->
                intent.putExtra("PACK_ID", pack.id)
                intent.putExtra("PACK_NAME", pack.name)
                intent.putExtra("PACK_ORIGIN", pack.origin)
                intent.putExtra("LOCATION", pack.locationId)
                intent.putExtra("DEST_LOCATION", pack.locationDestId)
                startActivity(intent)
            }
        }
        recyclerView.adapter = packAdapter
    }

    private fun fetchPacksAndDisplay() {
        coroutineScope.launch {
            try {
                val packs = odooXmlRpcClient.fetchPacks() // Implement this method in OdooXmlRpcClient
                withContext(Dispatchers.Main) {
                    packAdapter.updatePack(packs)
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
                fetchPacksAndDisplay()
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
