package com.example.warehousetet

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

//class PackActivity : AppCompatActivity() {
//
//    private lateinit var packAdapter: PackAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var credentialManager: CredentialManager
//    private var refreshJob: Job? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_pack)
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//
//        credentialManager = CredentialManager(this)
//        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
//
//        initializeRecyclerView()
//        fetchPacksAndDisplay()
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
//    private fun initializeRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.packRecyclerView) // Make sure your layout file for PickActivity includes a RecyclerView with this ID
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        packAdapter = PackAdapter(listOf()) { pack ->
//            // Launch ProductsActivity with pick ID, similar to how it's done with receipts
//            Intent(this, PackProductsActivity::class.java).also { intent ->
//                intent.putExtra("PACK_ID", pack.id)
//                intent.putExtra("PACK_NAME", pack.name)
//                intent.putExtra("PACK_ORIGIN", pack.origin)
//                intent.putExtra("LOCATION", pack.locationId)
//                intent.putExtra("DEST_LOCATION", pack.locationDestId)
//                startActivity(intent)
//            }
//        }
//        recyclerView.adapter = packAdapter
//    }
//
//    private fun fetchPacksAndDisplay() {
//        coroutineScope.launch {
//            try {
//                val packs = odooXmlRpcClient.fetchPacks() // Implement this method in OdooXmlRpcClient
//                withContext(Dispatchers.Main) {
//                    val emptyStateLayout = findViewById<LinearLayout>(R.id.emptyStateLayout)
//                    if (packs.isEmpty()) {
//                        findViewById<RecyclerView>(R.id.packRecyclerView).visibility = View.GONE
//                        emptyStateLayout.visibility = View.VISIBLE // Show the empty state layout
//                    } else {
//                        packAdapter.updatePack(packs)
//                        findViewById<RecyclerView>(R.id.packRecyclerView).visibility = View.VISIBLE
//                        emptyStateLayout.visibility = View.GONE // Hide the empty state layout
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("PackActivity", "Error fetching packs: ${e.localizedMessage}")
//            }
//        }
//    }
//
//
//
//    private fun startPeriodicRefresh() {
//        refreshJob?.cancel() // Cancel any existing job to avoid duplicates
//        refreshJob = coroutineScope.launch {
//            while (isActive) {
//                fetchPacksAndDisplay()
//                delay(5000) // Refresh every 5 seconds
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
//
//}


class PackActivity : AppCompatActivity() {

    private lateinit var packAdapter: PackAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private var refreshJob: Job? = null
    private var isSearching = false

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_widget_button)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                packAdapter.filter(newText ?: "")
                return true
            }
        })

        searchView?.setOnQueryTextFocusChangeListener { _, hasFocus ->
            isSearching = hasFocus
            manageRefresh()
        }

        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                isSearching = true
                manageRefresh()  // Pause refresh when search is active
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                isSearching = false
                manageRefresh()  // Resume refresh when search is not active
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
        val recyclerView: RecyclerView = findViewById(R.id.packRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        packAdapter = PackAdapter(listOf()) { pack ->
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
                val packs = odooXmlRpcClient.fetchPacks()
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
}
