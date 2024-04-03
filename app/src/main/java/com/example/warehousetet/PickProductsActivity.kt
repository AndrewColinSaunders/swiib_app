package com.example.warehousetet

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickProductsActivity : AppCompatActivity() {
    private lateinit var pickProductsAdapter: PickProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button

    //    private var productBarcodes = hashMapOf<String, String>()
    private var productSerialNumbers = hashMapOf<ProductDOKey, MutableList<String>>()
    val lotQuantities: MutableMap<ProductDOKey, Int> = mutableMapOf()
    private var quantityMatches = mutableMapOf<ProductDOKey, Boolean>()
    private var barcodeToProductIdMap = mutableMapOf<String, Int>()

    private var deliveryOrderName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pick_activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))

        barcodeInput = findViewById(R.id.doBarcodeInput)
        confirmButton = findViewById(R.id.doConfirmButton)

        val deliveryOrderId = intent.getIntExtra("DELIVERY_ORDER_ID", -1)
        val deliveryOrderName = intent.getStringExtra("DELIVERY_ORDER_NAME") ?: "Delivery Order"

        // Set the title to the delivery order's name
        val titleTextView: TextView = findViewById(R.id.doProductsTitleTextView)
        titleTextView.text = deliveryOrderName

        pickProductsAdapter = PickProductsAdapter(emptyList(), mapOf(), deliveryOrderId)
        setupRecyclerView()

        if (deliveryOrderId != -1) {
            fetchProductsForDeliveryOrder(deliveryOrderId)
        } else {
            Log.e("PickProductsActivity", "Invalid delivery order ID passed to PickProductsActivity.")
        }

        findViewById<Button>(R.id.doConfirmButton).setOnClickListener {
            findViewById<EditText>(R.id.barcodeInput).text.clear()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupBarcodeVerification(deliveryOrderId)
        loadMatchStatesFromPreferences(deliveryOrderId)
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.doProductsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = pickProductsAdapter
    }

    private fun showRedToast(message: String) {
        val toast = Toast.makeText(this@PickProductsActivity, message, Toast.LENGTH_SHORT)
        val view = toast.view

        // Get the TextView of the default Toast view
        val text = view?.findViewById<TextView>(android.R.id.message)

        // Set the background color of the Toast view
        view?.background?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)

        // Set the text color to be more visible on the red background, if needed
        text?.setTextColor(Color.WHITE)

        toast.show()
    }
    private fun showGreenToast(message: String) {
        val toast = Toast.makeText(this@PickProductsActivity, message, Toast.LENGTH_SHORT)
        val view = toast.view

        // Get the TextView of the default Toast view
        val text = view?.findViewById<TextView>(android.R.id.message)

        // Retrieve the success_green color from resources
        val successGreen = ContextCompat.getColor(this@PickProductsActivity, R.color.success_green)

        // Set the background color of the Toast view to success_green
        view?.background?.setColorFilter(successGreen, PorterDuff.Mode.SRC_IN)

        // Set the text color to be more visible on the green background, if needed
        text?.setTextColor(Color.WHITE)

        toast.show()
    }

    private fun fetchProductsForDeliveryOrder(deliveryOrderId: Int) {
        coroutineScope.launch {
            Log.d("PickProductsActivity", "Fetching products for delivery order ID: $deliveryOrderId")

            // Attempt to fetch products associated with the delivery order
            val fetchedProducts = try {
                odooXmlRpcClient.fetchProductsForReceipt(deliveryOrderId)
            } catch (e: Exception) {
                Log.e("PickProductsActivity", "Error fetching products for delivery order: ${e.localizedMessage}")
                emptyList<Product>() // Adjust this to your product data class
            }

            // Fetch barcodes and other additional details for all fetched products
            fetchBarcodesForProducts(fetchedProducts)

            // Initialize a list to store the updated products with additional details
            val updatedProductsWithDetails = mutableListOf<Product>()

            fetchedProducts.forEach { product ->
                coroutineScope.launch(Dispatchers.IO) {
                    // Assuming fetchProductTrackingAndExpirationByName can be used for delivery orders as well
                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(product.name) ?: Pair("none", false)
                    Log.d("PickProductsActivity", "Product: ${product.name}, Uses Expiration Date: ${trackingAndExpiration.second}")
                    val barcode = barcodeToProductIdMap.filterValues { it == product.id }.keys.firstOrNull()

                    // Update product details to include tracking type, expiration date, and barcode
                    val updatedProduct = product.copy(
                        trackingType = trackingAndExpiration.first,
                        useExpirationDate = trackingAndExpiration.second,
                        barcode = barcode
                    )
                    updatedProductsWithDetails.add(updatedProduct)
                }.join() // Wait for all coroutine operations within the forEach loop to complete
            }

            withContext(Dispatchers.Main) {
                Log.d("PickProductsActivity", "Updating UI with detailed products for delivery order")
    //            pickProductsAdapter.updateProducts(updatedProductsWithDetails)
                updateUIForProducts(updatedProductsWithDetails, deliveryOrderId)
            }
        }
    }
    private fun updateUIForProducts(products: List<Product>, deliveryOrderId: Int) {
        // Assuming barcodeToProductIdMap and productSerialNumbers are already populated or handled within the fetched products loop
        val newQuantityMatches = products.associate {
            ProductDOKey(it.id, deliveryOrderId) to (quantityMatches[ProductDOKey(it.id, deliveryOrderId)] ?: false)
        }.toMutableMap()

        // Now update the quantityMatches and UI accordingly
        quantityMatches.clear()
        quantityMatches.putAll(newQuantityMatches)
        pickProductsAdapter.updateProducts(products, deliveryOrderId, quantityMatches)
    }

    private fun fetchBarcodesForProducts(products: List<Product>) {
        products.forEach { product ->
            coroutineScope.launch {
                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
                barcode?.let {
                    // Assuming barcodeToProductIdMap should map barcode to product ID
                    synchronized(this@PickProductsActivity) { // Ensure to use your current activity or context correctly
                        barcodeToProductIdMap[barcode] = product.id
                    }
                }
            }
        }
    }

    private fun setupBarcodeVerification(deliveryOrderId: Int) {
        confirmButton.setOnClickListener {
            val enteredBarcode = barcodeInput.text.toString().trim()
            verifyBarcode(enteredBarcode, deliveryOrderId)
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

    private fun verifyBarcode(scannedBarcode: String, deliveryOrderId: Int) {
        coroutineScope.launch {
            val productId = barcodeToProductIdMap[scannedBarcode]
            if (productId != null) {
                val product = pickProductsAdapter.products.find { it.id == productId }
                product?.let {
                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(it.name)
                    val trackingType = trackingAndExpiration?.first ?: "none"

                    when (trackingType) {
                        "serial" -> {
                            Log.d("verifyBarcode", "Tracking Type: Serial")
                            withContext(Dispatchers.Main) {
                                promptForSerialNumber(it.name, deliveryOrderId, it.id)
                            }
                        }
                        "lot" -> {
                            Log.d("verifyBarcode", "Tracking Type: Lot")
                            withContext(Dispatchers.Main) {
                                // Implementation for lot-tracked products; likely prompting for lot number
                            }
                        }
                        "none" -> {
                            Log.d("verifyBarcode", "Tracking Type: None")
                            withContext(Dispatchers.Main) {
                                // Handle non-serialized, non-lot-tracked products
                            }
                        }
                        else -> {
                            Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
                            withContext(Dispatchers.Main) {
                                // Optional: handle any other specific cases
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Log.d("verifyBarcode", "Barcode not found")
                    // Handle the case where the barcode isn't found; you might show a toast message or alert dialog
                }
            }
        }
    }

    private fun promptForSerialNumber(productName: String, deliveryOrderId: Int, productId: Int) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter serial number"
        }

        var actionExecuted = false

        // Build the dialog but don't show it yet
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Enter Serial Number")
            .setMessage("Enter the serial number for $productName.")
            .setView(editText)
            .setNegativeButton("Cancel", null)

        // Create the dialog from the builder
        val dialog = dialogBuilder.create()

        // Now set the positive button separately to have access to 'dialog' variable
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            if (!actionExecuted) {
                actionExecuted = true
                val enteredSerialNumber = editText.text.toString().trim()
                if (enteredSerialNumber.isNotEmpty()) {
                    coroutineScope.launch {
                        val product = pickProductsAdapter.products.find { it.id == productId }
                        val key = ProductDOKey(productId, deliveryOrderId) // Adjusted to use ProductDOKey assuming you have such a class
                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }

                        if (!serialList.contains(enteredSerialNumber)) {
                            serialList.add(enteredSerialNumber)
//                            odooXmlRpcClient.updateMoveLinesWithoutExpiration(deliveryOrderId, productId, enteredSerialNumber)
                            odooXmlRpcClient.updateMoveLinesForPick(deliveryOrderId, productId, enteredSerialNumber)

                            updateProductMatchState(productId, deliveryOrderId, matched = true, serialList)
                            withContext(Dispatchers.Main) {
                                showGreenToast("Serial number added for $productName. ${serialList.size} verified")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showRedToast("Serial number already entered for $productName")
                            }
                        }
                    }
                } else {

                        showRedToast("Please enter a serial number")

                }
            }
        }

        // Set up dialog properties related to keyboard input
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener {
            editText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }

        editText.setOnEditorActionListener { _, actionId, event ->
            if ((actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) && !actionExecuted) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }

        dialog.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed() // This will handle the back action
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }





//    private fun updateProductMatchState(
//        productId: Int,
//        deliveryOrderId: Int,
//        matched: Boolean = false,
//        serialNumbers: MutableList<String>? = null,
//        lotQuantity: Int? = null
//    ) {
//        val key = ProductDOKey(productId, deliveryOrderId)
//
//        // Get product and its expected quantity with a fallback to 0 if not found or null
//        val expectedQuantity = pickProductsAdapter.products.find { it.id == productId }?.quantity?.toInt() ?: 0
//
//        // Update match state based on the logic for serialized or lot-tracked products
//        if (serialNumbers != null) {
//            // Serialized products logic
//            quantityMatches[key] = serialNumbers.size == expectedQuantity
//        } else if (lotQuantity != null) {
//            // Lot products logic
//            val currentQuantity = lotQuantities.getOrDefault(key, 0) + lotQuantity
//            lotQuantities[key] = currentQuantity
//            quantityMatches[key] = currentQuantity >= expectedQuantity
//        } else {
//            // For non-serialized and non-lotted products, directly use the matched parameter
//            quantityMatches[key] = matched
//        }
//
//        // Check if all products are matched after updating the match state
//        val allProductsMatched = checkAllProductsMatched(deliveryOrderId)
//
//        // Save match state to preferences
//        saveMatchStateToPreferences(key, quantityMatches[key] == true)
//
//        // Update UI accordingly
//        val position = pickProductsAdapter.findProductPositionById(productId)
//        if (position != -1) {
//            runOnUiThread { pickProductsAdapter.notifyItemChanged(position) }
//        }
//        if (allProductsMatched) {
//            coroutineScope.launch {
//                val validated = odooXmlRpcClient.validateOperation(deliveryOrderId)
//                withContext(Dispatchers.Main) {
//                    if (validated) {
////                        Log.d("ProductsActivity", "Receipt validated successfully.")
//                        showGreenToast("Receipt validated")
//                        // Redirect to ReceiptsActivity
//                        val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
//                        startActivity(intent)
//                        finish() // Optional: if you want to remove the current activity from the stack
//                    } else {
////                        Log.e("ProductsActivity", "Failed to validate receipt.")
//                        showRedToast("Failed to validate receipt")
//                    }
//                }
//            }
//        }
//
//    }
private fun updateProductMatchState(
    productId: Int,
    deliveryOrderId: Int,
    matched: Boolean = false,
    serialNumbers: MutableList<String>? = null,
    lotQuantity: Int? = null
) {
    val key = ProductDOKey(productId, deliveryOrderId)
    val product = pickProductsAdapter.products.find { it.id == productId }
    val expectedQuantity = product?.quantity?.toInt() ?: 0

    if (serialNumbers != null) {
        quantityMatches[key] = serialNumbers.size == expectedQuantity
        // Log to verify match state update
        Log.d("MatchState", "Product ID: $productId, Verified: ${serialNumbers.size}/$expectedQuantity")
    } else if (lotQuantity != null) {
        val currentQuantity = lotQuantities.getOrDefault(key, 0) + lotQuantity
        lotQuantities[key] = currentQuantity
        quantityMatches[key] = currentQuantity >= expectedQuantity
    } else {
        quantityMatches[key] = matched
    }

    val allProductsMatched = checkAllProductsMatched(deliveryOrderId)
    saveMatchStateToPreferences(key, quantityMatches[key] == true)

    val position = pickProductsAdapter.findProductPositionById(productId)
    if (position != -1) {
        runOnUiThread { pickProductsAdapter.notifyItemChanged(position) }
    }

    if (allProductsMatched) {
        coroutineScope.launch {
            val validated = odooXmlRpcClient.validateOperation(deliveryOrderId)
            withContext(Dispatchers.Main) {
                if (validated) {
                    showGreenToast("Receipt validated")
                    val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    showRedToast("Failed to validate receipt")
                }
            }
        }
    }
}


    private fun checkAllProductsMatched(deliveryOrderId: Int): Boolean {
        // Filter the quantityMatches for the current receiptId
        return quantityMatches.filter { it.key.DeliveryOrderId == deliveryOrderId }.all { it.value }
    }


    private fun saveMatchStateToPreferences(key: ProductDOKey, matched: Boolean) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("${key.productId}_${key.DeliveryOrderId}", matched)
            apply()
        }
    }

    private fun loadMatchStatesFromPreferences(deliveryOrderId: Int) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        val tempQuantityMatches = mutableMapOf<ProductDOKey, Boolean>()

        sharedPref.all.forEach { (prefKey, value) ->
            if (value is Boolean) {
                val parts = prefKey.split("_").let { if (it.size == 2) it else null }
                parts?.let {
                    try {
                        val productId = it[0].toInt()
                        val prefDeliveryOrderId = it[1].toInt()
                        if (prefDeliveryOrderId == deliveryOrderId) {
                            val key = ProductDOKey(productId, prefDeliveryOrderId)
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
            pickProductsAdapter.updateProducts(pickProductsAdapter.products, deliveryOrderId, quantityMatches)
        }
    }
}

