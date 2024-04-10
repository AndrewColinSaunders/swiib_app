package com.example.warehousetet

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val lotQuantities: MutableMap<ProductPickKey, Int> = mutableMapOf()
    private var quantityMatches = mutableMapOf<ProductPickKey, Boolean>()
    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
    // Assuming this is declared at the class level
    private val accumulatedQuantities: MutableMap<Int, Double> = mutableMapOf()


    private var pickName: String? = null
    private var destLocationName: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pick_activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))

        barcodeInput = findViewById(R.id.pickBarcodeInput)
        confirmButton = findViewById(R.id.pickConfirmButton)

        val pickId = intent.getIntExtra("PICK_ID", -1)
        val pickName = intent.getStringExtra("PICK_NAME") ?: "Pick"
//        val titleTextView: TextView = findViewById(R.id.pickProductsTitleTextView)
//        titleTextView.text = pickName

        val locationName = intent.getStringExtra("LOCATION")
        val locationTextView: TextView = findViewById(R.id.sourceLocationId)
        locationTextView.text = locationName

        destLocationName = intent.getStringExtra("DEST_LOCATION")
        val destLocationTextView: TextView = findViewById(R.id.destinationLocationId)
        destLocationTextView.text = destLocationName

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

//        findViewById<Button>(R.id.doConfirmButton).setOnClickListener {
//            findViewById<EditText>(R.id.barcodeInput).text.clear()
//        }
        findViewById<Button>(R.id.pickClearButton).setOnClickListener {
            findViewById<EditText>(R.id.pickBarcodeInput).text.clear()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupBarcodeVerification(pickId)
        loadMatchStatesFromPreferences(pickId)
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
            Log.d("PickProductsActivity", "Fetching products for delivery order ID: $pickId")

            // Attempt to fetch products associated with the delivery order
            val fetchedProducts = try {
                odooXmlRpcClient.fetchProductsForReceipt(pickId)
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
                updateUIForProducts(updatedProductsWithDetails, pickId)
            }
        }
    }
    private fun updateUIForProducts(products: List<Product>, deliveryOrderId: Int) {
        // Assuming barcodeToProductIdMap and productSerialNumbers are already populated or handled within the fetched products loop
        val newQuantityMatches = products.associate {
            ProductPickKey(it.id, deliveryOrderId) to (quantityMatches[ProductPickKey(it.id, deliveryOrderId)] ?: false)
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

//    private fun verifyBarcode(scannedBarcode: String, deliveryOrderId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            val packagingProductInfo = odooXmlRpcClient.fetchProductIdFromPackagingBarcode(scannedBarcode)
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
//                                promptForSerialNumber(it.name, deliveryOrderId, it.id)
//                            }
//                        }
//                        "lot" -> {
//                            Log.d("verifyBarcode", "Tracking Type: Lot")
//                            withContext(Dispatchers.Main) {
//                                // Implementation for lot-tracked products; likely prompting for lot number
//                            }
//                        }
//                        "none" -> {
//                            Log.d("verifyBarcode", "Tracking Type: None")
//                            withContext(Dispatchers.Main) {
//                                // Handle non-serialized, non-lot-tracked products
//                            }
//                        }
//                        else -> {
//                            Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
//                            withContext(Dispatchers.Main) {
//                                // Optional: handle any other specific cases
//                            }
//                        }
//                    }
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    Log.d("verifyBarcode", "Barcode not found in product.template model")
//                    Log.d("verifyBarcode", "Found barcode in product.packaging model. Product ID: $packagingProductInfo")
//                    // Handle the case where the barcode isn't found; you might show a toast message or alert dialog
//                }
//            }
//        }
//    }
    private fun verifyBarcode(scannedBarcode: String, pickId: Int) {
        coroutineScope.launch {
            val productId = barcodeToProductIdMap[scannedBarcode]
            // Adjusted to use the updated method that returns both product ID and quantity
            val packagingProductInfo = odooXmlRpcClient.fetchProductIdFromPackagingBarcode(scannedBarcode)
            val quantity = packagingProductInfo?.second
            if (productId != null) {
                val product = pickProductsAdapter.products.find { it.id == productId }
                product?.let {
                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(it.name)
                    val trackingType = trackingAndExpiration?.first ?: "none"

                    when (trackingType) {
                        "serial" -> {
                            Log.d("verifyBarcode", "Tracking Type: Serial")
                            withContext(Dispatchers.Main) {
                                promptForSerialNumber(it.name, pickId, it.id)
                            }
                        }
                        "lot" -> {
                            Log.d("verifyBarcode", "Tracking Type: Lot")
                            withContext(Dispatchers.Main) {
                                // Prompt for a lot number for lot-tracked products
                                promptForLotNumber(it.name, pickId, it.id)
                            }

                        }
                        "none" -> {
                            Log.d("verifyBarcode", "Tracking Type: None")
                            withContext(Dispatchers.Main) {
                                promptForProductQuantity(it.name, it.quantity, pickId, it.id, false)                            }
                        }
                        else -> {
                            Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
                            // Optional: handle any other specific cases
                        }
                    }
                }
            } else if (packagingProductInfo != null) {
                // Log the found product ID and quantity from the product.packaging model
                Log.d("verifyBarcode", "Found barcode in product.packaging model. Product ID: ${packagingProductInfo.first}, Quantity: ${packagingProductInfo.second}")
            } else {
                withContext(Dispatchers.Main) {
                    Log.d("verifyBarcode", "Barcode not found in product.template or product.packaging models")
                    // Handle the case where the barcode isn't found in either model
                }
            }
        }
    }

//    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//
//        val dialogBuilder = AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the serial number for $productName.")
//            .setView(editText)
//            .setNegativeButton("Cancel", null)
//
//        val dialog = dialogBuilder.create()
//
//        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//            val enteredSerialNumber = editText.text.toString().trim()
//            if (enteredSerialNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
//                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
//                        // Serial number exists, proceed
//                        val key = ProductPickKey(productId, pickId)
//                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//
//                        if (!serialList.contains(enteredSerialNumber)) {
//                            serialList.add(enteredSerialNumber)
//                            odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber)
//
//                            updateProductMatchState(productId, pickId, matched = true, serialList)
//                            withContext(Dispatchers.Main) {
//                                showGreenToast("Serial number added for $productName. ${serialList.size} verified")
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                showRedToast("Serial number already entered for $productName")
//                            }
//                        }
//                    } else {
//                        // Serial number does not exist, notify and prompt again
//                        withContext(Dispatchers.Main) {
//                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
//                            promptForSerialNumber(productName, pickId, productId) // Prompt again
//                        }
//                    }
//                }
//            } else {
//                // No serial number entered, prompt again
//                showRedToast("Please enter a serial number")
//                promptForSerialNumber(productName, pickId, productId)
//            }
//        }
//
//        dialog.show()
//        // Immediately show the keyboard for input
//        editText.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//    }


//        private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int) {
//            // Create the parent layout
//            val container = LinearLayout(this).apply {
//                orientation = LinearLayout.VERTICAL
//                layoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//                )
//                setPadding(16, 8, 16, 16)
//            }
//
//// Create the serial number input
//            val serialNumberInput = EditText(this).apply {
//                inputType = InputType.TYPE_CLASS_TEXT
//                hint = "Enter serial number"
//            }
//            container.addView(serialNumberInput)
//
//// Add subheading TextView for the "Store to" input
//            val subheading = TextView(this).apply {
//                text = "Store To Location" // Your subheading text here
//                textSize = 14f // Adjust text size as needed
//                // Optional: styling for the subheading
//                setTypeface(null, Typeface.BOLD)
//                val topMargin = (8 * resources.displayMetrics.density).toInt() // Adjust top margin as needed
//                layoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//                ).apply {
//                    setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
//                }
//            }
//            container.addView(subheading)
//
//// Create the "Store to" input with destLocationName as default value
//            val storeToInput = EditText(this).apply {
//                inputType = InputType.TYPE_CLASS_TEXT
//                hint = "Store to"
//                setText(destLocationName)
//
//                // Convert dp to pixels for setting margins
//                val marginInPixels = (8 * resources.displayMetrics.density).toInt()
//
//                // Create LayoutParams and set margins
//                layoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//                ).apply {
//                    setMargins(0, marginInPixels, 0, 0) // Adding top margin
//                }
//            }
//            container.addView(storeToInput)
//
//
//
//            val dialogBuilder = AlertDialog.Builder(this)
//                .setTitle("Enter Serial Number")
//                .setMessage("Enter the serial number for $productName.")
//                .setView(container)
//                .setNegativeButton("Cancel", null)
//
//            val dialog = dialogBuilder.create()
//
//            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//                val enteredSerialNumber = serialNumberInput.text.toString().trim()
//                val enteredStoreTo = storeToInput.text.toString().trim()
//
//                if (enteredSerialNumber.isNotEmpty()) {
//                    coroutineScope.launch {
//                        val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
//
//                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
//                                                    // Serial number exists, proceed
//                            val key = ProductPickKey(productId, pickId)
//                            val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                                serialList.add(enteredSerialNumber)
//                                odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber)
//
//                                updateProductMatchState(productId, pickId, matched = true, serialList)
//                                withContext(Dispatchers.Main) {
//                                    showGreenToast("Serial number added for $productName. ${serialList.size} verified")
//                                }
//                        } else {
//                            // Serial number does not exist, notify and prompt again
//                            withContext(Dispatchers.Main) {
//                                showRedToast("Serial number does not exist. Please enter a valid serial number.")
//                                promptForSerialNumber(productName, pickId, productId) // Re-prompt
//                            }
//                        }
//                    }
//                } else {
//
//                        showRedToast("Please enter a serial number")
//                        promptForSerialNumber(productName, pickId, productId) // Re-prompt
//
//                }
//            }
//
//            dialog.show()
//            // Show keyboard
//            serialNumberInput.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
//        }
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
//        val storeToInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Store to"
//            setText(destLocationName) // Assuming destLocationName is defined elsewhere in your code
//        }
//        container.addView(storeToInput)
//
//        val dialogBuilder = AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the serial number for $productName.")
//            .setView(container)
//            .setNegativeButton("Cancel", null)
//
//        val dialog = dialogBuilder.create()
//
//        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//            val enteredSerialNumber = serialNumberInput.text.toString().trim()
//            val enteredStoreTo = storeToInput.text.toString().trim()
//
//            if (enteredSerialNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
//                        val key = ProductPickKey(productId, pickId)
//                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
//                        // Serial number exists, proceed
//                        serialList.add(enteredSerialNumber)
//                        odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber, enteredStoreTo) // Updated to include enteredStoreTo
//                            updateProductMatchState(productId, pickId, matched = true, serialList)
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
    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int) {
        // Create the parent layout
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 8, 16, 16)
        }

        // Create the serial number input
        val serialNumberInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter serial number"
        }
        container.addView(serialNumberInput)

        // Add subheading TextView for the "Store to" input
        val subheading = TextView(this).apply {
            text = "Store To Location"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            val topMargin = (8 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
            }
        }
        container.addView(subheading)

        // Create the "Store to" input with destLocationName as default value
//        val storeToInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Store to"
//            setText(destLocationName) // Assuming destLocationName is defined elsewhere in your code and remains unchanged
//        }

    val storeToInput = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_TEXT
        hint = "Store to"
        setText(destLocationName) // Set the default text to the EditText

        // Adding a focus change listener instead
        onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            // Clear the text when the EditText gains focus if it contains the default location name
            if (hasFocus && text.toString() == destLocationName) {
                setText("")
            }
        }
    }
    container.addView(storeToInput)

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Enter Serial Number")
            .setMessage("Enter the Serial number for $productName.")
            .setView(container)
            .setNegativeButton("Cancel", null)

        val dialog = dialogBuilder.create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            val enteredSerialNumber = serialNumberInput.text.toString().trim()
            val enteredStoreTo = storeToInput.text.toString().trim() // This captures the user input but does not change destLocationName

            if (enteredSerialNumber.isNotEmpty()) {
                coroutineScope.launch {
                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
                    val key = ProductPickKey(productId, pickId)
                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
                        // Serial number exists, proceed
                        serialList.add(enteredSerialNumber)
                        odooXmlRpcClient.updateMoveLinesForPick(pickId, productId, enteredSerialNumber, enteredStoreTo) // Updated to include enteredStoreTo
                        updateProductMatchState(productId, pickId, matched = true, serialList)

                        withContext(Dispatchers.Main) {
                            showGreenToast("Serial number added for $productName.")
                        }
                    } else {
                        // Serial number does not exist, notify and prompt again
                        withContext(Dispatchers.Main) {
                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
                            promptForSerialNumber(productName, pickId, productId) // Re-prompt
                        }
                    }
                }
            } else {
                showRedToast("Please enter a serial number")
                promptForSerialNumber(productName, pickId, productId) // Re-prompt
            }
        }

        dialog.show()
        // Show keyboard
        serialNumberInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
    }

//    private fun promptForProductQuantity(productName: String, expectedQuantity: Double, pickId: Int, productId: Int, recount: Boolean = false) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
//            hint = "Enter product quantity"
//        }
//
//        AlertDialog.Builder(this)
//            .setTitle(if (recount) "Recount Required" else "Enter Quantity")
//            .setMessage(if (recount) "Recount for $productName. Enter the exact quantity." else "Enter the exact quantity for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredQuantity = editText.text.toString().toDoubleOrNull()
//                if (enteredQuantity != null && enteredQuantity == expectedQuantity) {
////                    Toast.makeText(this, "Quantity entered for $productName", Toast.LENGTH_LONG).show()
//                    showGreenToast("Quantity updated for $productName")
//                    updateProductMatchState(productId, pickId, true)
//                } else if (!recount) {
//                    promptForProductQuantity(productName, expectedQuantity, pickId, productId, recount = true)
//                } else {
//                    val localPickName = pickName // Copy the mutable property to a local variable
//
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        if (localPickName != null) { // Use the local copy for the check
//                            val buyerDetails = odooXmlRpcClient.fetchAndLogBuyerDetails(localPickName)
//                            if (buyerDetails != null) {
//                                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, localPickName, productName) // Pass the local copy to the function
//                                withContext(Dispatchers.Main) {
////                                    Toast.makeText(this@ProductsActivity, "Flagged ${buyerDetails.login}. Email sent.", Toast.LENGTH_LONG).show()
//                                    showRedToast("Flagged")
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
////                                    Toast.makeText(this@ProductsActivity, "Flagged, but buyer details not found.", Toast.LENGTH_LONG).show()
//                                    showRedToast("Flagged, but buyer details not found")
//                                }
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
////                                Toast.makeText(this@ProductsActivity, "Receipt name is null or not found", Toast.LENGTH_LONG).show()
//                                showRedToast("Pick name is null or not found")
//                            }
//                        }
//                    }
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }

    private fun promptForProductQuantity(
        productName: String,
        expectedQuantity: Double,
        pickId: Int,
        productId: Int,
        recount: Boolean = false
    ) {
        // Parent layout for EditText inputs
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(50, 20, 50, 20) // Adjust padding as necessary
        }

        // EditText for entering the product quantity
        val quantityEditText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter product quantity"
        }
        layout.addView(quantityEditText)

        // EditText for "Store To" location input
        val storeToLocationEditText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter store to location"
            setText(destLocationName) // Pre-populate with the default location name
        }
        layout.addView(storeToLocationEditText)

        AlertDialog.Builder(this)
            .setTitle(if (recount) "Recount Required" else "Enter Quantity")
            .setMessage(if (recount) "Recount for $productName. Enter the exact quantity." else "Enter the exact quantity for $productName.")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                val enteredQuantity = quantityEditText.text.toString().toDoubleOrNull()
                val enteredLocation = storeToLocationEditText.text.toString()

                if (enteredQuantity != null) {
                    // Accumulate entered quantity for the product
                    val totalQuantity = accumulatedQuantities.getOrDefault(productId, 0.0) + enteredQuantity
                    accumulatedQuantities[productId] = totalQuantity

                    // Check if total entered quantity matches expected quantity
                    val matched = totalQuantity == expectedQuantity
                    // Update match state based on total entered quantity
                    updateProductMatchState(productId, pickId, matched)

                    if (matched) {
                        showGreenToast("Quantity matched for $productName")
                    } else {
                        showRedToast("Total quantity does not match expected. Please enter more.")
                        Log.d("match state", "Total quantity: $totalQuantity")
                    }

                    // Create a stock.move.line for the entered quantity, regardless of match state
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val result = odooXmlRpcClient.createStockMoveLineForUntrackedProduct(pickId, productId, enteredQuantity, enteredLocation)
                            withContext(Dispatchers.Main) {
                                if (result) {
                                    showGreenToast("Stock move line created for $productName at $enteredLocation")
                                } else {
                                    showRedToast("Failed to create stock move line for $productName ID: $productId")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("OdooXmlRpcClient", "Error creating stock move line for untracked product: ${e.message}", e)
                        }
                    }
                } else if (!recount) {
                    promptForProductQuantity(productName, expectedQuantity, pickId, productId, recount = true)
                } else {
                    // Handle recount failure
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }




    private fun promptForLotNumber(productName: String, pickId: Int, productId: Int) {
        // Create the parent layout
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 8, 16, 16)
        }

        // Create the serial number input
        val lotNumberInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter lot number"
        }
        container.addView(lotNumberInput)

        // Add subheading TextView for the "Store to" input
        val subheading = TextView(this).apply {
            text = "Store To Location"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            val topMargin = (8 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, topMargin, 0, 0) // Adding top margin for spacing
            }
        }
        container.addView(subheading)

        // Create the "Store to" input with destLocationName as default value
//        val storeToInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Store to"
//            setText(destLocationName) // Assuming destLocationName is defined elsewhere in your code and remains unchanged
//        }

        val storeToInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Store to"
            setText(destLocationName) // Set the default text to the EditText

            // Adding a focus change listener instead
            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                // Clear the text when the EditText gains focus if it contains the default location name
                if (hasFocus && text.toString() == destLocationName) {
                    setText("")
                }
            }
        }
        container.addView(storeToInput)

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Enter lot Number")
            .setMessage("Enter the lot number for $productName.")
            .setView(container)
            .setNegativeButton("Cancel", null)

        val dialog = dialogBuilder.create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            val enteredLotNumber = lotNumberInput.text.toString().trim()
            val enteredStoreTo = storeToInput.text.toString().trim() // This captures the user input but does not change destLocationName

            if (enteredLotNumber.isNotEmpty()) {
                coroutineScope.launch {
                    val lotNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
//                    val key = ProductPickKey(productId, pickId)
//                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
                    if (lotNumbers?.contains(enteredLotNumber) == true) {
                        coroutineScope.launch {
                            withContext(Dispatchers.Main) {
                                // Assume promptForLotQuantity is correctly defined elsewhere to handle these parameters
                                promptForLotQuantity(productName, pickId, productId, enteredLotNumber, enteredStoreTo)
                            }
}
                        withContext(Dispatchers.Main) {
                            showGreenToast("Serial number added for $productName.")
                        }
                    } else {
                        // Serial number does not exist, notify and prompt again
                        withContext(Dispatchers.Main) {
                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
                            promptForSerialNumber(productName, pickId, productId) // Re-prompt
                        }
                    }
                }
            } else {
                showRedToast("Please enter a serial number")
                promptForSerialNumber(productName, pickId, productId) // Re-prompt
            }
        }

        dialog.show()
        // Show keyboard
        lotNumberInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(lotNumberInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun promptForLotQuantity(productName: String, pickId: Int, productId: Int, lotNumber: String, location: String) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter quantity"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter Quantity")
            .setMessage("Enter the TOTAL quantity for the lot of $productName.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val enteredQuantity = editText.text.toString().toIntOrNull()
                if (enteredQuantity != null) {
                    coroutineScope.launch {
                        // Update the quantity for the lot without requiring expiration date
                        odooXmlRpcClient.updateMoveLinesWithoutExpirationWithLot(pickId, productId, lotNumber, enteredQuantity, location)
                        updateProductMatchState(productId, pickId, matched = false, lotQuantity = enteredQuantity)
                        withContext(Dispatchers.Main) {
                            showGreenToast("Quantity updated for lot.")

                        }
                    }
                } else {
                        // Show toast message for invalid quantity
                        showRedToast("Invalid quantity entered.")
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Ensure the keyboard is shown when the EditText gains focus
        dialog.setOnShowListener {
            editText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.show()
    }

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
//        productId: Int,
//        pickId: Int,
//        matched: Boolean = false,
//        serialNumbers: MutableList<String>? = null,
//        lotQuantity: Int? = null
//    ) {
//        val key = ProductPickKey(productId, pickId)
//        val product = pickProductsAdapter.products.find { it.id == productId }
//        val expectedQuantity = product?.quantity?.toInt() ?: 0
//
//        if (serialNumbers != null) {
//            quantityMatches[key] = serialNumbers.size == expectedQuantity
//            // Log to verify match state update
//            Log.d("MatchState", "Product ID: $productId, Verified: ${serialNumbers.size}/$expectedQuantity")
//        } else if (lotQuantity != null) {
//            val currentQuantity = lotQuantities.getOrDefault(key, 0) + lotQuantity
//            lotQuantities[key] = currentQuantity
//            quantityMatches[key] = currentQuantity >= expectedQuantity
//        } else {
//            quantityMatches[key] = matched
//
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
private fun updateProductMatchState(
    productId: Int,
    pickId: Int,
    matched: Boolean = false,
    serialNumbers: MutableList<String>? = null,
    lotQuantity: Int? = null,
    enteredQuantity: Double? = null // Use Double for finer quantity control
) {
    val key = ProductPickKey(productId, pickId)
    val product = pickProductsAdapter.products.find { it.id == productId }
    val expectedQuantity = product?.quantity?.toDouble() ?: 0.0 // Convert to Double for consistency

    when {
        serialNumbers != null -> {
            // Handling for serial-numbered products
            quantityMatches[key] = serialNumbers.size.toDouble() == expectedQuantity
        }
        lotQuantity != null -> {
            // Handling for lot-numbered products
            val currentQuantity = lotQuantities.getOrDefault(key, 0) + lotQuantity
            lotQuantities[key] = currentQuantity
            quantityMatches[key] = currentQuantity.toDouble() == expectedQuantity
        }
        enteredQuantity != null -> {
            // Handling for untracked products
            val totalQuantity = accumulatedQuantities.getOrDefault(productId, 0.0) + enteredQuantity
            accumulatedQuantities[productId] = totalQuantity
            quantityMatches[key] = totalQuantity == expectedQuantity


        }
        else -> {
            quantityMatches[key] = matched
        }
    }

    val allProductsMatched = checkAllProductsMatched(pickId)
    saveMatchStateToPreferences(key, quantityMatches[key] == true)

    val position = pickProductsAdapter.findProductPositionById(productId)
    if (position != -1) {
        runOnUiThread { pickProductsAdapter.notifyItemChanged(position) }
    }

    if (allProductsMatched) {
        coroutineScope.launch {
            val validated = odooXmlRpcClient.validateOperation(pickId)
            withContext(Dispatchers.Main) {
                if (validated) {
                    showGreenToast("Receipt validated for ${pickId}")
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



    private fun checkAllProductsMatched(pickId: Int): Boolean {
        // Filter the quantityMatches for the current receiptId
        return quantityMatches.filter { it.key.pickId == pickId }.all { it.value }
    }


    private fun saveMatchStateToPreferences(key: ProductPickKey, matched: Boolean) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("${key.productId}_${key.pickId}", matched)
            apply()
        }
    }

    private fun loadMatchStatesFromPreferences(pickId: Int) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        val tempQuantityMatches = mutableMapOf<ProductPickKey, Boolean>()

//        sharedPref.all.forEach { (prefKey, value) ->
//            if (value is Boolean) {
//                val parts = prefKey.split("_").let { if (it.size == 2) it else null }
//                parts?.let {
//                    try {
//                        val productId = it[0].toInt()
//                        val prefPickId = it[1].toInt()
//                        if (prefPickId == pickId) {
//                            val key = ProductPickKey(productId, prefPickId)
//                            tempQuantityMatches[key] = value
//                        }
//                        else{
//                            Log.e("PickProductsActivity", "Something went wrong.")
//                        }
//                    } catch (e: NumberFormatException) {
//                        Log.e("PickProductsActivity", "Error parsing shared preference key: $prefKey", e)
//                    }
//                }
//            }
        sharedPref.all.forEach { (prefKey, value) ->
            if (value is Boolean) {
                val parts = prefKey.split("_")
                if (parts.size == 2) {
                    try {
                        val productId = parts[0].toInt()
                        val prefPickId = parts[1].toInt()
                        if (prefPickId == pickId) {
                            val key = ProductPickKey(productId, prefPickId)
                            tempQuantityMatches[key] = value
                        }
                    } catch (e: NumberFormatException) {
                        Log.e("PickProductsActivity", "Error parsing shared preference key: $prefKey", e)
                    }
                } else {
                    // This is a better place to log a detailed message about the formatting issue
                    Log.e("PickProductsActivity", "Incorrectly formatted shared preference key: $prefKey. Expected format: productId_pickId")
                }
            }


    }

        quantityMatches.clear()
        quantityMatches.putAll(tempQuantityMatches)

        // Now update the adapter with the loaded match states
        runOnUiThread {
            pickProductsAdapter.updateProducts(pickProductsAdapter.products, pickId, quantityMatches)
        }
    }



//    suspend fun displayLocationsForPick(pickId: Int) {
//        val config = odooXmlRpcClient.getClientConfig("object")
//        if (config == null) {
//            Log.e("PickProductsActivity", "Client configuration is null, aborting displayLocationsForPick.")
//            return
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = odooXmlRpcClient.credentialManager.getUserId()
//        val password = odooXmlRpcClient.credentialManager.getPassword() ?: ""
//
//        // Define the domain to fetch move lines for the specific pick ID
//        val moveLineDomain = listOf(listOf("picking_id", "=", pickId))
//
//        // Specify the fields to fetch - you want the location_id and location_dest_id names
//        val moveLineFields = listOf("location_id", "location_dest_id")
//        val database= Constants.DATABASE
//        val moveLineParams = listOf(
//            database,
//            userId,
//            password,
//            "stock.move.line",
//            "search_read",
//            listOf(moveLineDomain),
//            mapOf("fields" to moveLineFields, "limit" to 1)  // Assuming you want just one move line to simplify
//        )
//
//        try {
//            val moveLineResult = client.execute("execute_kw", moveLineParams) as Array<Any>
//            val moveLine = moveLineResult.mapNotNull { it as? Map<String, Any> }.firstOrNull()
//
//            // Assuming locations are returned as Array<Any> where the first element is the ID and the second is the name
//            val locationName = (moveLine?.get("location_id") as? Array<Any>)?.get(1)?.toString()
//            val destinationLocationName = (moveLine?.get("location_dest_id") as? Array<Any>)?.get(1)?.toString()
//
//            // Log the names of the locations
//            Log.d("PickProductsActivity", "Pick ID: $pickId, Location: $locationName, Destination Location: $destinationLocationName")
//
//        } catch (e: Exception) {
//            Log.e("PickProductsActivity", "Error fetching location information for pick ID: $pickId", e)
//        }
//    }
// Ensure this function is called within a coroutine scope
//    suspend fun displayLocationsForPick(pickId: Int) {
//        val config = odooXmlRpcClient.getClientConfig("object")
//        if (config == null) {
//            Log.e("PickProductsActivity", "Client configuration is null, aborting displayLocationsForPick.")
//            return
//        }
//        val client = XmlRpcClient().also { it.setConfig(config) }
//        val userId = odooXmlRpcClient.credentialManager.getUserId()
//        val password = odooXmlRpcClient.credentialManager.getPassword() ?: ""
//
//        val moveLineDomain = listOf(listOf("picking_id", "=", pickId))
//        val moveLineFields = listOf("location_id")
//        val database = Constants.DATABASE
//        val moveLineParams = listOf(
//            database,
//            userId,
//            password,
//            "stock.move.line",
//            "search_read",
//            listOf(moveLineDomain),
//            mapOf("fields" to moveLineFields, "limit" to 1)
//        )
//
//        try {
//            val moveLineResult = client.execute("execute_kw", moveLineParams) as Array<Any>
//            val moveLine = moveLineResult.mapNotNull { it as? Map<String, Any> }.firstOrNull()
//            val locationName = (moveLine?.get("location_id") as? Array<Any>)?.get(1)?.toString()
//
//            // Switch back to the Main thread to show the dialog
//            withContext(Dispatchers.Main) {
//                showLocationDialog(locationName ?: "Unknown")
//            }
//
//        } catch (e: Exception) {
//            Log.e("PickProductsActivity", "Error fetching location information for pick ID: $pickId", e)
//        }
//    }
//        fun showLocationDialog(locationName: String) {
//            AlertDialog.Builder(this)
//                .setTitle("Location")
//                .setMessage("Location for this pick: $locationName")
//                .setPositiveButton("OK", null)
//                .show()
//        }





}

