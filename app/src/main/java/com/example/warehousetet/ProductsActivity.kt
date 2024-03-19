//package com.example.warehousetet
//
//
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.text.Editable
//import android.text.InputType
//import android.text.TextWatcher
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
//import java.text.ParseException
//import java.text.SimpleDateFormat
//import java.util.Locale
//import java.util.Properties
//import javax.mail.Message
//import javax.mail.MessagingException
//import javax.mail.PasswordAuthentication
//import javax.mail.Session
//import javax.mail.Transport
//import javax.mail.internet.InternetAddress
//import javax.mail.internet.MimeMessage
//
//class ProductsActivity : AppCompatActivity() {
//    private lateinit var productsAdapter: ProductsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var barcodeInput: EditText
//    private lateinit var confirmButton: Button
//
//    private var productBarcodes = hashMapOf<String, String>()
//    private var productSerialNumbers = hashMapOf<ProductReceiptKey, MutableList<String>>()
//    private var quantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
//
//    private var receiptName: String? = null
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_products)
//
//        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
//        barcodeInput = findViewById(R.id.barcodeInput)
//        confirmButton = findViewById(R.id.confirmButton)
//        val receiptId = intent.getIntExtra("RECEIPT_ID", -1)
//        receiptName = intent.getStringExtra("RECEIPT_NAME")
//        Log.d("ProductsActivity", "Received receipt name: $receiptName")
//
//        productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId)
//
//        if (receiptId != -1) {
//            setupRecyclerView()
//            fetchProductsForReceipt(receiptId)
//        } else {
//            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity.")
//        }
//
//        setupBarcodeVerification(receiptId)
//        loadMatchStatesFromPreferences(receiptId)
//
//    }
//
//    private fun setupRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = productsAdapter
//    }
//
//    private fun setupBarcodeVerification(receiptId: Int) {
//        confirmButton.setOnClickListener {
//            val enteredBarcode = barcodeInput.text.toString().trim()
//            verifyBarcode(enteredBarcode, receiptId)
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
//
//    private fun hideKeyboard() {
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
//    }
//
//
//    private fun fetchProductsForReceipt(receiptId: Int) {
//        coroutineScope.launch {
//            Log.d("fetchProductsForReceipt", "Fetching products for receipt ID: $receiptId")
//
//            // Fetch basic product information first.
//            val fetchedProducts: List<Product> = try {
//                odooXmlRpcClient.fetchProductsForReceipt(receiptId)
//            } catch (e: Exception) {
//                Log.e("fetchProductsForReceipt", "Error fetching products: ${e.localizedMessage}")
//                emptyList()
//            }
//
//            // Ensure barcodes and additional details are fetched for all fetched products
//            fetchBarcodesForProducts(fetchedProducts)
//
//            // Initialize a list to store the updated products with additional details
//            val updatedProductsWithDetails = mutableListOf<Product>()
//
//            fetchedProducts.forEach { product ->
//                coroutineScope.launch(Dispatchers.IO) {
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(product.name) ?: Pair("none", false)
//                    Log.d("ProductExpiration", "Product: ${product.name}, Uses Expiration Date: ${trackingAndExpiration.second}")
//                    val barcode = barcodeToProductIdMap.filterValues { it == product.id }.keys.firstOrNull()
//
//                    // Update each product with its tracking type, useExpirationDate, and barcode, without expanding them
//                    val updatedProduct = product.copy(
//                        trackingType = trackingAndExpiration.first,
//                        useExpirationDate = trackingAndExpiration.second,
//                        barcode = barcode
//                    )
//                    updatedProductsWithDetails.add(updatedProduct)
//                }.join() // Wait for all async operations to complete
//            }
//
//            withContext(Dispatchers.Main) {
//                Log.d("fetchProductsForReceipt", "Updating UI with products and their details")
//                updateUIForProducts(updatedProductsWithDetails, receiptId)
//            }
//        }
//    }
//
//
//    private fun fetchBarcodesForProducts(products: List<Product>) {
//        products.forEach { product ->
//            coroutineScope.launch {
//                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.name)
//                barcode?.let {
//                    // Assuming barcodeToProductIdMap should map barcode to product ID
//                    synchronized(this@ProductsActivity) {
//                        barcodeToProductIdMap[barcode] = product.id
//                    }
//                }
//            }
//        }
//    }
//
//    private fun updateUIForProducts(products: List<Product>, receiptId: Int) {
//        // Assuming barcodeToProductIdMap and productSerialNumbers are already populated or handled within the fetched products loop
//        val newQuantityMatches = products.associate {
//            ProductReceiptKey(it.id, receiptId) to (quantityMatches[ProductReceiptKey(it.id, receiptId)] ?: false)
//        }.toMutableMap()
//
//        // Now update the quantityMatches and UI accordingly
//        quantityMatches.clear()
//        quantityMatches.putAll(newQuantityMatches)
//        productsAdapter.updateProducts(products, receiptId, quantityMatches)
//    }
//
//
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
//
//    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the serial number for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { dialog, _ ->
//                val enteredSerialNumber = editText.text.toString().trim()
//                if (enteredSerialNumber.isNotEmpty()) {
//                    coroutineScope.launch {
//                        val serialNumbers = odooXmlRpcClient.fetchSerialNumbersByProductName(productName)
//                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
//                            val product = productsAdapter.products.find { it.id == productId }
//                            val key = ProductReceiptKey(productId, receiptId)
//                            val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//
//                            // Check if the serial number has already been entered
//                            if (!serialList.contains(enteredSerialNumber)) {
//                                withContext(Dispatchers.Main) {
//                                    if (product != null && product.useExpirationDate == true) {
//                                        // If the product requires an expiration date, prompt for it
//                                        dialog.dismiss() // Dismiss the current dialog to show the next one
//                                        promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber)
//                                    } else {
//                                        // For products that do not require an expiration date, update state directly
//                                        serialList.add(enteredSerialNumber)
//                                        val isMatched = serialList.size == product?.quantity?.toInt()
//                                        updateProductMatchState(productId, receiptId, isMatched, serialList)
//
//                                        Toast.makeText(this@ProductsActivity, "Serial number added for $productName. ${serialList.size}/${product?.quantity?.toInt()} verified", Toast.LENGTH_LONG).show()
//                                    }
//                                }
//                            } else {
//                                // Serial number has already been entered, inform the user
//                                withContext(Dispatchers.Main) {
//                                    Toast.makeText(this@ProductsActivity, "Serial number already entered for $productName", Toast.LENGTH_LONG).show()
//                                }
//                            }
//                        } else {
//                            // Serial number does not match any in the list from Odoo
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(this@ProductsActivity, "Serial number does not match for $productName", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                    }
//                } else {
//                    // No serial number entered
//                    Toast.makeText(this, "Please enter a serial number.", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//
//
//
//
//
//    private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER
//            hint = "Enter expiration date (dd/MM/yy)"
//        }
//
//        setupDateInputField(editText)
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Expiration Date")
//            .setMessage("Enter the expiration date for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredExpirationDate = editText.text.toString().trim()
//                val convertedDate = convertToFullDateTime(enteredExpirationDate, "00:00:00")
//                if (isValidDateFormat("dd/MM/yyyy HH:mm:ss", convertedDate)) {
//                    val key = ProductReceiptKey(productId, receiptId)
//                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                    if (!serialList.contains(serialNumber)) {
//                        serialList.add(serialNumber)
//                        // Assume match state change and verification occurs here
//                        val isMatched = serialList.size == productsAdapter.products.find { it.id == productId }?.quantity?.toInt()
//                        updateProductMatchState(productId, receiptId, isMatched, serialList)
//
//                        Toast.makeText(this, "Serial number added for $productName. ${serialList.size}/${productsAdapter.products.find { it.id == productId }?.quantity?.toInt()} verified", Toast.LENGTH_LONG).show()
//                    } else {
//                        Toast.makeText(this, "Serial number already entered for $productName", Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    Toast.makeText(this, "Invalid expiration date entered. Please use the format dd/MM/yy.", Toast.LENGTH_LONG).show()
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//
//    private fun convertToFullDateTime(simplifiedDate: String, defaultTime: String): String {
//        return try {
//            // Ensure parsing matches the user input format hinted in the EditText
//            val date = SimpleDateFormat("dd/MM/yy", Locale.US).parse(simplifiedDate)
//            // Use SimpleDateFormat to format the date, appending the default time only once
//            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date) + " $defaultTime"
//        } catch (e: ParseException) {
//            ""
//        }
//    }
//
//
//
//    private fun setupDateInputField(editText: EditText) {
//        editText.addTextChangedListener(object : TextWatcher {
//            private var current = ""
//            private val maxDigits = 6 // Maximum number of digits for the date: ddMMyy
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//            override fun afterTextChanged(s: Editable?) {
//                var userInput = s.toString().replace(Regex("[^\\d]"), "")
//                // Limit the user input to maxDigits
//                if (userInput.length > maxDigits) {
//                    userInput = userInput.substring(0, maxDigits)
//                }
//
//                // Perform the formatting to dd/MM/yy
//                if (userInput != current) {
//                    if (userInput.length > 4) {
//                        userInput = "${userInput.substring(0, 2)}/${userInput.substring(2, 4)}/${userInput.substring(4)}"
//                    } else if (userInput.length > 2) {
//                        userInput = "${userInput.substring(0, 2)}/${userInput.substring(2)}"
//                    }
//
//                    editText.removeTextChangedListener(this)
//                    current = userInput // Update the current tracking variable
//                    editText.setText(userInput)
//                    editText.setSelection(userInput.length)
//                    editText.addTextChangedListener(this)
//                }
//            }
//        })
//    }
//
//
//
//    private fun isValidDateFormat(format: String, value: String): Boolean {
//        return try {
//            val dateFormat = SimpleDateFormat(format, Locale.US)
//            dateFormat.isLenient = false
//            dateFormat.parse(value)
//            true
//        } catch (e: ParseException) {
//            false
//        }
//    }
//
//
//    private fun promptForProductQuantity(productName: String, expectedQuantity: Double, receiptId: Int, productId: Int, recount: Boolean = false) {
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
//                    Toast.makeText(this, "Correct quantity entered for $productName", Toast.LENGTH_LONG).show()
//                    updateProductMatchState(productId, receiptId, true)
//                } else if (!recount) {
//                    promptForProductQuantity(productName, expectedQuantity, receiptId, productId, recount = true)
//                } else {
//                    val localReceiptName = receiptName // Copy the mutable property to a local variable
//
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        if (localReceiptName != null) { // Use the local copy for the check
//                            val buyerDetails = odooXmlRpcClient.fetchAndLogBuyerDetails(localReceiptName)
//                            if (buyerDetails != null) {
//                                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, localReceiptName, productName) // Pass the local copy to the function
//                                withContext(Dispatchers.Main) {
//                                    Toast.makeText(this@ProductsActivity, "Flagged ${buyerDetails.login}. Email sent.", Toast.LENGTH_LONG).show()
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    Toast.makeText(this@ProductsActivity, "Flagged, but buyer details not found.", Toast.LENGTH_LONG).show()
//                                }
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(this@ProductsActivity, "Receipt name is null or not found", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                    }
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun sendEmailToBuyer(buyerEmail: String, buyerName: String, receiptName: String?, productName: String) {
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
//                subject = "Action Required: Discrepancy in Received Quantity for Receipt $receiptName"
//                setText("""
//                Dear $buyerName,
//
//                During a recent receipt event, we identified a discrepancy in the quantities received for the following item:
//
//                - Receipt ID: $receiptName
//                - Product: $productName
//
//                The recorded quantity does not match the expected quantity as per our purchase order. This discrepancy requires your immediate attention and action.
//
//                Please review the receipt and product details at your earliest convenience and undertake the necessary steps to rectify this discrepancy. It is crucial to address these issues promptly to maintain accurate inventory records and ensure operational efficiency.
//
//                Thank you for your prompt attention to this matter.
//
//                Best regards,
//                The Swiib team
//            """.trimIndent())
//            }
//            Transport.send(message)
//            Log.d("EmailSender", "Email sent successfully to $buyerEmail.")
//        } catch (e: MessagingException) {
//            Log.e("EmailSender", "Failed to send email.", e)
//        }
//    }
//
//
//    private fun updateProductMatchState(productId: Int, receiptId: Int, matched: Boolean, serialNumbers: MutableList<String>? = null) {
//        val key = ProductReceiptKey(productId, receiptId)
//
//        // Determine the match state.
//        // For serialized products, check if the list size matches the product quantity.
//        // For non-serialized products, use the matched parameter.
//        val product = productsAdapter.products.find { it.id == productId }
//        val isMatched = when {
//            serialNumbers != null && product?.trackingType == "serial" -> serialNumbers.size == product.quantity.toInt()
//            else -> matched
//        }
//
//        quantityMatches[key] = isMatched
//        saveMatchStateToPreferences(key, isMatched)
//
//        val position = productsAdapter.findProductPositionById(productId)
//        if (position != -1) {
//            runOnUiThread { productsAdapter.notifyItemChanged(position) }
//        }
//
//        // Provide feedback if needed.
//        if (serialNumbers != null && !isMatched) {
//            val message = "Partial quantity matched for $productId. ${serialNumbers.size}/${product?.quantity} verified."
//            runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
//        } else if (isMatched) {
//            val message = "Verification complete for product ID: $productId."
//            runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
//        }
//    }
//
//    private fun saveMatchStateToPreferences(key: ProductReceiptKey, matched: Boolean) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        with(sharedPref.edit()) {
//            putBoolean("${key.productId}_${key.receiptId}", matched)
//            apply()
//        }
//    }
//
//    private fun loadMatchStatesFromPreferences(receiptId: Int) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val tempQuantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//
//        sharedPref.all.forEach { (prefKey, value) ->
//            if (value is Boolean) {
//                val parts = prefKey.split("_").let { if (it.size == 2) it else null }
//                parts?.let {
//                    try {
//                        val productId = it[0].toInt()
//                        val prefReceiptId = it[1].toInt()
//                        if (prefReceiptId == receiptId) {
//                            val key = ProductReceiptKey(productId, prefReceiptId)
//                            tempQuantityMatches[key] = value
//                        }
//                        else{
//
//                        }
//                    } catch (e: NumberFormatException) {
//                        Log.e("ProductsActivity", "Error parsing shared preference key: $prefKey", e)
//                    }
//                }
//            }
//        }
//
//        quantityMatches.clear()
//        quantityMatches.putAll(tempQuantityMatches)
//
//        // Now update the adapter with the loaded match states
//        runOnUiThread {
//            productsAdapter.updateProducts(productsAdapter.products, receiptId, quantityMatches)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        coroutineScope.cancel()
//    }
//}
//



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
                        "none" -> withContext(Dispatchers.Main) {
                            // Prompt for product quantity for non-serialized products
                            promptForProductQuantity(it.name, it.quantity, receiptId, it.id, false)
                        }
                        else -> {
                            // For non-serialized products or those without a defined tracking type,
                            // directly check and update the match state if the quantity is verified.
                            val key = ProductReceiptKey(productId, receiptId)
                            val serialList = productSerialNumbers[key]
                            if (serialList != null && serialList.size == it.quantity.toInt()) {
                                // If the number of serial numbers matches the product quantity, update the match state to true.
                                updateProductMatchState(productId, receiptId, true)
                            } else if (trackingType == "none") {
                                // For products without serialization, update the match state immediately.
                                updateProductMatchState(productId, receiptId, true)
                            } else {
                                // For serialized products that don't yet match the quantity, keep the state unchanged.
                                // Optionally, you can provide feedback to the user here.
                            }
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

//    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Lot/Serial Number")
//            .setMessage("Enter the lot/serial number for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { dialog, _ ->
//                val enteredSerialNumber = editText.text.toString().trim()
//                if (enteredSerialNumber.isNotEmpty()) {
//                    coroutineScope.launch {
//                        val serialNumbers = odooXmlRpcClient.fetchSerialNumbersByProductName(productName)
//                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
//                            val product = productsAdapter.products.find { it.id == productId }
//                            val key = ProductReceiptKey(productId, receiptId)
//                            val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//
//                            // Check if the serial number has already been entered
//                            if (!serialList.contains(enteredSerialNumber)) {
//                                withContext(Dispatchers.Main) {
//                                    if (product != null && product.useExpirationDate == true) {
//                                        // If the product requires an expiration date, prompt for it
//                                        dialog.dismiss() // Dismiss the current dialog to show the next one
//                                        promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber)
//                                    } else {
//                                        // For products that do not require an expiration date, update state directly
//                                        serialList.add(enteredSerialNumber)
//                                        val isMatched = serialList.size == product?.quantity?.toInt()
//                                        updateProductMatchState(productId, receiptId, isMatched, serialList)
//
//                                        Toast.makeText(this@ProductsActivity, "Serial number added for $productName. ${serialList.size}/${product?.quantity?.toInt()} verified", Toast.LENGTH_LONG).show()
//                                    }
//                                }
//                            } else {
//                                // Serial number has already been entered, inform the user
//                                withContext(Dispatchers.Main) {
//                                    Toast.makeText(this@ProductsActivity, "Serial number already entered for $productName", Toast.LENGTH_LONG).show()
//                                }
//                            }
//                        } else {
//                            // Serial number does not match any in the list from Odoo
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(this@ProductsActivity, "Serial number does not match for $productName", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                    }
//                } else {
//                    // No serial number entered
//                    Toast.makeText(this, "Please enter a serial number.", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//

    //current
//private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the serial number for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { dialog, _ ->
//                val enteredSerialNumber = editText.text.toString().trim()
//                if (enteredSerialNumber.isNotEmpty()) {
//                    // Directly proceed to check if the product requires an expiration date.
//                    val product = productsAdapter.products.find { it.id == productId }
//                    if (product != null && product.useExpirationDate == true) {
//                        // Dismiss the current dialog to prompt for the expiration date.
//                        dialog.dismiss()
//                        promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber)
//                    } else {
//                        // If no expiration date is needed, display a message and don't proceed to update.
//                        // No need to call updateStockMoveLine here.
//                        Toast.makeText(this, "Product does not require an expiration date. Serial number noted.", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    // No serial number entered.
//                    Toast.makeText(this, "Please enter a serial number.", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }




//    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the serial number for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { dialog, _ ->
//                val enteredSerialNumber = editText.text.toString().trim()
//                if (enteredSerialNumber.isNotEmpty()) {
//                    coroutineScope.launch {
//                        val product = productsAdapter.products.find { it.id == productId }
//                        val key = ProductReceiptKey(productId, receiptId)
//                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//
//                        serialList.add(enteredSerialNumber) // Add serial number to the list
//
//                        // Check if product requires an expiration date
//                        if (product?.useExpirationDate == true) {
//                            // Prompt for expiration date
//                            dialog.dismiss()
//                            promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber)
//                        } else {
//                            // For products that don't require an expiration date, call updateMoveLinesByPicking with an empty date
//                            odooXmlRpcClient.updateMoveLinesWithoutExpiration(receiptId, productId, enteredSerialNumber)
//
//                            // Update match state and display message
//                            updateProductMatchState(productId, receiptId, matched = true, serialList)
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(this@ProductsActivity, "Serial number added for $productName without requiring an expiration date.", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                    }
//                } else {
//                    Toast.makeText(this, "Please enter a serial number.", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }

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










//    private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER
//            hint = "Enter expiration date (dd/MM/yy)"
//        }
//
//        setupDateInputField(editText)
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Expiration Date")
//            .setMessage("Enter the expiration date for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredExpirationDate = editText.text.toString().trim()
//                val convertedDate = convertToFullDateTime(enteredExpirationDate, "00:00:00")
//                if (isValidDateFormat("dd/MM/yyyy HH:mm:ss", convertedDate)) {
//                    val key = ProductReceiptKey(productId, receiptId)
//                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                    if (!serialList.contains(serialNumber)) {
//                        serialList.add(serialNumber)
//                        // Assume match state change and verification occurs here
//                        val isMatched = serialList.size == productsAdapter.products.find { it.id == productId }?.quantity?.toInt()
//                        updateProductMatchState(productId, receiptId, isMatched, serialList)
//
//                        Toast.makeText(this, "Serial number added for $productName. ${serialList.size}/${productsAdapter.products.find { it.id == productId }?.quantity?.toInt()} verified", Toast.LENGTH_LONG).show()
//                    } else {
//                        Toast.makeText(this, "Serial number already entered for $productName", Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    Toast.makeText(this, "Invalid expiration date entered. Please use the format dd/MM/yy.", Toast.LENGTH_LONG).show()
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String) {
//    val editText = EditText(this).apply {
//        inputType = InputType.TYPE_CLASS_NUMBER
//        hint = "Enter expiration date (dd/MM/yy)"
//    }
//
//    setupDateInputField(editText)
//
//    AlertDialog.Builder(this)
//        .setTitle("Enter Expiration Date")
//        .setMessage("Enter the expiration date for $productName.")
//        .setView(editText)
//        .setPositiveButton("OK") { _, _ ->
//            val enteredExpirationDate = editText.text.toString().trim()
//            val convertedDate = convertToFullDateTime(enteredExpirationDate)
//            if (isValidDateFormat(convertedDate)) {
//                coroutineScope.launch {
//                    odooXmlRpcClient.updateMoveLinesByPicking(receiptId, productId, serialNumber, convertedDate)
//                }
//            } else {
//                Toast.makeText(this, "Invalid expiration date entered. Please use the format dd/MM/yy.", Toast.LENGTH_LONG).show()
//            }
//        }
//        .setNegativeButton("Cancel", null)
//        .show()
//}
//

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


    private fun convertToOdooDateFormat(simplifiedDate: String): String {
        return try {
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(simplifiedDate)
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date) // Assuming Odoo expects "dd/MM/yyyy"
        } catch (e: ParseException) {
            Log.e("YourApp", "Date parsing error: ${e.localizedMessage}")
            ""
        }
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









//    private fun isValidDateFormat(value: String): Boolean {
//        return try {
//            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
//            true
//        } catch (e: ParseException) {
//            Log.e("YourApp", "Invalid date format: ${e.localizedMessage}")
//            false
//        }
//    }





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


    private fun updateProductMatchState(productId: Int, receiptId: Int, matched: Boolean, serialNumbers: MutableList<String>? = null) {
        val key = ProductReceiptKey(productId, receiptId)

        // Determine the match state.
        // For serialized products, check if the list size matches the product quantity.
        // For non-serialized products, use the matched parameter.
        val product = productsAdapter.products.find { it.id == productId }
        val isMatched = when {
            serialNumbers != null && product?.trackingType == "serial" -> serialNumbers.size == product.quantity.toInt()
            else -> matched
        }

        quantityMatches[key] = isMatched
        saveMatchStateToPreferences(key, isMatched)

        val position = productsAdapter.findProductPositionById(productId)
        if (position != -1) {
            runOnUiThread { productsAdapter.notifyItemChanged(position) }
        }

        // Provide feedback if needed.
        if (serialNumbers != null && !isMatched) {
            val message = "Partial quantity matched for $productId. ${serialNumbers.size}/${product?.quantity} verified."
            runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        } else if (isMatched) {
            val message = "Verification complete for product ID: $productId."
            runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
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

