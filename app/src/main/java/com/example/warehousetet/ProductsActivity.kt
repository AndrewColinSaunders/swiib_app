package com.example.warehousetet

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductsActivity : AppCompatActivity() {
    private lateinit var productsAdapter: ProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))

        val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
        if (receiptId != -1) {
            setupRecyclerView()
            fetchProductsForReceipt(receiptId)
        } else {
            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity")
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productsAdapter = ProductsAdapter(emptyList())
        recyclerView.adapter = productsAdapter
    }

    private fun fetchProductsForReceipt(receiptId: Int) {
        coroutineScope.launch {
            val products = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
            withContext(Dispatchers.Main) {
                productsAdapter.updateProducts(products)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
