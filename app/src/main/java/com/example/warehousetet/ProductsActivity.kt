package com.example.warehousetet


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ProductsActivity : AppCompatActivity() {
    private lateinit var productsAdapter: ProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button

    private var productBarcodes = hashMapOf<String, String>()
    private var productSerialNumbers = hashMapOf<ProductReceiptKey, MutableList<String>>()
    val lotQuantities: MutableMap<ProductReceiptKey, Int> = mutableMapOf()
    private var quantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
    private var barcodeToProductIdMap = mutableMapOf<String, Int>()

    private var receiptName: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        barcodeInput = findViewById(R.id.barcodeInput)
        confirmButton = findViewById(R.id.confirmButton)
        val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
        receiptName = intent.getStringExtra("RECEIPT_NAME")
        Log.d("ProductsActivity", "Received receipt name: $receiptName")

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
            Log.d("fetchProductsForReceipt", "Fetching products for receipt ID: $receiptId")

            // Fetch basic product information first.
            val fetchedProducts: List<Product> = try {
                odooXmlRpcClient.fetchProductsForReceipt(receiptId)
            } catch (e: Exception) {
                Log.e("fetchProductsForReceipt", "Error fetching products: ${e.localizedMessage}")
                emptyList()
            }

            // Ensure barcodes and additional details are fetched for all fetched products
            fetchBarcodesForProducts(fetchedProducts)

            // Initialize a list to store the updated products with additional details
            val updatedProductsWithDetails = mutableListOf<Product>()

            fetchedProducts.forEach { product ->
                coroutineScope.launch(Dispatchers.IO) {
                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(product.name) ?: Pair("none", false)
                    Log.d("ProductExpiration", "Product: ${product.name}, Uses Expiration Date: ${trackingAndExpiration.second}")
                    val barcode = barcodeToProductIdMap.filterValues { it == product.id }.keys.firstOrNull()

                    // Update each product with its tracking type, useExpirationDate, and barcode, without expanding them
                    val updatedProduct = product.copy(
                        trackingType = trackingAndExpiration.first,
                        useExpirationDate = trackingAndExpiration.second,
                        barcode = barcode
                    )
                    updatedProductsWithDetails.add(updatedProduct)
                }.join() // Wait for all async operations to complete
            }

            withContext(Dispatchers.Main) {
                Log.d("fetchProductsForReceipt", "Updating UI with products and their details")
                updateUIForProducts(updatedProductsWithDetails, receiptId)
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


//    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            if (productId != null) {
//                val product = productsAdapter.products.find { it.id == productId }
//                product?.let {
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(it.name)
//                    val trackingType = trackingAndExpiration?.first ?: "none"
//
//                    when (trackingType) {
//                        "serial" -> withContext(Dispatchers.Main) {
//                            // Prompt for a serial number for serialized products
//                            promptForSerialNumber(it.name, receiptId, it.id)
//                        }
//                        "none" -> withContext(Dispatchers.Main) {
//                            // Prompt for product quantity for non-serialized products
//                            promptForProductQuantity(it.name, it.quantity, receiptId, it.id, false)
//                        }
//                        else -> {
//                            // For non-serialized products or those without a defined tracking type,
//                            // directly check and update the match state if the quantity is verified.
//                            val key = ProductReceiptKey(productId, receiptId)
//                            val serialList = productSerialNumbers[key]
//                            if (serialList != null && serialList.size == it.quantity.toInt()) {
//                                // If the number of serial numbers matches the product quantity, update the match state to true.
//                                updateProductMatchState(productId, receiptId, true)
//                            } else if (trackingType == "none") {
//                                // For products without serialization, update the match state immediately.
//                                updateProductMatchState(productId, receiptId, true)
//                            } else {
//                                // For serialized products that don't yet match the quantity, keep the state unchanged.
//                                // Optionally, you can provide feedback to the user here.
//                            }
//                        }
//                    }
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@ProductsActivity, "Barcode not found.", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
    coroutineScope.launch {
        val productId = barcodeToProductIdMap[scannedBarcode]
        if (productId != null) {
            val product = productsAdapter.products.find { it.id == productId }
            product?.let {
                val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(it.name)
                val trackingType = trackingAndExpiration?.first ?: "none"

                when (trackingType) {
                    "serial" -> withContext(Dispatchers.Main) {
                        // Prompt for a serial number for serialized products
                        promptForSerialNumber(it.name, receiptId, it.id)
                    }
                    "lot" -> withContext(Dispatchers.Main) {
                        // Prompt for a lot number for lot-tracked products
                        promptForLotNumber(it.name, receiptId, it.id)
                    }
                    "none" -> withContext(Dispatchers.Main) {
                        // Prompt for product quantity for non-serialized products
                        promptForProductQuantity(it.name, it.quantity, receiptId, it.id, false)
                    }
                    else -> {
                        // Handle other cases as needed
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ProductsActivity, "Barcode not found.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int) {
    val editText = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_TEXT
        hint = "Enter lot number"
    }

    AlertDialog.Builder(this)
        .setTitle("Enter Lot Number")
        .setMessage("Enter the lot number for $productName.")
        .setView(editText)
        .setPositiveButton("OK") { _, _ ->
            val enteredLotNumber = editText.text.toString().trim()
            if (enteredLotNumber.isNotEmpty()) {
                coroutineScope.launch {
                    val product = productsAdapter.products.find { it.id == productId }
                    if (product?.useExpirationDate == true) {
                        withContext(Dispatchers.Main) {
                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, true)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, false)
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}

    private fun promptForLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotNumber: String, quantity: Int) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER // Ensure numerical input for date; consider TYPE_CLASS_DATETIME or custom date picker
            hint = "Enter expiration date (dd/MM/yyyy)"
        }

        setupDateInputField(editText) // Assuming this sets up a listener for proper date formatting

        AlertDialog.Builder(this)
            .setTitle("Enter Expiration Date")
            .setMessage("Enter the expiration date for the lot of $productName.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val enteredExpirationDate = editText.text.toString().trim()
                val convertedDate = convertToFullDateTime(enteredExpirationDate) // Ensure this converts to "yyyy-MM-dd"
                if (isValidDateFormat(convertedDate)) {
                    coroutineScope.launch {
                        odooXmlRpcClient.updateMoveLinesByPickingWithLot(receiptId, productId, lotNumber, quantity, convertedDate).also {
                            // Assume this method successfully updates the lot with expiration date
                            updateProductMatchState(productId, receiptId, matched = false, lotQuantity = quantity) // Update match state with new lot quantity
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Lot expiration date updated successfully.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Invalid expiration date entered. Please use the format dd/MM/yyyy.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

private fun promptForLotQuantity(productName: String, receiptId: Int, productId: Int, lotNumber: String, requiresExpirationDate: Boolean) {
    val editText = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        hint = "Enter quantity"
    }

    AlertDialog.Builder(this)
        .setTitle("Enter Quantity")
        .setMessage("Enter the quantity for the lot of $productName.")
        .setView(editText)
        .setPositiveButton("OK") { _, _ ->
            val enteredQuantity = editText.text.toString().toIntOrNull()
            if (enteredQuantity != null) {
                coroutineScope.launch {
                    if (requiresExpirationDate) {
                        withContext(Dispatchers.Main) {
                            promptForLotExpirationDate(productName, receiptId, productId, lotNumber, enteredQuantity)
                        }
                    } else {
                        odooXmlRpcClient.updateMoveLinesWithoutExpirationWithLot(receiptId, productId, lotNumber, enteredQuantity)
                        // Here, after updating the quantity for the lot without expiration, update the match state
                        updateProductMatchState(productId, receiptId, matched = false, lotQuantity = enteredQuantity)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProductsActivity, "Quantity updated for lot without expiration date.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}


    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter serial number"
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Serial Number")
            .setMessage("Enter the serial number for $productName.")
            .setView(editText)
            .setPositiveButton("OK") { dialog, _ ->
                val enteredSerialNumber = editText.text.toString().trim()
                if (enteredSerialNumber.isNotEmpty()) {
                    coroutineScope.launch {
                        val product = productsAdapter.products.find { it.id == productId }
                        val key = ProductReceiptKey(productId, receiptId)
                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }

                        if (!serialList.contains(enteredSerialNumber)) {
                            // Serial number is new, proceed with checks
                            if (product?.useExpirationDate == true) {
                                // Since promptForExpirationDate() will interact with UI, ensure we are on the main thread
                                withContext(Dispatchers.Main) {
                                    dialog.dismiss() // Dismiss the current dialog before showing the new one
                                    promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber)
                                }
                            } else {
                                // No expiration date required; proceed with updates
                                odooXmlRpcClient.updateMoveLinesWithoutExpiration(receiptId, productId, enteredSerialNumber)
                                serialList.add(enteredSerialNumber) // Add the serial number to the list
                                updateProductMatchState(productId, receiptId, matched = true, serialList)

                                // Display toast on main thread
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ProductsActivity, "Serial number added for $productName without requiring an expiration date.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            // Serial number already exists; inform the user
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Serial number already entered for $productName.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Please enter a serial number.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupDateInputField(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var current = ""
            private val ddMMyyFormat = "ddMMyy"
            private val slash = '/'

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                var userInput = s.toString().filter { it.isDigit() }

                if (userInput.length > ddMMyyFormat.length) {
                    userInput = userInput.substring(0, ddMMyyFormat.length)
                }

                var formattedInput = ""
                userInput.forEachIndexed { index, c ->
                    when (index) {
                        2, 4 -> formattedInput += "$slash$c" // Add slashes after day and month
                        else -> formattedInput += c
                    }
                }

                if (formattedInput != current) {
                    editText.removeTextChangedListener(this)
                    current = formattedInput
                    editText.setText(formattedInput)
                    editText.setSelection(formattedInput.length)
                    editText.addTextChangedListener(this)
                }
            }
        })
    }
    private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER // Use TEXT to allow slashes in date input
            hint = "Enter expiration date (dd/MM/yyyy)" // Updated hint for clarity
        }

        setupDateInputField(editText) // This function should setup a date input, possibly with a DatePicker dialog

        AlertDialog.Builder(this)
            .setTitle("Enter Expiration Date")
            .setMessage("Enter the expiration date for $productName.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val enteredExpirationDate = editText.text.toString().trim()
                // Use your existing function to convert date to the expected format
                val convertedDate = convertToFullDateTime(enteredExpirationDate) // Assuming this outputs "dd/MM/yyyy"
                if (isValidDateFormat(convertedDate)) { // Adjust this function call if needed
                    coroutineScope.launch {
                        // First, update serial numbers and match state
                        val key = ProductReceiptKey(productId, receiptId)
                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
                        if (!serialList.contains(serialNumber)) {
                            serialList.add(serialNumber)
                            // Here, determine if the match state needs to be updated based on your logic
                            val isMatched = serialList.size == productsAdapter.products.find { it.id == productId }?.quantity?.toInt()
                            updateProductMatchState(productId, receiptId, isMatched, serialList)

                            // Now, proceed to update the move lines in Odoo
                            odooXmlRpcClient.updateMoveLinesByPicking(receiptId, productId, serialNumber, convertedDate)

                            // Inform the user of success
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Serial number added for $productName. ${serialList.size}/${productsAdapter.products.find { it.id == productId }?.quantity?.toInt()} verified", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Serial number already entered for $productName", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@ProductsActivity, "Invalid expiration date entered. Please use the format dd/MM/yyyy.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isValidDateFormat(value: String): Boolean {
        return try {
            // Adjust to check against the "yyyy-MM-dd" format, as that's what your conversion function outputs
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
            true
        } catch (e: ParseException) {
            Log.e("YourApp", "Invalid date format: ${e.localizedMessage}")
            false
        }
    }


    private fun convertToFullDateTime(simplifiedDate: String): String {
        return try {
            val date = SimpleDateFormat("dd/MM/yy", Locale.US).parse(simplifiedDate)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        } catch (e: ParseException) {
            Log.e("YourApp", "Date parsing error: ${e.localizedMessage}")
            ""
        }
    }

    private fun promptForProductQuantity(productName: String, expectedQuantity: Double, receiptId: Int, productId: Int, recount: Boolean = false) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter product quantity"
        }

        AlertDialog.Builder(this)
            .setTitle(if (recount) "Recount Required" else "Enter Quantity")
            .setMessage(if (recount) "Recount for $productName. Enter the exact quantity." else "Enter the exact quantity for $productName.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val enteredQuantity = editText.text.toString().toDoubleOrNull()
                if (enteredQuantity != null && enteredQuantity == expectedQuantity) {
                    Toast.makeText(this, "Correct quantity entered for $productName", Toast.LENGTH_LONG).show()
                    updateProductMatchState(productId, receiptId, true)
                } else if (!recount) {
                    promptForProductQuantity(productName, expectedQuantity, receiptId, productId, recount = true)
                } else {
                    val localReceiptName = receiptName // Copy the mutable property to a local variable

                    lifecycleScope.launch(Dispatchers.IO) {
                        if (localReceiptName != null) { // Use the local copy for the check
                            val buyerDetails = odooXmlRpcClient.fetchAndLogBuyerDetails(localReceiptName)
                            if (buyerDetails != null) {
                                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, localReceiptName, productName) // Pass the local copy to the function
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ProductsActivity, "Flagged ${buyerDetails.login}. Email sent.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ProductsActivity, "Flagged, but buyer details not found.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Receipt name is null or not found", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendEmailToBuyer(buyerEmail: String, buyerName: String, receiptName: String?, productName: String) {
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
                subject = "Action Required: Discrepancy in Received Quantity for Receipt $receiptName"
                setText("""
                Dear $buyerName,

                During a recent receipt event, we identified a discrepancy in the quantities received for the following item:

                - Receipt ID: $receiptName
                - Product: $productName

                The recorded quantity does not match the expected quantity as per our purchase order. This discrepancy requires your immediate attention and action.

                Please review the receipt and product details at your earliest convenience and undertake the necessary steps to rectify this discrepancy. It is crucial to address these issues promptly to maintain accurate inventory records and ensure operational efficiency.

                Thank you for your prompt attention to this matter.

                Best regards,
                The Swiib team
            """.trimIndent())
            }
            Transport.send(message)
            Log.d("EmailSender", "Email sent successfully to $buyerEmail.")
        } catch (e: MessagingException) {
            Log.e("EmailSender", "Failed to send email.", e)
        }
    }

private fun updateProductMatchState(
    productId: Int,
    receiptId: Int,
    matched: Boolean = false,
    serialNumbers: MutableList<String>? = null,
    lotQuantity: Int? = null
) {
    val key = ProductReceiptKey(productId, receiptId)

    // Get product and its expected quantity
    val product = productsAdapter.products.find { it.id == productId }
    val expectedQuantity = product?.quantity?.toInt() ?: 0

    // Determine the match state based on tracking type and quantities
    val isMatched = when {
        // For serialized products, check if the list size matches the product quantity.
        serialNumbers != null && product?.trackingType == "serial" -> serialNumbers.size == expectedQuantity
        // For lot products, check if the entered lot quantity matches or exceeds the product quantity.
        lotQuantity != null && product?.trackingType == "lot" -> {
            val currentTotalQuantity = lotQuantities.getOrDefault(key, 0) + lotQuantity
            lotQuantities[key] = currentTotalQuantity
            currentTotalQuantity >= expectedQuantity
        }
        // For non-serialized and non-lotted products, use the matched parameter.
        else -> matched
    }

    // Update and save the match state
    quantityMatches[key] = isMatched
    saveMatchStateToPreferences(key, isMatched)

    // Update the UI
    val position = productsAdapter.findProductPositionById(productId)
    if (position != -1) {
        runOnUiThread { productsAdapter.notifyItemChanged(position) }
    }

    // Provide feedback based on the match state
    val message = when {
        serialNumbers != null && !isMatched -> "Partial quantity matched for $productId. ${serialNumbers.size}/$expectedQuantity verified."
        lotQuantity != null && !isMatched -> "Partial quantity matched for $productId. ${lotQuantities[key]}/$expectedQuantity verified."
        isMatched -> "Verification complete for product ID: $productId."
        else -> "Please verify the product details."
    }
    runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
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

