//package com.example.warehousetet
//
//import PickAdapter
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.view.Menu
//import android.view.MenuItem
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.widget.SearchView
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.*
//
//class PickActivity : AppCompatActivity() {
//
//    private lateinit var pickAdapter: PickAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var credentialManager: CredentialManager
//    private var refreshJob: Job? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_pick)
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//
//        credentialManager = CredentialManager(this)
//        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
//
//        initializeRecyclerView()
//        fetchPicksAndDisplay()
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
////    override fun onCreateOptionsMenu(menu: Menu): Boolean {
////        menuInflater.inflate(R.menu.menu_main, menu)
////        return true
////    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_main, menu)
//        val searchItem = menu.findItem(R.id.action_widget_button)
//        val searchView = searchItem.actionView as? SearchView
//
//        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                // Not used in this case
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                pickAdapter.filter(newText ?: "")
//                return true
//            }
//        })
//
//        searchView?.setOnCloseListener {
//            pickAdapter.filter("")
//            true
//        }
//
//        return true
//    }
//
//
//
//
//    private fun initializeRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.pickRecyclerView) // Make sure your layout file for PickActivity includes a RecyclerView with this ID
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        pickAdapter = PickAdapter(listOf()) { pick ->
//            // Launch ProductsActivity with pick ID, similar to how it's done with receipts
//            Intent(this, PickProductsActivity::class.java).also { intent ->
//                intent.putExtra("PICK_ID", pick.id)
//                intent.putExtra("PICK_NAME", pick.name)
//                intent.putExtra("PICK_ORIGIN", pick.origin)
//                intent.putExtra("LOCATION", pick.locationId)
//                intent.putExtra("DEST_LOCATION", pick.locationDestId)
//                startActivity(intent)
//            }
//            coroutineScope.launch {
//                odooXmlRpcClient.fetchAndLogBuyerDetails(pick.name)
//            }
//        }
//        recyclerView.adapter = pickAdapter
//    }
//
//    private fun fetchPicksAndDisplay() {
//        coroutineScope.launch {
//            try {
//                val picks = odooXmlRpcClient.fetchPicks() // Implement this method in OdooXmlRpcClient
//                withContext(Dispatchers.Main) {
//                    pickAdapter.updatePicks(picks)
//                }
//            } catch (e: Exception) {
//                Log.e("PickActivity", "Error fetching picks: ${e.localizedMessage}")
//            }
//        }
//    }
//
//    private fun startPeriodicRefresh() {
//        refreshJob?.cancel() // Cancel any existing job to avoid duplicates
//        refreshJob = coroutineScope.launch {
//            while (isActive) {
//                fetchPicksAndDisplay()
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
//                onBackPressed()
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

import PickAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class PickActivity : AppCompatActivity() {

    private lateinit var pickAdapter: PickAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private var refreshJob: Job? = null
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick)

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


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_widget_button)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                pickAdapter.filter(newText ?: "")
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
        val recyclerView: RecyclerView = findViewById(R.id.pickRecyclerView) // Make sure your layout file for PickActivity includes a RecyclerView with this ID
        recyclerView.layoutManager = LinearLayoutManager(this)
        pickAdapter = PickAdapter(listOf()) { pick ->
            // Launch ProductsActivity with pick ID, similar to how it's done with receipts
            Intent(this, PickProductsActivity::class.java).also { intent ->
                intent.putExtra("PICK_ID", pick.id)
                intent.putExtra("PICK_NAME", pick.name)
                intent.putExtra("PICK_ORIGIN", pick.origin)
                intent.putExtra("LOCATION", pick.locationId)
                intent.putExtra("DEST_LOCATION", pick.locationDestId)
                startActivity(intent)
            }
            coroutineScope.launch {
                odooXmlRpcClient.fetchAndLogBuyerDetails(pick.name)
            }
        }
        recyclerView.adapter = pickAdapter
    }

    private fun fetchPicksAndDisplay() {
        coroutineScope.launch {
            try {
                val picks = odooXmlRpcClient.fetchPicks() // Implement this method in OdooXmlRpcClient
                withContext(Dispatchers.Main) {
                    pickAdapter.updatePicks(picks)
                }
            } catch (e: Exception) {
                Log.e("PickActivity", "Error fetching picks: ${e.localizedMessage}")
            }
        }
    }

    private fun startPeriodicRefresh() {
        stopPeriodicRefresh()  // Ensure any existing job is stopped to avoid duplicates
        refreshJob = coroutineScope.launch {
            while (isActive) {
                fetchPicksAndDisplay()
                delay(5000)  // Example refresh interval
            }
        }
    }

    private fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
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
