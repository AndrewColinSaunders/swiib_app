package com.example.warehousetet

import android.content.Context
<<<<<<< Updated upstream
=======
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
>>>>>>> Stashed changes
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
<<<<<<< Updated upstream
=======
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
>>>>>>> Stashed changes
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
<<<<<<< Updated upstream
=======
import android.widget.TextView
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
        if (receiptId != -1) {
            setupRecyclerView()
            fetchProductsForReceipt(receiptId)
        } else {
            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity.")
=======
//    // Set ActionBar background color
//    supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#212d40"))) // Use the color #212d40

//    // Change the status bar color to match the ActionBar
//    window.statusBarColor = ContextCompat.getColor(this, R.color.cardGrey) // Use the color defined as cardGrey

    odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
    barcodeInput = findViewById(R.id.barcodeInput)
    confirmButton = findViewById(R.id.confirmButton)
    val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
    receiptName = intent.getStringExtra("RECEIPT_NAME")
    Log.d("ProductsActivity", "Received receipt name: $receiptName")
    val titleTextView: TextView = findViewById(R.id.productsTitleTextView)
    titleTextView.text = receiptName

    productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId)

    if (receiptId != -1) {
        setupRecyclerView()
        fetchProductsForReceipt(receiptId)
    } else {
        Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity.")
    }

    findViewById<Button>(R.id.clearButton).setOnClickListener {
        findViewById<EditText>(R.id.barcodeInput).text.clear()
    }


    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    setupBarcodeVerification(receiptId)
    loadMatchStatesFromPreferences(receiptId)
}


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_products_activity, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val menuItem = menu.findItem(R.id.action_flag_receipt)
        val spanString = SpannableString(menuItem.title).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@ProductsActivity, R.color.danger_red)), 0, length, 0)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        menuItem.title = spanString
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
        R.id.action_flag_receipt -> {
            AlertDialog.Builder(this).apply {
                setTitle("Flag Receipt")
                setMessage("Are you sure you want to flag this receipt?")
                setPositiveButton("Flag Receipt") { _, _ ->
                    flagReceipt()
                }
                setNegativeButton(android.R.string.cancel, null)
            }.show()
            true
        }
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}

    private fun showRedToast(message: String) {
        // Inflate the custom toast layout
        val inflater = layoutInflater
        val layout =
            inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container))

        // Find the TextView within the custom layout and set the message
        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message

        // Set the background color of the custom toast to #4CAF50
        val dangerRed = ContextCompat.getColor(applicationContext, R.color.danger_red)
        layout.background.setColorFilter(dangerRed, PorterDuff.Mode.SRC_IN)
        textView.setTextColor(Color.WHITE) // Ensure the text color is white for contrast

        // Create and show the custom toast
        with(Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }
    private fun showGreenToast(message: String) {
        // Inflate the custom toast layout
        val inflater = layoutInflater
        val layout =
            inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container))

        // Find the TextView within the custom layout and set the message
        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message

        // Set the background color of the custom toast to #4CAF50
        val successGreen = ContextCompat.getColor(applicationContext, R.color.success_green)
        layout.background.setColorFilter(successGreen, PorterDuff.Mode.SRC_IN)
        textView.setTextColor(Color.WHITE) // Ensure the text color is white for contrast

        // Create and show the custom toast
        with(Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }

    private fun flagReceipt() {
    val receiptId = intent.getIntExtra("RECEIPT_ID", -1) // Assuming you have receiptId
    if (receiptId == -1) {
        Log.e("ProductsActivity", "Invalid receipt ID")
        return
    }

    coroutineScope.launch {
        val receiptName = this@ProductsActivity.receiptName ?: return@launch
        odooXmlRpcClient.fetchAndLogBuyerDetails(receiptName)?.let { buyerDetails ->
            sendEmailFlaggingReceipt(buyerDetails.login, buyerDetails.name, receiptName)
            withContext(Dispatchers.Main) {
                Log.d("ProductsActivity", "Receipt flagged and buyer notified via email.")
                showRedToast("Receipt flagged")
            }
        } ?: run {
            withContext(Dispatchers.Main) {
                Log.e("ProductsActivity", "Failed to fetch buyer details or flag the receipt.")
//                Toast.makeText(this@ProductsActivity, "Failed to flag receipt", Toast.LENGTH_SHORT).show()
                  showRedToast("Failed to flag receipt")
            }
        }
    }
}

    private fun sendEmailFlaggingReceipt(buyerEmail: String, buyerName: String, receiptName: String) {
        val props = Properties().apply {
            put("mail.smtp.host", "mail.dattec.co.za")
            put("mail.smtp.socketFactory.port", "465")
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.auth", "true")
            put("mail.smtp.port", "465")
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
            runOnUiThread {
                productsAdapter.notifyItemChanged(position)
=======
            runOnUiThread { productsAdapter.notifyItemChanged(position) }
        }
        if (allProductsMatched) {
            coroutineScope.launch {
                val validated = odooXmlRpcClient.validateOperation(this@ProductsActivity,receiptId)
                withContext(Dispatchers.Main) {
                    if (validated) {
//                        Log.d("ProductsActivity", "Receipt validated successfully.")
                           showGreenToast("Receipt validated")
                        // Redirect to ReceiptsActivity
                        val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
                        startActivity(intent)
                        finish() // Optional: if you want to remove the current activity from the stack
                    } else {
//                        Log.e("ProductsActivity", "Failed to validate receipt.")
                        showRedToast("Failed to validate receipt")
                    }
                }
>>>>>>> Stashed changes
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