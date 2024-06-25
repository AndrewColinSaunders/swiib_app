//package com.example.warehousetet
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.widget.LinearLayout
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.widget.SearchView
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.*
//
//class InternalTransfersActivity : AppCompatActivity() {
//
//    private lateinit var internalTransfersAdapter: InternalTransfersAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var credentialManager: CredentialManager
//    private var refreshJob: Job? = null
//    private var isSearching = false
//    private lateinit var emptyStateLayout: LinearLayout
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_pick) // Ensure this is the correct layout file
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//
//        credentialManager = CredentialManager(this)
//        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
//
//        emptyStateLayout = findViewById(R.id.emptyStateLayout) // Ensure this ID matches your layout file
//
//        initializeRecyclerView()
//        fetchTransfersAndDisplay()
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
//            override fun onQueryTextSubmit(query: String?): Boolean = false
//            override fun onQueryTextChange(newText: String?): Boolean {
//                internalTransfersAdapter.filter(newText ?: "")
//                toggleEmptyView()
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
//                manageRefresh()
//                return true
//            }
//
//            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
//                isSearching = false
//                manageRefresh()
//                return true
//            }
//        })
//
//        return true
//    }
//
//    private fun manageRefresh() {
//        if (isSearching) stopPeriodicRefresh() else startPeriodicRefresh()
//    }
//
//    private fun initializeRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.pickRecyclerView) // Ensure this ID matches your layout file
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        internalTransfersAdapter = InternalTransfersAdapter(listOf()) { transfer ->
//            // Launch IntTransferProductsActivity with transfer ID, similar to how it's done with picks
//            Intent(this, IntTransferProductsActivity::class.java).also { intent ->
//                intent.putExtra("TRANSFER_ID", transfer.id)
//                intent.putExtra("TRANSFER_NAME", transfer.name)
//                intent.putExtra("TRANSFER_ORIGIN", transfer.origin)
//                intent.putExtra("LOCATION", transfer.locationId)
//                intent.putExtra("DEST_LOCATION", transfer.locationDestId)
//                startActivity(intent)
//            }
//            coroutineScope.launch {
//                odooXmlRpcClient.fetchAndLogBuyerDetails(transfer.name)
//            }
//        }
//        recyclerView.adapter = internalTransfersAdapter
//    }
//
//    private fun fetchTransfersAndDisplay() {
//        coroutineScope.launch {
//            try {
//                val transfers = odooXmlRpcClient.fetchIntTransfers() // This method needs to be implemented
//                withContext(Dispatchers.Main) {
//                    internalTransfersAdapter.updateTransfers(transfers)
//                    toggleEmptyView()
//                }
//            } catch (e: Exception) {
//                Log.e("InternalTransfersActivity", "Error fetching transfers: ${e.localizedMessage}")
//            }
//        }
//    }
//
//    private fun startPeriodicRefresh() {
//        refreshJob?.cancel()
//        refreshJob = coroutineScope.launch {
//            while (isActive) {
//                fetchTransfersAndDisplay()
//                delay(5000)
//            }
//        }
//    }
//
//    private fun stopPeriodicRefresh() {
//        refreshJob?.cancel()
//        refreshJob = null
//    }
//
//    private fun toggleEmptyView() {
//        if (internalTransfersAdapter.itemCount == 0) {
//            emptyStateLayout.visibility = View.VISIBLE
//        } else {
//            emptyStateLayout.visibility = View.GONE
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == android.R.id.home) {
//            onBackPressedDispatcher.onBackPressed()
//            return true
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

class InternalTransfersActivity : AppCompatActivity() {

    private lateinit var internalTransfersAdapter: InternalTransfersAdapter
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private lateinit var emptyStateLayout: LinearLayout
    private val viewModel: InternalTransfersViewModel by viewModels { InternalTransfersViewModelFactory(odooXmlRpcClient) }
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick) // Ensure this is the correct layout file

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)

        emptyStateLayout = findViewById(R.id.emptyStateLayout) // Ensure this ID matches your layout file

        initializeRecyclerView()
        fetchTransfersAndDisplay()
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
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                internalTransfersAdapter.filter(newText ?: "")
                toggleEmptyView()
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
        if (isSearching) stopPeriodicRefresh() else startPeriodicRefresh()
    }

    private fun initializeRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.pickRecyclerView) // Ensure this ID matches your layout file
        recyclerView.layoutManager = LinearLayoutManager(this)
        internalTransfersAdapter = InternalTransfersAdapter(listOf()) { transfer ->
            Intent(this, IntTransferProductsActivity::class.java).also { intent ->
                intent.putExtra("TRANSFER_ID", transfer.id)
                intent.putExtra("TRANSFER_NAME", transfer.name)
                intent.putExtra("TRANSFER_ORIGIN", transfer.origin)
                intent.putExtra("LOCATION", transfer.locationId)
                intent.putExtra("DEST_LOCATION", transfer.locationDestId)
                startActivity(intent)
            }
            viewModel.fetchAndLogBuyerDetails(transfer.name)
        }
        recyclerView.adapter = internalTransfersAdapter
    }

    private fun fetchTransfersAndDisplay() {
        viewModel.fetchTransfersAndDisplay {
            internalTransfersAdapter.updateTransfers(it)
            toggleEmptyView()
        }
    }

    private fun startPeriodicRefresh() {
        viewModel.startPeriodicRefresh {
            fetchTransfersAndDisplay()
        }
    }

    private fun stopPeriodicRefresh() {
        viewModel.stopPeriodicRefresh()
    }

    private fun toggleEmptyView() {
        if (internalTransfersAdapter.itemCount == 0) {
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            emptyStateLayout.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clear()
    }
}
