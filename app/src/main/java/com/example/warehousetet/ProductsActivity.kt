//package com.example.warehousetet
//
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class ProductsActivity : AppCompatActivity() {
//    private lateinit var productsAdapter: ProductsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//
//    private var productBarcodes = hashMapOf<String, String>() // Map product names to their barcodes
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_products)
//
//        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
//
//        val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
//        if (receiptId != -1) {
//            setupRecyclerView()
//            fetchProductsForReceipt(receiptId)
//        } else {
//            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity")
//        }
//        fetchBarcodesForProducts()
//    }
//
//    private fun setupRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        productsAdapter = ProductsAdapter(emptyList())
//        recyclerView.adapter = productsAdapter
//    }
//
//    private fun fetchProductsForReceipt(receiptId: Int) {
//        coroutineScope.launch {
//            val products = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
//            withContext(Dispatchers.Main) {
//                productsAdapter.updateProducts(products)
//            }
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        coroutineScope.cancel()
//    }
//
//    private fun fetchBarcodesForProducts() {
//        productsAdapter.products.forEach { product ->
//            coroutineScope.launch {
//                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
//                if (barcode != null) {
//                    productBarcodes[product.name] = barcode
//                }
//            }
//        }
//    }
//}


package com.example.warehousetet

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class ProductsActivity : AppCompatActivity() {
    private lateinit var productsAdapter: ProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button

    private var productBarcodes = hashMapOf<String, String>() // Map product names to their barcodes
    private var productSerialNumbers = hashMapOf<String, List<String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        productsAdapter = ProductsAdapter(emptyList())

        val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
        if (receiptId != -1) {
            setupRecyclerView()
            fetchProductsForReceipt(receiptId)
        } else {
            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity")
        }

        barcodeInput = findViewById(R.id.barcodeInput)
        confirmButton = findViewById(R.id.confirmButton)
        confirmButton.setOnClickListener {
            val enteredBarcode = barcodeInput.text.toString().trim()
            verifyBarcode(enteredBarcode)

            // Hide the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
        }

        barcodeInput.setOnEditorActionListener { v, actionId, event ->
            val handleEnter = if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                // Trigger the same logic as the confirmButton's onClick
                val enteredBarcode = barcodeInput.text.toString().trim()
                verifyBarcode(enteredBarcode)

                // Hide the keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                true // Return true to consume the event
            } else {
                false // Return false to let other handlers process the event
            }

            handleEnter
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productsAdapter
    }

//    private fun fetchProductsForReceipt(receiptId: Int) {
//        coroutineScope.launch {
//            val products = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
//            withContext(Dispatchers.Main) {
//                productsAdapter.updateProducts(products)
//                fetchBarcodesForProducts(products) // Fetch barcodes after products are updated
//            }
//        }
//    }
private fun fetchProductsForReceipt(receiptId: Int) {
    coroutineScope.launch {
        // Fetch products for the receipt
        val originalProducts = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
        val adjustedProducts = mutableListOf<Product>()

        // For each product, fetch its tracking type and adjust the list accordingly
        originalProducts.forEach { product ->
            val trackingType = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductTrackingByName(product.name)
            }

            when (trackingType) {
                "serial", "lot" -> {
                    // For 'serial' and 'lot', add individual entries for each unit
                    repeat(product.quantity.toInt()) {
                        adjustedProducts.add(product.copy())
                    }
                }
                else -> {
                    // For 'none', add a single entry for the product
                    adjustedProducts.add(product)
                }
            }
        }

        // Update the adapter and fetch barcodes for the adjusted list of products
        withContext(Dispatchers.Main) {
            productsAdapter.updateProducts(adjustedProducts)
            fetchBarcodesForProducts(adjustedProducts)
        }
    }
}
    private fun fetchBarcodesForProducts(products: List<Product>) {
        products.forEach { product ->
            // Fetch barcode
            coroutineScope.launch {
                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
                barcode?.let {
                    synchronized(this@ProductsActivity) {
                        productBarcodes[product.name] = it
                    }
                }
            }
            // Fetch serial numbers
            coroutineScope.launch {
                val serialNumbers = odooXmlRpcClient.fetchSerialNumbersByProductName(product.name)
                serialNumbers?.let {
                    synchronized(this@ProductsActivity) {
                        productSerialNumbers[product.name] = it
                    }
                }
            }
        }
    }

//    private fun fetchBarcodesForProducts(products: List<Product>) {
//        products.forEach { product ->
//            coroutineScope.launch {
//                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
//                barcode?.let {
//                    synchronized(this@ProductsActivity) {
//                        productBarcodes[product.name] = it
//                    }
//                }
//            }
//        }
//    }
//
//    private fun verifyBarcode(scannedBarcode: String) {
//        // Check if the barcode matches any product's barcode
//        val productName = synchronized(this) { productBarcodes.filterValues { it == scannedBarcode }.keys.firstOrNull() }
//
//        if (productName != null) {
//            // Product matched, now fetch its tracking type asynchronously
//            lifecycleScope.launch(Dispatchers.IO) {
//                try {
//                    val trackingType = odooXmlRpcClient.fetchProductTrackingByName(productName)
//                    val trackingMessage = when (trackingType) {
//                        "serial" -> "by unique serial number"
//                        "lot" -> "by lots"
//                        "none" -> "no tracking"
//                        else -> "tracking information not available"
//                    }
//                    // Switch back to the main thread to show success message including tracking type
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@ProductsActivity, "Success: Product found for the entered barcode. Tracking: $trackingMessage.", Toast.LENGTH_LONG).show()
//                    }
//                } catch (e: Exception) {
//                    // Handle potential errors, for example, network issues or parsing errors
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@ProductsActivity, "Error fetching tracking info: ${e.message}", Toast.LENGTH_LONG).show()
//                    }
//                }
//            }
//        } else {
//            // Run on UI thread to show error message if no matching product found
//            runOnUiThread {
//                Toast.makeText(this, "Error: No matching product found for the entered barcode.", Toast.LENGTH_LONG).show()
//            }
//        }
//    }

    private fun verifyBarcode(scannedBarcode: String) {
        // Check if the barcode matches any product's barcode
        val productNameByBarcode = synchronized(this) {
            productBarcodes.filterValues { it == scannedBarcode }.keys.firstOrNull()
        }

        // Check if the scanned barcode matches any product's serial numbers
        val productNameBySerial = synchronized(this) {
            productSerialNumbers.entries.firstOrNull { entry ->
                entry.value.contains(scannedBarcode)
            }?.key
        }

        when {
            productNameByBarcode != null -> {
                // Verify product by barcode if a matching product name is found
                verifyProductByBarcode(productNameByBarcode, scannedBarcode)
            }
            productNameBySerial != null -> {
                // If no product barcode matched, but a serial number did, show success message
                Toast.makeText(this, "Success: Serial number matched.", Toast.LENGTH_LONG).show()
            }
            else -> {
                // If neither barcode nor serial number matched, show error message
                Toast.makeText(this, "Error: No matching product found for the entered barcode.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun verifyProductByBarcode(productName: String, scannedBarcode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val trackingType = odooXmlRpcClient.fetchProductTrackingByName(productName)
                val trackingMessage = when (trackingType) {
                    "serial" -> "by unique serial number"
                    "lot" -> "by lots"
                    "none" -> "no tracking"
                    null -> "No tracking information available" // Handle null case explicitly if needed
                    else -> "tracking information not available"
                }

                // Switch back to the main thread to show success message including tracking type
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductsActivity, "Success: Product found for the entered barcode. Tracking: $trackingMessage.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Handle potential errors, for example, network issues or parsing errors
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductsActivity, "Error fetching tracking info: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }





    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
