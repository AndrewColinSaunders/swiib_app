//package com.example.warehousetet
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.PorterDuff
//import android.graphics.Typeface
//import android.os.Bundle
//import android.text.InputType
//import android.util.Log
//import android.view.KeyEvent
//import android.view.MenuItem
//import android.view.View
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.widget.Button
//import android.widget.EditText
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.util.Properties
//import javax.mail.Message
//import javax.mail.MessagingException
//import javax.mail.PasswordAuthentication
//import javax.mail.Session
//import javax.mail.Transport
//import javax.mail.internet.InternetAddress
//import javax.mail.internet.MimeMessage
//
//class PickProductsActivity : AppCompatActivity() {
//    private lateinit var pickProductsAdapter: PickProductsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var barcodeInput: EditText
//    private lateinit var confirmButton: Button
//    private lateinit var constants: Constants
//
//    //    private var productBarcodes = hashMapOf<String, String>()
//    private var productSerialNumbers = hashMapOf<ProductPickKey, MutableList<String>>()
//    val lotQuantities: MutableMap<ProductPickKey, Int> = mutableMapOf()
//    private var quantityMatches = mutableMapOf<ProductPickKey, Boolean>()
//    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
//    // Assuming this is declared at the class level
//    private val accumulatedQuantities: MutableMap<Int, Double> = mutableMapOf()
//
//
//    private var pickName: String? = null
//    private var destLocationName: String? = null
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.pick_activity_products)
//
//        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
//
//        barcodeInput = findViewById(R.id.pickBarcodeInput)
//        confirmButton = findViewById(R.id.pickConfirmButton)
//
//        val pickId = intent.getIntExtra("PICK_ID", -1)
//        val pickName = intent.getStringExtra("PICK_NAME") ?: "Pick"
////        val titleTextView: TextView = findViewById(R.id.pickProductsTitleTextView)
////        titleTextView.text = pickName
//
//        val locationName = intent.getStringExtra("LOCATION")
//        val locationTextView: TextView = findViewById(R.id.sourceLocationId)
//        locationTextView.text = locationName
//
//        destLocationName = intent.getStringExtra("DEST_LOCATION")
//        val destLocationTextView: TextView = findViewById(R.id.destinationLocationId)
//        destLocationTextView.text = destLocationName
//
//        supportActionBar?.title = pickName
//
//        pickProductsAdapter = PickProductsAdapter(emptyList(), mapOf(), pickId)
//        setupRecyclerView()
//
//        if (pickId != -1) {
//            fetchProductsForPick(pickId)
//            coroutineScope.launch {
//                try {
////                    displayLocationsForPick(pickId)
//                } catch (e: Exception) {
//                    Log.e("PickProductsActivity", "Error displaying locations for pick: ${e.message}")
//                }
//            }
//        } else {
//            Log.e("PickProductsActivity", "Invalid delivery order ID passed to PickProductsActivity.")
//        }
//
//
//        findViewById<Button>(R.id.pickClearButton).setOnClickListener {
//            findViewById<EditText>(R.id.pickBarcodeInput).text.clear()
//        }
//
//        findViewById<Button>(R.id.resetButton).setOnClickListener {
//            resetMatchStates(pickId)  // Call the reset function when the button is clicked
//        }
//
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        setupBarcodeVerification(pickId)
//        loadMatchStatesFromPreferences(pickId)
//    }
//
//    private fun setupRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.pickProductsRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = pickProductsAdapter
//    }
//
//    private fun showRedToast(message: String) {
//        val toast = Toast.makeText(this@PickProductsActivity, message, Toast.LENGTH_SHORT)
//        val view = toast.view
//
//        // Get the TextView of the default Toast view
//        val text = view?.findViewById<TextView>(android.R.id.message)
//
//        // Set the background color of the Toast view
//        view?.background?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
//
//        // Set the text color to be more visible on the red background, if needed
//        text?.setTextColor(Color.WHITE)
//
//        toast.show()
//    }
//    private fun showGreenToast(message: String) {
//        val toast = Toast.makeText(this@PickProductsActivity, message, Toast.LENGTH_SHORT)
//        val view = toast.view
//
//        // Get the TextView of the default Toast view
//        val text = view?.findViewById<TextView>(android.R.id.message)
//
//        // Retrieve the success_green color from resources
//        val successGreen = ContextCompat.getColor(this@PickProductsActivity, R.color.success_green)
//
//        // Set the background color of the Toast view to success_green
//        view?.background?.setColorFilter(successGreen, PorterDuff.Mode.SRC_IN)
//
//        // Set the text color to be more visible on the green background, if needed
//        text?.setTextColor(Color.WHITE)
//
//        toast.show()
//    }
//
//    private fun fetchProductsForPick(pickId: Int) {
//        coroutineScope.launch {
//            Log.d("PickProductsActivity", "Fetching products for delivery order ID: $pickId")
//
//            // Attempt to fetch products associated with the delivery order
//            val fetchedProducts = try {
//                odooXmlRpcClient.fetchProductsForReceipt(pickId)
//            } catch (e: Exception) {
//                Log.e("PickProductsActivity", "Error fetching products for delivery order: ${e.localizedMessage}")
//                emptyList<Product>() // Adjust this to your product data class
//            }
//
//            // Fetch barcodes and other additional details for all fetched products
//            fetchBarcodesForProducts(fetchedProducts)
//
//            // Initialize a list to store the updated products with additional details
//            val updatedProductsWithDetails = mutableListOf<Product>()
//
//            fetchedProducts.forEach { product ->
//                coroutineScope.launch(Dispatchers.IO) {
//                    // Assuming fetchProductTrackingAndExpirationByName can be used for delivery orders as well
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(product.name) ?: Pair("none", false)
//                    Log.d("PickProductsActivity", "Product: ${product.name}, Uses Expiration Date: ${trackingAndExpiration.second}")
//                    val barcode = barcodeToProductIdMap.filterValues { it == product.id }.keys.firstOrNull()
//
//                    // Update product details to include tracking type, expiration date, and barcode
//                    val updatedProduct = product.copy(
//                        trackingType = trackingAndExpiration.first,
//                        useExpirationDate = trackingAndExpiration.second,
//                        barcode = barcode
//                    )
//                    updatedProductsWithDetails.add(updatedProduct)
//                    odooXmlRpcClient.fetchMoveLinesByPickingId(pickId)
//                }.join() // Wait for all coroutine operations within the forEach loop to complete
//            }
//
//            withContext(Dispatchers.Main) {
//                Log.d("PickProductsActivity", "Updating UI with detailed products for delivery order")
//    //            pickProductsAdapter.updateProducts(updatedProductsWithDetails)
//                updateUIForProducts(updatedProductsWithDetails, pickId)
//            }
//        }
//    }
//    private fun updateUIForProducts(products: List<Product>, deliveryOrderId: Int) {
//        // Assuming barcodeToProductIdMap and productSerialNumbers are already populated or handled within the fetched products loop
//        val newQuantityMatches = products.associate {
//            ProductPickKey(it.id, deliveryOrderId) to (quantityMatches[ProductPickKey(it.id, deliveryOrderId)] ?: false)
//        }.toMutableMap()
//
//        // Now update the quantityMatches and UI accordingly
//        quantityMatches.clear()
//        quantityMatches.putAll(newQuantityMatches)
//        pickProductsAdapter.updateProducts(products, deliveryOrderId, quantityMatches)
//    }
//
//    private fun fetchBarcodesForProducts(products: List<Product>) {
//        products.forEach { product ->
//            coroutineScope.launch {
//                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
//                barcode?.let {
//                    // Assuming barcodeToProductIdMap should map barcode to product ID
//                    synchronized(this@PickProductsActivity) { // Ensure to use your current activity or context correctly
//                        barcodeToProductIdMap[barcode] = product.id
//                    }
//                }
//            }
//        }
//    }
//
//    private fun setupBarcodeVerification(pickId: Int) {
//        confirmButton.setOnClickListener {
//            val enteredBarcode = barcodeInput.text.toString().trim()
//            verifyBarcode(enteredBarcode, pickId)
//            hideKeyboard()
//        }
//
//        barcodeInput.setOnEditorActionListener { _, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                confirmButton.performClick()
//                true
//            } else false
//        }
//    }
//    private fun hideKeyboard() {
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
//    }
//
////    private fun verifyBarcode(scannedBarcode: String, deliveryOrderId: Int) {
////        coroutineScope.launch {
////            val productId = barcodeToProductIdMap[scannedBarcode]
////            val packagingProductInfo = odooXmlRpcClient.fetchProductIdFromPackagingBarcode(scannedBarcode)
////            if (productId != null) {
////                val product = pickProductsAdapter.products.find { it.id == productId }
////                product?.let {
////                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(it.name)
////                    val trackingType = trackingAndExpiration?.first ?: "none"
////
////                    when (trackingType) {
////                        "serial" -> {
////                            Log.d("verifyBarcode", "Tracking Type: Serial")
////                            withContext(Dispatchers.Main) {
////                                promptForSerialNumber(it.name, deliveryOrderId, it.id)
////                            }
////                        }
////                        "lot" -> {
////                            Log.d("verifyBarcode", "Tracking Type: Lot")
////                            withContext(Dispatchers.Main) {
////                                // Implementation for lot-tracked products; likely prompting for lot number
////                            }
////                        }
////                        "none" -> {
////                            Log.d("verifyBarcode", "Tracking Type: None")
////                            withContext(Dispatchers.Main) {
////                                // Handle non-serialized, non-lot-tracked products
////                            }
////                        }
////                        else -> {
////                            Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
////                            withContext(Dispatchers.Main) {
////                                // Optional: handle any other specific cases
////                            }
////                        }
////                    }
////                }
////            } else {
////                withContext(Dispatchers.Main) {
////                    Log.d("verifyBarcode", "Barcode not found in product.template model")
////                    Log.d("verifyBarcode", "Found barcode in product.packaging model. Product ID: $packagingProductInfo")
////                    // Handle the case where the barcode isn't found; you might show a toast message or alert dialog
////                }
////            }
////        }
////    }
//    private fun verifyBarcode(scannedBarcode: String, pickId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            // Adjusted to use the updated method that returns both product ID and quantity
//            val packagingProductInfo = odooXmlRpcClient.fetchProductIdFromPackagingBarcode(scannedBarcode)
//            val quantity = packagingProductInfo?.second
//            if (productId != null) {
//                val product = pickProductsAdapter.products.find { it.id == productId }
//                product?.let {
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(it.name)
//                    val trackingType = trackingAndExpiration?.first ?: "none"
//
//                    when (trackingType) {
//                        "serial" -> {
//                            Log.d("verifyBarcode", "Tracking Type: Serial")
//                            withContext(Dispatchers.Main) {
//                                promptForSerialNumber(it.name, pickId, it.id)
//                            }
//                        }
//                        "lot" -> {
//                            Log.d("verifyBarcode", "Tracking Type: Lot")
//                            withContext(Dispatchers.Main) {
//                                // Prompt for a lot number for lot-tracked products
//                                promptForLotNumber(it.name, pickId, it.id)
//                            }
//
//                        }
//                        "none" -> {
//                            Log.d("verifyBarcode", "Tracking Type: None")
//                            withContext(Dispatchers.Main) {
//                                promptForProductQuantity(it.name, it.quantity, pickId, it.id, false)                            }
//                        }
//                        else -> {
//                            Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
//                            // Optional: handle any other specific cases
//                        }
//                    }
//                }
//            } else if (packagingProductInfo != null) {
//                // Log the found product ID and quantity from the product.packaging model
//                Log.d("verifyBarcode", "Found barcode in product.packaging model. Product ID: ${packagingProductInfo.first}, Quantity: ${packagingProductInfo.second}")
//            } else {
//                withContext(Dispatchers.Main) {
//                    Log.d("verifyBarcode", "Barcode not found in product.template or product.packaging models")
//                    // Handle the case where the barcode isn't found in either model
//                }
//            }
//        }
//    }
//
////    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int) {
////        val editText = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Enter serial number"
////        }
////
////        val dialogBuilder = AlertDialog.Builder(this)
////            .setTitle("Enter Serial Number")
////            .setMessage("Enter the serial number for $productName.")
////            .setView(editText)
////            .setNegativeButton("Cancel", null)
////
////        val dialog = dialogBuilder.create()
////
////        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
////            val enteredSerialNumber = editText.text.toString().trim()
////            if (enteredSerialNumber.isNotEmpty()) {
////                coroutineScope.launch {
////                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
////                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
////                        // Serial number exists, proceed
////                        val key = ProductPickKey(productId, pickId)
////                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
////
////                        if (!serialList.contains(enteredSerialNumber)) {
////                            serialList.add(enteredSerialNumber)
////                            odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber)
////
////                            updateProductMatchState(productId, pickId, matched = true, serialList)
////                            withContext(Dispatchers.Main) {
////                                showGreenToast("Serial number added for $productName. ${serialList.size} verified")
////                            }
////                        } else {
////                            withContext(Dispatchers.Main) {
////                                showRedToast("Serial number already entered for $productName")
////                            }
////                        }
////                    } else {
////                        // Serial number does not exist, notify and prompt again
////                        withContext(Dispatchers.Main) {
////                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
////                            promptForSerialNumber(productName, pickId, productId) // Prompt again
////                        }
////                    }
////                }
////            } else {
////                // No serial number entered, prompt again
////                showRedToast("Please enter a serial number")
////                promptForSerialNumber(productName, pickId, productId)
////            }
////        }
////
////        dialog.show()
////        // Immediately show the keyboard for input
////        editText.requestFocus()
////        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
////    }
//
//
////        private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int) {
////            // Create the parent layout
////            val container = LinearLayout(this).apply {
////                orientation = LinearLayout.VERTICAL
////                layoutParams = LinearLayout.LayoutParams(
////                    LinearLayout.LayoutParams.MATCH_PARENT,
////                    LinearLayout.LayoutParams.WRAP_CONTENT
////                )
////                setPadding(16, 8, 16, 16)
////            }
////
////// Create the serial number input
////            val serialNumberInput = EditText(this).apply {
////                inputType = InputType.TYPE_CLASS_TEXT
////                hint = "Enter serial number"
////            }
////            container.addView(serialNumberInput)
////
////// Add subheading TextView for the "Store to" input
////            val subheading = TextView(this).apply {
////                text = "Store To Location" // Your subheading text here
////                textSize = 14f // Adjust text size as needed
////                // Optional: styling for the subheading
////                setTypeface(null, Typeface.BOLD)
////                val topMargin = (8 * resources.displayMetrics.density).toInt() // Adjust top margin as needed
////                layoutParams = LinearLayout.LayoutParams(
////                    LinearLayout.LayoutParams.WRAP_CONTENT,
////                    LinearLayout.LayoutParams.WRAP_CONTENT
////                ).apply {
////                    setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
////                }
////            }
////            container.addView(subheading)
////
////// Create the "Store to" input with destLocationName as default value
////            val storeToInput = EditText(this).apply {
////                inputType = InputType.TYPE_CLASS_TEXT
////                hint = "Store to"
////                setText(destLocationName)
////
////                // Convert dp to pixels for setting margins
////                val marginInPixels = (8 * resources.displayMetrics.density).toInt()
////
////                // Create LayoutParams and set margins
////                layoutParams = LinearLayout.LayoutParams(
////                    LinearLayout.LayoutParams.MATCH_PARENT,
////                    LinearLayout.LayoutParams.WRAP_CONTENT
////                ).apply {
////                    setMargins(0, marginInPixels, 0, 0) // Adding top margin
////                }
////            }
////            container.addView(storeToInput)
////
////
////
////            val dialogBuilder = AlertDialog.Builder(this)
////                .setTitle("Enter Serial Number")
////                .setMessage("Enter the serial number for $productName.")
////                .setView(container)
////                .setNegativeButton("Cancel", null)
////
////            val dialog = dialogBuilder.create()
////
////            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
////                val enteredSerialNumber = serialNumberInput.text.toString().trim()
////                val enteredStoreTo = storeToInput.text.toString().trim()
////
////                if (enteredSerialNumber.isNotEmpty()) {
////                    coroutineScope.launch {
////                        val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
////
////                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
////                                                    // Serial number exists, proceed
////                            val key = ProductPickKey(productId, pickId)
////                            val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
////                                serialList.add(enteredSerialNumber)
////                                odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber)
////
////                                updateProductMatchState(productId, pickId, matched = true, serialList)
////                                withContext(Dispatchers.Main) {
////                                    showGreenToast("Serial number added for $productName. ${serialList.size} verified")
////                                }
////                        } else {
////                            // Serial number does not exist, notify and prompt again
////                            withContext(Dispatchers.Main) {
////                                showRedToast("Serial number does not exist. Please enter a valid serial number.")
////                                promptForSerialNumber(productName, pickId, productId) // Re-prompt
////                            }
////                        }
////                    }
////                } else {
////
////                        showRedToast("Please enter a serial number")
////                        promptForSerialNumber(productName, pickId, productId) // Re-prompt
////
////                }
////            }
////
////            dialog.show()
////            // Show keyboard
////            serialNumberInput.requestFocus()
////            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////            imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
////        }
////    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int) {
////        // Create the parent layout
////        val container = LinearLayout(this).apply {
////            orientation = LinearLayout.VERTICAL
////            layoutParams = LinearLayout.LayoutParams(
////                LinearLayout.LayoutParams.MATCH_PARENT,
////                LinearLayout.LayoutParams.WRAP_CONTENT
////            )
////            setPadding(16, 8, 16, 16)
////        }
////
////        // Create the serial number input
////        val serialNumberInput = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Enter serial number"
////        }
////        container.addView(serialNumberInput)
////
////        // Add subheading TextView for the "Store to" input
////        val subheading = TextView(this).apply {
////            text = "Store To Location"
////            textSize = 14f
////            setTypeface(null, Typeface.BOLD)
////            val topMargin = (8 * resources.displayMetrics.density).toInt()
////            layoutParams = LinearLayout.LayoutParams(
////                LinearLayout.LayoutParams.WRAP_CONTENT,
////                LinearLayout.LayoutParams.WRAP_CONTENT
////            ).apply {
////                setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
////            }
////        }
////        container.addView(subheading)
////
////        // Create the "Store to" input with destLocationName as default value
////        val storeToInput = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Store to"
////            setText(destLocationName) // Assuming destLocationName is defined elsewhere in your code
////        }
////        container.addView(storeToInput)
////
////        val dialogBuilder = AlertDialog.Builder(this)
////            .setTitle("Enter Serial Number")
////            .setMessage("Enter the serial number for $productName.")
////            .setView(container)
////            .setNegativeButton("Cancel", null)
////
////        val dialog = dialogBuilder.create()
////
////        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
////            val enteredSerialNumber = serialNumberInput.text.toString().trim()
////            val enteredStoreTo = storeToInput.text.toString().trim()
////
////            if (enteredSerialNumber.isNotEmpty()) {
////                coroutineScope.launch {
////                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
////                        val key = ProductPickKey(productId, pickId)
////                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
////                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
////                        // Serial number exists, proceed
////                        serialList.add(enteredSerialNumber)
////                        odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber, enteredStoreTo) // Updated to include enteredStoreTo
////                            updateProductMatchState(productId, pickId, matched = true, serialList)
////
////                        withContext(Dispatchers.Main) {
////                            showGreenToast("Serial number added for $productName.")
////                        }
////                    } else {
////                        // Serial number does not exist, notify and prompt again
////                        withContext(Dispatchers.Main) {
////                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
////                            promptForSerialNumber(productName, pickId, productId) // Re-prompt
////                        }
////                    }
////                }
////            } else {
////                showRedToast("Please enter a serial number")
////                promptForSerialNumber(productName, pickId, productId) // Re-prompt
////            }
////        }
////
////        dialog.show()
////        // Show keyboard
////        serialNumberInput.requestFocus()
////        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
////    }
//    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int) {
//        // Create the parent layout
//        val container = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//            setPadding(16, 8, 16, 16)
//        }
//
//        // Create the serial number input
//        val serialNumberInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//        container.addView(serialNumberInput)
//
//        // Add subheading TextView for the "Store to" input
//        val subheading = TextView(this).apply {
//            text = "Store To Location"
//            textSize = 14f
//            setTypeface(null, Typeface.BOLD)
//            val topMargin = (8 * resources.displayMetrics.density).toInt()
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
//            }
//        }
//        container.addView(subheading)
//
//        // Create the "Store to" input with destLocationName as default value
////        val storeToInput = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Store to"
////            setText(destLocationName) // Assuming destLocationName is defined elsewhere in your code and remains unchanged
////        }
//
//    val storeToInput = EditText(this).apply {
//        inputType = InputType.TYPE_CLASS_TEXT
//        hint = "Store to"
//        setText(destLocationName) // Set the default text to the EditText
//
//        // Adding a focus change listener instead
//        onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
//            // Clear the text when the EditText gains focus if it contains the default location name
//            if (hasFocus && text.toString() == destLocationName) {
//                setText("")
//            }
//        }
//    }
//    container.addView(storeToInput)
//
//        val dialogBuilder = AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the Serial number for $productName.")
//            .setView(container)
//            .setNegativeButton("Cancel", null)
//
//        val dialog = dialogBuilder.create()
//
//        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//            val enteredSerialNumber = serialNumberInput.text.toString().trim()
//            val enteredStoreTo = storeToInput.text.toString().trim() // This captures the user input but does not change destLocationName
//
//            if (enteredSerialNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
//                    val key = ProductPickKey(productId, pickId)
//                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
//                        // Serial number exists, proceed
//                        serialList.add(enteredSerialNumber)
//                        odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber, enteredStoreTo) // Updated to include enteredStoreTo
//                        updateProductMatchState(productId, pickId, matched = true, serialList)
//
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("Serial number added for $productName.")
//                        }
//                    } else {
//                        // Serial number does not exist, notify and prompt again
//                        withContext(Dispatchers.Main) {
//                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
//                            promptForSerialNumber(productName, pickId, productId) // Re-prompt
//                        }
//                    }
//                }
//            } else {
//                showRedToast("Please enter a serial number")
//                promptForSerialNumber(productName, pickId, productId) // Re-prompt
//            }
//        }
//
//        dialog.show()
//        // Show keyboard
//        serialNumberInput.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
//    }
//
////    private fun promptForProductQuantity(productName: String, expectedQuantity: Double, pickId: Int, productId: Int, recount: Boolean = false) {
////        val editText = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
////            hint = "Enter product quantity"
////        }
////
////        AlertDialog.Builder(this)
////            .setTitle(if (recount) "Recount Required" else "Enter Quantity")
////            .setMessage(if (recount) "Recount for $productName. Enter the exact quantity." else "Enter the exact quantity for $productName.")
////            .setView(editText)
////            .setPositiveButton("OK") { _, _ ->
////                val enteredQuantity = editText.text.toString().toDoubleOrNull()
////                if (enteredQuantity != null && enteredQuantity == expectedQuantity) {
//////                    Toast.makeText(this, "Quantity entered for $productName", Toast.LENGTH_LONG).show()
////                    showGreenToast("Quantity updated for $productName")
////                    updateProductMatchState(productId, pickId, true)
////                } else if (!recount) {
////                    promptForProductQuantity(productName, expectedQuantity, pickId, productId, recount = true)
////                } else {
////                    val localPickName = pickName // Copy the mutable property to a local variable
////
////                    lifecycleScope.launch(Dispatchers.IO) {
////                        if (localPickName != null) { // Use the local copy for the check
////                            val buyerDetails = odooXmlRpcClient.fetchAndLogBuyerDetails(localPickName)
////                            if (buyerDetails != null) {
////                                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, localPickName, productName) // Pass the local copy to the function
////                                withContext(Dispatchers.Main) {
//////                                    Toast.makeText(this@ProductsActivity, "Flagged ${buyerDetails.login}. Email sent.", Toast.LENGTH_LONG).show()
////                                    showRedToast("Flagged")
////                                }
////                            } else {
////                                withContext(Dispatchers.Main) {
//////                                    Toast.makeText(this@ProductsActivity, "Flagged, but buyer details not found.", Toast.LENGTH_LONG).show()
////                                    showRedToast("Flagged, but buyer details not found")
////                                }
////                            }
////                        } else {
////                            withContext(Dispatchers.Main) {
//////                                Toast.makeText(this@ProductsActivity, "Receipt name is null or not found", Toast.LENGTH_LONG).show()
////                                showRedToast("Pick name is null or not found")
////                            }
////                        }
////                    }
////                }
////            }
////            .setNegativeButton("Cancel", null)
////            .show()
////    }
//
//    private fun promptForProductQuantity(
//        productName: String,
//        expectedQuantity: Double,
//        pickId: Int,
//        productId: Int,
//        recount: Boolean = false
//    ) {
//        // Parent layout for EditText inputs
////        val layout = LinearLayout(this).apply {
////            orientation = LinearLayout.VERTICAL
////            layoutParams = LinearLayout.LayoutParams(
////                LinearLayout.LayoutParams.MATCH_PARENT,
////                LinearLayout.LayoutParams.WRAP_CONTENT
////            )
////            setPadding(50, 20, 50, 20) // Adjust padding as necessary
////        }
//        val layout = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//            setPadding(16, 8, 16, 16) // Adjusted padding to match
//        }
//
//        // EditText for entering the product quantity
////        val quantityEditText = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
////            hint = "Enter product quantity"
////        }
////        layout.addView(quantityEditText)
//        val quantityEditText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
//            hint = "Enter product quantity"
//        }
//        layout.addView(quantityEditText)
//
//        val subheading = TextView(this).apply {
//            text = "Store To Location"
//            textSize = 14f
//            setTypeface(null, Typeface.BOLD)
//            val topMargin = (8 * resources.displayMetrics.density).toInt()
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
//            }
//        }
//        layout.addView(subheading)
//
//        // EditText for "Store To" location input
////        val storeToLocationEditText = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Enter store to location"
////            setText(destLocationName) // Pre-populate with the default location name
////        }
//        val storeToLocationEditText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Store to location"
//            setText(destLocationName) // Pre-populate with the default location name
//
//            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
//                // Clear the text when the EditText gains focus if it contains the default location name
//                if (hasFocus && text.toString() == destLocationName) {
//                    setText("")
//                }
//            }
//        }
//        layout.addView(storeToLocationEditText)
//
//        AlertDialog.Builder(this)
//            .setTitle(if (recount) "Recount Required" else "Enter Quantity")
//            .setMessage(if (recount) "Recount for $productName. Enter the exact quantity." else "Enter the exact quantity for $productName.")
//            .setView(layout)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredQuantity = quantityEditText.text.toString().toDoubleOrNull()
//                val enteredLocation = storeToLocationEditText.text.toString()
//
//                if (enteredQuantity != null) {
//                    // Accumulate entered quantity for the product
//                    val totalQuantity = accumulatedQuantities.getOrDefault(productId, 0.0) + enteredQuantity
//                    accumulatedQuantities[productId] = totalQuantity
//
//                    // Check if total entered quantity matches expected quantity
//                    val matched = totalQuantity == expectedQuantity
//                    // Update match state based on total entered quantity
//                    updateProductMatchState(productId, pickId, matched)
//
//                    if (matched) {
//                        showGreenToast("Quantity matched for $productName")
//                    } else {
//                        showRedToast("Total quantity does not match expected. Please enter more.")
//                        Log.d("match state", "Total quantity: $totalQuantity")
//                    }
//
//                    // Create a stock.move.line for the entered quantity, regardless of match state
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        try {
//                            val result = odooXmlRpcClient.createStockMoveLineForUntrackedProduct(pickId, productId, enteredQuantity, enteredLocation)
//                            withContext(Dispatchers.Main) {
//                                if (result) {
//                                    showGreenToast("Stock move line created for $productName at $enteredLocation")
//                                } else {
//                                    showRedToast("Failed to create stock move line for $productName ID: $productId")
//                                }
//                            }
//                        } catch (e: Exception) {
//                            Log.e("OdooXmlRpcClient", "Error creating stock move line for untracked product: ${e.message}", e)
//                        }
//                    }
//                } else if (!recount) {
//                    promptForProductQuantity(productName, expectedQuantity, pickId, productId, recount = true)
//                } else {
//                    // Handle recount failure
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun promptForLotNumber(productName: String, pickId: Int, productId: Int) {
//        // Create the parent layout
//        val container = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//            setPadding(16, 8, 16, 16)
//        }
//
//        // Create the serial number input
//        val lotNumberInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter lot number"
//        }
//        container.addView(lotNumberInput)
//
//        // Add subheading TextView for the "Store to" input
//        val subheading = TextView(this).apply {
//            text = "Store To Location"
//            textSize = 14f
//            setTypeface(null, Typeface.BOLD)
//            val topMargin = (8 * resources.displayMetrics.density).toInt()
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
//            }
//        }
//        container.addView(subheading)
//
//        // Create the "Store to" input with destLocationName as default value
////        val storeToInput = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Store to"
////            setText(destLocationName) // Assuming destLocationName is defined elsewhere in your code and remains unchanged
////        }
//
//        val storeToInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Store to"
//            setText(destLocationName) // Set the default text to the EditText
//
//            // Adding a focus change listener instead
//            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
//                // Clear the text when the EditText gains focus if it contains the default location name
//                if (hasFocus && text.toString() == destLocationName) {
//                    setText("")
//                }
//            }
//        }
//        container.addView(storeToInput)
//
//        val dialogBuilder = AlertDialog.Builder(this)
//            .setTitle("Enter lot Number")
//            .setMessage("Enter the lot number for $productName.")
//            .setView(container)
//            .setNegativeButton("Cancel", null)
//
//        val dialog = dialogBuilder.create()
//
//        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//            val enteredLotNumber = lotNumberInput.text.toString().trim()
//            val enteredStoreTo = storeToInput.text.toString().trim() // This captures the user input but does not change destLocationName
//
//            if (enteredLotNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val lotNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
////                    val key = ProductPickKey(productId, pickId)
////                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                    if (lotNumbers?.contains(enteredLotNumber) == true) {
//                        coroutineScope.launch {
//                            withContext(Dispatchers.Main) {
//                                // Assume promptForLotQuantity is correctly defined elsewhere to handle these parameters
//                                promptForLotQuantity(productName, pickId, productId, enteredLotNumber, enteredStoreTo)
//                            }
//}
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("Serial number added for $productName.")
//                        }
//                    } else {
//                        // Serial number does not exist, notify and prompt again
//                        withContext(Dispatchers.Main) {
//                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
//                            promptForSerialNumber(productName, pickId, productId) // Re-prompt
//                        }
//                    }
//                }
//            } else {
//                showRedToast("Please enter a serial number")
//                promptForSerialNumber(productName, pickId, productId) // Re-prompt
//            }
//        }
//
//        dialog.show()
//        // Show keyboard
//        lotNumberInput.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(lotNumberInput, InputMethodManager.SHOW_IMPLICIT)
//    }
//
//    private fun promptForLotQuantity(productName: String, pickId: Int, productId: Int, lotNumber: String, location: String) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER
//            hint = "Enter quantity"
//        }
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Enter Quantity")
//            .setMessage("Enter the TOTAL quantity for the lot of $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredQuantity = editText.text.toString().toIntOrNull()
//                if (enteredQuantity != null) {
//                    coroutineScope.launch {
//                        // Update the quantity for the lot without requiring expiration date
//                        odooXmlRpcClient.updateMoveLinesWithoutExpirationWithLot(pickId, productId, lotNumber, enteredQuantity, location)
//                        updateProductMatchState(productId, pickId, matched = false, lotQuantity = enteredQuantity)
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("Quantity updated for lot.")
//
//                        }
//                    }
//                } else {
//                        // Show toast message for invalid quantity
//                        showRedToast("Invalid quantity entered.")
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .create()
//
//        // Ensure the keyboard is shown when the EditText gains focus
//        dialog.setOnShowListener {
//            editText.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        dialog.show()
//    }
//
//    private fun sendEmailToBuyer(buyerEmail: String, buyerName: String, pickName: String?, productName: String) {
//        val props = Properties().apply {
//            put("mail.smtp.host", "mail.dattec.co.za")
//            put("mail.smtp.socketFactory.port", "465")
//            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
//            put("mail.smtp.auth", "true")
//            put("mail.smtp.port", "465")
//        }
//
//        val session = Session.getDefaultInstance(props, object : javax.mail.Authenticator() {
//            override fun getPasswordAuthentication(): PasswordAuthentication {
//                return PasswordAuthentication("info@dattec.co.za", "0s3*X4n)#m,z") // Replace with your actual password
//            }
//        })
//
//        try {
//            val message = MimeMessage(session).apply {
//                setFrom(InternetAddress("info@dattec.co.za"))
//                setRecipients(Message.RecipientType.TO, InternetAddress.parse(buyerEmail))
//                subject = "Action Required: Discrepancy in Received Quantity for Receipt $pickName"
//                setText("""
//                Dear $buyerName,
//
//                During a recent receipt event, we identified a discrepancy in the quantities received for the following item:
//
//                - Receipt ID: $pickName
//                - Product: $productName
//
//                The recorded quantity does not match the expected quantity as per our purchase order. This discrepancy requires your immediate attention and action.
//
//                Please review the receipt and product details at your earliest convenience and undertake the necessary steps to rectify this discrepancy. It is crucial to address these issues promptly to maintain accurate inventory records and ensure operational efficiency.
//
//                Thank you for your prompt attention to this matter.
//
//                Best regards,
//                The Swiib Team
//            """.trimIndent())
//            }
//            Transport.send(message)
//            Log.d("EmailSender", "Email sent successfully to $buyerEmail.")
//        } catch (e: MessagingException) {
//            Log.e("EmailSender", "Failed to send email.", e)
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> {
//                onBackPressed() // This will handle the back action
//                return true
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }
//
////    private fun updateProductMatchState(
////        productId: Int,
////        pickId: Int,
////        matched: Boolean = false,
////        serialNumbers: MutableList<String>? = null,
////        lotQuantity: Int? = null
////    ) {
////        val key = ProductPickKey(productId, pickId)
////        val product = pickProductsAdapter.products.find { it.id == productId }
////        val expectedQuantity = product?.quantity?.toInt() ?: 0
////
////        if (serialNumbers != null) {
////            quantityMatches[key] = serialNumbers.size == expectedQuantity
////            // Log to verify match state update
////            Log.d("MatchState", "Product ID: $productId, Verified: ${serialNumbers.size}/$expectedQuantity")
////        } else if (lotQuantity != null) {
////            val currentQuantity = lotQuantities.getOrDefault(key, 0) + lotQuantity
////            lotQuantities[key] = currentQuantity
////            quantityMatches[key] = currentQuantity >= expectedQuantity
////        } else {
////            quantityMatches[key] = matched
////
////        }
////
////        val allProductsMatched = checkAllProductsMatched(pickId)
////        saveMatchStateToPreferences(key, quantityMatches[key] == true)
////
////        val position = pickProductsAdapter.findProductPositionById(productId)
////        if (position != -1) {
////            runOnUiThread { pickProductsAdapter.notifyItemChanged(position) }
////        }
////
////        if (allProductsMatched) {
////            coroutineScope.launch {
////                val validated = odooXmlRpcClient.validateOperation(pickId)
////                withContext(Dispatchers.Main) {
////                    if (validated) {
////                        showGreenToast("Receipt validated for ${pickId}")
////                        val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
////                        startActivity(intent)
////                        finish()
////                    } else {
////                        showRedToast("Failed to validate receipt")
////                    }
////                }
////            }
////        }
////    }
//    private fun updateProductMatchState(
//        productId: Int,
//        pickId: Int,
//        matched: Boolean = false,
//        serialNumbers: MutableList<String>? = null,
//        lotQuantity: Int? = null,
//        enteredQuantity: Double? = null // Use Double for finer quantity control
//    ) {
//        val key = ProductPickKey(productId, pickId)
//        val product = pickProductsAdapter.products.find { it.id == productId }
//        val expectedQuantity = product?.quantity?.toDouble() ?: 0.0 // Convert to Double for consistency
//
//        when {
//            serialNumbers != null -> {
//                // Handling for serial-numbered products
//                quantityMatches[key] = serialNumbers.size.toDouble() == expectedQuantity
//            }
//            lotQuantity != null -> {
//                // Handling for lot-numbered products
//                val currentQuantity = lotQuantities.getOrDefault(key, 0) + lotQuantity
//                lotQuantities[key] = currentQuantity
//                quantityMatches[key] = currentQuantity.toDouble() == expectedQuantity
//            }
//            enteredQuantity != null -> {
//                // Handling for untracked products
//                val totalQuantity = accumulatedQuantities.getOrDefault(productId, 0.0) + enteredQuantity
//                accumulatedQuantities[productId] = totalQuantity
//                quantityMatches[key] = totalQuantity == expectedQuantity
//
//
//            }
//            else -> {
//                quantityMatches[key] = matched
//            }
//        }
//
//        val allProductsMatched = checkAllProductsMatched(pickId)
//        saveMatchStateToPreferences(key, quantityMatches[key] == true)
//
//        val position = pickProductsAdapter.findProductPositionById(productId)
//        if (position != -1) {
//            runOnUiThread { pickProductsAdapter.notifyItemChanged(position) }
//        }
//
//        if (allProductsMatched) {
//            coroutineScope.launch {
//                val validated = odooXmlRpcClient.validateOperation(pickId)
//                withContext(Dispatchers.Main) {
//                    if (validated) {
//                        showGreenToast("Receipt validated for ${pickId}")
//                        val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
//                        startActivity(intent)
//                        finish()
//                    } else {
//                        showRedToast("Failed to validate receipt")
//                    }
//                }
//            }
//        }
//    }
//
//
//
//    private fun checkAllProductsMatched(pickId: Int): Boolean {
//        // Filter the quantityMatches for the current receiptId
//        return quantityMatches.filter { it.key.pickId == pickId }.all { it.value }
//    }
//
//
//    private fun saveMatchStateToPreferences(key: ProductPickKey, matched: Boolean) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        with(sharedPref.edit()) {
//            putBoolean("${key.productId}_${key.pickId}", matched)
//            apply()
//        }
//    }
//
//    private fun loadMatchStatesFromPreferences(pickId: Int) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val tempQuantityMatches = mutableMapOf<ProductPickKey, Boolean>()
//
//        sharedPref.all.forEach { (prefKey, value) ->
//            if (value is Boolean) {
//                val parts = prefKey.split("_")
//                if (parts.size == 2) {
//                    try {
//                        val productId = parts[0].toInt()
//                        val prefPickId = parts[1].toInt()
//                        if (prefPickId == pickId) {
//                            val key = ProductPickKey(productId, prefPickId)
//                            tempQuantityMatches[key] = value
//                        }
//                    } catch (e: NumberFormatException) {
//                        Log.e("PickProductsActivity", "Error parsing shared preference key: $prefKey", e)
//                    }
//                } else {
//                    // This is a better place to log a detailed message about the formatting issue
//                    Log.e("PickProductsActivity", "Incorrectly formatted shared preference key: $prefKey. Expected format: productId_pickId")
//                }
//            }
//
//
//    }
//
//        quantityMatches.clear()
//        quantityMatches.putAll(tempQuantityMatches)
//
//        // Now update the adapter with the loaded match states
//        runOnUiThread {
//            pickProductsAdapter.updateProducts(pickProductsAdapter.products, pickId, quantityMatches)
//        }
//    }
//
//
//    private fun resetMatchStates(pickId: Int) {
//        // Reset all in-memory data structures
//        quantityMatches.keys.filter { it.pickId == pickId }.forEach {
//            quantityMatches[it] = false
//            accumulatedQuantities[it.productId] = 0.0
//            lotQuantities[it] = 0
//        }
//
//        // Clear shared preferences for the pickId
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val editor = sharedPref.edit()
//        sharedPref.all.keys.forEach { key ->
//            if (key.endsWith("_$pickId")) {
//                editor.remove(key)
//            }
//        }
//        editor.apply()
//
//        // Notify the adapter to refresh the UI
//        runOnUiThread {
//            pickProductsAdapter.notifyDataSetChanged()  // This assumes your adapter handles the display based on the quantityMatches map.
//        }
//
//        // Optionally, show a toast message
//        showRedToast("All data reset for pick ID $pickId")
//    }
//
//
//
//
//
//
//
//}
//



package com.example.warehousetet

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class PickProductsActivity : AppCompatActivity() {
    private lateinit var pickProductsAdapter: PickProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button
    private lateinit var constants: Constants

    //    private var productBarcodes = hashMapOf<String, String>()
    private var productSerialNumbers = hashMapOf<ProductPickKey, MutableList<String>>()
    val lotQuantities: MutableMap<ProductPickKey, Double> = mutableMapOf()
    private var quantityMatches = mutableMapOf<ProductPickKey, Boolean>()
    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
    // Assuming this is declared at the class level
    private val accumulatedQuantities: MutableMap<Int, Double> = mutableMapOf()
    private val confirmedLines = mutableSetOf<Int>()
    private var pickId: Int = -1

    private var pickName: String? = null
    private var destLocationName: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pick_activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))

        barcodeInput = findViewById(R.id.pickBarcodeInput)
        confirmButton = findViewById(R.id.pickConfirmButton)

//        val pickId = intent.getIntExtra("PICK_ID", -1)
         pickId = intent.getIntExtra("PICK_ID", -1)
        val pickName = intent.getStringExtra("PICK_NAME") ?: "Pick"
//        val titleTextView: TextView = findViewById(R.id.pickProductsTitleTextView)
//        titleTextView.text = pickName

        val locationName = intent.getStringExtra("LOCATION")
//        val locationTextView: TextView = findViewById(R.id.sourceLocationId)
//        locationTextView.text = locationName

        destLocationName = intent.getStringExtra("DEST_LOCATION")
//        val destLocationTextView: TextView = findViewById(R.id.destinationLocationId)
//        destLocationTextView.text = destLocationName

        supportActionBar?.title = pickName

        pickProductsAdapter = PickProductsAdapter(emptyList(), mapOf(), pickId)
        setupRecyclerView()

        if (pickId != -1) {
            fetchProductsForPick(pickId)
            coroutineScope.launch {
                try {
//                    displayLocationsForPick(pickId)
                } catch (e: Exception) {
                    Log.e("PickProductsActivity", "Error displaying locations for pick: ${e.message}")
                }
            }
        } else {
            Log.e("PickProductsActivity", "Invalid delivery order ID passed to PickProductsActivity.")
        }


        findViewById<Button>(R.id.pickClearButton).setOnClickListener {
            findViewById<EditText>(R.id.pickBarcodeInput).text.clear()
        }

//        findViewById<Button>(R.id.resetButton).setOnClickListener {
//            resetMatchStates(pickId)  // Call the reset function when the button is clicked
//        }


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupBarcodeVerification(pickId)
        restoreButtonVisibility(pickId)
        loadMatchStatesFromPreferences(pickId)
        restoreButtonVisibility(pickId)
    }
    override fun onResume() {
        super.onResume()
        // Restore visibility state whenever the activity resumes
        restoreButtonVisibility(pickId)
    }
    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.pickProductsRecyclerView)
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

    private fun fetchProductsForPick(pickId: Int) {
        coroutineScope.launch {
            try {
                Log.d("PickProductsActivity", "Fetching products for pick ID: $pickId")
                val fetchedLines = odooXmlRpcClient.fetchMoveLinesByPickingId(pickId)
                val updatedMoveLinesWithDetails = mutableListOf<MoveLine>()
                odooXmlRpcClient.fetchResultPackagesByPickingId(pickId)
                fetchedLines.forEach { moveLine ->
                    coroutineScope.launch(Dispatchers.IO) {
                        // Assuming fetchProductTrackingAndExpirationByName can be used for delivery orders as well
                        val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(moveLine.productName) ?: Pair("none", false)
                        val barcode = barcodeToProductIdMap.filterValues { it == moveLine.productId }.keys.firstOrNull()
                        Log.d("PickProductsActivity", "Product: ${moveLine.productName}, Uses Expiration Date: ${trackingAndExpiration.second}")
//                        val barcode = fetchBarcodesForProducts(fetchedLines)

                        // Update move line details to include tracking type, expiration date, and barcode
                        val updatedMoveLine = moveLine.copy(

                            barcode = barcode.toString()
                        )
                        synchronized(updatedMoveLinesWithDetails) {
                            updatedMoveLinesWithDetails.add(updatedMoveLine)
                        }
                    }
                }
                fetchBarcodesForProducts(fetchedLines)

                coroutineScope.launch(Dispatchers.Main) {
                    // Ensure all operations are completed
                    updatedMoveLinesWithDetails.forEach {
                        joinAll()
                    }
                    updateUIForProducts(fetchedLines, pickId)
                    Log.d("PickProductsActivity", "UI updated with detailed move lines for Pick")
                }
            } catch (e: Exception) {
                Log.e("PickProductsActivity", "Error fetching move lines or updating UI: ${e.localizedMessage}", e)
            }
        }
    }

    private fun updateUIForProducts(moveLines: List<MoveLine>, pickId: Int) {
        // Explicitly defining types in the map creation
        val newQuantityMatches: MutableMap<ProductPickKey, Boolean> = moveLines.associateBy({ ProductPickKey(it.id, pickId) }, { moveLine ->
            quantityMatches.getOrDefault(ProductPickKey(moveLine.id, pickId), false)
        }).toMutableMap()

        // Update the quantityMatches and the adapter for RecyclerView
        quantityMatches.clear()
        quantityMatches.putAll(newQuantityMatches)
        pickProductsAdapter.updateProducts(moveLines, pickId, quantityMatches)
        Log.d("PickProductsActivity", "Adapter updated with new products and match states.")
    }

    private fun fetchBarcodesForProducts(moveLine: List<MoveLine>) {
        moveLine.forEach { moveLine ->
            coroutineScope.launch {
                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(moveLine.productName)
                barcode?.let {
                    // Assuming barcodeToProductIdMap should map barcode to product ID
                    synchronized(this@PickProductsActivity) { // Ensure to use your current activity or context correctly
                        barcodeToProductIdMap[barcode] = moveLine.productId
                    }
                }
            }
        }
    }

    private fun setupBarcodeVerification(pickId: Int) {
        confirmButton.setOnClickListener {
            val enteredBarcode = barcodeInput.text.toString().trim()
            verifyBarcode(enteredBarcode, pickId)
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


//    private fun verifyBarcode(scannedBarcode: String, pickId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            // Adjusted to use the updated method that returns both product ID and quantity
//            val packagingProductInfo = odooXmlRpcClient.fetchProductIdFromPackagingBarcode(scannedBarcode)
//            val quantity = packagingProductInfo?.second
//            if (productId != null) {
//                val product = pickProductsAdapter.lines.find { it.productId == productId }
//                product?.let {
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(it.productName)
//                    val trackingType = trackingAndExpiration?.first ?: "none"
//
//                    when (trackingType) {
//                        "serial" -> {
//                            Log.d("verifyBarcode", "Tracking Type: Serial")
//                            withContext(Dispatchers.Main) {
//                                promptForSerialNumber(it.productName, pickId, it.productId)
//                            }
//                        }
//                        "lot" -> {
//                            Log.d("verifyBarcode", "Tracking Type: Lot")
//                            withContext(Dispatchers.Main) {
//                                // Prompt for a lot number for lot-tracked products
//                                promptForLotNumber(it.productName, pickId, it.productId)
//                            }
//                        }
//                        "none" -> {
//                            Log.d("verifyBarcode", "Tracking Type: None")
//                            withContext(Dispatchers.Main) {
//                                displayQuantityDialog(it.productName, it.quantity, pickId, it.productId)
//                            }
//                        }
//                        else -> {
//                            Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
//                            // Optional: handle any other specific cases
//                        }
//                    }
//                }
//            } else if (packagingProductInfo != null) {
//                // Log the found product ID and quantity from the product.packaging model
//                Log.d("verifyBarcode", "Found barcode in product.packaging model. Product ID: ${packagingProductInfo.first}, Quantity: ${packagingProductInfo.second}")
//            } else {
//                withContext(Dispatchers.Main) {
//                    Log.d("verifyBarcode", "Barcode not found in product.template or product.packaging models")
//                    // Handle the case where the barcode isn't found in either model
//                }
//            }
//        }
//    }


    private fun verifyBarcode(scannedBarcode: String, pickId: Int) {
        coroutineScope.launch {
            val productId = barcodeToProductIdMap[scannedBarcode]
            val packagingProductInfo = odooXmlRpcClient.fetchProductIdFromPackagingBarcode(scannedBarcode)

            if (productId != null) {
                val productLines = pickProductsAdapter.lines.filter { it.productId == productId }.sortedBy { it.id }
                val nextProductLine = productLines.find { !confirmedLines.contains(it.id) }

                nextProductLine?.let { productLine ->
                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(productLine.productName)
                    val trackingType = trackingAndExpiration?.first ?: "none"

                    withContext(Dispatchers.Main) {
                        when (trackingType) {
                            "serial" -> {
                                promptForSerialNumber(productLine.productName, pickId, productLine.productId, productLine.locationId, productLine.id)
                            }
                            "lot" -> {
//                                promptForLotNumber(productLine.productName, pickId, productLine.productId)
                                promptConfirmQuantity(productLine.productName, productLine.lotName, productLine.quantity, productLine.locationName, productLine.id, pickId)
                            }
                            "none" -> {
                                displayQuantityDialog(productLine.productName, productLine.quantity, pickId, productLine.id)
//                                confirmedLines.add(productLine.id) // Mark this line as confirmed after processing
                            }
                            else -> Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
                        }
                    }
                }
            } else if (packagingProductInfo != null) {
                Log.d("verifyBarcode", "Found barcode in product.packaging model. Product ID: ${packagingProductInfo.first}, Quantity: ${packagingProductInfo.second}")
            } else {
                withContext(Dispatchers.Main) {
                    Log.d("verifyBarcode", "Barcode not found in product.template or product.packaging models")
                }
            }
        }
    }

    private fun displayQuantityDialog(productName: String, expectedQuantity: Double, pickId: Int, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_confirmation, null)
        val textViewConfirmation = dialogView.findViewById<TextView>(R.id.ConfirmationTextView)
        val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        val fullText = "Confirm the quantity of $expectedQuantity for $productName has been picked."
        val spannableString = SpannableString(fullText)

        // Styling for expectedQuantity
        val quantityStart = fullText.indexOf("$expectedQuantity")
        val quantityEnd = quantityStart + "$expectedQuantity".length
        spannableString.setSpan(StyleSpan(Typeface.BOLD), quantityStart, quantityEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.1f), quantityStart, quantityEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // 10% larger

        // Styling for productName
        val productNameStart = fullText.indexOf("$productName")
        val productNameEnd = productNameStart + "$productName".length
        spannableString.setSpan(StyleSpan(Typeface.BOLD), productNameStart, productNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.1f), productNameStart, productNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // 10% larger

        textViewConfirmation.text = spannableString

        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle("Confirm Picked Quantity")
            setView(dialogView)
            create()
        }.show()

        buttonConfirm.setOnClickListener {
            // Pass the lineId to match state update function
            updateProductMatchState(lineId, pickId)
            confirmedLines.add(lineId)
            alertDialog.dismiss()  // Close the dialog after confirmation
        }

        buttonCancel.setOnClickListener {
            alertDialog.dismiss()  // Close the dialog when cancel is clicked
        }
    }


    private fun promptConfirmQuantity(productName: String, lotName: String, quantity: Double, locationName: String, lineId: Int, pickId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_quantity, null)
        dialogView.findViewById<TextView>(R.id.textProductInfo).text = "Product: $productName"
        dialogView.findViewById<TextView>(R.id.textQuantityInfo).text = "Quantity: $quantity"
        dialogView.findViewById<TextView>(R.id.textLotInfo).text = "LOT: $lotName"
        dialogView.findViewById<TextView>(R.id.textSourceLocationInfo).text = "Pick From: $locationName"

        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Pick")
            .setView(dialogView)
            .create()

        confirmButton.setOnClickListener {
            confirmLotNumber(productName, quantity, lotName, lineId, pickId)
            showGreenToast("Quantity confirmed for $productName.")
            alertDialog.dismiss()  // Close the dialog after confirmation
        }
        buttonCancel.setOnClickListener {
            alertDialog.dismiss()  // Close the dialog when cancel is clicked
        }

        alertDialog.show()
    }

    private fun confirmLotNumber(productName: String, quantity: Double, lotName: String, lineId: Int, pickId: Int) {
        // Inflate the custom layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_entry, null)
        val textProductInfo: TextView = dialogView.findViewById(R.id.textProductInfo)
        val textLotInfo: TextView = dialogView.findViewById(R.id.textLotInfo)
        val editText: EditText = dialogView.findViewById(R.id.editLotNumber)
        val textQuantityInfo: TextView = dialogView.findViewById(R.id.textQuantityInfo)
        val confirmButton: Button = dialogView.findViewById(R.id.confirmButton)
        val buttonCancel: Button = dialogView.findViewById(R.id.buttonCancel)

        // Set the texts for TextViews
        textProductInfo.text = "Product: $productName"
        textQuantityInfo.text = "Quantity: $quantity"
        textLotInfo.text = "LOT: $lotName"

        // Configure and show AlertDialog
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle("Confirm LOT Number")
            setView(dialogView)
            create()
        }.show()

        // Set up the confirm button action
        confirmButton.setOnClickListener {
            val enteredLotNumber = editText.text.toString()
            if (enteredLotNumber == lotName) {
                showGreenToast("Correct LOT number confirmed for $productName.")
                updateProductMatchState(lineId, pickId)
                confirmedLines.add(lineId)
                alertDialog.dismiss()  // Close the dialog when the correct number is confirmed
            } else {
                showRedToast("Incorrect LOT number entered.")
            }
        }
        buttonCancel.setOnClickListener {
            alertDialog.dismiss()  // Close the dialog when cancel is clicked
        }

        // Automatically show keyboard when dialog appears
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

//    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int, locationId: Int, lineId: Int) {
//        // Create the parent layout
//        val container = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//            setPadding(16, 8, 16, 16)
//        }
//
//        // Create the serial number input
//        val serialNumberInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//        container.addView(serialNumberInput)
//
//        // Add subheading TextView for the "Store to" input
//        val subheading = TextView(this).apply {
//            text = "Store To Location"
//            textSize = 14f
//            setTypeface(null, Typeface.BOLD)
//            val topMargin = (8 * resources.displayMetrics.density).toInt()
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
//            }
//        }
//        container.addView(subheading)
//
//        val storeToInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Store to"
//            setText(destLocationName) // Set the default text to the EditText
//
//            // Adding a focus change listener instead
//            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
//                // Clear the text when the EditText gains focus if it contains the default location name
//                if (hasFocus && text.toString() == destLocationName) {
//                    setText("")
//                }
//            }
//        }
//        container.addView(storeToInput)
//
//        val dialogBuilder = AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the Serial number for $productName.")
//            .setView(container)
//            .setNegativeButton("Cancel", null)
//
//        val dialog = dialogBuilder.create()
//
//        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//            val enteredSerialNumber = serialNumberInput.text.toString().trim()
//            val destLocation = storeToInput.text.toString().trim() // This captures the user input but does not change destLocationName
//
//            if (enteredSerialNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
//                    val key = ProductPickKey(productId, pickId)
//                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
//                        // Serial number exists, proceed
//                        serialList.add(enteredSerialNumber)
//                        odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber, destLocation, locationId) // Updated to include enteredStoreTo
////                        updateProductMatchState(productId, pickId, matched = true, serialList)
//
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("Serial number added for $productName.")
//                        }
//                    } else {
//                        // Serial number does not exist, notify and prompt again
//                        withContext(Dispatchers.Main) {
//                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
//                            promptForSerialNumber(productName, pickId, productId, locationId, lineId) // Re-prompt
//                        }
//                    }
//                }
//            } else {
//                showRedToast("Please enter a serial number")
//                promptForSerialNumber(productName, pickId, productId, locationId, lineId) // Re-prompt
//            }
//        }
//
//        dialog.show()
//        // Show keyboard
//        serialNumberInput.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
//    }

    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int, locationId: Int, lineId: Int) {
        // Inflate the layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
        val storeToInput = dialogView.findViewById<EditText>(R.id.storeToInput)
        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)

        // Set default text for the 'Store To' input
        storeToInput.setText(destLocationName)
        storeToInput.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && storeToInput.text.toString() == destLocationName) {
                storeToInput.setText("")
            }
        }

        // Configure the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter Serial Number for $productName")
            .setView(dialogView)
            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
            .create()

        // Set up the confirm button action
        buttonConfirmSN.setOnClickListener {
            val enteredSerialNumber = serialNumberInput.text.toString().trim()
            val destLocation = storeToInput.text.toString().trim()

            if (enteredSerialNumber.isNotEmpty()) {
                coroutineScope.launch {
                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
                    val serialList = productSerialNumbers.getOrPut(ProductPickKey(productId, pickId)) { mutableListOf() }

                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
                        serialList.add(enteredSerialNumber)
                        odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber, destLocation, locationId)
                        withContext(Dispatchers.Main) {
                            updateProductMatchState(lineId, pickId)
                            confirmedLines.add(lineId)
                            showGreenToast("Serial number added for $productName.")
                            dialog.dismiss()
                            fetchProductsForPick(pickId)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
                            promptForSerialNumber(productName, pickId, productId, locationId, lineId) // Re-prompt
                        }
                    }
                }
            } else {
                showRedToast("Please enter a serial number.")
                promptForSerialNumber(productName, pickId, productId, locationId, lineId) // Re-prompt
            }
        }

        // Set up the cancel button action
        buttonCancelSN.setOnClickListener {
            dialog.dismiss()  // Dismiss the dialog
        }

        dialog.show()

        // Request focus and show keyboard for the serial number input
        serialNumberInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
    }




//    private fun promptForProductQuantity(
//        productName: String,
//        expectedQuantity: Double,
//        pickId: Int,
//        productId: Int,
//        recount: Boolean = false
//    ) {
//        val layout = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//            setPadding(16, 8, 16, 16) // Standard padding
//        }
//
//        val quantityEditText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
//            hint = "Enter product quantity"
//        }
//        layout.addView(quantityEditText)
//
//        val subheading = TextView(this).apply {
//            text = "Store To Location"
//            textSize = 14f
//            setTypeface(null, Typeface.BOLD)
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                setMargins(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
//            }
//        }
//        layout.addView(subheading)
//
//        val storeToLocationEditText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Store to location"
//            setText(destLocationName)
//        }
//        layout.addView(storeToLocationEditText)
//
//        AlertDialog.Builder(this)
//            .setTitle(if (recount) "Recount Required" else "Enter Quantity")
//            .setMessage("Enter the exact quantity for $productName.")
//            .setView(layout)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredQuantity = quantityEditText.text.toString().toDoubleOrNull()
//                val enteredLocation = storeToLocationEditText.text.toString()
//
//                if (enteredQuantity != null) {
//                    coroutineScope.launch(Dispatchers.IO) {
//                        // Update the database and state
//                        val successfulUpdate = odooXmlRpcClient.createStockMoveLineForUntrackedProduct(pickId, productId, enteredQuantity, enteredLocation)
//                        withContext(Dispatchers.Main) {
//                            if (successfulUpdate) {
////                                updateProductMatchState(productId, pickId, enteredQuantity == expectedQuantity)
//                                fetchProductsForPick(pickId) // Refetch and update UI after state change
//                            } else {
//                                showRedToast("Failed to update product data.")
//                            }
//                        }
//                    }
//                } else if (!recount) {
//                    promptForProductQuantity(productName, expectedQuantity, pickId, productId, true)
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }

//    private fun promptForLotNumber(productName: String, pickId: Int, productId: Int) {
//        // Create the parent layout
//        val container = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//            setPadding(16, 8, 16, 16)
//        }
//
//        // Create the serial number input
//        val lotNumberInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter lot number"
//        }
//        container.addView(lotNumberInput)
//
//        // Add subheading TextView for the "Store to" input
//        val subheading = TextView(this).apply {
//            text = "Store To Location"
//            textSize = 14f
//            setTypeface(null, Typeface.BOLD)
//            val topMargin = (8 * resources.displayMetrics.density).toInt()
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
//            }
//        }
//        container.addView(subheading)
//
//        val storeToInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Store to"
//            setText(destLocationName) // Set the default text to the EditText
//
//            // Adding a focus change listener instead
//            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
//                // Clear the text when the EditText gains focus if it contains the default location name
//                if (hasFocus && text.toString() == destLocationName) {
//                    setText("")
//                }
//            }
//        }
//        container.addView(storeToInput)
//
//        val dialogBuilder = AlertDialog.Builder(this)
//            .setTitle("Enter lot Number")
//            .setMessage("Enter the lot number for $productName.")
//            .setView(container)
//            .setNegativeButton("Cancel", null)
//
//        val dialog = dialogBuilder.create()
//
//        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//            val enteredLotNumber = lotNumberInput.text.toString().trim()
//            val enteredStoreTo = storeToInput.text.toString().trim() // This captures the user input but does not change destLocationName
//
//            if (enteredLotNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val lotNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
////                    val key = ProductPickKey(productId, pickId)
////                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                    if (lotNumbers?.contains(enteredLotNumber) == true) {
//                        coroutineScope.launch {
//                            withContext(Dispatchers.Main) {
//                                // Assume promptForLotQuantity is correctly defined elsewhere to handle these parameters
//                                promptForLotQuantity(productName, pickId, productId, enteredLotNumber, enteredStoreTo)
//                            }
//                        }
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("lot number added for $productName.")
//                        }
//                    } else {
//                        // Serial number does not exist, notify and prompt again
//                        withContext(Dispatchers.Main) {
//                            showRedToast("Serial number does not exist. Please enter a valid lot number.")
//                            promptForLotNumber(productName, pickId, productId) // Re-prompt
//                        }
//                    }
//                }
//            } else {
//                showRedToast("Please enter a lot number")
//                promptForLotNumber(productName, pickId, productId) // Re-prompt
//            }
//        }
//
//        dialog.show()
//        // Show keyboard
//        lotNumberInput.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(lotNumberInput, InputMethodManager.SHOW_IMPLICIT)
//    }


//    private fun promptForLotQuantity(productName: String, pickId: Int, productId: Int, lotNumber: String, location: String) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER
//            hint = "Enter quantity for lot $lotNumber"
//        }
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Enter Lot Quantity")
//            .setMessage("Enter the quantity for the lot $lotNumber of $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredQuantity = editText.text.toString().toIntOrNull()
//                if (enteredQuantity != null) {
//                    coroutineScope.launch {
//                        odooXmlRpcClient.updateMoveLinesWithoutExpirationWithLot(pickId, productId, lotNumber, enteredQuantity, location)
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("Quantity $enteredQuantity added for lot $lotNumber of $productName.")
//                            // Assume we accumulate lot quantities in a different function
//                        }
//                    }
//                } else {
//                    showRedToast("Invalid quantity entered.")
//                }
//            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .create()
//
//        dialog.setOnShowListener {
//            editText.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        dialog.show()
//    }



    private fun sendEmailToBuyer(buyerEmail: String, buyerName: String, pickName: String?, productName: String) {
        val props = Properties().apply {
            put("mail.smtp.host", "mail.dattec.co.za")
            put("mail.smtp.socketFactory.port", "465")
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.auth", "true")
            put("mail.smtp.port", "465")
        }

        val session = Session.getDefaultInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication("info@dattec.co.za", "0s3*X4n)#m,z") // Replace with your actual password
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress("info@dattec.co.za"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(buyerEmail))
                subject = "Action Required: Discrepancy in Received Quantity for Receipt $pickName"
                setText("""
                Dear $buyerName,

                During a recent receipt event, we identified a discrepancy in the quantities received for the following item:

                - Receipt ID: $pickName
                - Product: $productName

                The recorded quantity does not match the expected quantity as per our purchase order. This discrepancy requires your immediate attention and action.

                Please review the receipt and product details at your earliest convenience and undertake the necessary steps to rectify this discrepancy. It is crucial to address these issues promptly to maintain accurate inventory records and ensure operational efficiency.

                Thank you for your prompt attention to this matter.

                Best regards,
                The Swiib Team
            """.trimIndent())
            }
            Transport.send(message)
            Log.d("EmailSender", "Email sent successfully to $buyerEmail.")
        } catch (e: MessagingException) {
            Log.e("EmailSender", "Failed to send email.", e)
        }
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
//        lineId: Int,
//        pickId: Int,
//        matched: Boolean = true,
//        serialNumbers: MutableList<String>? = null,
//    ) {
//        val key = ProductPickKey(lineId, pickId)
//        val productLine = pickProductsAdapter.lines.find { it.id == lineId }
//
//        if (productLine != null) {
//            quantityMatches[key] = matched
//
//            // Refresh the UI to reflect the updated state
//            runOnUiThread {
//                val position = pickProductsAdapter.findProductPositionById(lineId)
//                if (position != -1) {
//                    pickProductsAdapter.notifyItemChanged(position)
//                }
//            }
//        } else {
//            Log.e("updateProductMatchState", "No line found for ID $lineId")
//        }
//
//        saveMatchStateToPreferences(key, quantityMatches[key] == true)
//    }
    private fun updateProductMatchState(
        lineId: Int,
        pickId: Int,
        matched: Boolean = true,
        serialNumbers: MutableList<String>? = null,
    ) {
        val key = ProductPickKey(lineId, pickId)
        val productLine = pickProductsAdapter.lines.find { it.id == lineId }

        if (productLine != null) {
            quantityMatches[key] = matched

            // Refresh the UI to reflect the updated state
            runOnUiThread {
                val position = pickProductsAdapter.findProductPositionById(lineId)
                if (position != -1) {
                    pickProductsAdapter.notifyItemChanged(position)
                }
                checkAndToggleValidateButton(pickId)
            }
        } else {
            Log.e("updateProductMatchState", "No line found for ID $lineId")
        }

        saveMatchStateToPreferences(key, quantityMatches[key] == true)
    }


    private fun checkAndToggleValidateButton(pickId: Int) {
        val allMatched = quantityMatches.filterKeys { it.pickId == pickId }.all { it.value }
        val validateButton = findViewById<Button>(R.id.pickValidateButton)

        validateButton.visibility = if (allMatched) View.VISIBLE else View.GONE
        saveButtonVisibilityState(pickId, allMatched)

        if (allMatched) {
            validateButton.setOnClickListener {
                coroutineScope.launch {
                    val validationSuccessful = odooXmlRpcClient.validateOperation(pickId)
                    runOnUiThread {
                        if (validationSuccessful) {
                            Toast.makeText(applicationContext, "Picking validated successfully.", Toast.LENGTH_SHORT).show()
                            // Redirect to PickActivity upon successful validation
                            val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
                            startActivity(intent)
                            finish()  // Optionally call finish() if you want to remove this activity from the back stack
                        } else {
                            Toast.makeText(applicationContext, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
//    private fun checkAndToggleValidateButton(pickId: Int) {
//        val allMatched = quantityMatches.filterKeys { it.pickId == pickId }.all { it.value }
//        val validateButton = findViewById<Button>(R.id.pickValidateButton)
//
//        validateButton.visibility = if (allMatched) View.VISIBLE else View.GONE
//        saveButtonVisibilityState(pickId, allMatched)
//
//        if (allMatched) {
//            validateButton.setOnClickListener {
//                coroutineScope.launch {
//                    odooXmlRpcClient.validateOperation(pickId)  // Call validateOperation but ignore its return value
//                    runOnUiThread {
//                        Toast.makeText(applicationContext, "Picking validated successfully.", Toast.LENGTH_SHORT).show()
//                        val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
//                        startActivity(intent)
//                        finish()  // Remove this activity from the back stack
//                    }
//                }
//            }
//        }
//    }


    private fun saveButtonVisibilityState(pickId: Int, visible: Boolean) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("ValidateButtonVisible_$pickId", visible)
            apply()
        }
    }

    private fun restoreButtonVisibility(pickId: Int) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        val isVisible = sharedPref.getBoolean("ValidateButtonVisible_$pickId", false)
        val validateButton = findViewById<Button>(R.id.pickValidateButton)
        validateButton.visibility = if (isVisible) View.VISIBLE else View.GONE

        if (isVisible) {
            setupValidateButtonListener(pickId)
        }
    }

    private fun setupValidateButtonListener(pickId: Int) {
        val validateButton = findViewById<Button>(R.id.pickValidateButton)
        validateButton.setOnClickListener {
            coroutineScope.launch {
                val validationSuccessful = odooXmlRpcClient.validateOperation(pickId)
                runOnUiThread {
                    if (validationSuccessful) {
                        Toast.makeText(applicationContext, "Picking validated successfully.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
                        startActivity(intent)
                        finish()  // Optionally call finish() if you want to remove this activity from the back stack
                    } else {
                        Toast.makeText(applicationContext, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkAllProductsMatched(pickId: Int): Boolean {
        // Filter the quantityMatches for the current receiptId
        return quantityMatches.filter { it.key.pickId == pickId }.all { it.value }
    }

    private fun saveMatchStateToPreferences(key: ProductPickKey, matched: Boolean) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("${key.moveLineId}_${key.pickId}", matched)
            apply()
        }
    }

//    private fun loadMatchStatesFromPreferences(pickId: Int) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val tempQuantityMatches = mutableMapOf<ProductPickKey, Boolean>()
//
//        sharedPref.all.forEach { (prefKey, value) ->
//            if (value is Boolean) {
//                val parts = prefKey.split("_")
//                if (parts.size == 2) {
//                    try {
//                        val productId = parts[0].toInt()
//                        val prefPickId = parts[1].toInt()
//                        if (prefPickId == pickId) {
//                            val key = ProductPickKey(productId, prefPickId)
//                            tempQuantityMatches[key] = value
//                        }
//                    } catch (e: NumberFormatException) {
//                        Log.e("PickProductsActivity", "Error parsing shared preference key: $prefKey", e)
//                    }
//                } else {
//                    // This is a better place to log a detailed message about the formatting issue
//                    Log.e("PickProductsActivity", "Incorrectly formatted shared preference key: $prefKey. Expected format: productId_pickId")
//                }
//            }
//
//        }
//
//        quantityMatches.clear()
//        quantityMatches.putAll(tempQuantityMatches)
//
//        // Now update the adapter with the loaded match states
//        runOnUiThread {
//            pickProductsAdapter.updateProducts(pickProductsAdapter.lines, pickId, quantityMatches)
//        }
//    }
    private fun loadMatchStatesFromPreferences(pickId: Int) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        val tempQuantityMatches = mutableMapOf<ProductPickKey, Boolean>()

        sharedPref.all.forEach { (prefKey, value) ->
            if (value is Boolean) {
                if (prefKey.startsWith("ValidateButtonVisible")) {
                    // Correct handling of the ValidateButtonVisible key
                    val parts = prefKey.split("_")
                    if (parts.size == 2) {
                        val storedPickId = parts[1].toIntOrNull()
                        if (storedPickId == pickId) {
                            findViewById<Button>(R.id.pickValidateButton).visibility = if (value) View.VISIBLE else View.GONE
                        }
                    }
                } else {
                    // Handling standard product match keys
                    val parts = prefKey.split("_")
                    if (parts.size == 2) {
                        try {
                            val moveLineId = parts[0].toInt()
                            val prefPickId = parts[1].toInt()
                            if (prefPickId == pickId) {
                                val key = ProductPickKey(moveLineId, prefPickId)
                                tempQuantityMatches[key] = value
                            }
                        } catch (e: NumberFormatException) {
                            Log.e("PickProductsActivity", "Error parsing shared preference key: $prefKey", e)
                        }
                    } else {
                        Log.e("PickProductsActivity", "Incorrectly formatted shared preference key: $prefKey. Expected format: productId_pickId")
                    }
                }
            }
        }

        quantityMatches.clear()
        quantityMatches.putAll(tempQuantityMatches)

        // Now update the adapter with the loaded match states
        runOnUiThread {
            pickProductsAdapter.updateProducts(pickProductsAdapter.lines, pickId, quantityMatches)
        }
    }

    private fun resetMatchStates(pickId: Int) {
        // Reset all in-memory data structures
        quantityMatches.keys.filter { it.pickId == pickId }.forEach {
            quantityMatches[it] = false
            accumulatedQuantities[it.moveLineId] = 0.0
            lotQuantities[it] = 0.0
        }

        // Clear shared preferences for the pickId
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        sharedPref.all.keys.forEach { key ->
            if (key.endsWith("_$pickId")) {
                editor.remove(key)
            }
        }
        editor.apply()

        // Notify the adapter to refresh the UI
        runOnUiThread {
            pickProductsAdapter.notifyDataSetChanged()  // This assumes your adapter handles the display based on the quantityMatches map.
        }

        // Optionally, show a toast message
        showRedToast("All data reset for pick ID $pickId")
    }


}

