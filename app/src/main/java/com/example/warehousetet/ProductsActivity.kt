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
//            // Expand products based on their tracking type and quantity
//            val expandedProductsWithDetails = mutableListOf<Product>()
//
//            fetchedProducts.forEach { product ->
//                coroutineScope.launch(Dispatchers.IO) {
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(product.name) ?: Pair("none", false)
//                    Log.d("ProductExpiration", "Product: ${product.name}, Uses Expiration Date: ${trackingAndExpiration.second}")
//                    val barcode = barcodeToProductIdMap.filterValues { it == product.id }.keys.firstOrNull()
//
//                    if (trackingAndExpiration.first == "serial" && product.quantity > 1) {
//                        // For products with 'serial' tracking type, create individual entries based on quantity
//                        repeat(product.quantity.toInt()) { index ->
//                            val individualProduct = product.copy(
//                                id = product.id, // Consider generating a unique ID if needed
//                                barcode = barcode,
//                                trackingType = trackingAndExpiration.first,
//                                useExpirationDate = trackingAndExpiration.second,
//                                quantity = 1.0 // Since it's an individual entry, quantity is set to 1
//                            )
//                            expandedProductsWithDetails.add(individualProduct)
//                        }
//                    } else {
//                        // For other products, add them directly
//                        val singleProduct = product.copy(
//                            trackingType = trackingAndExpiration.first,
//                            useExpirationDate = trackingAndExpiration.second,
//                            barcode = barcode
//                        )
//                        expandedProductsWithDetails.add(singleProduct)
//                    }
//                }.join() // Wait for all async operations to complete
//            }
//
//            withContext(Dispatchers.Main) {
//                Log.d("fetchProductsForReceipt", "Updating UI with expanded products and details")
//                updateUIForProducts(expandedProductsWithDetails, receiptId)
//            }
//        }
//    }
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

    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter serial number"
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Serial Number")
            .setMessage("Enter the serial number for $productName.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val enteredSerialNumber = editText.text.toString().trim()
                if (enteredSerialNumber.isNotEmpty()) {
                    coroutineScope.launch {
                        val serialNumbers = odooXmlRpcClient.fetchSerialNumbersByProductName(productName)
                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
                            val key = ProductReceiptKey(productId, receiptId)
                            val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
                            if (!serialList.contains(enteredSerialNumber)) {
                                serialList.add(enteredSerialNumber)
                                if (serialList.size == productsAdapter.products.find { it.id == productId }?.quantity?.toInt()) {
                                    updateProductMatchState(productId, receiptId, true, serialList)
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ProductsActivity, "Serial number added for $productName. ${serialList.size}/${productsAdapter.products.find { it.id == productId }?.quantity?.toInt()} verified", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ProductsActivity, "Serial number already entered for $productName", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Serial number does not match for $productName", Toast.LENGTH_LONG).show()
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
    // Update the match state based on the count of serial numbers for serialized products
    val isMatched = serialNumbers?.let { it.size == productsAdapter.products.find { product -> product.id == productId }?.quantity?.toInt() } ?: matched
    quantityMatches[key] = isMatched

    // Save the match state persistently
    saveMatchStateToPreferences(key, isMatched)

    // Update UI to reflect the current match state
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

