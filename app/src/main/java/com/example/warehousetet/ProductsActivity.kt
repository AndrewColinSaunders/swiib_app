//package com.example.warehousetet
//
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.PorterDuff
//import android.graphics.Typeface
//import android.os.Bundle
//import android.text.Editable
//import android.text.InputType
//import android.text.Spannable
//import android.text.SpannableString
//import android.text.TextWatcher
//import android.text.style.ForegroundColorSpan
//import android.text.style.StyleSpan
//import android.util.Log
//import android.view.KeyEvent
//import android.view.Menu
//import android.view.MenuItem
//import android.view.WindowManager
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
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
//    //    private var productBarcodes = hashMapOf<String, String>()
//    private var productSerialNumbers = hashMapOf<ProductReceiptKey, MutableList<String>>()
//    val lotQuantities: MutableMap<ProductReceiptKey, Int> = mutableMapOf()
//    private var quantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
//
//    private var receiptName: String? = null
//
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
//        val titleTextView: TextView = findViewById(R.id.productsTitleTextView)
//        titleTextView.text = receiptName
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
//        findViewById<Button>(R.id.clearButton).setOnClickListener {
//            findViewById<EditText>(R.id.barcodeInput).text.clear()
//        }
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        setupBarcodeVerification(receiptId)
//        loadMatchStatesFromPreferences(receiptId)
//    }
//
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_products_activity, menu)
//        return true
//    }
//
//    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
//        super.onPrepareOptionsMenu(menu)
//        val menuItem = menu.findItem(R.id.action_flag_receipt)
//        val spanString = SpannableString(menuItem.title).apply {
//            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@ProductsActivity, R.color.danger_red)), 0, length, 0)
//            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//        menuItem.title = spanString
//        return true
//    }
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_flag_receipt -> {
//                AlertDialog.Builder(this).apply {
//                    setTitle("Flag Receipt")
//                    setMessage("Are you sure you want to flag this receipt?")
//                    setPositiveButton("Flag Receipt") { _, _ ->
//                        flagReceipt()
//                    }
//                    setNegativeButton(android.R.string.cancel, null)
//                }.show()
//                true
//            }
//            android.R.id.home -> {
//                onBackPressed()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    private fun showRedToast(message: String) {
//        val toast = Toast.makeText(this@ProductsActivity, message, Toast.LENGTH_SHORT)
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
//        val toast = Toast.makeText(this@ProductsActivity, message, Toast.LENGTH_SHORT)
//        val view = toast.view
//
//        // Get the TextView of the default Toast view
//        val text = view?.findViewById<TextView>(android.R.id.message)
//
//        // Retrieve the success_green color from resources
//        val successGreen = ContextCompat.getColor(this@ProductsActivity, R.color.success_green)
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
//    private fun flagReceipt() {
//        val receiptId = intent.getIntExtra("RECEIPT_ID", -1) // Assuming you have receiptId
//        if (receiptId == -1) {
//            Log.e("ProductsActivity", "Invalid receipt ID")
//            return
//        }
//
//        coroutineScope.launch {
//            val receiptName = this@ProductsActivity.receiptName ?: return@launch
//            odooXmlRpcClient.fetchAndLogBuyerDetails(receiptName)?.let { buyerDetails ->
//                sendEmailFlaggingReceipt(buyerDetails.login, buyerDetails.name, receiptName)
//                withContext(Dispatchers.Main) {
//                    Log.d("ProductsActivity", "Receipt flagged and buyer notified via email.")
//                    showRedToast("Receipt flagged")
//                }
//            } ?: run {
//                withContext(Dispatchers.Main) {
//                    Log.e("ProductsActivity", "Failed to fetch buyer details or flag the receipt.")
////                Toast.makeText(this@ProductsActivity, "Failed to flag receipt", Toast.LENGTH_SHORT).show()
//                    showRedToast("Failed to flag receipt")
//                }
//            }
//        }
//    }
//
//    private fun sendEmailFlaggingReceipt(buyerEmail: String, buyerName: String, receiptName: String) {
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
//                return PasswordAuthentication("info@dattec.co.za", "0s3*X4n)#m,z")
//            }
//        })
//
//        try {
//            val message = MimeMessage(session).apply {
//                setFrom(InternetAddress("info@dattec.co.za"))
//                setRecipients(Message.RecipientType.TO, InternetAddress.parse(buyerEmail))
//                subject = "Receipt Flagged for Review"
//                setText("""
//            Dear $buyerName,
//
//            A receipt has been flagged by an application user.
//            The receipt named '$receiptName' has been flagged for review. There may be discrepancies that require your immediate attention.
//
//            Regards,
//            The Swiib Team
//            """.trimIndent())
//            }
//            Transport.send(message)
//            Log.d("EmailSender", "Email sent successfully to $buyerEmail.")
//        } catch (e: MessagingException) {
//            Log.e("EmailSender", "Failed to send email.", e)
//        }
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
//                        "lot" -> withContext(Dispatchers.Main) {
//                            // Prompt for a lot number for lot-tracked products
//                            promptForLotNumber(it.name, receiptId, it.id)
//                        }
//                        "none" -> withContext(Dispatchers.Main) {
//                            // Prompt for product quantity for non-serialized products
//                            promptForProductQuantity(it.name, it.quantity, receiptId, it.id, false)
//                        }
//                        else -> {
//                            // Handle other cases as needed
//                        }
//                    }
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    //                Toast.makeText(this@ProductsActivity, "Barcode not found.", Toast.LENGTH_SHORT).show()
//                    showRedToast("Barcode not found")
//                }
//            }
//        }
//    }
//
//
//    private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter lot number"
//        }
//
//        var actionExecuted = false
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Enter Lot Number")
//            .setMessage("Enter the lot number for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                if (!actionExecuted) {
//                    actionExecuted = true
//                    val enteredLotNumber = editText.text.toString().trim()
//                    if (enteredLotNumber.isNotEmpty()) {
//                        coroutineScope.launch {
//                            val product = productsAdapter.products.find { it.id == productId }
//                            if (product?.useExpirationDate == true) {
//                                withContext(Dispatchers.Main) {
//                                    // Assume promptForLotQuantity is correctly defined elsewhere to handle these parameters
//                                    promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, true)
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    // Assume promptForLotQuantity is correctly defined elsewhere to handle these parameters
//                                    promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, false)
//                                }
//                            }
//                        }
//                    } else {
//                        showRedToast("Please enter a lot number.")
//                    }
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .create()
//
//        // Request focus and show the keyboard when the dialog is shown
//        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
//        dialog.setOnShowListener {
//            editText.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        editText.setOnEditorActionListener { _, actionId, event ->
//            if ((actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) && !actionExecuted) {
//                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.performClick()
//                true
//            } else {
//                false
//            }
//        }
//
//        dialog.show()
//    }
//
//
//    private fun promptForLotQuantity(productName: String, receiptId: Int, productId: Int, lotNumber: String, requiresExpirationDate: Boolean) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER
//            hint = "Enter quantity"
//        }
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Enter Quantity")
//            .setMessage("Enter the quantity for the lot of $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredQuantity = editText.text.toString().toIntOrNull()
//                if (enteredQuantity != null) {
//                    coroutineScope.launch {
//                        if (requiresExpirationDate) {
//                            withContext(Dispatchers.Main) {
//                                // Logic to prompt for lot expiration date
//                                promptForLotExpirationDate(productName, receiptId, productId, lotNumber, enteredQuantity)
//                            }
//                        } else {
//                            // Logic for handling the quantity update without expiration date
////                            odooXmlRpcClient.updateMoveLinesWithoutExpirationWithLot(receiptId, productId, lotNumber, enteredQuantity)
//                            updateProductMatchState(productId, receiptId, matched = false, lotQuantity = enteredQuantity)
//                            withContext(Dispatchers.Main) {
//                                showGreenToast("Quantity updated for lot without expiration date.")
//                            }
//                        }
//                    }
//                } else {
//                    // Show toast message for invalid quantity
//                    Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show()
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
//
//    private fun promptForLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotNumber: String, quantity: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER // Ensure numerical input for date; consider TYPE_CLASS_DATETIME or custom date picker
//            hint = "Enter expiration date (dd/MM/yyyy)"
//        }
//
//        setupDateInputField(editText) // Assuming this sets up a listener for proper date formatting
//
//        AlertDialog.Builder(this)
//            .setTitle("Enter Expiration Date")
//            .setMessage("Enter the expiration date for the lot of $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredExpirationDate = editText.text.toString().trim()
//                val convertedDate = convertToFullDateTime(enteredExpirationDate) // Ensure this converts to "yyyy-MM-dd"
//                if (isValidDateFormat(convertedDate)) {
//                    coroutineScope.launch {
//                        odooXmlRpcClient.updateMoveLinesByPickingWithLot(receiptId, productId, lotNumber, quantity, convertedDate).also {
//                            // Assume this method successfully updates the lot with expiration date
//                            updateProductMatchState(productId, receiptId, matched = false, lotQuantity = quantity) // Update match state with new lot quantity
//                            withContext(Dispatchers.Main) {
////                                Toast.makeText(this@ProductsActivity, "Lot expiration date updated successfully.", Toast.LENGTH_SHORT).show()
//                                showGreenToast("Lot expiration date updated")
//                            }
//                        }
//                    }
//                } else {
////                    Toast.makeText(this, "Invalid expiration date entered. Please use the format dd/MM/yyyy.", Toast.LENGTH_SHORT).show()
//                    showRedToast("Invalid expiration date entered. Please use the format DD/MM/YY")
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//
//        var actionExecuted = false
//
//        // Build the dialog but don't show it yet
//        val dialogBuilder = AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the serial number for $productName.")
//            .setView(editText)
//            .setNegativeButton("Cancel", null)
//
//        // Create the dialog from the builder
//        val dialog = dialogBuilder.create()
//
//        // Now set the positive button separately to have access to 'dialog' variable
//        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//            if (!actionExecuted) {
//                actionExecuted = true
//                val enteredSerialNumber = editText.text.toString().trim()
//                if (enteredSerialNumber.isNotEmpty()) {
//                    coroutineScope.launch {
//                        val product = productsAdapter.products.find { it.id == productId }
//                        val key = ProductReceiptKey(productId, receiptId)
//                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//
//                        if (!serialList.contains(enteredSerialNumber)) {
//                            if (product?.useExpirationDate == true) {
//                                withContext(Dispatchers.Main) {
//                                    dialog.dismiss() // Correctly reference 'dialog' here
//                                    promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber)
//                                }
//                            } else {
//                                odooXmlRpcClient.updateMoveLinesWithoutExpiration(receiptId, productId, enteredSerialNumber)
//                                serialList.add(enteredSerialNumber)
//                                updateProductMatchState(productId, receiptId, matched = true, serialList)
//                                withContext(Dispatchers.Main) {
//                                    showGreenToast("Serial number added for $productName. ${serialList.size}/${product?.quantity?.toInt()} verified")
//                                }
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                showRedToast("Serial number already entered for $productName")
//                            }
//                        }
//                    }
//                } else {
//                    showRedToast("Please enter a serial number")
//                }
//            }
//        }
//
//        // Set up dialog properties related to keyboard input
//        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
//        dialog.setOnShowListener {
//            editText.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        editText.setOnEditorActionListener { _, actionId, event ->
//            if ((actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) && !actionExecuted) {
//                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
//                true
//            } else {
//                false
//            }
//        }
//
//        dialog.show()
//    }
//
//    private fun setupDateInputField(editText: EditText) {
//        editText.addTextChangedListener(object : TextWatcher {
//            private var current = ""
//            private val ddMMyyFormat = "ddMMyy"
//            private val slash = '/'
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//            override fun afterTextChanged(s: Editable?) {
//                var userInput = s.toString().filter { it.isDigit() }
//
//                if (userInput.length > ddMMyyFormat.length) {
//                    userInput = userInput.substring(0, ddMMyyFormat.length)
//                }
//
//                var formattedInput = ""
//                userInput.forEachIndexed { index, c ->
//                    when (index) {
//                        2, 4 -> formattedInput += "$slash$c" // Add slashes after day and month
//                        else -> formattedInput += c
//                    }
//                }
//
//                if (formattedInput != current) {
//                    editText.removeTextChangedListener(this)
//                    current = formattedInput
//                    editText.setText(formattedInput)
//                    editText.setSelection(formattedInput.length)
//                    editText.addTextChangedListener(this)
//                }
//            }
//        })
//    }
//
//    private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER // Adjusted for numeric input, setupDateInputField will handle formatting
//            hint = "Enter expiration date (dd/MM/yyyy)"
//        }
//
//        // Setup the EditText for date input
//        setupDateInputField(editText)
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Enter Expiration Date")
//            .setMessage("Enter the expiration date for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                val enteredExpirationDate = editText.text.toString().trim()
//                val convertedDate = convertToFullDateTime(enteredExpirationDate) // Ensure this conversion to full date
//                if (isValidDateFormat(convertedDate)) {
//                    coroutineScope.launch {
//                        val key = ProductReceiptKey(productId, receiptId)
//                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//                        if (!serialList.contains(serialNumber)) {
//                            serialList.add(serialNumber)
//                            // Proceed to update the backend as required, using the serial number and converted date
//                            odooXmlRpcClient.updateMoveLinesByPicking(receiptId, productId, serialNumber, convertedDate)
//                            // Determine if the match state needs to be updated
//                            val isMatched = serialList.size == productsAdapter.products.find { it.id == productId }?.quantity?.toInt()
//                            updateProductMatchState(productId, receiptId, isMatched, serialList)
//                            withContext(Dispatchers.Main) {
//                                showGreenToast("Serial number added for $productName. ${serialList.size}/${productsAdapter.products.find { it.id == productId }?.quantity?.toInt()} verified")
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                showRedToast("Serial number already entered for $productName")
//                            }
//                        }
//                    }
//                } else {
//                    showRedToast("Invalid expiration date entered. Please use the format DD/MM/YY")
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .create()
//
//        dialog.setOnShowListener {
//            editText.requestFocus()
//            editText.post {
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
//            }
//        }
//
//        dialog.show()
//    }
//
//    private fun isValidDateFormat(value: String): Boolean {
//        return try {
//            // Adjust to check against the "yyyy-MM-dd" format, as that's what your conversion function outputs
//            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
//            true
//        } catch (e: ParseException) {
//            Log.e("YourApp", "Invalid date format: ${e.localizedMessage}")
//            false
//        }
//    }
//
//    private fun convertToFullDateTime(simplifiedDate: String): String {
//        return try {
//            val date = SimpleDateFormat("dd/MM/yy", Locale.US).parse(simplifiedDate)
//            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
//        } catch (e: ParseException) {
//            Log.e("YourApp", "Date parsing error: ${e.localizedMessage}")
//            ""
//        }
//    }
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
////                    Toast.makeText(this, "Quantity entered for $productName", Toast.LENGTH_LONG).show()
//                    showGreenToast("Quantity updated for $productName")
//                    updateProductMatchState(productId, receiptId, true)
//                } else if (!recount) {
//                    promptForProductQuantity(productName, expectedQuantity, receiptId, productId, recount = true)
//                } else {
//                    val localReceiptId = receiptName // Copy the mutable property to a local variable
//
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        if (localReceiptId != null) { // Use the local copy for the check
//                            val buyerDetails = odooXmlRpcClient.fetchAndLogBuyerDetails(localReceiptId)
//                            if (buyerDetails != null) {
//                                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, receiptName, productName) // Pass the local copy to the function
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
//                                showRedToast("Receipt name is null or not found")
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
//
//    private fun updateProductMatchState(
//        productId: Int,
//        receiptId: Int,
//        matched: Boolean = false,
//        serialNumbers: MutableList<String>? = null,
//        lotQuantity: Int? = null
//    ) {
//        val key = ProductReceiptKey(productId, receiptId)
//
//        // Get product and its expected quantity with a fallback to 0 if not found or null
//        val expectedQuantity = productsAdapter.products.find { it.id == productId }?.quantity?.toInt() ?: 0
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
//        val allProductsMatched = checkAllProductsMatched(receiptId)
//
//        // Save match state to preferences
//        saveMatchStateToPreferences(key, quantityMatches[key] == true)
//
//        // Update UI accordingly
//        val position = productsAdapter.findProductPositionById(productId)
//        if (position != -1) {
//            runOnUiThread { productsAdapter.notifyItemChanged(position) }
//        }
//        if (allProductsMatched) {
//            coroutineScope.launch {
//                val validated = odooXmlRpcClient.validateOperation(receiptId)
//                withContext(Dispatchers.Main) {
//                    if (validated) {
////                        Log.d("ProductsActivity", "Receipt validated successfully.")
//                        showGreenToast("Receipt validated")
//                        // Redirect to ReceiptsActivity
//                        val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
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
//
//
//    private fun checkAllProductsMatched(receiptId: Int): Boolean {
//        // Filter the quantityMatches for the current receiptId
//        return quantityMatches.filter { it.key.receiptId == receiptId }.all { it.value }
//    }
//
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
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
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

class ProductsActivity : AppCompatActivity(), ProductsAdapter.OnProductClickListener{
    private lateinit var productsAdapter: ProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button

    //    private var productBarcodes = hashMapOf<String, String>()
    private var productSerialNumbers = hashMapOf<ProductReceiptKey, MutableList<String>>()
    val lotQuantities: MutableMap<ProductReceiptKey, Int> = mutableMapOf()
    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
    private var quantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
    private val confirmedLines = mutableSetOf<Int>()


    private var receiptName: String? = null
    private var receiptId: Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        barcodeInput = findViewById(R.id.barcodeInput)
        confirmButton = findViewById(R.id.confirmButton)
        receiptId = intent.getIntExtra("RECEIPT_ID", -1)
        receiptName = intent.getStringExtra("RECEIPT_NAME")
        Log.d("ProductsActivity", "Received receipt name: $receiptName")

        supportActionBar?.title = receiptName

        productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId, this)

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
        restoreButtonVisibility(receiptId)
        setupBarcodeVerification(receiptId)
        loadMatchStatesFromPreferences(receiptId)
    }
    override fun onResume() {
        super.onResume()
        // Restore visibility state whenever the activity resumes
        restoreButtonVisibility(receiptId)
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
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_flag_receipt -> {
//                AlertDialog.Builder(this).apply {
//                    setTitle("Flag Receipt")
//                    setMessage("Are you sure you want to flag this receipt?")
//                    setPositiveButton("Flag Receipt") { _, _ ->
//                        flagReceipt()
//                    }
//                    setNegativeButton(android.R.string.cancel, null)
//                }.show()
//                true
//            }
//            android.R.id.home -> {
//                onBackPressed()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_flag_receipt -> {
                // Inflate the custom layout for the dialog
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create()

                // Find buttons and set up click listeners
                dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    dialog.dismiss()
                }

                dialogView.findViewById<Button>(R.id.btnFlagPick).setOnClickListener {
                    flagReceipt()
                    dialog.dismiss()
                }

                dialog.show()
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
        val toast = Toast.makeText(this@ProductsActivity, message, Toast.LENGTH_SHORT)
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
        val toast = Toast.makeText(this@ProductsActivity, message, Toast.LENGTH_SHORT)
        val view = toast.view

        // Get the TextView of the default Toast view
        val text = view?.findViewById<TextView>(android.R.id.message)

        // Retrieve the success_green color from resources
        val successGreen = ContextCompat.getColor(this@ProductsActivity, R.color.success_green)

        // Set the background color of the Toast view to success_green
        view?.background?.setColorFilter(successGreen, PorterDuff.Mode.SRC_IN)

        // Set the text color to be more visible on the green background, if needed
        text?.setTextColor(Color.WHITE)

        toast.show()
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
                    captureImage(receiptId)
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
        }

        val session = Session.getDefaultInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication("info@dattec.co.za", "0s3*X4n)#m,z")
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress("info@dattec.co.za"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(buyerEmail))
                subject = "Receipt Flagged for Review"
                setText("""
            Dear $buyerName,
            
            A receipt has been flagged by an application user. 
            The receipt named '$receiptName' has been flagged for review. There may be discrepancies that require your immediate attention.
            
            Regards,
            The Swiib Team
            """.trimIndent())
            }
            Transport.send(message)
            Log.d("EmailSender", "Email sent successfully to $buyerEmail.")
        } catch (e: MessagingException) {
            Log.e("EmailSender", "Failed to send email.", e)
        }
    }

//    private fun setupRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = productsAdapter
//    }
    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
        // Ensure you are passing all necessary parameters in the correct order
        // The parameters should be (List<MoveLine>, Map<ProductPickKey, Boolean>, Map<Int, String>, Int, OnProductClickListener)
       productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId, this)
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

    private fun fetchProductsForReceipt(pickId: Int) {
        coroutineScope.launch {
            try {
                Log.d("PickProductsActivity", "Fetching products for pick ID: $pickId")
                val fetchedLines = odooXmlRpcClient.fetchReceiptMoveLinesByPickingId(pickId)
                val updatedMoveLinesWithDetails = mutableListOf<ReceiptMoveLine>()

                // Fetch additional package information if required
                odooXmlRpcClient.fetchResultPackagesByPickingId(pickId)

                val fetchJobs = fetchedLines.map { moveLines ->
                    coroutineScope.async(Dispatchers.IO) {
                        val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(moveLines.productName) ?: Pair("none", false)
                        val barcode = barcodeToProductIdMap.filterValues { it == moveLines.productId }.keys.firstOrNull()?.toString()

                        moveLines.copy(
                            trackingType = trackingAndExpiration?.first ?: "none",
                            useExpirationDate = trackingAndExpiration.second,
                            barcode = barcode
                        )
                    }
                }

                updatedMoveLinesWithDetails.addAll(fetchJobs.awaitAll())

                // Optionally fetch barcodes for all products in the fetched lines
                fetchBarcodesForProducts(fetchedLines)

                withContext(Dispatchers.Main) {
                    // After all async operations complete, update the UI with the detailed move lines
                    updateUIForProducts(updatedMoveLinesWithDetails, pickId)
                    Log.d("PickProductsActivity", "UI updated with detailed move lines for Pick")
                }
            } catch (e: Exception) {
                Log.e("PickProductsActivity", "Error fetching move lines or updating UI: ${e.localizedMessage}", e)
            }
        }
    }

    private fun fetchBarcodesForProducts(products: List<ReceiptMoveLine>) {
        products.forEach { product ->
            coroutineScope.launch {
                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.productName)
                barcode?.let {
                    // Assuming barcodeToProductIdMap should map barcode to product ID
                    synchronized(this@ProductsActivity) {
                        barcodeToProductIdMap[barcode] = product.productId
                    }
                }
            }
        }
    }

    private fun updateUIForProducts(products: List<ReceiptMoveLine>, receiptId: Int) {
        // Assuming barcodeToProductIdMap and productSerialNumbers are already populated or handled within the fetched products loop
        val newQuantityMatches = products.associate {
            ProductReceiptKey(it.id, receiptId) to (quantityMatches[ProductReceiptKey(it.id, receiptId)] ?: false)
        }.toMutableMap()

        // Now update the quantityMatches and UI accordingly
        quantityMatches.clear()
        quantityMatches.putAll(newQuantityMatches)
        productsAdapter.updateProducts(products, receiptId, quantityMatches)
    }

    //prompt for serial numbers all at once
//    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            if (productId != null) {
//                val productLines = productsAdapter.moveLines.filter { it.productId == productId }
//                Log.d("verifyBarcode", "Product Lines: ${productLines.size}")
//
//                productLines.forEach { productLine ->
//                    // Directly use the trackingType from the productLine object
//                    val trackingType = productLine.trackingType
//                    Log.d("verifyBarcode", "Tracking Type: $trackingType")
//
//                    withContext(Dispatchers.Main) {
//                        // Specific condition for "lot" type products that have been confirmed
//                        if (trackingType == "lot" && confirmedLines.contains(productLine.id)) {
//                            showAddNewLotDialog(productLine.productName, receiptId, productLine.productId, trackingType, productLine.id, productLine.useExpirationDate)
//                        } else {
//                            // Process according to tracking type if not already confirmed
//                            when (trackingType) {
//                                "serial" -> promptForSerialNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
//                                "lot" -> promptForLotNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
//                                "none" -> promptForProductQuantity(productLine.productName, productLine.expectedQuantity, receiptId, productLine.productId, productLine.id, false)
//                                else -> Log.d("verifyBarcode", "Unhandled tracking type: $trackingType")
//                            }
//                        }
//                    }
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    showRedToast("Barcode not found")
//                }
//            }
//        }
//    }
    //prompt for one serial number at a time
//    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            if (productId != null) {
//                val productLines = productsAdapter.moveLines.filter { it.productId == productId }.sortedBy { it.id }
//                val nextProductLine = productLines.find { !confirmedLines.contains(it.id) }
//
//                nextProductLine?.let { productLine ->
//                    val trackingType = productLine.trackingType
//
//                    withContext(Dispatchers.Main) {
//                        if (trackingType == "serial") {
//                            promptForSerialNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
//                            confirmedLines.add(productLine.id)
//                        } else if (trackingType == "lot") {
//                            if (!confirmedLines.contains(productLine.id)) {
//                                promptForLotNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
//                            } else {
//
//                            }
//                        } else {
//                            promptForProductQuantity(productLine.productName, productLine.expectedQuantity, receiptId, productLine.productId, productLine.id, false)
//                        }
//                    }
//                } ?: withContext(Dispatchers.Main) {
//                    showRedToast("All items for this product have been processed or no such product found.")
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    showRedToast("Barcode not found")
//                }
//            }
//        }
//    }
    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
        coroutineScope.launch {
            val productId = barcodeToProductIdMap[scannedBarcode]
            if (productId != null) {
                val productLines = productsAdapter.moveLines.filter { it.productId == productId }.sortedBy { it.id }
                val nextProductLine = productLines.find { !confirmedLines.contains(it.id) }

                nextProductLine?.let { productLine ->
                    val trackingType = productLine.trackingType

                    withContext(Dispatchers.Main) {
                        when {
                            // Specific condition for "lot" type products that have been confirmed
                            trackingType == "lot" && confirmedLines.contains(productLine.id) -> {
                                showAddNewLotDialog(productLine.productName, receiptId, productLine.productId, trackingType, productLine.id, productLine.useExpirationDate)
                            }
                            trackingType == "serial" -> {
                                promptForSerialNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
                                confirmedLines.add(productLine.id)
                            }
                            trackingType == "lot" -> {
                                promptForLotNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
                            }
                            trackingType == "none" -> {
                                promptForProductQuantity(productLine.productName, productLine.expectedQuantity, receiptId, productLine.productId, productLine.id, false)
                            }
                            else -> {
                                Log.d("verifyBarcode", "Unhandled tracking type: $trackingType")
                            }
                        }
                    }
                } ?: withContext(Dispatchers.Main) {
                    showRedToast("All items for this product have been processed or no such product found.")
                }
            } else {
                withContext(Dispatchers.Main) {
                    showRedToast("Barcode not found")
                }
            }
        }
    }

    private fun showAddNewLotDialog(productName: String, receiptId: Int, productId: Int, trackingType: String, lineId: Int, usesExpirationDate: Boolean?) {
        // Inflate the custom layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_lot, null)
//        val textDialogTitle = dialogView.findViewById<TextView>(R.id.textDialogTitle)
        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)

        // Set the dynamic parts of the layout
//        textDialogTitle.text = "Add new lot"
        textDialogMessage.text = "This product has already been confirmed. Would you like to add a new lot for $productName?"

        // Build and show the AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Optional: makes the dialog modal, i.e., it cannot be dismissed by tapping outside of it.
            .create()

        // Find and setup the buttons from the layout
        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)
        val addNewLotButton = dialogView.findViewById<Button>(R.id.buttonAddNewLot)

        // Setup button listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        addNewLotButton.setOnClickListener {
            dialog.dismiss()
            promptForNewLotNumber(productName, receiptId, productId, lineId, trackingType, usesExpirationDate)

        }
        // Display the dialog
        dialog.show()
    }
    private fun promptForNewLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int, trackingType: String, usesExpirationDate: Boolean?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)

        // Retrieve and set the views
        val textEnterLotTitle = dialogView.findViewById<TextView>(R.id.textEnterLotTitle)
        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
        editTextLotNumber.setHintTextColor(Color.WHITE)
        // Update the message to include the product name
        textEnterLotMessage.text = "Enter the lot number for $productName:"

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Optional: makes the dialog modal, i.e., it cannot be dismissed by tapping outside of it.
            .create()

        // Setup button listeners
        buttonCancelLot.setOnClickListener {
            dialog.dismiss()
        }

        buttonConfirmLot.setOnClickListener {
            val enteredLotNumber = editTextLotNumber.text.toString().trim()
            if (enteredLotNumber.isNotEmpty()) {
                showNewLotQuantityDialog(receiptId, productId, productName, lineId, trackingType, enteredLotNumber, usesExpirationDate)
                Log.d("LotNumberInput", "Confirmed lot number: $enteredLotNumber for Product ID: $productId")
                // Optionally update the backend or local state here
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
            }
        }

        // Display the dialog
        dialog.show()
    }

    private fun showNewLotQuantityDialog(receiptId: Int, productId: Int, productName: String, lineId: Int, trackingType: String, lotName: String, usesExpirationDate: Boolean?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_quantity_input, null)

        val textEnterLotQuantityTitle = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityTitle)
        val textEnterLotQuantityMessage = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityMessage)
        val editTextLotQuantity = dialogView.findViewById<EditText>(R.id.editTextLotQuantity)
        val buttonCancelQuantity = dialogView.findViewById<Button>(R.id.buttonCancelQuantity)
        val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
        editTextLotQuantity.setHintTextColor(Color.WHITE)

        // Setting dynamic text for the lot name
        textEnterLotQuantityMessage.text = "Enter quantity for lot: $lotName"

        // Create the AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set the button handlers
        buttonCancelQuantity.setOnClickListener {
            dialog.dismiss()  // Dismiss the dialog without taking action
        }

        buttonConfirmQuantity.setOnClickListener {
            val enteredQuantity = editTextLotQuantity.text.toString().toDoubleOrNull()
            if (enteredQuantity != null) {
                // Check if usesExpirationDate is true or false
                if (!usesExpirationDate!!) {
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.Main) {
                                odooXmlRpcClient.createMoveLineForReceiving(receiptId, productId, lotName, enteredQuantity)
                                dialog.dismiss()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {

                            }
                        }
                    }
                } else {
                    // Call the function to prompt for the expiration date
                    promptForNewLotExpirationDate(productName, receiptId, productId, lotName, enteredQuantity, lineId)
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Invalid quantity entered. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Show the dialog
        dialog.show()
    }


    private fun promptForNewLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotName: String, quantity: Double, lineId: Int) {
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
//                        (pickingId: Int, productId: Int, lotName: String, quantity: Double, expirationDate: String)
                        odooXmlRpcClient.createMoveLineWithExpirationForReceiving(receiptId, productId, lotName, quantity, convertedDate).also {
                            updateProductMatchState(lineId, receiptId, matched = true)
                            confirmedLines.add(lineId)
                            withContext(Dispatchers.Main) {
                                showGreenToast("Lot expiration date updated")
                            }
                        }
                    }
                } else {
                    showRedToast("Invalid expiration date entered. Please use the format DD/MM/YY")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
//    private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter lot number"
//        }
//        editText.setHintTextColor(Color.WHITE)
//        var actionExecuted = false
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Enter Lot Number")
//            .setMessage("Enter the lot number for $productName.")
//            .setView(editText)
//            .setPositiveButton("OK") { _, _ ->
//                if (!actionExecuted) {
//                    actionExecuted = true
//                    val enteredLotNumber = editText.text.toString().trim()
//                    if (enteredLotNumber.isNotEmpty()) {
//                        coroutineScope.launch {
//                            val product = productsAdapter.moveLines.find { it.productId == productId }
//                            if (product?.useExpirationDate == true) {
//                                withContext(Dispatchers.Main) {
//                                    // Assume promptForLotQuantity is correctly defined elsewhere to handle these parameters
//                                    promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, true, lineId)
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    // Assume promptForLotQuantity is correctly defined elsewhere to handle these parameters
//                                    promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, false, lineId)
//                                }
//                            }
//                        }
//                    } else {
//                        showRedToast("Please enter a lot number.")
//                    }
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .create()
//
//        // Request focus and show the keyboard when the dialog is shown
//        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
//        dialog.setOnShowListener {
//            editText.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        editText.setOnEditorActionListener { _, actionId, event ->
//            if ((actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) && !actionExecuted) {
//                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.performClick()
//                true
//            } else {
//                false
//            }
//        }
//
//        dialog.show()
//    }
    private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
        // Inflate the custom layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)

        textEnterLotMessage.text = "Enter the lot number for $productName."
        editTextLotNumber.setHintTextColor(Color.WHITE)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // This disables dismissing the dialog by clicking outside it.
            .create()

        // Handle the Cancel button
        buttonCancelLot.setOnClickListener {
            dialog.dismiss()
        }

        // Handle the Confirm button
        buttonConfirmLot.setOnClickListener {
            val enteredLotNumber = editTextLotNumber.text.toString().trim()
            if (enteredLotNumber.isNotEmpty()) {
                coroutineScope.launch {
                    val product = productsAdapter.moveLines.find { it.productId == productId }
                    if (product?.useExpirationDate == true) {
                        withContext(Dispatchers.Main) {
                            // Assuming promptForLotQuantity is correctly defined elsewhere to handle these parameters
                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, true, lineId)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            // Assuming promptForLotQuantity is correctly defined elsewhere to handle these parameters
                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, false, lineId)
                        }
                    }
                }
            } else {
                showRedToast("Please enter a lot number.")
            }
            dialog.dismiss()
        }

        // Request focus and show the keyboard when the dialog is shown
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener {
            editTextLotNumber.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.show()
    }

    private fun promptForLotQuantity(productName: String, receiptId: Int, productId: Int, lotNumber: String, requiresExpirationDate: Boolean, lineId: Int) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter quantity"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter Quantity")
            .setMessage("Enter the quantity for the lot of $productName.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val enteredQuantity = editText.text.toString().toIntOrNull()
                if (enteredQuantity != null) {
                    coroutineScope.launch {
                        if (requiresExpirationDate) {
                            withContext(Dispatchers.Main) {
                                // Logic to prompt for lot expiration date
                                promptForLotExpirationDate(productName, receiptId, productId, lotNumber, enteredQuantity, lineId)
                            }
                        } else {
                            // Logic for handling the quantity update without expiration date
                            odooXmlRpcClient.updateMoveLineLotAndQuantity(lineId, receiptId, productId, lotNumber, enteredQuantity)
                            confirmedLines.add(lineId)
                            updateProductMatchState(lineId, receiptId, matched = true)
                            withContext(Dispatchers.Main) {
                                showGreenToast("Quantity updated for lot without expiration date.")
                            }
                        }
                    }
                } else {
                    // Show toast message for invalid quantity
                    Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show()
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

    private fun promptForLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotNumber: String, quantity: Int, lineId: Int) {
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
                            odooXmlRpcClient.updateMoveLineLotExpiration(lineId, receiptId, productId, lotNumber, quantity, convertedDate).also {
                            updateProductMatchState(lineId, receiptId, matched = true)
                            confirmedLines.add(lineId)
                            withContext(Dispatchers.Main) {
                                showGreenToast("Lot expiration date updated")
                            }
                        }
                    }
                } else {
                    showRedToast("Invalid expiration date entered. Please use the format DD/MM/YY")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

//    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
//        val editText = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_TEXT
//            hint = "Enter serial number"
//        }
//
//        var actionExecuted = false
//
//        // Build the dialog but don't show it yet
//        val dialogBuilder = AlertDialog.Builder(this)
//            .setTitle("Enter Serial Number")
//            .setMessage("Enter the serial number for $productName.")
//            .setView(editText)
//            .setNegativeButton("Cancel", null)
//
//        // Create the dialog from the builder
//        val dialog = dialogBuilder.create()
//
//        // Now set the positive button separately to have access to 'dialog' variable
//        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
//            if (!actionExecuted) {
//                actionExecuted = true
//                val enteredSerialNumber = editText.text.toString().trim()
//                if (enteredSerialNumber.isNotEmpty()) {
//                    coroutineScope.launch {
//                        val product = productsAdapter.moveLines.find { it.productId == productId }
//                        val key = ProductReceiptKey(productId, receiptId)
//                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
//
//                        if (!serialList.contains(enteredSerialNumber)) {
//                            if (product?.useExpirationDate == true) {
//                                withContext(Dispatchers.Main) {
//                                    dialog.dismiss() // Correctly reference 'dialog' here
//                                    promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber, lineId)
//                                }
//                            } else {
//                                odooXmlRpcClient.updateMoveLineSerialNumber(lineId, receiptId, productId, enteredSerialNumber)
//                                serialList.add(enteredSerialNumber)
//                                updateProductMatchState(lineId, receiptId, true)
//                                confirmedLines.add(lineId)
//
//                                withContext(Dispatchers.Main) {
//                                    showGreenToast("Serial number added for $productName.")
//                                }
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                showRedToast("Serial number already entered for $productName")
//                            }
//                        }
//                    }
//                } else {
//                    showRedToast("Please enter a serial number")
//                }
//            }
//        }
//
//        // Set up dialog properties related to keyboard input
//        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
//        dialog.setOnShowListener {
//            editText.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        editText.setOnEditorActionListener { _, actionId, event ->
//            if ((actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) && !actionExecuted) {
//                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
//                true
//            } else {
//                false
//            }
//        }
//
//        dialog.show()
//    }
    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvMessage)

        // Set the message dynamically
        tvMessage.text = "Product: $productName."

        var actionExecuted = false

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
            .create()

        // Now set the positive button separately to have access to 'dialog' variable
        buttonConfirmSN.setOnClickListener {
            if (!actionExecuted) {
                actionExecuted = true
                val enteredSerialNumber = serialNumberInput.text.toString().trim()
                if (enteredSerialNumber.isNotEmpty()) {
                    coroutineScope.launch {
                        val product = productsAdapter.moveLines.find { it.productId == productId }
                        val key = ProductReceiptKey(productId, receiptId)
                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }

                        if (!serialList.contains(enteredSerialNumber)) {
                            if (product?.useExpirationDate == true) {
                                withContext(Dispatchers.Main) {
                                    dialog.dismiss() // Correctly reference 'dialog' here
                                    promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber, lineId)
                                }
                            } else {
                                odooXmlRpcClient.updateMoveLineSerialNumber(lineId, receiptId, productId, enteredSerialNumber)
                                serialList.add(enteredSerialNumber)
                                updateProductMatchState(lineId, receiptId, true)
                                confirmedLines.add(lineId)

                                withContext(Dispatchers.Main) {
                                    showGreenToast("Serial number added for $productName.")
                                    dialog.dismiss()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showRedToast("Serial number already entered for $productName")
                                serialNumberInput.setText("")
                                serialNumberInput.requestFocus()
                            }
                        }
                    }
                } else {
                    showRedToast("Please enter a serial number")
                    serialNumberInput.setText("")
                    serialNumberInput.requestFocus()
                }
            }
        }

        buttonCancelSN.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        serialNumberInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
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

    private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String, lineId: Int) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER // Adjusted for numeric input, setupDateInputField will handle formatting
            hint = "Enter expiration date (dd/MM/yyyy)"
        }

        // Setup the EditText for date input
        setupDateInputField(editText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter Expiration Date")
            .setMessage("Enter the expiration date for $productName.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val enteredExpirationDate = editText.text.toString().trim()
                val convertedDate = convertToFullDateTime(enteredExpirationDate) // Ensure this conversion to full date
                if (isValidDateFormat(convertedDate)) {
                    coroutineScope.launch {
                        val key = ProductReceiptKey(lineId, receiptId)
                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
                        if (!serialList.contains(serialNumber)) {
                            serialList.add(serialNumber)
                            // Proceed to update the backend as required, using the serial number and converted date
                            odooXmlRpcClient.updateMoveLineSerialExpirationDate(lineId, receiptId, productId, serialNumber, convertedDate )
                            confirmedLines.add(lineId)
                            // Determine if the match state needs to be updated
                            val isMatched = serialList.size == productsAdapter.moveLines.find { it.productId == productId }?.quantity?.toInt()
                            updateProductMatchState(lineId, receiptId, isMatched)
                            withContext(Dispatchers.Main) {
                                showGreenToast("Serial number added for $productName.")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showRedToast("Serial number already entered for $productName")
                            }
                        }
                    }
                } else {
                    showRedToast("Invalid expiration date entered. Please use the format DD/MM/YY")
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            editText.requestFocus()
            editText.post {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
            }
        }

        dialog.show()
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


    private fun promptForProductQuantity(productName: String, expectedQuantity: Double, receiptId: Int, productId: Int, lineId: Int, recount: Boolean = false) {
        // Inflate the custom layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_input, null)
        val editText = dialogView.findViewById<EditText>(R.id.quantityInput)
        val titleTextView = dialogView.findViewById<TextView>(R.id.titleTextView)
        titleTextView.text = if (recount) "Recount Required for $productName" else "Enter Quantity for $productName"

        editText.setHintTextColor(Color.WHITE)

        // Setup AlertDialog with custom view but no buttons
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Disable dismissing the dialog by pressing back or touching outside
            .create()

        // Find and setup the custom buttons
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)

        // Cancel button logic
        cancelButton.setOnClickListener {
            dialog.dismiss() // This dismisses the dialog
        }

        // Confirm button logic
        confirmButton.setOnClickListener {
            val enteredQuantity = editText.text.toString().toDoubleOrNull()
            if (enteredQuantity != null) {
                // Always update the quantity, regardless of its correctness
                lifecycleScope.launch(Dispatchers.IO) {
                    odooXmlRpcClient.updateMoveLineQuantityUntracked(
                        lineId,
                        receiptId,
                        productId,
                        enteredQuantity
                    )
                }

                // Check if the quantity is correct
                if (enteredQuantity == expectedQuantity) {
                    showGreenToast("Quantity updated for $productName")
                    updateProductMatchState(lineId, receiptId, true)
                    confirmedLines.add(lineId)
                } else if (!recount) {
                    promptForProductQuantity(productName, expectedQuantity, receiptId, productId, lineId, recount = true)
                } else {
                    handleIncorrectRecount(productName, receiptId)
                }

                dialog.dismiss() // Dismiss the dialog after confirming
            } else {
                Toast.makeText(this, "Invalid quantity entered", Toast.LENGTH_SHORT).show()
            }
        }

        // Display the dialog
        dialog.show()
    }

    private fun handleIncorrectRecount(productName: String, receiptId: Int) {
        val localReceiptId = receiptName  // Copy the mutable property to a local variable

        lifecycleScope.launch(Dispatchers.IO) {
            val buyerDetails = localReceiptId?.let { odooXmlRpcClient.fetchAndLogBuyerDetails(it) }
            if (buyerDetails != null) {
                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, localReceiptId, productName) // Pass the local copy to the function
                withContext(Dispatchers.Main) {
                    showRedToast("Flagged ${buyerDetails.login}. Email sent.")
                }
            } else {
                withContext(Dispatchers.Main) {
                    showRedToast("Flagged, but buyer details not found.")
                }
            }
        }
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

                - Receipt Name: $receiptName
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

    private fun updateProductMatchState(
        lineId: Int,
        pickId: Int,
        matched: Boolean = true,
    ) {
        val key = ProductReceiptKey(lineId, pickId)
        val productLine = productsAdapter.moveLines.find { it.id == lineId }

        if (productLine != null) {
            quantityMatches[key] = matched

            // Refresh the UI to reflect the updated state
            runOnUiThread {
                val position = productsAdapter.findProductPositionById(lineId)
                if (position != -1) {
                    productsAdapter.notifyItemChanged(position)
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
                            val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
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
                        val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
                        startActivity(intent)
                        finish()  // Optionally call finish() if you want to remove this activity from the back stack
                    } else {
                        Toast.makeText(applicationContext, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    private fun saveMatchStateToPreferences(key: ProductReceiptKey, matched: Boolean) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("${key.moveLineId}_${key.pickId}", matched)
            apply()
        }
    }


    private fun loadMatchStatesFromPreferences(pickId: Int) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        val tempQuantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()

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
                                val key = ProductReceiptKey(moveLineId, prefPickId)
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
            productsAdapter.updateProducts(productsAdapter.moveLines, pickId, quantityMatches)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onProductClick(product: ReceiptMoveLine) {
        showProductDialog(product)
    }

//    private fun showProductDialog(product: ReceiptMoveLine) {
//        // Inflate the custom layout for the dialog
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.receipt_product_details_dialog, null)
//
//        // Retrieve all the TextViews and other views
//        val textProductName = dialogView.findViewById<TextView>(R.id.textProductName)
//        val textProductQuantity = dialogView.findViewById<TextView>(R.id.textProductQuantity)
//        val textProductToLocation = dialogView.findViewById<TextView>(R.id.textProductToLocation)
//        val textProductLotNumber = dialogView.findViewById<TextView>(R.id.textProductLotNumber)
//        val editTextProductLotNumber = dialogView.findViewById<EditText>(R.id.editTextProductLotNumber)
//        val lotNumberLayout = dialogView.findViewById<LinearLayout>(R.id.lotNumberLayout)
//        val buttonEditLotNumber = dialogView.findViewById<ImageButton>(R.id.buttonEditLotNumber)
//        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
//        val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
//        val editTextQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)
//        val buttonEditQuantity = dialogView.findViewById<ImageButton>(R.id.buttonEditQuantity)
//
//        // Create and show the dialog
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//        dialog.show()
//
//        // Set values to the TextViews
//        textProductName.text = product.productName
//        textProductQuantity.text = "${product.quantity}"
//        textProductToLocation.text = "${product.locationDestName}"
//        textProductLotNumber.text = "${product.lotName}"
//
//        // Initially set the visibility to avoid any flashing or incorrect states
//        lotNumberLayout.visibility = View.GONE
//        buttonEditLotNumber.visibility = View.GONE
//        buttonEditQuantity.visibility = View.GONE
//
//        coroutineScope.launch {
//            withContext(Dispatchers.Main) {
//                // Default initial settings before checking specific conditions
//                lotNumberLayout.visibility = View.GONE
//                buttonEditLotNumber.visibility = View.GONE
//                buttonEditQuantity.visibility = View.VISIBLE  // Default to visible unless conditions dictate otherwise
//
//                if (product.trackingType == "serial") {
//                    // For 'serial' type, always hide the quantity edit button
//                    buttonEditQuantity.visibility = View.GONE
//                    if (product.lotName.isNotEmpty()) {
//                        // Only show lot editing if lot name is not empty
//                        lotNumberLayout.visibility = View.VISIBLE
//                        buttonEditLotNumber.visibility = View.VISIBLE
//                    }
//                } else if (product.trackingType == "lot" && product.lotName.isNotEmpty()) {
//                    // For 'lot' type with a non-empty lot name, enable editing
//                    lotNumberLayout.visibility = View.VISIBLE
//                    buttonEditLotNumber.visibility = View.VISIBLE
//                    buttonEditQuantity.visibility = View.VISIBLE
//                } else if (product.trackingType == "none") {
//                    // For 'none' tracking type, hide all lot number editing UI
//                    lotNumberLayout.visibility = View.GONE
//                    buttonEditLotNumber.visibility = View.GONE
//                } else {
//                    // Default to visibility but not interactable if conditions are not met
//                    lotNumberLayout.visibility = View.VISIBLE
//                    buttonEditLotNumber.visibility = View.GONE
//                    buttonEditQuantity.visibility = View.VISIBLE
//                }
//            }
//        }
//
//        // Setup button to toggle editing of quantity
//        buttonEditQuantity.setOnClickListener {
//            if (editTextQuantity.visibility == View.GONE) {
//                editTextQuantity.visibility = View.VISIBLE
//                editTextQuantity.setText(product.quantity.toString())
//                textProductQuantity.visibility = View.GONE
//            } else {
//                editTextQuantity.visibility = View.GONE
//                textProductQuantity.visibility = View.VISIBLE
//                textProductQuantity.text = "Quantity: ${editTextQuantity.text}"
//            }
//        }
//        // Configure buttonEditLotNumber listener
//        buttonEditLotNumber.setOnClickListener {
//            if (editTextProductLotNumber.visibility == View.GONE) {
//                editTextProductLotNumber.visibility = View.VISIBLE
//                editTextProductLotNumber.setText(product.lotName)
//                textProductLotNumber.visibility = View.GONE
//            } else {
//                editTextProductLotNumber.visibility = View.GONE
//                textProductLotNumber.visibility = View.VISIBLE
//                textProductLotNumber.text = editTextProductLotNumber.text.toString()
//            }
//        }
//        buttonConfirmQuantity.setOnClickListener {
//            dialog.dismiss()
//        }
//        buttonCancel.setOnClickListener {
//            dialog.dismiss()
//        }
//    }

    private fun showProductDialog(product: ReceiptMoveLine) {
        // Inflate the custom layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.receipt_product_details_dialog, null)

        // Retrieve all the TextViews and other views
        val textProductName = dialogView.findViewById<TextView>(R.id.textProductName)
        val textProductQuantity = dialogView.findViewById<TextView>(R.id.textProductQuantity)
        val textProductToLocation = dialogView.findViewById<TextView>(R.id.textProductToLocation)
        val textProductLotNumber = dialogView.findViewById<TextView>(R.id.textProductLotNumber)
        val editTextProductLotNumber = dialogView.findViewById<EditText>(R.id.editTextProductLotNumber)
        val lotNumberLayout = dialogView.findViewById<LinearLayout>(R.id.lotNumberLayout)
        val buttonEditLotNumber = dialogView.findViewById<ImageButton>(R.id.buttonEditLotNumber)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
        val editTextQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)
        val buttonEditQuantity = dialogView.findViewById<ImageButton>(R.id.buttonEditQuantity)

        // Set values to the TextViews
        textProductName.text = product.productName
        textProductQuantity.text = "${product.quantity}"
        textProductToLocation.text = "${product.locationDestName}"
        textProductLotNumber.text = "${product.lotName}"

        // Flags to track if edits were made
        var quantityChanged = false
        var lotNameChanged = false

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.show()

        // Setup visibility based on product tracking type
        lotNumberLayout.visibility = View.GONE
        buttonEditLotNumber.visibility = View.GONE
        buttonEditQuantity.visibility = View.GONE

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                if (product.trackingType == "serial") {
                    buttonEditQuantity.visibility = View.GONE
                    if (product.lotName.isNotEmpty()) {
                        lotNumberLayout.visibility = View.VISIBLE
                        buttonEditLotNumber.visibility = View.VISIBLE
                    }
                } else if (product.trackingType == "lot" && product.lotName.isNotEmpty()) {
                    lotNumberLayout.visibility = View.VISIBLE
                    buttonEditLotNumber.visibility = View.VISIBLE
                    buttonEditQuantity.visibility = View.VISIBLE
                } else if (product.trackingType == "none") {
                    lotNumberLayout.visibility = View.GONE
                    buttonEditLotNumber.visibility = View.GONE
                } else {
                    lotNumberLayout.visibility = View.VISIBLE
                    buttonEditLotNumber.visibility = View.GONE
                    buttonEditQuantity.visibility = View.VISIBLE
                }
            }
        }

        // Button handlers for editing quantity and lot number
        buttonEditQuantity.setOnClickListener {
            editTextQuantity.visibility = View.VISIBLE
            editTextQuantity.setText(product.quantity.toString())
            textProductQuantity.visibility = View.GONE
            quantityChanged = true
        }
        buttonEditLotNumber.setOnClickListener {
            editTextProductLotNumber.visibility = View.VISIBLE
            editTextProductLotNumber.setText(product.lotName)
            textProductLotNumber.visibility = View.GONE
            lotNameChanged = true
        }

        // Confirm and cancel button handling
        buttonConfirmQuantity.setOnClickListener {
            coroutineScope.launch {
                when {
                    quantityChanged && lotNameChanged -> {
                        odooXmlRpcClient.updateMoveLineQuantityForReceipt(product.id, receiptId, editTextQuantity.text.toString().toInt())
                        odooXmlRpcClient.updateMoveLinesForReceipt(product.id, receiptId, editTextProductLotNumber.text.toString())
                    }
                    quantityChanged -> odooXmlRpcClient.updateMoveLineQuantityForReceipt(product.id, receiptId, editTextQuantity.text.toString().toInt())
                    lotNameChanged -> odooXmlRpcClient.updateMoveLinesForReceipt(product.id, receiptId, editTextProductLotNumber.text.toString())
                }
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                }
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 1001
    }

    private fun captureImage(pickId: Int) {
        // Inflate the custom layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.capture_image_dialog, null)

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Find buttons and set up click listeners
        dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCaptureImage).setOnClickListener {
            dialog.dismiss()
            openCamera()
        }

        dialog.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, PickProductsActivity.CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PickProductsActivity.CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

                Log.d("CaptureImage", "Encoded image: $encodedImage") // Log the encoded string or its length

                coroutineScope.launch {
                    val updateResult = odooXmlRpcClient.updatePickingImage(receiptId, encodedImage)
                    Log.d("OdooUpdate", "Update result: $updateResult") // Log the result from the server
                }
            } else {
                Log.e("CaptureImage", "Failed to capture image")
            }
        }
    }

}

