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

class PackActivity : AppCompatActivity() {

    private lateinit var packAdapter: PackAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pack)
//        registerBackPressHandler()

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
                    val emptyStateLayout = findViewById<LinearLayout>(R.id.emptyStateLayout)
                    if (packs.isEmpty()) {
                        findViewById<RecyclerView>(R.id.packRecyclerView).visibility = View.GONE
                        emptyStateLayout.visibility = View.VISIBLE // Show the empty state layout
                    } else {
                        packAdapter.updatePack(packs)
                        findViewById<RecyclerView>(R.id.packRecyclerView).visibility = View.VISIBLE
                        emptyStateLayout.visibility = View.GONE // Hide the empty state layout
                    }
                }
            } catch (e: Exception) {
                Log.e("PackActivity", "Error fetching packs: ${e.localizedMessage}")
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


    //============================================================================================================
    //                        Androids built in back button at the bottom of the screen
    //                             NB!!!!    INCLUDE IN EVERY ACTIVITY    NB!!!!
    //============================================================================================================
//    private fun registerBackPressHandler() {
//        if (Build.VERSION.SDK_INT >= 33) {
//            onBackInvokedDispatcher.registerOnBackInvokedCallback(
//                OnBackInvokedDispatcher.PRIORITY_DEFAULT
//            ) {
//                // Back is pressed... Finishing the activity
//                finish()
//            }
//        } else {
//            onBackPressedDispatcher.addCallback(this /* lifecycle owner */) {
//                // Back is pressed... Finishing the activity
//                finish()
//            }
//        }
//    }
}
