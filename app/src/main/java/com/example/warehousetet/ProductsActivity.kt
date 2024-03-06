//package com.example.warehousetet
//
//import android.content.Context
//import android.os.Bundle
//import android.text.InputType
//import android.util.Log
//import android.view.KeyEvent
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.widget.Button
//import android.widget.EditText
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.*
//
//class ProductsActivity : AppCompatActivity() {
//    private lateinit var productsAdapter: ProductsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var barcodeInput: EditText
//    private lateinit var confirmButton: Button
//
//    private var productBarcodes = hashMapOf<String, String>() // Map product names to their barcodes
//    private var productSerialNumbers = hashMapOf<String, List<String>>()
//
//    private var quantityMatches = mutableMapOf<String, Boolean>()
//
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_products)
//
//        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
////        productsAdapter = ProductsAdapter(emptyList())
//        productsAdapter = ProductsAdapter(emptyList(), quantityMatches)
//
//        var receiptId = intent.getIntExtra("RECEIPT_ID", -1)
//        if (receiptId != -1) {
//            setupRecyclerView()
//            fetchProductsForReceipt(receiptId)
//        } else {
//            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity")
//        }
//
//        receiptId = intent.getIntExtra("RECEIPT_ID", -1)
//
//        barcodeInput = findViewById(R.id.barcodeInput)
//        confirmButton = findViewById(R.id.confirmButton)
//        confirmButton.setOnClickListener {
//            val enteredBarcode = barcodeInput.text.toString().trim()
//            verifyBarcode(enteredBarcode, receiptId)
//
//            // Hide the keyboard
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
//        }
//
//        barcodeInput.setOnEditorActionListener { v, actionId, event ->
//            val handleEnter = if (actionId == EditorInfo.IME_ACTION_DONE ||
//                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                // Trigger the same logic as the confirmButton's onClick
//                val enteredBarcode = barcodeInput.text.toString().trim()
//                verifyBarcode(enteredBarcode, receiptId)
//
//                // Hide the keyboard
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.hideSoftInputFromWindow(v.windowToken, 0)
//
//                true // Return true to consume the event
//            } else {
//                false // Return false to let other handlers process the event
//            }
//
//            handleEnter
//        }
//        loadMatchStatesFromPreferences()
//    }
//
//    private fun setupRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = productsAdapter
//    }
////    private fun fetchProductsForReceipt(receiptId: Int) {
////        coroutineScope.launch {
////            val originalProducts = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
////            val adjustedProducts = mutableListOf<Product>()
////            val newQuantityMatches = mutableMapOf<String, Boolean>()
////
////            originalProducts.forEach { product ->
////                val trackingType = withContext(Dispatchers.IO) {
////                    odooXmlRpcClient.fetchProductTrackingByName(product.name)
////                }
////
////                when (trackingType) {
////                    "serial" -> {
////                        repeat(product.quantity.toInt()) {
////                            adjustedProducts.add(product.copy())
////                            newQuantityMatches[product.name] = false // Assume not matched initially
////                        }
////                    }
////                    "lot" -> {
////                        adjustedProducts.add(product)
////                        newQuantityMatches[product.name] = false // Lot products are initially not matched
////                    }
////                    else -> {
////                        adjustedProducts.add(product)
////                        newQuantityMatches[product.name] = false // 'None' products are initially not matched
////                    }
////                }
////            }
////
////            withContext(Dispatchers.Main) {
////                quantityMatches.clear() // Clear previous state
////                quantityMatches.putAll(newQuantityMatches) // Set new state
////                productsAdapter.updateProducts(adjustedProducts, quantityMatches)
////                fetchBarcodesForProducts(adjustedProducts)
////            }
////        }
////    }
//private fun fetchProductsForReceipt(receiptId: Int) {
//    coroutineScope.launch {
//        val originalProducts = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
//        val adjustedProducts = mutableListOf<Product>()
//        val newQuantityMatches = mutableMapOf<String, Boolean>()
//
//        originalProducts.forEach { product ->
//            val trackingType = withContext(Dispatchers.IO) {
//                odooXmlRpcClient.fetchProductTrackingByName(product.name)
//            }
//
//            when (trackingType) {
//                "serial", "lot" -> {
//                    // Assuming all serial or lot products initially do not match
//                    repeat(product.quantity.toInt()) {
//                        adjustedProducts.add(product.copy())
//                        newQuantityMatches[product.name] = false
//                    }
//                }
//                else -> {
//                    // 'None' or undefined tracking types are also initially considered not matched
//                    adjustedProducts.add(product)
//                    newQuantityMatches[product.name] = false
//                }
//            }
//        }
//
//        withContext(Dispatchers.Main) {
//            // Once products are fetched and initial match states determined, load saved states
//            loadMatchStatesFromPreferences(newQuantityMatches)
//            // Update adapter with fetched products and their match states
//            productsAdapter.updateProducts(adjustedProducts, quantityMatches)
//            fetchBarcodesForProducts(adjustedProducts)
//        }
//    }
//}
//
//    private fun fetchBarcodesForProducts(products: List<Product>) {
//        products.forEach { product ->
//            // Fetch barcode
//            coroutineScope.launch {
//                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
//                barcode?.let {
//                    synchronized(this@ProductsActivity) {
//                        productBarcodes[product.name] = it
//                    }
//                }
//            }
//            // Fetch serial numbers
//            coroutineScope.launch {
//                val serialNumbers = odooXmlRpcClient.fetchSerialNumbersByProductName(product.name)
//                serialNumbers?.let {
//                    synchronized(this@ProductsActivity) {
//                        productSerialNumbers[product.name] = it
//                    }
//                }
//            }
//        }
//    }
//
////    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
////        // First, check if the barcode matches any product's barcode
////        val productNameByBarcode = synchronized(this) {
////            productBarcodes.filterValues { it == scannedBarcode }.keys.firstOrNull()
////        }
////
////        if (productNameByBarcode != null) {
////            // If a product barcode matched, verify product by barcode
////            verifyProductByBarcode(productNameByBarcode, scannedBarcode)
////        } else {
////            // No product barcode matched; check against serial numbers
////            lifecycleScope.launch(Dispatchers.IO) {
////                val productNameBySerial = productSerialNumbers.entries.firstOrNull { entry ->
////                    scannedBarcode in entry.value
////                }?.key
////
////                if (productNameBySerial != null) {
////                    // Fetch tracking type for matched product by serial number
////                    val trackingType = odooXmlRpcClient.fetchProductTrackingByName(productNameBySerial)
////                    withContext(Dispatchers.Main) {
////                        when (trackingType) {
////                            "serial" -> Toast.makeText(this@ProductsActivity, "Success: Serial number matched.", Toast.LENGTH_LONG).show()
////                            "lot" -> promptForLotQuantity(receiptId)
////
////                            else -> Toast.makeText(this@ProductsActivity, "Error: No tracking information available.", Toast.LENGTH_LONG).show()
////                        }
////                    }
////                } else {
////                    // If neither barcode nor serial number matched, show error message
////                    withContext(Dispatchers.Main) {
////                        Toast.makeText(this@ProductsActivity, "Error: No matching product found for the entered barcode.", Toast.LENGTH_LONG).show()
////                    }
////                }
////            }
////        }
////    }
//
//    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
//        // First, check if the barcode matches any product's barcode
//        val productNameByBarcode = synchronized(this) {
//            productBarcodes.filterValues { it == scannedBarcode }.keys.firstOrNull()
//        }
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            if (productNameByBarcode != null) {
//                // If a product barcode matched, verify product by barcode and check for 'none' tracking type
//                val trackingType = odooXmlRpcClient.fetchProductTrackingByName(productNameByBarcode)
//                when (trackingType) {
//                    "serial", "lot" -> verifyProductByBarcode(productNameByBarcode, scannedBarcode)
//                    "none" -> {
//                        val products = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
//                        val matchedProduct = products.firstOrNull { it.name == productNameByBarcode }
//                        matchedProduct?.let { product ->
//                            withContext(Dispatchers.Main) {
//                                promptForProductQuantityIfNone(product.name, product.quantity)
//                            }
//                        }
//                    }
//                    else -> withContext(Dispatchers.Main) {
//                        Toast.makeText(this@ProductsActivity, "Error: Tracking information unavailable.", Toast.LENGTH_LONG).show()
//                    }
//                }
//            } else {
//                // No product barcode matched; check against serial numbers
//                val productNameBySerial = productSerialNumbers.entries.firstOrNull { entry ->
//                    scannedBarcode in entry.value
//                }?.key
//
//                if (productNameBySerial != null) {
//                    // Serial number matched, fetch tracking type
//                    val trackingType = odooXmlRpcClient.fetchProductTrackingByName(productNameBySerial)
//                    when (trackingType) {
//                        "serial" -> withContext(Dispatchers.Main) {
//                            Toast.makeText(this@ProductsActivity, "Success: Serial number matched.", Toast.LENGTH_LONG).show()
//                        }
//                        "lot" -> promptForLotQuantity(receiptId)
//                        "none" -> {
//                            // Handle 'none' tracking type for serial numbers if applicable
//                        }
//                        else -> withContext(Dispatchers.Main) {
//                            Toast.makeText(this@ProductsActivity, "Error: No tracking information available.", Toast.LENGTH_LONG).show()
//                        }
//                    }
//                } else {
//                    // If neither barcode nor serial number matched, show error message
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@ProductsActivity, "Error: No matching product found for the entered barcode.", Toast.LENGTH_LONG).show()
//                    }
//                }
//            }
//        }
//    }
//
////    private fun promptForProductQuantityIfNone(productName: String, expectedQuantity: Double) {
////        val editText = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_NUMBER
////            hint = "Enter product quantity"
////        }
////
////        AlertDialog.Builder(this)
////            .setTitle("Enter Quantity")
////            .setMessage("Enter product quantity for $productName. \nTracking: [none]")
////            .setView(editText)
////            .setPositiveButton("OK") { _, _ ->
////                val enteredQuantity = editText.text.toString().toDoubleOrNull()
////                if (enteredQuantity != null) {
////                    if (enteredQuantity == expectedQuantity) {
////                        Toast.makeText(this, "Quantity matches for $productName.", Toast.LENGTH_LONG).show()
////                        quantityMatches[productName] = true
////                    } else {
////                        Toast.makeText(this, "Entered quantity does not match the expected quantity for $productName.", Toast.LENGTH_LONG).show()
////                    }
////                } else {
////                    Toast.makeText(this, "Invalid quantity entered", Toast.LENGTH_LONG).show()
////                }
////            }
////            .setNegativeButton("Cancel", null)
////            .show()
////    }
//private fun promptForProductQuantityIfNone(productName: String, expectedQuantity: Double) {
//    val editText = EditText(this).apply {
//        inputType = InputType.TYPE_CLASS_NUMBER
//        hint = "Enter product quantity"
//    }
//
//    AlertDialog.Builder(this)
//        .setTitle("Enter Quantity")
//        .setMessage("Enter product quantity for $productName. \nTracking: [none]")
//        .setView(editText)
//        .setPositiveButton("OK") { _, _ ->
//            val enteredQuantity = editText.text.toString().toDoubleOrNull()
//            if (enteredQuantity != null && enteredQuantity == expectedQuantity) {
//                Toast.makeText(this, "Quantity matches for $productName.", Toast.LENGTH_LONG).show()
//                quantityMatches[productName] = true
//                saveMatchStateToPreferences(productName, true) // Save state to preferences
//                productsAdapter.updateProducts(productsAdapter.products, quantityMatches) // This might need to be adjusted based on where you call it
//            } else {
//                Toast.makeText(this, "Entered quantity does not match the expected quantity for $productName.", Toast.LENGTH_LONG).show()
//            }
//
//            // Refresh the adapter with the new match statuses
//            productsAdapter.updateProducts(productsAdapter.products, quantityMatches)
//        }
//        .setNegativeButton("Cancel", null)
//        .show()
//}
//
//
//
//    private fun verifyProductByBarcode(productName: String, scannedBarcode: String) {
//    lifecycleScope.launch(Dispatchers.IO) {
//        try {
//            val trackingType = odooXmlRpcClient.fetchProductTrackingByName(productName)
//            withContext(Dispatchers.Main) {
//                // Instead of showing a Toast message, prompt for quantity input
//                promptForProductQuantity(productName, trackingType)
//            }
//        } catch (e: Exception) {
//            // Handle potential errors, for example, network issues or parsing errors
//            withContext(Dispatchers.Main) {
//                Toast.makeText(this@ProductsActivity, "Error fetching tracking info: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//}
//
//    private fun promptForProductQuantity(productName: String, trackingType: String?) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER
//            hint = "Enter product quantity"
//        }
//
//        val trackingMessage = when (trackingType) {
//            "serial" -> "Tracked by unique serial number"
//            "lot" -> "Tracked by lots"
//            "none" -> "No tracking"
//            else -> "Tracking information not available"
//        }
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Quantity")
//            .setMessage("Enter product quantity for $productName. Tracking: $trackingMessage")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredQuantity = editText.text.toString().toDoubleOrNull()
//                if (enteredQuantity != null) {
//                    Toast.makeText(this, "Quantity for $productName: $enteredQuantity", Toast.LENGTH_LONG).show()
//                    // Handle the entered quantity as needed
//                } else {
//                    Toast.makeText(this, "Invalid quantity entered", Toast.LENGTH_LONG).show()
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun promptForLotQuantity(receiptId: Int) {
//        val enteredSerialOrLotNumber = barcodeInput.text.toString().trim()
//
//        lifecycleScope.launch {
//            val products = withContext(Dispatchers.IO) { odooXmlRpcClient.fetchProductsForReceipt(receiptId) }
//            var matchedQuantity: Double? = null
//
//            // Search for a product matching the entered serial/lot number
//            for (product in products) {
//                val serialNumbers = withContext(Dispatchers.IO) { odooXmlRpcClient.fetchSerialNumbersByProductName(product.name) }
//                if (serialNumbers?.contains(enteredSerialOrLotNumber) == true) {
//                    matchedQuantity = product.quantity
//                    break
//                }
//            }
//
//            withContext(Dispatchers.Main) {
//                if (matchedQuantity != null) {
//                    showQuantityDialog(enteredSerialOrLotNumber, matchedQuantity!!)
//                } else {
//                    Toast.makeText(this@ProductsActivity, "No matching product found for the entered serial/lot number.", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun showQuantityDialog(serialOrLotNumber: String, matchedQuantity: Double) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER
//            hint = "Enter product quantity"
//        }
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Quantity")
//            .setMessage("Enter product quantity for lot: $serialOrLotNumber")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredQuantity = editText.text.toString().toDoubleOrNull()
//                if (enteredQuantity != null) {
//                    if (enteredQuantity == matchedQuantity) {
//                        Toast.makeText(this, "Quantity matches for lot $serialOrLotNumber.", Toast.LENGTH_LONG).show()
//                    } else {
//                        Toast.makeText(this, "Entered quantity does not match expected quantity.", Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    Toast.makeText(this, "Invalid quantity entered", Toast.LENGTH_LONG).show()
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//    private fun updateAdapterWithProductsAndMatches(products: List<Product>) {
//        // Assuming you have determined the quantityMatches by now
//        // For demonstration, let's say all products match (replace this with actual logic)
//        val newQuantityMatches = products.associate { it.name to true }
//        productsAdapter.updateProducts(products, newQuantityMatches)
//    }
//
//
//    private fun saveMatchStateToPreferences(productName: String, matches: Boolean) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE) ?: return
//        with(sharedPref.edit()) {
//            putBoolean(productName, matches)
//            apply()
//        }
//    }
//    private fun loadMatchStatesFromPreferences(newQuantityMatches: MutableMap<String, Boolean>) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE) ?: return
//        quantityMatches.clear() // Clear any existing states to refresh with current data
//
//        // Load saved states; if not saved before, keep the new match state
//        newQuantityMatches.keys.forEach { productName ->
//            quantityMatches[productName] = sharedPref.getBoolean(productName, newQuantityMatches[productName] ?: false)
//        }
//
//        // Now that we have the latest match states, update the adapter
//        productsAdapter.updateProducts(productsAdapter.products, quantityMatches)
//    }
//
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        coroutineScope.cancel()
//    }
//}
//
//
//
//package com.example.warehousetet
//
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.text.InputType
//import android.util.Log
//import android.view.KeyEvent
//import android.view.View
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.*
//
//class ProductsActivity : AppCompatActivity() {
//    private lateinit var productsAdapter: ProductsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var barcodeInput: EditText
//    private lateinit var confirmButton: Button
//
//    private var productBarcodes = hashMapOf<String, String>()
//    private var productSerialNumbers = hashMapOf<String, List<String>>()
//    private var quantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_products)
//
//        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
//        barcodeInput = findViewById(R.id.barcodeInput)
//        confirmButton = findViewById(R.id.confirmButton)
//        productsAdapter = ProductsAdapter(emptyList(), quantityMatches)
//
//        val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
//        if (receiptId != -1) {
//            setupRecyclerView()
//            fetchProductsForReceipt(receiptId)
//        } else {
//            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity.")
//        }
//
//        setupBarcodeVerification()
//        loadMatchStatesFromPreferences()
//    }
//
//    private fun setupRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = productsAdapter
//    }
//
//    //    private fun setupBarcodeVerification() {
////        confirmButton.setOnClickListener {
////            val enteredBarcode = barcodeInput.text.toString().trim()
////            verifyBarcode(enteredBarcode)
////            hideKeyboard()
////        }
////
////        barcodeInput.setOnEditorActionListener { _, actionId, event ->
////            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
////                confirmButton.performClick()
////                true
////            } else false
////        }
////    }
//    private fun setupBarcodeVerification() {
//        // Retrieve the receipt ID when the activity is created or when needed.
//        val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
//
//        confirmButton.setOnClickListener {
//            val enteredBarcode = barcodeInput.text.toString().trim()
//            if (receiptId != -1) {
//                verifyBarcode(enteredBarcode, receiptId) // Pass receiptId to verifyBarcode
//                hideKeyboard()
//            } else {
//                Toast.makeText(this, "Invalid Receipt ID.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        barcodeInput.setOnEditorActionListener { _, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                confirmButton.performClick() // This will invoke the onClickListener above
//                true
//            } else false
//        }
//    }
//
//    private fun hideKeyboard() {
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
//    }
//
//    //    private fun fetchProductsForReceipt(receiptId: Int) {
////        coroutineScope.launch {
////            val products = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
////            Log.d("ProductsActivity", "Fetched products: $products")
////            // Assuming a method to fetch barcodes for all products
////            productBarcodes.clear()
////            productSerialNumbers.clear()
////            products.forEach { product ->
////                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
////                barcode?.let { productBarcodes[product.name] = it }
////
////                val serialNumbers = odooXmlRpcClient.fetchSerialNumbersByProductName(product.name)
////                serialNumbers?.let { productSerialNumbers[product.name] = it }
////            }
////            val newQuantityMatches = mutableMapOf<String, Boolean>()
////            products.forEach { product ->
////                newQuantityMatches[product.name] = quantityMatches[product.name] ?: false
////            }
////
////            withContext(Dispatchers.Main) {
////                quantityMatches = newQuantityMatches
////                productsAdapter.updateProducts(products, quantityMatches)
////            }
////        }
////    }
//    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
//    private fun fetchProductsForReceipt(receiptId: Int) {
//        coroutineScope.launch {
//            // Fetch products from your Odoo server for the specified receipt
//            val products = odooXmlRpcClient.fetchProductsForReceipt(receiptId)
//            Log.d("ProductsActivity", "Fetched products: $products")
//
//            // Reset barcode and serial numbers mapping
//            productBarcodes.clear()
//            productSerialNumbers.clear()
//
//            // Reset barcode to product ID mapping
//            barcodeToProductIdMap.clear()
//
//            // Prepare a new mapping for quantity matches with the new receipt
//            val newQuantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//
//            // Process each fetched product
//            products.forEach { product ->
//                // Update barcode to product ID mapping
//                product.barcode?.let { barcode ->
//                    barcodeToProductIdMap[barcode] = product.id
//                }
//
//                // Create a key for each product within the receipt
//                val key = ProductReceiptKey(product.id, receiptId)
//
//                // Initialize the match state for each product in the receipt
//                newQuantityMatches[key] = quantityMatches[key] ?: false
//            }
//
//            // Update the UI on the main thread
//            withContext(Dispatchers.Main) {
//                // Replace the current quantity matches with the new ones
//                quantityMatches.clear()
//                quantityMatches.putAll(newQuantityMatches)
//
//                // Notify the adapter to update the UI with the new data
//                productsAdapter.updateProducts(products, receiptId, quantityMatches)
//            }
//        }
//
//        //    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
////        coroutineScope.launch {
////            // Attempt to find a matched product based on barcode
////            val matchedProduct = productBarcodes.entries.find { it.value == scannedBarcode }
////
////            if (matchedProduct != null) {
////                // Retrieve product name and ID for further processing
////                val productName = matchedProduct.key
////                val productId = productSerialNumbers.filterKeys { it == productName }.keys.firstOrNull()?.let { productSerialNumbers[it]?.firstOrNull() } ?: -1 // Assuming you have a way to retrieve product ID
////
////                if (productId != -1) {
////                    // Create a ProductReceiptKey using the product ID and receipt ID
////                    val key = ProductReceiptKey(productId, receiptId)
////
////                    withContext(Dispatchers.Main) {
////                        // Update the match state in the map
////                        quantityMatches[key] = true
////                        productsAdapter.updateProducts(productsAdapter.products, receiptId, quantityMatches)
////                        Toast.makeText(this@ProductsActivity, "$productName matched and verified.", Toast.LENGTH_SHORT).show()
////                    }
////
////                    // Consider saving this state if necessary
////                    saveMatchStateToPreferences(key, true)
////                } else {
////                    withContext(Dispatchers.Main) {
////                        Toast.makeText(this@ProductsActivity, "Product ID not found.", Toast.LENGTH_SHORT).show()
////                    }
////                }
////            } else {
////                withContext(Dispatchers.Main) {
////                    Toast.makeText(this@ProductsActivity, "Barcode not found.", Toast.LENGTH_SHORT).show()
////                }
////            }
////        }
////    }
//        private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
//            coroutineScope.launch {
//                val productId = barcodeToProductIdMap[scannedBarcode]
//
//                if (productId != null) {
//                    val key = ProductReceiptKey(productId, receiptId)
//
//                    withContext(Dispatchers.Main) {
//                        quantityMatches[key] = true
//                        productsAdapter.updateProducts(
//                            productsAdapter.products,
//                            receiptId,
//                            quantityMatches
//                        )
//                        Toast.makeText(
//                            this@ProductsActivity,
//                            "Product matched and verified.",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//
//                    // Consider saving this state if necessary
//                    saveMatchStateToPreferences(key, true)
//                } else {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            this@ProductsActivity,
//                            "Barcode not found.",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//        }
//
//
////    private fun saveMatchStateToPreferences(productName: String, matches: Boolean) {
////        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
////        with(sharedPref.edit()) {
////            putBoolean(productName, matches)
////            apply()
////        }
////    }
//
//        private fun saveMatchStateToPreferences(key: ProductReceiptKey, matches: Boolean) {
//            // This function needs to serialize ProductReceiptKey in a way it can be stored and retrieved
//            // Consider using a combination of product ID and receipt ID as a unique string key
//            val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//            with(sharedPref.edit()) {
//                putBoolean("${key.productId}_${key.receiptId}", matches)
//                apply()
//            }
//        }
//
//        private fun loadMatchStatesFromPreferences() {
//            val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//            sharedPref.all.forEach { (key, value) ->
//                if (value is Boolean) {
//                    quantityMatches[key] = value
//                }
//            }
//            // Update the adapter with loaded match states
//            productsAdapter.updateProducts(productsAdapter.products, quantityMatches)
//        }
//
//        override fun onDestroy() {
//            super.onDestroy()
//            coroutineScope.cancel()
//        }
//    }
//}


package com.example.warehousetet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
