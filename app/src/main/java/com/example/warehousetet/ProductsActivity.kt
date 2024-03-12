package com.example.warehousetet

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class ProductsActivity : AppCompatActivity() {
    private lateinit var productsAdapter: ProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button

    private var productBarcodes = hashMapOf<String, String>()
    private var productSerialNumbers = hashMapOf<String, List<String>>()
    private var quantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
    private var barcodeToProductIdMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        barcodeInput = findViewById(R.id.barcodeInput)
        confirmButton = findViewById(R.id.confirmButton)
        val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
        productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId)

        if (receiptId != -1) {
            setupRecyclerView()
            fetchProductsForReceipt(receiptId)
        } else {
            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity.")
        }

        setupBarcodeVerification(receiptId)
        loadMatchStatesFromPreferences(receiptId)
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productsAdapter
    }

    private fun setupBarcodeVerification(receiptId: Int) {
        confirmButton.setOnClickListener {
            val enteredBarcode = barcodeInput.text.toString().trim()
            verifyBarcode(enteredBarcode, receiptId)
            hideKeyboard()
        }

        barcodeInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                confirmButton.performClick()
                true
            } else false
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
    }

    private fun fetchProductsForReceipt(receiptId: Int) {
        coroutineScope.launch {
            // Initially fetch products from the receipt
            val fetchedProducts = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
            // Populate barcodeToProductIdMap by fetching barcodes for all fetched products
            // This assumes fetchBarcodesForProducts not only fetches barcodes but also populates barcodeToProductIdMap appropriately
            fetchBarcodesForProducts(fetchedProducts)

            val productsWithTracking = mutableListOf<Product>()

            // Now, with barcodes fetched and mapped, proceed to process each product
            fetchedProducts.forEach { product ->
                val trackingType = odooXmlRpcClient.fetchProductTrackingByName(product.name) ?: "none"

                // Assuming barcode is now fetched and stored in barcodeToProductIdMap, retrieve it
                val barcode = barcodeToProductIdMap.entries.find { it.value == product.id }?.key

                when (trackingType) {
                    "serial", "lot" -> {
                        // For serial or lot tracking, consider how you want to handle these. This is a simplistic approach.
                        repeat(product.quantity.toInt()) {
                            productsWithTracking.add(product.copy(trackingType = trackingType, barcode = barcode))
                        }
                    }
                    else -> {
                        // For 'none', or any other case, add as a single entry
                        productsWithTracking.add(product.copy(trackingType = trackingType, barcode = barcode))
                    }
                }
            }

            // After processing, update UI on the main thread
            withContext(Dispatchers.Main) {
                updateUIForProducts(productsWithTracking, receiptId)
            }
        }
    }

    private fun fetchBarcodesForProducts(products: List<Product>) {
        products.forEach { product ->
            coroutineScope.launch {
                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
                barcode?.let {
                    // Assuming barcodeToProductIdMap should map barcode to product ID
                    synchronized(this@ProductsActivity) {
                        barcodeToProductIdMap[barcode] = product.id
                    }
                }
            }
        }
    }

    private fun updateUIForProducts(products: List<Product>, receiptId: Int) {
        // Assuming barcodeToProductIdMap and productSerialNumbers are already populated or handled within the fetched products loop
        val newQuantityMatches = products.associate {
            ProductReceiptKey(it.id, receiptId) to (quantityMatches[ProductReceiptKey(it.id, receiptId)] ?: false)
        }.toMutableMap()

        // Now update the quantityMatches and UI accordingly
        quantityMatches.clear()
        quantityMatches.putAll(newQuantityMatches)
        productsAdapter.updateProducts(products, receiptId, quantityMatches)
    }

    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
        coroutineScope.launch {
            val productId = barcodeToProductIdMap[scannedBarcode]
            if (productId != null) {
                val product = productsAdapter.products.find { it.id == productId }
                product?.let {
                    val trackingType = odooXmlRpcClient.fetchProductTrackingByName(it.name)
                    if (trackingType == "none") {
                        // Prompt for quantity if tracking type is 'none'
                        withContext(Dispatchers.Main) {
                            promptForProductQuantity(it.name, it.quantity, receiptId, it.id)
                        }
                    } else {
                        // For other tracking types, update match state directly
                        updateProductMatchState(productId, receiptId, true)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductsActivity, "Barcode not found.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun promptForProductQuantity(productName: String, expectedQuantity: Double, receiptId: Int, productId: Int) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter product quantity"
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Quantity")
            .setMessage("Enter the exact quantity for $productName.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val enteredQuantity = editText.text.toString().toDoubleOrNull()
                if (enteredQuantity != null && enteredQuantity == expectedQuantity) {
                    Toast.makeText(this, "Correct quantity entered for $productName", Toast.LENGTH_LONG).show()
                    updateProductMatchState(productId, receiptId, true)
                } else {
                    Toast.makeText(this, "Incorrect quantity entered", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun updateProductMatchState(productId: Int, receiptId: Int, matched: Boolean) {
        val key = ProductReceiptKey(productId, receiptId)
        quantityMatches[key] = matched

        // Save the match state persistently
        saveMatchStateToPreferences(key, matched)

        // Assuming you have a way to find the product's position in the adapter
        // This might involve adding a method in your adapter to return the position based on product ID
        val position = productsAdapter.findProductPositionById(productId)
        if (position != -1) {
            runOnUiThread {
                productsAdapter.notifyItemChanged(position)
            }
        }
    }
    private fun saveMatchStateToPreferences(key: ProductReceiptKey, matched: Boolean) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("${key.productId}_${key.receiptId}", matched)
            apply()
        }
    }

    private fun loadMatchStatesFromPreferences(receiptId: Int) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        val tempQuantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()

        sharedPref.all.forEach { (prefKey, value) ->
            if (value is Boolean) {
                val parts = prefKey.split("_").let { if (it.size == 2) it else null }
                parts?.let {
                    try {
                        val productId = it[0].toInt()
                        val prefReceiptId = it[1].toInt()
                        if (prefReceiptId == receiptId) {
                            val key = ProductReceiptKey(productId, prefReceiptId)
                            tempQuantityMatches[key] = value
                        }
                        else{

                        }
                    } catch (e: NumberFormatException) {
                        Log.e("ProductsActivity", "Error parsing shared preference key: $prefKey", e)
                    }
                }
            }
        }

        quantityMatches.clear()
        quantityMatches.putAll(tempQuantityMatches)

        // Now update the adapter with the loaded match states
        runOnUiThread {
            productsAdapter.updateProducts(productsAdapter.products, receiptId, quantityMatches)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}