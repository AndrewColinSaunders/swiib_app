//package com.example.warehousetet
//
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.graphics.PorterDuff
//import android.graphics.Typeface
//import android.os.Bundle
//import android.provider.MediaStore
//import android.text.Editable
//import android.text.InputType
//import android.text.Spannable
//import android.text.SpannableString
//import android.text.TextWatcher
//import android.text.style.ForegroundColorSpan
//import android.text.style.StyleSpan
//import android.util.Base64
//import android.util.Log
//import android.view.KeyEvent
//import android.view.LayoutInflater
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.view.WindowManager
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageButton
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import kotlinx.coroutines.*
//import java.io.ByteArrayOutputStream
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
//class ProductsActivity : AppCompatActivity(), ProductsAdapter.OnProductClickListener{
//    private lateinit var productsAdapter: ProductsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var barcodeInput: EditText
//    private lateinit var confirmButton: Button
//
//    //    private var productBarcodes = hashMapOf<String, String>()
////    private var productSerialNumbers = hashMapOf<ProductReceiptKey, MutableList<String>>()
////    private var productLotNumbers = hashMapOf<ProductReceiptKey, MutableList<String>>()
//
//    private var pickSerialNumbers = hashMapOf<Int, MutableList<String>>()
//    private var pickLotNumbers = hashMapOf<Int, MutableList<String>>()
//
//    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
//    private var quantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//    private val confirmedLines = mutableSetOf<Int>()
//
//    private var receiptName: String? = null
//    private var receiptId: Int = -1
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_products)
//
//        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
//        barcodeInput = findViewById(R.id.barcodeInput)
//        confirmButton = findViewById(R.id.confirmButton)
//        receiptId = intent.getIntExtra("RECEIPT_ID", -1)
//        receiptName = intent.getStringExtra("RECEIPT_NAME")
//        Log.d("ProductsActivity", "Received receipt name: $receiptName")
//
//        supportActionBar?.title = receiptName
//
//        productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId, this)
//
//        if (receiptId != -1) {
//            setupRecyclerView()
//            coroutineScope.launch { fetchProductsForReceipt(receiptId) }
//
//        } else {
//            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity.")
//        }
//
//        findViewById<Button>(R.id.clearButton).setOnClickListener {
//            findViewById<EditText>(R.id.barcodeInput).text.clear()
//        }
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        restoreButtonVisibility(receiptId)
//        setupBarcodeVerification(receiptId)
//        loadMatchStatesFromPreferences(receiptId)
//    }
//    override fun onResume() {
//        super.onResume()
//        // Restore visibility state whenever the activity resumes
//        restoreButtonVisibility(receiptId)
//    }
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
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_flag_receipt -> {
//                // Inflate the custom layout for the dialog
//                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, null)
//                val dialog = AlertDialog.Builder(this)
//                    .setView(dialogView)
//                    .create()
//
//                // Find buttons and set up click listeners
//                dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
//                    dialog.dismiss()
//                }
//
//                dialogView.findViewById<Button>(R.id.btnFlagPick).setOnClickListener {
//                    flagReceipt()
//                    dialog.dismiss()
//                }
//
//                dialog.show()
//                true
//            }
//            android.R.id.home -> {
//                onBackPressedDispatcher.onBackPressed()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
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
//                    captureImage(receiptId)
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
//        // Ensure you are passing all necessary parameters in the correct order
//        // The parameters should be (List<MoveLine>, Map<ProductPickKey, Boolean>, Map<Int, String>, Int, OnProductClickListener)
//       productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId, this)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = productsAdapter
//    }
//
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
////    private fun fetchProductsForReceipt(pickId: Int) {
////        coroutineScope.launch {
////            try {
////                Log.d("PickProductsActivity", "Fetching products for pick ID: $pickId")
////                val fetchedLines = odooXmlRpcClient.fetchReceiptMoveLinesByPickingId(pickId)
////                val updatedMoveLinesWithDetails = mutableListOf<ReceiptMoveLine>()
////
////                // Fetch additional package information if required
////                odooXmlRpcClient.fetchResultPackagesByPickingId(pickId)
////
////                val fetchJobs = fetchedLines.map { moveLines ->
////                    coroutineScope.async(Dispatchers.IO) {
////                        val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(moveLines.productName) ?: Pair("none", false)
////                        val barcode = barcodeToProductIdMap.filterValues { it == moveLines.productId }.keys.firstOrNull()?.toString()
////
////                        moveLines.copy(
////                            trackingType = trackingAndExpiration?.first ?: "none",
////                            useExpirationDate = trackingAndExpiration.second,
////                            barcode = barcode
////                        )
////                    }
////                }
////
////                updatedMoveLinesWithDetails.addAll(fetchJobs.awaitAll())
////
////                // Optionally fetch barcodes for all products in the fetched lines
////                fetchBarcodesForProducts(fetchedLines)
////
////                withContext(Dispatchers.Main) {
////                    // After all async operations complete, update the UI with the detailed move lines
////                    updateUIForProducts(updatedMoveLinesWithDetails, pickId)
////                    Log.d("PickProductsActivity", "UI updated with detailed move lines for Pick")
////                }
////            } catch (e: Exception) {
////                Log.e("PickProductsActivity", "Error fetching move lines or updating UI: ${e.localizedMessage}", e)
////            }
////        }
////    }
//    private suspend fun fetchProductsForReceipt(pickId: Int) {
//        try {
//            Log.d("PickProductsActivity", "Fetching products for pick ID: $pickId")
//            val fetchedLines = odooXmlRpcClient.fetchReceiptMoveLinesByPickingId(pickId)
//            val updatedMoveLinesWithDetails = mutableListOf<ReceiptMoveLine>()
//
//            // Optionally fetch additional package information if required
//            odooXmlRpcClient.fetchResultPackagesByPickingId(pickId)
//
//            // Fetch product tracking and expiration details asynchronously
//            val fetchJobs = fetchedLines.map { moveLine ->
//                coroutineScope.async(Dispatchers.IO) {
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(moveLine.productName) ?: Pair("none", false)
//                    val barcode = barcodeToProductIdMap.filterValues { it == moveLine.productId }.keys.firstOrNull()?.toString()
//
//                    moveLine.copy(
//                        trackingType = trackingAndExpiration.first ?: "none",
//                        useExpirationDate = trackingAndExpiration.second,
//                        barcode = barcode
//                    )
//                }
//            }
//
//            updatedMoveLinesWithDetails.addAll(fetchJobs.awaitAll())
//
//            // Optionally fetch barcodes for all products in the fetched lines
//            fetchBarcodesForProducts(fetchedLines)
//
//            // After all async operations complete, update the UI on the main thread
//            withContext(Dispatchers.Main) {
//                // Call updateUIForProducts instead of directly updating productsAdapter to maintain abstraction
//                updateUIForProducts(updatedMoveLinesWithDetails, pickId)
//                Log.d("PickProductsActivity", "UI updated with detailed move lines for Pick ID: $pickId")
//            }
//        } catch (e: Exception) {
//            Log.e("PickProductsActivity", "Error fetching move lines or updating UI: ${e.localizedMessage}", e)
//        }
//    }
//
//
//
//    private fun fetchBarcodesForProducts(products: List<ReceiptMoveLine>) {
//        products.forEach { product ->
//            coroutineScope.launch {
//                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.productName)
//                barcode?.let {
//                    // Assuming barcodeToProductIdMap should map barcode to product ID
//                    synchronized(this@ProductsActivity) {
//                        barcodeToProductIdMap[barcode] = product.productId
//                    }
//                }
//            }
//        }
//    }
//
//    private fun updateUIForProducts(products: List<ReceiptMoveLine>, receiptId: Int) {
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
//    //prompt for serial numbers all at once
////    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
////        coroutineScope.launch {
////            val productId = barcodeToProductIdMap[scannedBarcode]
////            if (productId != null) {
////                val productLines = productsAdapter.moveLines.filter { it.productId == productId }
////                Log.d("verifyBarcode", "Product Lines: ${productLines.size}")
////
////                productLines.forEach { productLine ->
////                    // Directly use the trackingType from the productLine object
////                    val trackingType = productLine.trackingType
////                    Log.d("verifyBarcode", "Tracking Type: $trackingType")
////
////                    withContext(Dispatchers.Main) {
////                        // Specific condition for "lot" type products that have been confirmed
////                        if (trackingType == "lot" && confirmedLines.contains(productLine.id)) {
////                            showAddNewLotDialog(productLine.productName, receiptId, productLine.productId, trackingType, productLine.id, productLine.useExpirationDate)
////                        } else {
////                            // Process according to tracking type if not already confirmed
////                            when (trackingType) {
////                                "serial" -> promptForSerialNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
////                                "lot" -> promptForLotNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
////                                "none" -> promptForProductQuantity(productLine.productName, productLine.expectedQuantity, receiptId, productLine.productId, productLine.id, false)
////                                else -> Log.d("verifyBarcode", "Unhandled tracking type: $trackingType")
////                            }
////                        }
////                    }
////                }
////            } else {
////                withContext(Dispatchers.Main) {
////                    showRedToast("Barcode not found")
////                }
////            }
////        }
////    }
//    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            if (productId != null) {
//                val productLines = productsAdapter.moveLines.filter { it.productId == productId }.sortedBy { it.id }
//
//                // Check for any confirmed lot lines first to handle them appropriately
//                val confirmedLotProductLine = productLines.find { it.trackingType == "lot" && confirmedLines.contains(it.id) }
//                if (confirmedLotProductLine != null) {
//                    withContext(Dispatchers.Main) {
//                        showAddNewLotDialog(confirmedLotProductLine.productName, receiptId, confirmedLotProductLine.productId, confirmedLotProductLine.trackingType, confirmedLotProductLine.id, confirmedLotProductLine.useExpirationDate)
//                    }
//                    return@launch
//                }
//
//                // Handle other cases with unconfirmed lines
//                val nextProductLine = productLines.find { !confirmedLines.contains(it.id) }
//                nextProductLine?.let { productLine ->
//                    val trackingType = productLine.trackingType
//                    withContext(Dispatchers.Main) {
//                        when (trackingType) {
//                            "serial" -> {
//                                promptForSerialNumber(
//                                    productLine.productName,
//                                    receiptId,
//                                    productLine.productId,
//                                    productLine.id
//                                )
//                            }
//                            "lot" -> {
//                                promptForLotNumber(
//                                    productLine.productName,
//                                    receiptId,
//                                    productLine.productId,
//                                    productLine.id
//                                )
//                            }
//                            "none" -> {
//                                promptForProductQuantity(
//                                    productLine.productName,
//                                    productLine.expectedQuantity,
//                                    receiptId,
//                                    productLine.productId,
//                                    productLine.id,
//                                    false
//                                )
//                            }
//                            else -> {
//                                Log.d("verifyBarcode", "Unhandled tracking type: $trackingType")
//                            }
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
//
//
//    private fun showAddNewLotDialog(productName: String, receiptId: Int, productId: Int, trackingType: String, lineId: Int, usesExpirationDate: Boolean?) {
//        // Inflate the custom layout for the dialog
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_lot, null)
////        val textDialogTitle = dialogView.findViewById<TextView>(R.id.textDialogTitle)
//        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
//
//        // Set the dynamic parts of the layout
////        textDialogTitle.text = "Add new lot"
//        textDialogMessage.text = "This product has already been confirmed. Would you like to add a new lot for $productName?"
//
//        // Build and show the AlertDialog
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false) // Optional: makes the dialog modal, i.e., it cannot be dismissed by tapping outside of it.
//            .create()
//
//        // Find and setup the buttons from the layout
//        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)
//        val addNewLotButton = dialogView.findViewById<Button>(R.id.buttonAddNewLot)
//
//        // Setup button listeners
//        cancelButton.setOnClickListener {
//            dialog.dismiss()
//        }
//        addNewLotButton.setOnClickListener {
//            dialog.dismiss()
//            promptForNewLotNumber(productName, receiptId, productId, lineId, trackingType, usesExpirationDate)
//
//        }
//        // Display the dialog
//        dialog.show()
//    }
//
////    private fun promptForNewLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int, trackingType: String, usesExpirationDate: Boolean?) {
////        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
////
////        // Retrieve and set the views
////        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
////        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
////        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
////        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
////        editTextLotNumber.setHintTextColor(Color.WHITE)
////        textEnterLotMessage.text = "Enter the lot number for $productName:"
////
////        val dialog = AlertDialog.Builder(this)
////            .setView(dialogView)
////            .setCancelable(false)
////            .create()
////
////        buttonConfirmLot.setOnClickListener {
////            val enteredLotNumber = editTextLotNumber.text.toString().trim()
////            if (enteredLotNumber.isNotEmpty()) {
////                coroutineScope.launch {
////                    val key = ProductReceiptKey(productId, receiptId)
////                    val lotList = productLotNumbers.getOrPut(key) { mutableListOf() }
////
////                    if (!lotList.contains(enteredLotNumber)) {
////                        lotList.add(enteredLotNumber)  // Add the new lot number to the list
////                        withContext(Dispatchers.Main) {
////                            dialog.dismiss()
////                            showNewLotQuantityDialog(receiptId, productId, productName, lineId, trackingType, enteredLotNumber, usesExpirationDate)
////                        }
////                    } else {
////                        withContext(Dispatchers.Main) {
////                            Toast.makeText(this@ProductsActivity, "Lot number already entered for $productName. Please enter a different number.", Toast.LENGTH_LONG).show()
////                            editTextLotNumber.setText("")
////                            editTextLotNumber.requestFocus()
////                        }
////                    }
////                }
////            } else {
////                Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
////                editTextLotNumber.requestFocus()
////            }
////        }
////
////        buttonCancelLot.setOnClickListener {
////            dialog.dismiss()
////        }
////
////        dialog.show()
////        editTextLotNumber.requestFocus()
////        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
////    }
//private fun promptForNewLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int, trackingType: String, usesExpirationDate: Boolean?) {
//    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
//    val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
//    val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
//    val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
//    val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
//    editTextLotNumber.setHintTextColor(Color.WHITE)
//    textEnterLotMessage.text = "Enter the lot number for $productName:"
//
//    val dialog = AlertDialog.Builder(this)
//        .setView(dialogView)
//        .setCancelable(false)
//        .create()
//
//    buttonConfirmLot.setOnClickListener {
//        val enteredLotNumber = editTextLotNumber.text.toString().trim()
//        if (enteredLotNumber.isNotEmpty()) {
//            coroutineScope.launch {
//                val lotList = pickLotNumbers.getOrPut(receiptId) { mutableListOf() }
//
//                if (!lotList.contains(enteredLotNumber)) {
//                    lotList.add(enteredLotNumber)
//                    withContext(Dispatchers.Main) {
//                        dialog.dismiss()
//                        showNewLotQuantityDialog(receiptId, productId, productName, lineId, trackingType, enteredLotNumber, usesExpirationDate)
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@ProductsActivity, "Lot number already entered for this picking operation. Please enter a different number.", Toast.LENGTH_LONG).show()
//                        editTextLotNumber.setText("")
//                        editTextLotNumber.requestFocus()
//                    }
//                }
//            }
//        } else {
//            Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
//            editTextLotNumber.requestFocus()
//        }
//    }
//
//    buttonCancelLot.setOnClickListener {
//        dialog.dismiss()
//    }
//
//    dialog.show()
//    editTextLotNumber.requestFocus()
//    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//    imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
//}
//
//
//    private suspend fun showNewLotQuantityDialog(receiptId: Int, productId: Int, productName: String, lineId: Int, trackingType: String, lotName: String, usesExpirationDate: Boolean?) {
//        // Fetch the latest data first, ensure UI updates post-fetch are on the main thread
//        withContext(Dispatchers.IO) {
//            fetchProductsForReceipt(receiptId)
//        }
//
//        withContext(Dispatchers.Main) {
//            val dialogView = LayoutInflater.from(this@ProductsActivity).inflate(R.layout.dialog_lot_quantity_input, null)
//            val textEnterLotQuantityMessage = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityMessage)
//            val editTextLotQuantity = dialogView.findViewById<EditText>(R.id.editTextLotQuantity)
//            val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
//            editTextLotQuantity.setHintTextColor(Color.WHITE)
//
//            textEnterLotQuantityMessage.text = "Enter quantity for lot: $lotName"
//
//            val dialog = AlertDialog.Builder(this@ProductsActivity)
//                .setView(dialogView)
//                .setCancelable(false)
//                .create()
//
//            buttonConfirmQuantity.setOnClickListener {
//                val enteredQuantity = editTextLotQuantity.text.toString().toDoubleOrNull()
//                if (enteredQuantity != null) {
//                    coroutineScope.launch {
//                        if (usesExpirationDate == true) {
//                            // No need to call createMoveLineWithExpirationForReceiving here
//                            // Just call the promptForNewLotExpirationDate function directly
//                            promptForNewLotExpirationDate(productName, receiptId, productId, lotName, enteredQuantity, lineId)
//                            dialog.dismiss()
//                        } else {
//                            // Otherwise, create the move line immediately
//                            try {
//                                val response = odooXmlRpcClient.createMoveLineForReceiving(receiptId, productId, lotName, enteredQuantity)
//                                val newLineId = response?.get("line_id") as? Int
//                                if (newLineId != null) {
//                                    updateProductMatchState(newLineId, receiptId, true)
//                                    withContext(Dispatchers.Main) {
//                                        dialog.dismiss()
//                                        showGreenToast("Lot quantity updated and match state set.")
//                                    }
//                                } else {
//                                    withContext(Dispatchers.Main) {
//                                        showRedToast("Failed to create new move line or retrieve line ID.")
//                                    }
//                                }
//                            } catch (e: Exception) {
//                                withContext(Dispatchers.Main) {
//                                    Log.e("OdooXmlRpcClient", "Error creating move line for receiving: ${e.localizedMessage}", e)
//                                    showRedToast("Error in creating move line.")
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    Toast.makeText(this@ProductsActivity, "Invalid quantity entered. Please try again.", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            dialog.show()
//        }
//    }
////    private suspend fun showNewLotQuantityDialog(receiptId: Int, productId: Int, productName: String, lineId: Int, trackingType: String, lotName: String, usesExpirationDate: Boolean?) {
////        fetchProductsForReceipt(receiptId)
////        withContext(Dispatchers.Main) {
////            val dialogView = LayoutInflater.from(this@ProductsActivity).inflate(R.layout.dialog_lot_quantity_input, null)
////            val textEnterLotQuantityMessage = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityMessage)
////            val editTextLotQuantity = dialogView.findViewById<EditText>(R.id.editTextLotQuantity)
////            val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
////            editTextLotQuantity.setHintTextColor(Color.WHITE)
////
////            textEnterLotQuantityMessage.text = "Enter quantity for lot: $lotName"
////
////            val dialog = AlertDialog.Builder(this@ProductsActivity).setView(dialogView).setCancelable(false).create()
////
////            buttonConfirmQuantity.setOnClickListener {
////                val enteredQuantity = editTextLotQuantity.text.toString().toDoubleOrNull()
////                if (enteredQuantity != null) {
////                    coroutineScope.launch {
////                        if (usesExpirationDate == true) {
////                            promptForNewLotExpirationDate(productName, receiptId, productId, lotName, enteredQuantity)
////                        } else {
////                            try {
////                                val response = odooXmlRpcClient.createMoveLineForReceiving(receiptId, productId, lotName, enteredQuantity)
////                                val newLineId = response?.get("line_id") as? Int
////                                if (newLineId != null) {
////                                    updateProductMatchState(newLineId, receiptId, true)
////                                    withContext(Dispatchers.Main) {
////                                        dialog.dismiss()
////                                        showGreenToast("Lot quantity updated and match state set.")
////                                    }
////                                } else {
////                                    withContext(Dispatchers.Main) {
////                                        showRedToast("Failed to create new move line or retrieve line ID.")
////                                    }
////                                }
////                            } catch (e: Exception) {
////                                withContext(Dispatchers.Main) {
////                                    Log.e("OdooXmlRpcClient", "Error creating move line for receiving: ${e.localizedMessage}", e)
////                                    showRedToast("Error in creating move line.")
////                                }
////                            }
////                        }
////                    }
////                } else {
////                    Toast.makeText(this@ProductsActivity, "Invalid quantity entered. Please try again.", Toast.LENGTH_SHORT).show()
////                }
////            }
////
////            dialog.show()
////        }
////    }
//
//    private fun promptForNewLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotName: String, quantity: Double, lineId: Int) {
//        // Move UI-related operations to the main thread
//        coroutineScope.launch {
//            withContext(Dispatchers.Main) {
//                val dialogView = LayoutInflater.from(this@ProductsActivity).inflate(R.layout.dialog_expiration_date, null)
//
//                val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
//                textDialogMessage.text = "Enter the expiration date for the lot of $productName."
//
//                val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
//                setupDateInputField(editTextExpirationDate) // Assumes this method is thread-safe or also switched to main thread
//                editTextExpirationDate.setHintTextColor(Color.WHITE)
//
//                val dialog = AlertDialog.Builder(this@ProductsActivity)
//                    .setView(dialogView)
//                    .create()
//
//                var lastClickTime = 0L
//                dialogView.findViewById<Button>(R.id.buttonOk).setOnClickListener {
//                    if (System.currentTimeMillis() - lastClickTime < 1000) {
//                        return@setOnClickListener // Debounce time of 1 second
//                    }
//                    lastClickTime = System.currentTimeMillis()
//
//                    val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
//                    val convertedDate = convertToFullDateTime(enteredExpirationDate) // Ensure this method is thread-safe
//                    if (isValidDateFormat(convertedDate)) {
//                        // Perform network request on IO Dispatcher
//                        coroutineScope.launch(Dispatchers.IO) {
//                            try {
//                                val response = odooXmlRpcClient.createMoveLineWithExpirationForReceiving(receiptId, productId, lotName, quantity, convertedDate)
//                                val newLineId = response?.get("line_id") as? Int
//                                if (newLineId != null) {
//                                    updateProductMatchState(newLineId, receiptId, matched = true)
//                                    confirmedLines.add(newLineId)
//                                    fetchProductsForReceipt(receiptId)
//                                } else {
//                                    withContext(Dispatchers.Main) {
//                                        showRedToast("Failed to create new move line or retrieve line ID.")
//                                    }
//                                }
//                            } catch (e: Exception) {
//                                withContext(Dispatchers.Main) {
//                                    Log.e("OdooXmlRpcClient", "Error creating move line with expiration: ${e.localizedMessage}", e)
//                                    showRedToast("Error in creating move line with expiration.")
//                                }
//                            } finally {
//                                withContext(Dispatchers.Main) {
//                                    dialog.dismiss()
//                                }
//                            }
//                        }
//                    } else {
//                        showRedToast("Invalid expiration date entered. Please use the format DD/MM/YYYY")
//                    }
//                }
//
//                dialogView.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
//                    dialog.dismiss()
//                }
//
//                dialog.show()
//            }
//        }
//    }
////private fun promptForNewLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotName: String, quantity: Double) {
////    coroutineScope.launch {
////        withContext(Dispatchers.Main) {
////            val dialogView = LayoutInflater.from(this@ProductsActivity).inflate(R.layout.dialog_expiration_date, null)
////            val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
////            textDialogMessage.text = "Enter the expiration date for the lot of $productName."
////
////            val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
////            setupDateInputField(editTextExpirationDate)
////            editTextExpirationDate.setHintTextColor(Color.WHITE)
////
////            val dialog = AlertDialog.Builder(this@ProductsActivity).setView(dialogView).create()
////
////            dialogView.findViewById<Button>(R.id.buttonOk).setOnClickListener {
////                val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
////                val convertedDate = convertToFullDateTime(enteredExpirationDate)
////                if (isValidDateFormat(convertedDate)) {
////                    coroutineScope.launch(Dispatchers.IO) {
////                        try {
////                            val response = odooXmlRpcClient.createMoveLineWithExpirationForReceiving(receiptId, productId, lotName, quantity, convertedDate)
////                            val newLineId = response?.get("line_id") as? Int
////                            if (newLineId != null) {
////                                updateProductMatchState(newLineId, receiptId, true)
////                                fetchProductsForReceipt(receiptId)
////                                withContext(Dispatchers.Main) {
////                                    showGreenToast("Lot expiration date updated")
////                                    dialog.dismiss()
////                                }
////                            } else {
////                                withContext(Dispatchers.Main) {
////                                    showRedToast("Failed to create new move line or retrieve line ID.")
////                                }
////                            }
////                        } catch (e: Exception) {
////                            withContext(Dispatchers.Main) {
////                                Log.e("OdooXmlRpcClient", "Error creating move line with expiration: ${e.localizedMessage}", e)
////                                showRedToast("Error in creating move line with expiration.")
////                            }
////                        }
////                    }
////                } else {
////                    showRedToast("Invalid expiration date entered. Please use the format DD/MM/YYYY")
////                }
////            }
////
////            dialogView.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
////                dialog.dismiss()
////            }
////
////            dialog.show()
////        }
////    }
////}
//
//
////    private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
////        // Inflate the custom layout for the dialog
////        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
////        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
////        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
////        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
////        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
////
////        textEnterLotMessage.text = "Enter the lot number for $productName."
////        editTextLotNumber.setHintTextColor(Color.WHITE)
////
////        val dialog = AlertDialog.Builder(this)
////            .setView(dialogView)
////            .setCancelable(false) // This disables dismissing the dialog by clicking outside it.
////            .create()
////
////        // Handle the Cancel button
////        buttonCancelLot.setOnClickListener {
////            dialog.dismiss()
////        }
////
////        // Handle the Confirm button
////        buttonConfirmLot.setOnClickListener {
////            val enteredLotNumber = editTextLotNumber.text.toString().trim()
////            if (enteredLotNumber.isNotEmpty()) {
////                coroutineScope.launch {
////                    val product = productsAdapter.moveLines.find { it.productId == productId }
////                    if (product?.useExpirationDate == true) {
////                        withContext(Dispatchers.Main) {
////                            // Assuming promptForLotQuantity is correctly defined elsewhere to handle these parameters
////                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, true, lineId)
////                        }
////                    } else {
////                        withContext(Dispatchers.Main) {
////                            // Assuming promptForLotQuantity is correctly defined elsewhere to handle these parameters
////                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, false, lineId)
////                        }
////                    }
////                }
////            } else {
////                showRedToast("Please enter a lot number.")
////            }
////            dialog.dismiss()
////        }
////
////        // Request focus and show the keyboard when the dialog is shown
////        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
////        dialog.setOnShowListener {
////            editTextLotNumber.requestFocus()
////            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////            imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
////        }
////
////        dialog.show()
////    }
////private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
////    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
////    val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
////    val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
////    val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
////    val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
////
////    textEnterLotMessage.text = "Enter the lot number for $productName."
////    editTextLotNumber.setHintTextColor(Color.WHITE)
////
////    val dialog = AlertDialog.Builder(this)
////        .setView(dialogView)
////        .setCancelable(false)
////        .create()
////
////    buttonConfirmLot.setOnClickListener {
////        val enteredLotNumber = editTextLotNumber.text.toString().trim()
////        if (enteredLotNumber.isNotEmpty()) {
////            coroutineScope.launch {
////                val product = productsAdapter.moveLines.find { it.productId == productId }
////                val key = ProductReceiptKey(productId, receiptId)
////                val lotList = productLotNumbers.getOrPut(key) { mutableListOf() }
////
////                if (!lotList.contains(enteredLotNumber)) {
////                    lotList.add(enteredLotNumber)  // Add the new lot number to the list
////                    withContext(Dispatchers.Main) {
////                        dialog.dismiss()
////                        if (product?.useExpirationDate == true) {
////                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, true, lineId)
////                        } else {
////                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, false, lineId)
////                        }
////                    }
////                } else {
////                    withContext(Dispatchers.Main) {
////                        Toast.makeText(this@ProductsActivity, "Lot number already entered for $productName.", Toast.LENGTH_SHORT).show()
////                        editTextLotNumber.setText("")
////                        editTextLotNumber.requestFocus()
////                    }
////                }
////            }
////        } else {
////            Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
////            editTextLotNumber.requestFocus()
////        }
////    }
////
////    buttonCancelLot.setOnClickListener {
////        dialog.dismiss()
////    }
////
////    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
////    dialog.show()
////    editTextLotNumber.requestFocus()
////    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////    imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
////}
//    private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
//        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
//        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
//        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
//        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
//
//        textEnterLotMessage.text = "Enter the lot number for $productName."
//        editTextLotNumber.setHintTextColor(Color.WHITE)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false)
//            .create()
//
//        buttonConfirmLot.setOnClickListener {
//            val enteredLotNumber = editTextLotNumber.text.toString().trim()
//            if (enteredLotNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val lotList = pickLotNumbers.getOrPut(receiptId) { mutableListOf() }
//
//                    if (!lotList.contains(enteredLotNumber)) {
//                        lotList.add(enteredLotNumber)
//                        withContext(Dispatchers.Main) {
//                            dialog.dismiss()
//                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, productsAdapter.moveLines.find { it.productId == productId }?.useExpirationDate == true, lineId)
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@ProductsActivity, "Lot number already entered for this picking operation.", Toast.LENGTH_SHORT).show()
//                            editTextLotNumber.setText("")
//                            editTextLotNumber.requestFocus()
//                        }
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
//                editTextLotNumber.requestFocus()
//            }
//        }
//
//        buttonCancelLot.setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialog.show()
//        editTextLotNumber.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
//    }
//
//
//
//    private fun promptForLotQuantity(productName: String, receiptId: Int, productId: Int, lotNumber: String, requiresExpirationDate: Boolean, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_quantity_input, null)
//        val messageTextView = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityMessage)
//        val editTextLotQuantity = dialogView.findViewById<EditText>(R.id.editTextLotQuantity)
//        val buttonCancelQuantity = dialogView.findViewById<Button>(R.id.buttonCancelQuantity)
//        val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
//        editTextLotQuantity.setHintTextColor(Color.WHITE)
//
//        messageTextView.text = "Enter the quantity for the lot of $lotNumber."
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//
//        buttonConfirmQuantity.setOnClickListener {
//            val enteredQuantity = editTextLotQuantity.text.toString().toIntOrNull()
//            if (enteredQuantity != null) {
//                coroutineScope.launch {
//                    if (requiresExpirationDate) {
//                        withContext(Dispatchers.Main) {
//                            promptForLotExpirationDate(productName, receiptId, productId, lotNumber, enteredQuantity, lineId)
//                            dialog.dismiss()
//                        }
//                    } else {
//                        odooXmlRpcClient.updateMoveLineLotAndQuantity(lineId, receiptId, productId, lotNumber, enteredQuantity)
//                        confirmedLines.add(lineId)
//                        updateProductMatchState(lineId, receiptId, matched = true)
//                        fetchProductsForReceipt(receiptId)
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("Quantity updated for lot without expiration date.")
//                            dialog.dismiss()
//                        }
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        buttonCancelQuantity.setOnClickListener {
//            dialog.dismiss()
//        }
//
//        // Ensure the keyboard is shown when the EditText gains focus
//        dialog.setOnShowListener {
//            editTextLotQuantity.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editTextLotQuantity, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        dialog.show()
//    }
//
//    private fun promptForLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotNumber: String, quantity: Int, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expiration_date, null)
//
//        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
//        textDialogMessage.text = "Enter the expiration date for the lot of $productName."
//
//        val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
//        setupDateInputField(editTextExpirationDate)  // Assuming this sets up a listener for proper date formatting
//
//        editTextExpirationDate.setHintTextColor(Color.WHITE)
//
//        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
//        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//
//        buttonOk.setOnClickListener {
//            val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
//            val convertedDate = convertToFullDateTime(enteredExpirationDate)  // Ensure this converts to "yyyy-MM-dd"
//            if (isValidDateFormat(convertedDate)) {
//                coroutineScope.launch {
//                    odooXmlRpcClient.updateMoveLineLotExpiration(lineId, receiptId, productId, lotNumber, quantity, convertedDate).also {
//                        updateProductMatchState(lineId, receiptId, matched = true)
//                        confirmedLines.add(lineId)
//                        fetchProductsForReceipt(receiptId)
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("Lot expiration date updated")
//                            dialog.dismiss()
//                        }
//                    }
//                }
//            } else {
//
//                    showRedToast("Invalid expiration date entered. Please use the format DD/MM/YYYY")
//
//            }
//        }
//
//        buttonCancel.setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialog.show()
//    }
//
////    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
////        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
////        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
////        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
////        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
////        val productMessage = dialogView.findViewById<TextView>(R.id.ProductMessage)
////        serialNumberInput.setHintTextColor(Color.WHITE)
////        // Set the message dynamically
////        productMessage.text = "Product: $productName."
////
////        val dialog = AlertDialog.Builder(this)
////            .setView(dialogView)
////            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
////            .create()
////
////        // Now set the positive button separately to have access to 'dialog' variable
////        buttonConfirmSN.setOnClickListener {
////                val enteredSerialNumber = serialNumberInput.text.toString().trim()
////                if (enteredSerialNumber.isNotEmpty()) {
////                    coroutineScope.launch {
////                        val product = productsAdapter.moveLines.find { it.productId == productId }
////                        val key = ProductReceiptKey(productId, receiptId)
////                        val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
////
////                        if (!serialList.contains(enteredSerialNumber)) {
////                            if (product?.useExpirationDate == true) {
////                                withContext(Dispatchers.Main) {
////                                    dialog.dismiss() // Correctly reference 'dialog' here
////                                    promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber, lineId)
////                                }
////                            } else {
////                                odooXmlRpcClient.updateMoveLineSerialNumber(lineId, receiptId, productId, enteredSerialNumber)
////                                serialList.add(enteredSerialNumber)
////                                updateProductMatchState(lineId, receiptId, true)
////                                confirmedLines.add(lineId)
////                                fetchProductsForReceipt(receiptId)
////                                withContext(Dispatchers.Main) {
////                                    showGreenToast("Serial number added for $productName.")
////                                    dialog.dismiss()
////                                }
////                            }
////                        } else {
////                            withContext(Dispatchers.Main) {
////                                showRedToast("Serial number already entered for $productName")
////                                serialNumberInput.setText("")
////                                serialNumberInput.requestFocus()
////                            }
////                        }
////                    }
////                } else {
////                    showRedToast("Please enter a serial number")
////                    serialNumberInput.setText("")
////                    serialNumberInput.requestFocus()
////                }
////
////        }
////
////        buttonCancelSN.setOnClickListener {
////            dialog.dismiss()
////        }
////
////        dialog.show()
////        serialNumberInput.requestFocus()
////        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
////    }
////private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
////    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
////    val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
////    val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
////    val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
////    val productMessage = dialogView.findViewById<TextView>(R.id.ProductMessage)
////    serialNumberInput.setHintTextColor(Color.WHITE)
////    productMessage.text = "Product: $productName."
////
////    val dialog = AlertDialog.Builder(this)
////        .setView(dialogView)
////        .setCancelable(false)
////        .create()
////
////    buttonConfirmSN.setOnClickListener {
////        val enteredSerialNumber = serialNumberInput.text.toString().trim()
////        if (enteredSerialNumber.isNotEmpty()) {
////            coroutineScope.launch {
////                val serialList = pickSerialNumbers.getOrPut(receiptId) { mutableListOf() }
////                if (!serialList.contains(enteredSerialNumber)) {
////                    serialList.add(enteredSerialNumber)
////                    withContext(Dispatchers.IO) { // Ensure network call is on IO Dispatcher
////                        val updateResult = odooXmlRpcClient.updateMoveLineSerialNumber(lineId, receiptId, productId, enteredSerialNumber)
////                        withContext(Dispatchers.Main) { // Switch back to Main for UI updates
////                            if (updateResult) {
////                                updateProductMatchState(lineId, receiptId, true)
////                                confirmedLines.add(lineId)
////                                coroutineScope.launch { fetchProductsForReceipt(receiptId) }
////
////                                showGreenToast("Serial number added for $productName.")
////                                dialog.dismiss()
////                            } else {
////                                showRedToast("Failed to update serial number.")
////                            }
////                        }
////                    }
////                } else {
////                    withContext(Dispatchers.Main) {
////                        showRedToast("Serial number already entered for this picking operation")
////                        serialNumberInput.setText("")
////                        serialNumberInput.requestFocus()
////                    }
////                }
////            }
////        } else {
////            showRedToast("Please enter a serial number")
////            serialNumberInput.setText("")
////            serialNumberInput.requestFocus()
////        }
////    }
////
////    buttonCancelSN.setOnClickListener {
////        dialog.dismiss()
////    }
////
////    dialog.show()
////    serialNumberInput.requestFocus()
////    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////    imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
////}
//private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
//    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
//    val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
//    val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
//    val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
//    val productMessage = dialogView.findViewById<TextView>(R.id.ProductMessage)
//    serialNumberInput.setHintTextColor(Color.WHITE)
//    productMessage.text = "Product: $productName."
//
//    val dialog = AlertDialog.Builder(this)
//        .setView(dialogView)
//        .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
//        .create()
//
//    buttonConfirmSN.setOnClickListener {
//        val enteredSerialNumber = serialNumberInput.text.toString().trim()
//        if (enteredSerialNumber.isNotEmpty()) {
//            coroutineScope.launch {
//                val product = productsAdapter.moveLines.find { it.productId == productId }
//                if (product != null) {
//                    val serialList = pickSerialNumbers.getOrPut(receiptId) { mutableListOf() }
//                    if (!serialList.contains(enteredSerialNumber)) {
//                        if (product?.useExpirationDate == true) {
//                            withContext(Dispatchers.Main) {
//                                dialog.dismiss()
//                                promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber, lineId)
//                            }
//                        } else {
//                            withContext(Dispatchers.IO) {
//                                val updateResult = odooXmlRpcClient.updateMoveLineSerialNumber(lineId, receiptId, productId, enteredSerialNumber)
//                                withContext(Dispatchers.Main) {
//                                    if (updateResult) {
//                                        serialList.add(enteredSerialNumber)
//                                        updateProductMatchState(lineId, receiptId, true)
//                                        confirmedLines.add(lineId)
//                                        coroutineScope.launch { fetchProductsForReceipt(receiptId) }
//                                        showGreenToast("Serial number added for $productName.")
//                                        dialog.dismiss()
//                                    } else {
//                                        showRedToast("Failed to update serial number.")
//                                    }
//                                }
//                            }
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            showRedToast("Serial number already entered for this picking operation")
//                            serialNumberInput.setText("")
//                            serialNumberInput.requestFocus()
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        showRedToast("Product details not found.")
//                    }
//                }
//            }
//        } else {
//            showRedToast("Please enter a serial number")
//            serialNumberInput.setText("")
//            serialNumberInput.requestFocus()
//        }
//    }
//
//    buttonCancelSN.setOnClickListener {
//        dialog.dismiss()
//    }
//
//    dialog.show()
//    serialNumberInput.requestFocus()
//    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//    imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
//}
//
//
//
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
////    private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String, lineId: Int) {
////        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expiration_date, null)
////        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
////        textDialogMessage.text = "Enter the expiration date for $productName."
////        val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
////        setupDateInputField(editTextExpirationDate)
////
////        // Setting the hint color to white
////        editTextExpirationDate.setHintTextColor(ContextCompat.getColor(this, android.R.color.white))
////
////        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
////        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
////
////        val dialog = AlertDialog.Builder(this)
////            .setView(dialogView)
////            .create()
////
////        buttonOk.setOnClickListener {
////            val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
////            val convertedDate = convertToFullDateTime(enteredExpirationDate)
////            if (isValidDateFormat(convertedDate)) {
////                coroutineScope.launch {
//////                    val key = ProductReceiptKey(lineId, receiptId)
//////                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
////                    val serialList = pickSerialNumbers.getOrPut(receiptId) { mutableListOf() }
////                    if (!serialList.contains(serialNumber)) {
////                        serialList.add(serialNumber)
////                        odooXmlRpcClient.updateMoveLineSerialExpirationDate(lineId, receiptId, productId, serialNumber, convertedDate)
////                        confirmedLines.add(lineId)
//////                        val isMatched = serialList.size == productsAdapter.moveLines.find { it.productId == productId }?.quantity?.toInt()
////                        updateProductMatchState(lineId, receiptId, matched = true)
////                        fetchProductsForReceipt(receiptId)
////                        withContext(Dispatchers.Main) {
////                            showGreenToast("Serial number added for $productName.")
////                            dialog.dismiss()
////                        }
////                    } else {
////                        withContext(Dispatchers.Main) {
////                            showRedToast("Serial number already entered for $productName")
////                        }
////                    }
////                }
////            } else {
////                showRedToast("Invalid expiration date entered. Please use the format DD/MM/YYYY")
////            }
////        }
////
////        buttonCancel.setOnClickListener {
////            dialog.dismiss()
////        }
////
////        dialog.show()
////    }
//private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String, lineId: Int) {
//    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expiration_date, null)
//    val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
//    textDialogMessage.text = "Enter the expiration date for $productName."
//    val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
//    setupDateInputField(editTextExpirationDate) // Assumes this method setups date picker or similar
//
//    // Setting the hint color to white
//    editTextExpirationDate.setHintTextColor(ContextCompat.getColor(this, android.R.color.white))
//
//    val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
//    val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
//
//    val dialog = AlertDialog.Builder(this)
//        .setView(dialogView)
//        .create()
//
//    buttonOk.setOnClickListener {
//        val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
//        val convertedDate = convertToFullDateTime(enteredExpirationDate) // Assume this correctly formats the date
//        if (isValidDateFormat(convertedDate)) { // Assume this validates the formatted date
//            coroutineScope.launch {
//                odooXmlRpcClient.updateMoveLineSerialExpirationDate(lineId, receiptId, productId, serialNumber, convertedDate)
//                updateProductMatchState(lineId, receiptId, matched = true)
//                confirmedLines.add(lineId)
//                fetchProductsForReceipt(receiptId)
//                withContext(Dispatchers.Main) {
//                    showGreenToast("Expiration date updated and serial number added for $productName.")
//                    dialog.dismiss()
//                }
//            }
//        } else {
//                showRedToast("Invalid expiration date entered. Please use the format DD/MM/YYYY")
//        }
//    }
//
//    buttonCancel.setOnClickListener {
//        dialog.dismiss()
//    }
//
//    dialog.show()
//}
//
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
//    private fun promptForProductQuantity(productName: String, expectedQuantity: Double, receiptId: Int, productId: Int, lineId: Int, recount: Boolean = false) {
//        // Inflate the custom layout
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_input, null)
//        val editText = dialogView.findViewById<EditText>(R.id.quantityInput)
//        val titleTextView = dialogView.findViewById<TextView>(R.id.titleTextView)
//        titleTextView.text = if (recount) "Recount Required for $productName" else "Enter Quantity for $productName"
//
//        editText.setHintTextColor(Color.WHITE)
//
//        // Setup AlertDialog with custom view but no buttons
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false) // Disable dismissing the dialog by pressing back or touching outside
//            .create()
//
//        // Find and setup the custom buttons
//        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
//        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
//
//        // Cancel button logic
//        cancelButton.setOnClickListener {
//            dialog.dismiss() // This dismisses the dialog
//        }
//
//        // Confirm button logic
//        confirmButton.setOnClickListener {
//            val enteredQuantity = editText.text.toString().toDoubleOrNull()
//            if (enteredQuantity != null) {
//                // Always update the quantity, regardless of its correctness
//                lifecycleScope.launch(Dispatchers.IO) {
//                    odooXmlRpcClient.updateMoveLineQuantityUntracked(
//                        lineId,
//                        receiptId,
//                        productId,
//                        enteredQuantity
//                    )
//                    coroutineScope.launch {
//                        fetchProductsForReceipt(receiptId)
//                    }
//                }
//
//                // Check if the quantity is correct
//                if (enteredQuantity == expectedQuantity) {
//                    showGreenToast("Quantity updated for $productName")
//                    updateProductMatchState(lineId, receiptId, true)
//                    confirmedLines.add(lineId)
//                } else if (!recount) {
//                    promptForProductQuantity(productName, expectedQuantity, receiptId, productId, lineId, recount = true)
//                } else {
//                    handleIncorrectRecount(productName, receiptId)
//                }
//
//                dialog.dismiss() // Dismiss the dialog after confirming
//            } else {
//                Toast.makeText(this, "Invalid quantity entered", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        // Display the dialog
//        dialog.show()
//    }
//
//    private fun handleIncorrectRecount(productName: String, receiptId: Int) {
//        val localReceiptId = receiptName  // Copy the mutable property to a local variable
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            val buyerDetails = localReceiptId?.let { odooXmlRpcClient.fetchAndLogBuyerDetails(it) }
//            if (buyerDetails != null) {
//                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, localReceiptId, productName) // Pass the local copy to the function
//                withContext(Dispatchers.Main) {
//                    showRedToast("Flagged ${buyerDetails.login}. Email sent.")
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    showRedToast("Flagged, but buyer details not found.")
//                }
//            }
//        }
//    }
//
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
//                - Receipt Name: $receiptName
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
////    private fun updateProductMatchState(
////        lineId: Int,
////        pickId: Int,
////        matched: Boolean = true,
////    ) {
////        val key = ProductReceiptKey(lineId, pickId)
////        val productLine = productsAdapter.moveLines.find { it.id == lineId }
////
////        if (productLine != null) {
////            quantityMatches[key] = matched
////
////            // Refresh the UI to reflect the updated state
////            runOnUiThread {
////                val position = productsAdapter.findProductPositionById(lineId)
////                if (position != -1) {
////                    productsAdapter.notifyItemChanged(position)
////                }
////                checkAndToggleValidateButton(pickId)
////            }
////        } else {
////            Log.e("updateProductMatchState", "No line found for ID $lineId")
////        }
////
////        saveMatchStateToPreferences(key, quantityMatches[key] == true)
////    }
//private fun updateProductMatchState(lineId: Int, pickId: Int, matched: Boolean = true) {
//    coroutineScope.launch {
//        fetchProductsForReceipt(pickId)  // Fetch and wait for completion
//
//        withContext(Dispatchers.Main) {
//            val key = ProductReceiptKey(lineId, pickId)
//            val productLine = productsAdapter.moveLines.find { it.id == lineId }
//
//            if (productLine != null) {
//                quantityMatches[key] = matched
//                val position = productsAdapter.findProductPositionById(lineId)
//                if (position != -1) {
//                    productsAdapter.notifyItemChanged(position)
//                }
//                checkAndToggleValidateButton(pickId)
//                saveMatchStateToPreferences(key, matched)
//            } else {
//                Log.e("updateProductMatchState", "No line found for ID $lineId after refreshing data")
//            }
//        }
//    }
//}
//
//
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
//                    val validationSuccessful = odooXmlRpcClient.validateOperation(pickId)
//                    runOnUiThread {
//                        if (validationSuccessful) {
//                            Toast.makeText(applicationContext, "Receipt validated successfully.", Toast.LENGTH_SHORT).show()
//                            // Redirect to PickActivity upon successful validation
//                            val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
//                            startActivity(intent)
//                            finish()  // Optionally call finish() if you want to remove this activity from the back stack
//                        } else {
//                            Toast.makeText(applicationContext, "Failed to validate receipt.\nPlease flag or recount quantities. ", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun saveButtonVisibilityState(pickId: Int, visible: Boolean) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        with(sharedPref.edit()) {
//            putBoolean("ValidateButtonVisible_$pickId", visible)
//            apply()
//        }
//    }
//
//    private fun restoreButtonVisibility(pickId: Int) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val isVisible = sharedPref.getBoolean("ValidateButtonVisible_$pickId", false)
//        val validateButton = findViewById<Button>(R.id.pickValidateButton)
//        validateButton.visibility = if (isVisible) View.VISIBLE else View.GONE
//
//        if (isVisible) {
//            setupValidateButtonListener(pickId)
//        }
//    }
//
//    private fun setupValidateButtonListener(pickId: Int) {
//        val validateButton = findViewById<Button>(R.id.pickValidateButton)
//        validateButton.setOnClickListener {
//            coroutineScope.launch {
//                val validationSuccessful = odooXmlRpcClient.validateOperation(pickId)
//                runOnUiThread {
//                    if (validationSuccessful) {
//                        Toast.makeText(applicationContext, "Picking validated successfully.", Toast.LENGTH_SHORT).show()
//                        val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
//                        startActivity(intent)
//                        finish()  // Optionally call finish() if you want to remove this activity from the back stack
//                    } else {
//                        Toast.makeText(applicationContext, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
//    }
//
//
//    private fun saveMatchStateToPreferences(key: ProductReceiptKey, matched: Boolean) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        with(sharedPref.edit()) {
//            putBoolean("${key.moveLineId}_${key.pickId}", matched)
//            apply()
//        }
//    }
//
//
//    private fun loadMatchStatesFromPreferences(pickId: Int) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val tempQuantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//
//        sharedPref.all.forEach { (prefKey, value) ->
//            if (value is Boolean) {
//                if (prefKey.startsWith("ValidateButtonVisible")) {
//                    // Correct handling of the ValidateButtonVisible key
//                    val parts = prefKey.split("_")
//                    if (parts.size == 2) {
//                        val storedPickId = parts[1].toIntOrNull()
//                        if (storedPickId == pickId) {
//                            findViewById<Button>(R.id.pickValidateButton).visibility = if (value) View.VISIBLE else View.GONE
//                        }
//                    }
//                } else {
//                    // Handling standard product match keys
//                    val parts = prefKey.split("_")
//                    if (parts.size == 2) {
//                        try {
//                            val moveLineId = parts[0].toInt()
//                            val prefPickId = parts[1].toInt()
//                            if (prefPickId == pickId) {
//                                val key = ProductReceiptKey(moveLineId, prefPickId)
//                                tempQuantityMatches[key] = value
//                            }
//                        } catch (e: NumberFormatException) {
//                            Log.e("PickProductsActivity", "Error parsing shared preference key: $prefKey", e)
//                        }
//                    } else {
//                        Log.e("PickProductsActivity", "Incorrectly formatted shared preference key: $prefKey. Expected format: productId_pickId")
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
//            productsAdapter.updateProducts(productsAdapter.moveLines, pickId, quantityMatches)
//        }
//    }
//    override fun onDestroy() {
//        super.onDestroy()
//        coroutineScope.cancel()
//    }
//
//    override fun onProductClick(product: ReceiptMoveLine) {
//        showProductDialog(product)
//    }
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
//        editTextProductLotNumber.setHintTextColor(Color.WHITE)
//        editTextQuantity.setHintTextColor(Color.WHITE)
//
//        // Set values to the TextViews
//        textProductName.text = product.productName
//        textProductQuantity.text = "${product.quantity}"
//        textProductToLocation.text = "${product.locationDestName}"
//        textProductLotNumber.text = "${product.lotName}"
//
//        // Flags to track if edits were made
//        var quantityChanged = false
//        var lotNameChanged = false
//
//        // Create and show the dialog
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//        dialog.show()
//
//        // Setup visibility based on product tracking type
//        lotNumberLayout.visibility = View.GONE
//        buttonEditLotNumber.visibility = View.GONE
//        buttonEditQuantity.visibility = View.GONE
//
//        coroutineScope.launch {
//            withContext(Dispatchers.Main) {
//                if (product.trackingType == "serial") {
//                    buttonEditQuantity.visibility = View.GONE
//                    if (product.lotName.isNotEmpty()) {
//                        lotNumberLayout.visibility = View.VISIBLE
//                        buttonEditLotNumber.visibility = View.VISIBLE
//                    }
//                } else if (product.trackingType == "lot" && product.lotName.isNotEmpty()) {
//                    lotNumberLayout.visibility = View.VISIBLE
//                    buttonEditLotNumber.visibility = View.VISIBLE
//                    buttonEditQuantity.visibility = View.VISIBLE
//                } else if (product.trackingType == "none") {
//                    buttonEditQuantity.visibility = View.VISIBLE
//                    lotNumberLayout.visibility = View.GONE
//                    buttonEditLotNumber.visibility = View.GONE
//                } else {
//                    lotNumberLayout.visibility = View.VISIBLE
//                    buttonEditLotNumber.visibility = View.GONE
//                    buttonEditQuantity.visibility = View.VISIBLE
//                }
//            }
//        }
//        // Button handlers for editing quantity and lot number
//        buttonEditQuantity.setOnClickListener {
//            editTextQuantity.visibility = View.VISIBLE
//            editTextQuantity.setText(product.quantity.toString())
//            textProductQuantity.visibility = View.GONE
//            quantityChanged = true
//        }
//        buttonEditLotNumber.setOnClickListener {
//            editTextProductLotNumber.visibility = View.VISIBLE
//            editTextProductLotNumber.setText(product.lotName)
//            textProductLotNumber.visibility = View.GONE
//            lotNameChanged = true
//        }
//
//        buttonConfirmQuantity.setOnClickListener {
//            coroutineScope.launch(Dispatchers.IO) { // Start coroutine in IO Dispatcher for network operations
//                try {
//                    when {
//                        quantityChanged && lotNameChanged -> {
//                            val newQuantity = editTextQuantity.text.toString().toFloat().toInt()
//                            odooXmlRpcClient.updateMoveLineQuantityForReceipt(product.id, receiptId, newQuantity)
//                            odooXmlRpcClient.updateMoveLinesForReceipt(product.id, receiptId, editTextProductLotNumber.text.toString())
//                        }
//                        quantityChanged -> {
//                            val newQuantity = editTextQuantity.text.toString().toFloat().toInt()
//                            odooXmlRpcClient.updateMoveLineQuantityForReceipt(product.id, receiptId, newQuantity)
//                        }
//                        lotNameChanged -> {
//                            odooXmlRpcClient.updateMoveLinesForReceipt(product.id, receiptId, editTextProductLotNumber.text.toString())
//                        }
//                    }
//                    // Execute fetching of products on the same background thread
//                    fetchProductsForReceipt(receiptId)
//
//                    withContext(Dispatchers.Main) { // Switch to Main Dispatcher for UI operations
//                        dialog.dismiss()
//                    }
//                } catch (e: NumberFormatException) {
//                    withContext(Dispatchers.Main) { // Handle UI updates on the main thread
//                        showRedToast("Invalid input. Please enter a valid number.")
//                    }
//                } catch (e: Exception) {
//                    withContext(Dispatchers.Main) {
//                        showRedToast("Error during network operations: ${e.localizedMessage}")
//                    }
//                }
//            }
//        }
//
//
//        buttonCancel.setOnClickListener {
//            dialog.dismiss()
//        }
//    }
//
//    companion object {
//        private const val CAMERA_REQUEST_CODE = 1001
//    }
//
//    private fun captureImage(pickId: Int) {
//        // Inflate the custom layout for the dialog
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.capture_image_dialog, null)
//
//        // Create and show the dialog
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//
//        // Find buttons and set up click listeners
//        dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialogView.findViewById<Button>(R.id.btnCaptureImage).setOnClickListener {
//            dialog.dismiss()
//            openCamera()
//        }
//
//        dialog.show()
//    }
//
//    private fun openCamera() {
//        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (cameraIntent.resolveActivity(packageManager) != null) {
//            startActivityForResult(cameraIntent, PickProductsActivity.CAMERA_REQUEST_CODE)
//        } else {
//            Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PickProductsActivity.CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
//            val imageBitmap = data?.extras?.get("data") as? Bitmap
//            if (imageBitmap != null) {
//                val byteArrayOutputStream = ByteArrayOutputStream()
//                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
//                val byteArray = byteArrayOutputStream.toByteArray()
//                val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
//
//                Log.d("CaptureImage", "Encoded image: $encodedImage") // Log the encoded string or its length
//
//                coroutineScope.launch {
//                    val updateResult = odooXmlRpcClient.updatePickingImage(receiptId, encodedImage)
//                    Log.d("OdooUpdate", "Update result: $updateResult") // Log the result from the server
//                }
//            } else {
//                Log.e("CaptureImage", "Failed to capture image")
//            }
//        }
//    }
//
//}
//



package com.example.warehousetet


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.text.Editable
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
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
//
//class ProductsActivity : AppCompatActivity(), ProductsAdapter.OnProductClickListener{
//    private lateinit var productsAdapter: ProductsAdapter
//    private val coroutineScope = CoroutineScope(Dispatchers.IO)
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var barcodeInput: EditText
//    private lateinit var confirmButton: Button
//    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
//
//    private var pickSerialNumbers = hashMapOf<Int, MutableList<String>>()
//    private var pickLotNumbers = hashMapOf<Int, MutableList<String>>()
//
//    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
//    private var quantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//    private val confirmedLines = mutableSetOf<Int>()
//
//    private var receiptName: String? = null
//    private var receiptId: Int = -1
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_products)
//
//        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
//        barcodeInput = findViewById(R.id.barcodeInput)
//        confirmButton = findViewById(R.id.confirmButton)
//        receiptId = intent.getIntExtra("RECEIPT_ID", -1)
//        receiptName = intent.getStringExtra("RECEIPT_NAME")
//        Log.d("ProductsActivity", "Received receipt name: $receiptName")
//
//        supportActionBar?.title = receiptName
//
//        productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId, this)
//
//        if (receiptId != -1) {
//            setupRecyclerView()
//            coroutineScope.launch { fetchProductsForReceipt(receiptId) }
//
//        } else {
//            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity.")
//        }
//
//        findViewById<Button>(R.id.clearButton).setOnClickListener {
//            findViewById<EditText>(R.id.barcodeInput).text.clear()
//        }
//
//        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == RESULT_OK) {
//                val data = result.data
//                val imageBitmap = data?.parcelable<Bitmap>("data")
//                if (imageBitmap != null) {
//                    val byteArrayOutputStream = ByteArrayOutputStream()
//                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
//                    val byteArray = byteArrayOutputStream.toByteArray()
//                    val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
//
//                    Log.d("CaptureImage", "Encoded image: $encodedImage")
//
//                    lifecycleScope.launch {
//                        try {
//                            withContext(Dispatchers.IO) {
//                                odooXmlRpcClient.updatePickingImage(receiptId, encodedImage)
//                            }
//                            Log.d("OdooUpdate", "Image updated successfully on server")
//
//                            // Ensure UI updates are on the main thread
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(this@ProductsActivity, "Image updated successfully on server", Toast.LENGTH_SHORT).show()
//                                barcodeInput.setText("")
//                                barcodeInput.requestFocus()
//                            }
//                        } catch (e: Exception) {
//                            Log.e("OdooUpdate", "Failed to update image: ${e.localizedMessage}", e)
//
//                            // Ensure UI updates are on the main thread
//                            withContext(Dispatchers.IO) {
//                                Toast.makeText(this@ProductsActivity, "Failed to update image.", Toast.LENGTH_SHORT).show()
//                                barcodeInput.setText("")
//                                barcodeInput.requestFocus()
//                            }
//                        }
//                    }
//                } else {
//                    Log.e("CaptureImage", "Failed to capture image")
//                }
//            } else {
//                Log.e("CaptureImage", "Camera action was cancelled or failed")
//                Toast.makeText(this, "Camera action was cancelled or failed.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//
//        val enterManuallyButton: Button = findViewById(R.id.pickManualEntryButton)
//        val confirmButton: Button = findViewById(R.id.confirmButton)
//        val clearButton: Button = findViewById(R.id.clearButton)
//        val barcodeInput: EditText = findViewById(R.id.barcodeInput)
//
//        enterManuallyButton.setOnClickListener {
//            confirmButton.visibility = View.VISIBLE
//            clearButton.visibility = View.VISIBLE
//            enterManuallyButton.visibility = View.GONE
//            barcodeInput.apply {
//                setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
//                setTextColor(ContextCompat.getColor(context, android.R.color.black))
//                isCursorVisible = true
//                requestFocus()
//                setBackgroundResource(R.drawable.edittext_border)
//                hint = "Enter Barcode"
//                val layoutParams = this.layoutParams
//                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
//                this.layoutParams = layoutParams
//            }
//        }
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        restoreButtonVisibility(receiptId)
//        setupBarcodeVerification(receiptId)
//        loadMatchStatesFromPreferences(receiptId)
//    }
//    override fun onResume() {
//        super.onResume()
//        // Restore visibility state whenever the activity resumes
//        restoreButtonVisibility(receiptId)
//
//    }
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
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_flag_receipt -> {
//                showDialogForFlag()
//                true
//            }
//            android.R.id.home -> {
//                onBackPressedDispatcher.onBackPressed()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//    private fun showDialogForFlag() {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, null)
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//
//        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialogView.findViewById<Button>(R.id.btnFlagPick).setOnClickListener {
//            flagReceipt()
//            dialog.dismiss()
//        }
//
//        dialog.show()
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
//
//                    Toast.makeText(this@ProductsActivity, "Receipt flagged", Toast.LENGTH_LONG).show()
//                    captureImage(receiptId)
//                }
//            } ?: run {
//                withContext(Dispatchers.Main) {
//                    Log.e("ProductsActivity", "Failed to fetch buyer details or flag the receipt.")
//                Toast.makeText(this@ProductsActivity, "Failed to flag receipt", Toast.LENGTH_SHORT).show()
//                    barcodeInput.setText("")
//                    barcodeInput.requestFocus()
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
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        } catch (e: MessagingException) {
//            Log.e("EmailSender", "Failed to send email.", e)
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//    }
//
//    private fun setupRecyclerView() {
//        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
//        // Ensure you are passing all necessary parameters in the correct order
//        // The parameters should be (List<MoveLine>, Map<ProductPickKey, Boolean>, Map<Int, String>, Int, OnProductClickListener)
//        productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId, this)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = productsAdapter
//    }
//
//    private fun setupBarcodeVerification(receiptId: Int) {
//        fun performBarcodeVerification() {
//            val enteredBarcode = barcodeInput.text.toString().trim()
//            verifyBarcode(enteredBarcode, receiptId)
//            hideKeyboard()
//            barcodeInput.setText("")
//        }
//
//        confirmButton.setOnClickListener {
//            performBarcodeVerification()
//        }
//
//        barcodeInput.setOnEditorActionListener { _, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE ||
//                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                if (confirmButton.visibility == View.GONE || confirmButton.visibility == View.INVISIBLE) {
//                    performBarcodeVerification()
//                } else {
//                    confirmButton.performClick()
//                }
//                true
//            } else false
//        }
//    }
//    private fun hideKeyboard() {
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
//    }
//
//    private suspend fun fetchProductsForReceipt(pickId: Int) {
//        try {
//            Log.d("PickProductsActivity", "Fetching products for pick ID: $pickId")
//            val fetchedLines = odooXmlRpcClient.fetchReceiptMoveLinesByPickingId(pickId)
//            val updatedMoveLinesWithDetails = mutableListOf<ReceiptMoveLine>()
//
//            // Optionally fetch additional package information if required
//            odooXmlRpcClient.fetchResultPackagesByPickingId(pickId)
//
//            // Fetch product tracking and expiration details asynchronously
//            val fetchJobs = fetchedLines.map { moveLine ->
//                coroutineScope.async(Dispatchers.IO) {
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(moveLine.productName) ?: Pair("none", false)
//                    val barcode = barcodeToProductIdMap.filterValues { it == moveLine.productId }.keys.firstOrNull()?.toString()
//
//                    moveLine.copy(
//                        trackingType = trackingAndExpiration.first ?: "none",
//                        useExpirationDate = trackingAndExpiration.second,
//                        barcode = barcode
//                    )
//                }
//            }
//
//            updatedMoveLinesWithDetails.addAll(fetchJobs.awaitAll())
//
//            // Optionally fetch barcodes for all products in the fetched lines
//            fetchBarcodesForProducts(fetchedLines)
//
//            // After all async operations complete, update the UI on the main thread
//            withContext(Dispatchers.Main) {
//                // Call updateUIForProducts instead of directly updating productsAdapter to maintain abstraction
//                updateUIForProducts(updatedMoveLinesWithDetails, pickId)
//                Log.d("PickProductsActivity", "UI updated with detailed move lines for Pick ID: $pickId")
//            }
//        } catch (e: Exception) {
//            Log.e("PickProductsActivity", "Error fetching move lines or updating UI: ${e.localizedMessage}", e)
//        }
//    }
//
//
//
//    private fun fetchBarcodesForProducts(products: List<ReceiptMoveLine>) {
//        products.forEach { product ->
//            coroutineScope.launch {
//                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(product.productName)
//                barcode?.let {
//                    // Assuming barcodeToProductIdMap should map barcode to product ID
//                    synchronized(this@ProductsActivity) {
//                        barcodeToProductIdMap[barcode] = product.productId
//                    }
//                }
//            }
//        }
//    }
//
//    private fun updateUIForProducts(products: List<ReceiptMoveLine>, receiptId: Int) {
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
//    //prompt for serial numbers all at once
////    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
////        coroutineScope.launch {
////            val productId = barcodeToProductIdMap[scannedBarcode]
////            if (productId != null) {
////                val productLines = productsAdapter.moveLines.filter { it.productId == productId }
////                Log.d("verifyBarcode", "Product Lines: ${productLines.size}")
////
////                productLines.forEach { productLine ->
////                    // Directly use the trackingType from the productLine object
////                    val trackingType = productLine.trackingType
////                    Log.d("verifyBarcode", "Tracking Type: $trackingType")
////
////                    withContext(Dispatchers.Main) {
////                        // Specific condition for "lot" type products that have been confirmed
////                        if (trackingType == "lot" && confirmedLines.contains(productLine.id)) {
////                            showAddNewLotDialog(productLine.productName, receiptId, productLine.productId, trackingType, productLine.id, productLine.useExpirationDate)
////                        } else {
////                            // Process according to tracking type if not already confirmed
////                            when (trackingType) {
////                                "serial" -> promptForSerialNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
////                                "lot" -> promptForLotNumber(productLine.productName, receiptId, productLine.productId, productLine.id)
////                                "none" -> promptForProductQuantity(productLine.productName, productLine.expectedQuantity, receiptId, productLine.productId, productLine.id, false)
////                                else -> Log.d("verifyBarcode", "Unhandled tracking type: $trackingType")
////                            }
////                        }
////                    }
////                }
////            } else {
////                withContext(Dispatchers.Main) {
////                    showRedToast("Barcode not found")
////                }
////            }
////        }
////    }
//    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            if (productId != null) {
//                val productLines = productsAdapter.moveLines.filter { it.productId == productId }.sortedBy { it.id }
//
//                // Check for any confirmed lot lines first to handle them appropriately
//                val confirmedLotProductLine = productLines.find { it.trackingType == "lot" && confirmedLines.contains(it.id) }
//                if (confirmedLotProductLine != null) {
//                    withContext(Dispatchers.Main) {
//                        showAddNewLotDialog(confirmedLotProductLine.productName, receiptId, confirmedLotProductLine.productId, confirmedLotProductLine.useExpirationDate)
//                    }
//                    return@launch
//                }
//
//                // Handle other cases with unconfirmed lines
//                val nextProductLine = productLines.find { !confirmedLines.contains(it.id) }
//                nextProductLine?.let { productLine ->
//                    val trackingType = productLine.trackingType
//                    withContext(Dispatchers.Main) {
//                        when (trackingType) {
//                            "serial" -> {
//                                promptForSerialNumber(
//                                    productLine.productName,
//                                    receiptId,
//                                    productLine.productId,
//                                    productLine.id
//                                )
//                            }
//                            "lot" -> {
//                                promptForLotNumber(
//                                    productLine.productName,
//                                    receiptId,
//                                    productLine.productId,
//                                    productLine.id
//                                )
//                            }
//                            "none" -> {
//                                promptForProductQuantity(
//                                    productLine.productName,
//                                    productLine.expectedQuantity,
//                                    receiptId,
//                                    productLine.productId,
//                                    productLine.id,
//                                    false
//                                )
//                            }
//                            else -> {
//                                Log.d("verifyBarcode", "Unhandled tracking type: $trackingType")
//                                barcodeInput.setText("")
//                                barcodeInput.requestFocus()
//                            }
//                        }
//                    }
//                } ?: withContext(Dispatchers.Main) {
//                    Toast.makeText(this@ProductsActivity, "All items for this product have been processed or no such product found.", Toast.LENGTH_SHORT).show()
//                    barcodeInput.setText("")
//                    barcodeInput.requestFocus()
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@ProductsActivity, "Barcode not found", Toast.LENGTH_SHORT).show()
//                    barcodeInput.setText("")
//                    barcodeInput.requestFocus()
//                }
//            }
//        }
//    }
//
//
//    private fun showAddNewLotDialog(productName: String, receiptId: Int, productId: Int, usesExpirationDate: Boolean?) {
//        // Inflate the custom layout for the dialog
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_lot, null)
////        val textDialogTitle = dialogView.findViewById<TextView>(R.id.textDialogTitle)
//        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
//
//        // Set the dynamic parts of the layout
////        textDialogTitle.text = "Add new lot"
//        textDialogMessage.text = getString(R.string.already_confirmed_message, productName)
//
//        // Build and show the AlertDialog
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false) // Optional: makes the dialog modal, i.e., it cannot be dismissed by tapping outside of it.
//            .create()
//
//        // Find and setup the buttons from the layout
//        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)
//        val addNewLotButton = dialogView.findViewById<Button>(R.id.buttonAddNewLot)
//
//        // Setup button listeners
//        cancelButton.setOnClickListener {
//            dialog.dismiss()
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//        addNewLotButton.setOnClickListener {
//            dialog.dismiss()
//            promptForNewLotNumber(productName, receiptId, productId, usesExpirationDate)
//
//        }
//        // Display the dialog
//        dialog.show()
//    }
//
//    private fun promptForNewLotNumber(productName: String, receiptId: Int, productId: Int, usesExpirationDate: Boolean?) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
//        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
//        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
//        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
//        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
//        editTextLotNumber.setHintTextColor(Color.WHITE)
//        textEnterLotMessage.text = getString(R.string.enter_lot_number, productName)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false)
//            .create()
//
//        buttonConfirmLot.setOnClickListener {
//            val enteredLotNumber = editTextLotNumber.text.toString().trim()
//            if (enteredLotNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val lotList = pickLotNumbers.getOrPut(receiptId) { mutableListOf() }
//
//                    if (!lotList.contains(enteredLotNumber)) {
//                        lotList.add(enteredLotNumber)
//                        withContext(Dispatchers.Main) {
//                            dialog.dismiss()
//                            showNewLotQuantityDialog(receiptId, productId, productName, enteredLotNumber, usesExpirationDate)
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@ProductsActivity, "Lot number already entered for this picking operation. Please enter a different number.", Toast.LENGTH_LONG).show()
//                            editTextLotNumber.setText("")
//                            editTextLotNumber.requestFocus()
//                        }
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
//                editTextLotNumber.requestFocus()
//            }
//        }
//
//        buttonCancelLot.setOnClickListener {
//            dialog.dismiss()
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//
//        dialog.show()
//        editTextLotNumber.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
//    }
//
//
//    private suspend fun showNewLotQuantityDialog(receiptId: Int, productId: Int, productName: String, lotName: String, usesExpirationDate: Boolean?) {
//        // Fetch the latest data first, ensure UI updates post-fetch are on the main thread
//        withContext(Dispatchers.IO) {
//            fetchProductsForReceipt(receiptId)
//        }
//
//        withContext(Dispatchers.Main) {
//            val dialogView = LayoutInflater.from(this@ProductsActivity).inflate(R.layout.dialog_lot_quantity_input, null)
//            val textEnterLotQuantityMessage = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityMessage)
//            val editTextLotQuantity = dialogView.findViewById<EditText>(R.id.editTextLotQuantity)
//            val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
//            editTextLotQuantity.setHintTextColor(Color.WHITE)
//
//            textEnterLotQuantityMessage.text = getString(R.string.enter_lot_quantity, lotName)
//
//            val dialog = AlertDialog.Builder(this@ProductsActivity)
//                .setView(dialogView)
//                .setCancelable(false)
//                .create()
//
//            buttonConfirmQuantity.setOnClickListener {
//                val enteredQuantity = editTextLotQuantity.text.toString().toDoubleOrNull()
//                if (enteredQuantity != null) {
//                    coroutineScope.launch {
//                        if (usesExpirationDate == true) {
//                            // No need to call createMoveLineWithExpirationForReceiving here
//                            // Just call the promptForNewLotExpirationDate function directly
//                            promptForNewLotExpirationDate(productName, receiptId, productId, lotName, enteredQuantity)
//                            dialog.dismiss()
//                        } else {
//                            // Otherwise, create the move line immediately
//                            try {
//                                val response = odooXmlRpcClient.createMoveLineForReceiving(receiptId, productId, lotName, enteredQuantity)
//                                val newLineId = response?.get("line_id") as? Int
//                                if (newLineId != null) {
//                                    updateProductMatchState(newLineId, receiptId, true)
//                                    withContext(Dispatchers.Main) {
//                                        dialog.dismiss()
//                                        Toast.makeText(this@ProductsActivity, "Lot quantity updated and match state set.", Toast.LENGTH_SHORT).show()
//                                        barcodeInput.setText("")
//                                        barcodeInput.requestFocus()
//
//                                    }
//                                } else {
//                                    withContext(Dispatchers.Main) {
//                                        Toast.makeText(this@ProductsActivity, "Failed to create new move line or retrieve line ID.", Toast.LENGTH_SHORT).show()
//                                        barcodeInput.setText("")
//                                        barcodeInput.requestFocus()
//                                    }
//                                }
//                            } catch (e: Exception) {
//                                withContext(Dispatchers.Main) {
//                                    Log.e("OdooXmlRpcClient", "Error creating move line for receiving: ${e.localizedMessage}", e)
//                                    Toast.makeText(this@ProductsActivity, "Error in creating move line.", Toast.LENGTH_SHORT).show()
//                                    barcodeInput.setText("")
//                                    barcodeInput.requestFocus()
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    Toast.makeText(this@ProductsActivity, "Invalid quantity entered. Please try again.", Toast.LENGTH_SHORT).show()
//                    barcodeInput.setText("")
//                    barcodeInput.requestFocus()
//                }
//            }
//
//            dialog.show()
//        }
//    }
//
//    private fun promptForNewLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotName: String, quantity: Double) {
//        // Move UI-related operations to the main thread
//        coroutineScope.launch {
//            withContext(Dispatchers.Main) {
//                val dialogView = LayoutInflater.from(this@ProductsActivity).inflate(R.layout.dialog_expiration_date, null)
//
//                val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
//                textDialogMessage.text = getString(R.string.enter_expiration_date_lot, productName)
//
//                val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
//                setupDateInputField(editTextExpirationDate) // Assumes this method is thread-safe or also switched to main thread
//                editTextExpirationDate.setHintTextColor(Color.WHITE)
//
//                val dialog = AlertDialog.Builder(this@ProductsActivity)
//                    .setView(dialogView)
//                    .create()
//
//                var lastClickTime = 0L
//                dialogView.findViewById<Button>(R.id.buttonOk).setOnClickListener {
//                    if (System.currentTimeMillis() - lastClickTime < 1000) {
//                        return@setOnClickListener // Debounce time of 1 second
//                    }
//                    lastClickTime = System.currentTimeMillis()
//
//                    val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
//                    val convertedDate = convertToFullDateTime(enteredExpirationDate) // Ensure this method is thread-safe
//                    if (isValidDateFormat(convertedDate)) {
//                        // Perform network request on IO Dispatcher
//                        coroutineScope.launch(Dispatchers.IO) {
//                            try {
//                                val response = odooXmlRpcClient.createMoveLineWithExpirationForReceiving(receiptId, productId, lotName, quantity, convertedDate)
//                                val newLineId = response?.get("line_id") as? Int
//                                if (newLineId != null) {
//                                    updateProductMatchState(newLineId, receiptId, matched = true)
//                                    confirmedLines.add(newLineId)
//                                    fetchProductsForReceipt(receiptId)
//                                    barcodeInput.setText("")
//                                    barcodeInput.requestFocus()
//                                } else {
//                                    withContext(Dispatchers.Main) {
//                                        Toast.makeText(this@ProductsActivity, "Failed to create new move line or retrieve line ID.", Toast.LENGTH_SHORT).show()
//                                        barcodeInput.setText("")
//                                        barcodeInput.requestFocus()
//                                    }
//                                }
//                            } catch (e: Exception) {
//                                withContext(Dispatchers.Main) {
//                                    Log.e("OdooXmlRpcClient", "Error creating move line with expiration: ${e.localizedMessage}", e)
//                                    Toast.makeText(this@ProductsActivity, "Error in creating move line with expiration.", Toast.LENGTH_SHORT).show()
//                                    barcodeInput.setText("")
//                                    barcodeInput.requestFocus()
//                                }
//                            } finally {
//                                withContext(Dispatchers.Main) {
//                                    dialog.dismiss()
//                                    barcodeInput.setText("")
//                                    barcodeInput.requestFocus()
//                                }
//                            }
//                        }
//                    } else {
//                        Toast.makeText(this@ProductsActivity, "Invalid expiration date entered. Please use the format DD/MM/YYYY", Toast.LENGTH_SHORT).show()
//                        barcodeInput.setText("")
//                        barcodeInput.requestFocus()
//                    }
//                }
//
//                dialogView.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
//                    dialog.dismiss()
//                    barcodeInput.setText("")
//                    barcodeInput.requestFocus()
//                }
//
//                dialog.show()
//            }
//        }
//    }
//
//    private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
//        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
//        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
//        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
//        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
//
//        textEnterLotMessage.text = getString(R.string.enter_lot_number_simple, productName)
//        editTextLotNumber.setHintTextColor(Color.WHITE)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false)
//            .create()
//
//        buttonConfirmLot.setOnClickListener {
//            val enteredLotNumber = editTextLotNumber.text.toString().trim()
//            if (enteredLotNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val lotList = pickLotNumbers.getOrPut(receiptId) { mutableListOf() }
//
//                    if (!lotList.contains(enteredLotNumber)) {
//                        lotList.add(enteredLotNumber)
//                        withContext(Dispatchers.Main) {
//                            dialog.dismiss()
//                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, productsAdapter.moveLines.find { it.productId == productId }?.useExpirationDate == true, lineId)
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@ProductsActivity, "Lot number already entered for this picking operation.", Toast.LENGTH_SHORT).show()
//                            editTextLotNumber.setText("")
//                            editTextLotNumber.requestFocus()
//                        }
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
//                editTextLotNumber.requestFocus()
//            }
//        }
//
//        buttonCancelLot.setOnClickListener {
//            dialog.dismiss()
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//
//        dialog.show()
//        editTextLotNumber.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
//    }
//
//    private fun promptForLotQuantity(productName: String, receiptId: Int, productId: Int, lotNumber: String, requiresExpirationDate: Boolean, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_quantity_input, null)
//        val messageTextView = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityMessage)
//        val editTextLotQuantity = dialogView.findViewById<EditText>(R.id.editTextLotQuantity)
//        val buttonCancelQuantity = dialogView.findViewById<Button>(R.id.buttonCancelQuantity)
//        val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
//        editTextLotQuantity.setHintTextColor(Color.WHITE)
//
//        messageTextView.text = getString(R.string.enter_quantity_for_lot, lotNumber)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//
//        buttonConfirmQuantity.setOnClickListener {
//            val enteredQuantity = editTextLotQuantity.text.toString().toIntOrNull()
//            if (enteredQuantity != null) {
//                coroutineScope.launch {
//                    if (requiresExpirationDate) {
//                        withContext(Dispatchers.Main) {
//                            promptForLotExpirationDate(productName, receiptId, productId, lotNumber, enteredQuantity, lineId)
//                            dialog.dismiss()
//                        }
//                    } else {
//                        odooXmlRpcClient.updateMoveLineLotAndQuantity(lineId, receiptId, productId, lotNumber, enteredQuantity)
//                        confirmedLines.add(lineId)
//                        updateProductMatchState(lineId, receiptId, matched = true)
//                        fetchProductsForReceipt(receiptId)
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@ProductsActivity, "Quantity updated for lot without expiration date.", Toast.LENGTH_SHORT).show()
//                            dialog.dismiss()
//                            barcodeInput.setText("")
//                            barcodeInput.requestFocus()
//                        }
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show()
//                barcodeInput.setText("")
//                barcodeInput.requestFocus()
//            }
//        }
//
//        buttonCancelQuantity.setOnClickListener {
//            dialog.dismiss()
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//
//        // Ensure the keyboard is shown when the EditText gains focus
//        dialog.setOnShowListener {
//            editTextLotQuantity.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editTextLotQuantity, InputMethodManager.SHOW_IMPLICIT)
//        }
//
//        dialog.show()
//    }
//
//    private fun promptForLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotNumber: String, quantity: Int, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expiration_date, null)
//
//        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
//        textDialogMessage.text = getString(R.string.enter_expiration_date_lot, productName)
//
//        val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
//        setupDateInputField(editTextExpirationDate)  // Assuming this sets up a listener for proper date formatting
//
//        editTextExpirationDate.setHintTextColor(Color.WHITE)
//
//        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
//        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//
//        buttonOk.setOnClickListener {
//            val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
//            val convertedDate = convertToFullDateTime(enteredExpirationDate)  // Ensure this converts to "yyyy-MM-dd"
//            if (isValidDateFormat(convertedDate)) {
//                coroutineScope.launch {
//                    odooXmlRpcClient.updateMoveLineLotExpiration(lineId, receiptId, productId, lotNumber, quantity, convertedDate).also {
//                        updateProductMatchState(lineId, receiptId, matched = true)
//                        confirmedLines.add(lineId)
//                        fetchProductsForReceipt(receiptId)
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@ProductsActivity, "Lot expiration date updated.", Toast.LENGTH_SHORT).show()
//                            dialog.dismiss()
//                            barcodeInput.setText("")
//                            barcodeInput.requestFocus()
//                        }
//                    }
//                }
//            } else {
//                Toast.makeText(this@ProductsActivity, "Invalid expiration date entered. Please use the format DD/MM/YYYY", Toast.LENGTH_SHORT).show()
//                barcodeInput.setText("")
//                barcodeInput.requestFocus()
//            }
//        }
//
//        buttonCancel.setOnClickListener {
//            dialog.dismiss()
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//
//        dialog.show()
//    }
//
//    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
//        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
//        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
//        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
//        val productMessage = dialogView.findViewById<TextView>(R.id.ProductMessage)
//        serialNumberInput.setHintTextColor(Color.WHITE)
//        productMessage.text = getString(R.string.product_message, productName)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
//            .create()
//
//        buttonConfirmSN.setOnClickListener {
//            val enteredSerialNumber = serialNumberInput.text.toString().trim()
//            if (enteredSerialNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    val product = productsAdapter.moveLines.find { it.productId == productId }
//                    if (product != null) {
//                        val serialList = pickSerialNumbers.getOrPut(receiptId) { mutableListOf() }
//                        if (!serialList.contains(enteredSerialNumber)) {
//                            if (product.useExpirationDate == true) {
//                                withContext(Dispatchers.Main) {
//                                    dialog.dismiss()
//                                    promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber, lineId)
//                                }
//                            } else {
//                                withContext(Dispatchers.IO) {
//                                    val updateResult = odooXmlRpcClient.updateMoveLineSerialNumber(lineId, receiptId, productId, enteredSerialNumber)
//                                    withContext(Dispatchers.Main) {
//                                        if (updateResult) {
//                                            serialList.add(enteredSerialNumber)
//                                            updateProductMatchState(lineId, receiptId, true)
//                                            confirmedLines.add(lineId)
//                                            coroutineScope.launch { fetchProductsForReceipt(receiptId) }
//                                            Toast.makeText(this@ProductsActivity, "Serial number added for $productName.", Toast.LENGTH_SHORT).show()
//                                            dialog.dismiss()
//                                            barcodeInput.setText("")
//                                            barcodeInput.requestFocus()
//                                        } else {
//                                            Toast.makeText(this@ProductsActivity, "Failed to update serial number.", Toast.LENGTH_SHORT).show()
//                                            barcodeInput.setText("")
//                                            barcodeInput.requestFocus()
//                                        }
//                                    }
//                                }
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(this@ProductsActivity, "Serial number already entered for this picking operation.", Toast.LENGTH_SHORT).show()
//                                serialNumberInput.setText("")
//                                serialNumberInput.requestFocus()
//                            }
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@ProductsActivity, "Product details not found.", Toast.LENGTH_SHORT).show()
//                            barcodeInput.setText("")
//                            barcodeInput.requestFocus()
//                        }
//                    }
//                }
//            } else {
//                Toast.makeText(this@ProductsActivity, "Please enter a serial number", Toast.LENGTH_SHORT).show()
//                serialNumberInput.setText("")
//                serialNumberInput.requestFocus()
//            }
//        }
//
//        buttonCancelSN.setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialog.show()
//        serialNumberInput.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
//    }
//
//
//
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
//
//    private fun promptForExpirationDate(productName: String, receiptId: Int, productId: Int, serialNumber: String, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expiration_date, null)
//        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
//        textDialogMessage.text = getString(R.string.enter_expiration_date, productName)
//        val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
//        setupDateInputField(editTextExpirationDate) // Assumes this method setups date picker or similar
//
//        // Setting the hint color to white
//        editTextExpirationDate.setHintTextColor(ContextCompat.getColor(this, android.R.color.white))
//
//        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
//        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//
//        buttonOk.setOnClickListener {
//            val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
//            val convertedDate = convertToFullDateTime(enteredExpirationDate) // Assume this correctly formats the date
//            if (isValidDateFormat(convertedDate)) { // Assume this validates the formatted date
//                coroutineScope.launch {
//                    odooXmlRpcClient.updateMoveLineSerialExpirationDate(lineId, receiptId, productId, serialNumber, convertedDate)
//                    updateProductMatchState(lineId, receiptId, matched = true)
//                    confirmedLines.add(lineId)
//                    fetchProductsForReceipt(receiptId)
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@ProductsActivity, "Expiration date updated and serial number added for $productName.", Toast.LENGTH_SHORT).show()
//                        dialog.dismiss()
//                        barcodeInput.setText("")
//                        barcodeInput.requestFocus()
//                    }
//                }
//            } else {
//                Toast.makeText(this@ProductsActivity, "Invalid expiration date entered. Please use the format DD/MM/YYYY", Toast.LENGTH_SHORT).show()
//                barcodeInput.setText("")
//                barcodeInput.requestFocus()
//            }
//        }
//
//        buttonCancel.setOnClickListener {
//            dialog.dismiss()
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//
//        dialog.show()
//    }
//
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
//            if (date != null) {
//                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
//            } else {
//                Log.e("Swiib", "Date parsing error: parsed date is null")
//                ""
//            }
//        } catch (e: ParseException) {
//            Log.e("Swiib", "Date parsing error: ${e.localizedMessage}")
//            ""
//        }
//    }
//
//
//    private fun promptForProductQuantity(productName: String, expectedQuantity: Double, receiptId: Int, productId: Int, lineId: Int, recount: Boolean = false) {
//        // Inflate the custom layout
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_input, null)
//        val editText = dialogView.findViewById<EditText>(R.id.quantityInput)
//        val titleTextView = dialogView.findViewById<TextView>(R.id.titleTextView)
//        titleTextView.text = if (recount) "Recount Required for $productName" else "Enter Quantity for $productName"
//
//        editText.setHintTextColor(Color.WHITE)
//
//        // Setup AlertDialog with custom view but no buttons
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false) // Disable dismissing the dialog by pressing back or touching outside
//            .create()
//
//        // Find and setup the custom buttons
//        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
//        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
//
//        // Cancel button logic
//        cancelButton.setOnClickListener {
//            dialog.dismiss() // This dismisses the dialog
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//
//        // Confirm button logic
//        confirmButton.setOnClickListener {
//            val enteredQuantity = editText.text.toString().toDoubleOrNull()
//            if (enteredQuantity != null) {
//                // Always update the quantity, regardless of its correctness
//                lifecycleScope.launch(Dispatchers.IO) {
//                    odooXmlRpcClient.updateMoveLineQuantityUntracked(
//                        lineId,
//                        receiptId,
//                        productId,
//                        enteredQuantity
//                    )
//                    coroutineScope.launch {
//                        fetchProductsForReceipt(receiptId)
//                    }
//                }
//
//                // Check if the quantity is correct
//                if (enteredQuantity == expectedQuantity) {
//                    Toast.makeText(this@ProductsActivity, "Quantity updated for $productName", Toast.LENGTH_SHORT).show()
//                    updateProductMatchState(lineId, receiptId, true)
//                    confirmedLines.add(lineId)
//                    barcodeInput.setText("")
//                    barcodeInput.requestFocus()
//                } else if (!recount) {
//                    promptForProductQuantity(productName, expectedQuantity, receiptId, productId, lineId, recount = true)
//                } else {
//                    handleIncorrectRecount(productName)
//                }
//
//                dialog.dismiss() // Dismiss the dialog after confirming
//            } else {
//                Toast.makeText(this, "Invalid quantity entered", Toast.LENGTH_SHORT).show()
//                barcodeInput.setText("")
//                barcodeInput.requestFocus()
//            }
//        }
//
//        // Display the dialog
//        dialog.show()
//    }
//
//    private fun handleIncorrectRecount(productName: String) {
//        val localReceiptId = receiptName  // Copy the mutable property to a local variable
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            val buyerDetails = localReceiptId?.let { odooXmlRpcClient.fetchAndLogBuyerDetails(it) }
//            if (buyerDetails != null) {
//                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, localReceiptId, productName) // Pass the local copy to the function
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@ProductsActivity, "Flagged ${buyerDetails.login}. Email sent.", Toast.LENGTH_SHORT).show()
//                    barcodeInput.setText("")
//                    barcodeInput.requestFocus()
//                }
//            } else {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@ProductsActivity, "Flagged, but buyer details not found.", Toast.LENGTH_SHORT).show()
//                    barcodeInput.setText("")
//                    barcodeInput.requestFocus()
//                }
//            }
//        }
//    }
//
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
//                - Receipt Name: $receiptName
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
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        } catch (e: MessagingException) {
//            Log.e("EmailSender", "Failed to send email.", e)
//            barcodeInput.setText("")
//            barcodeInput.requestFocus()
//        }
//    }
//
//    private fun updateProductMatchState(lineId: Int, pickId: Int, matched: Boolean = true) {
//        coroutineScope.launch {
//            fetchProductsForReceipt(pickId)  // Fetch and wait for completion
//
//            withContext(Dispatchers.Main) {
//                val key = ProductReceiptKey(lineId, pickId)
//                val productLine = productsAdapter.moveLines.find { it.id == lineId }
//
//                if (productLine != null) {
//                    quantityMatches[key] = matched
//                    val position = productsAdapter.findProductPositionById(lineId)
//                    if (position != -1) {
//                        productsAdapter.notifyItemChanged(position)
//                    }
//                    checkAndToggleValidateButton(pickId)
//                    saveMatchStateToPreferences(key, matched)
//                } else {
//                    Log.e("updateProductMatchState", "No line found for ID $lineId after refreshing data")
//                }
//            }
//        }
//    }
//
//
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
//                    val validationSuccessful = odooXmlRpcClient.validateOperation(pickId)
//                    runOnUiThread {
//                        if (validationSuccessful) {
//                            Toast.makeText(applicationContext, "Receipt validated successfully.", Toast.LENGTH_SHORT).show()
//                            val mediaPlayer: MediaPlayer? = MediaPlayer.create(applicationContext, R.raw.button_pressed)
//                            mediaPlayer?.start()
//                            mediaPlayer?.setOnCompletionListener {
//                                it.release()
//                            }
//                            // Redirect to PickActivity upon successful validation
//                            val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
//                            startActivity(intent)
//                            finish()  // Optionally call finish() if you want to remove this activity from the back stack
//                            barcodeInput.setText("")
//                            barcodeInput.requestFocus()
//                        } else {
//                            Toast.makeText(applicationContext, "Failed to validate receipt.\nPlease flag or recount quantities. ", Toast.LENGTH_SHORT).show()
//                            barcodeInput.setText("")
//                            barcodeInput.requestFocus()
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun saveButtonVisibilityState(pickId: Int, visible: Boolean) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        with(sharedPref.edit()) {
//            putBoolean("ValidateButtonVisible_$pickId", visible)
//            apply()
//        }
//    }
//
//    private fun restoreButtonVisibility(pickId: Int) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val isVisible = sharedPref.getBoolean("ValidateButtonVisible_$pickId", false)
//        val validateButton = findViewById<Button>(R.id.pickValidateButton)
//        validateButton.visibility = if (isVisible) View.VISIBLE else View.GONE
//
//        if (isVisible) {
//            setupValidateButtonListener(pickId)
//        }
//    }
//
//    private fun setupValidateButtonListener(pickId: Int) {
//        val validateButton = findViewById<Button>(R.id.pickValidateButton)
//        validateButton.setOnClickListener {
//            coroutineScope.launch {
//                val validationSuccessful = odooXmlRpcClient.validateOperation(pickId)
//                runOnUiThread {
//                    if (validationSuccessful) {
//                        Toast.makeText(applicationContext, "Picking validated successfully.", Toast.LENGTH_SHORT).show()
//                        // Play the sound
//                        val mediaPlayer: MediaPlayer? = MediaPlayer.create(applicationContext, R.raw.button_pressed)
//                        mediaPlayer?.start()
//                        mediaPlayer?.setOnCompletionListener {
//                            it.release()
//                        }
//                        val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
//                        startActivity(intent)
//                        finish()  // Optionally call finish() if you want to remove this activity from the back stack
//                        barcodeInput.setText("")
//                        barcodeInput.requestFocus()
//                    } else {
//                        Toast.makeText(applicationContext, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
//                        barcodeInput.setText("")
//                        barcodeInput.requestFocus()
//                    }
//                }
//            }
//        }
//    }
//
//
//    private fun saveMatchStateToPreferences(key: ProductReceiptKey, matched: Boolean) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        with(sharedPref.edit()) {
//            putBoolean("${key.moveLineId}_${key.pickId}", matched)
//            apply()
//        }
//    }
//
//
//    private fun loadMatchStatesFromPreferences(pickId: Int) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val tempQuantityMatches = mutableMapOf<ProductReceiptKey, Boolean>()
//
//        sharedPref.all.forEach { (prefKey, value) ->
//            if (value is Boolean) {
//                if (prefKey.startsWith("ValidateButtonVisible")) {
//                    // Correct handling of the ValidateButtonVisible key
//                    val parts = prefKey.split("_")
//                    if (parts.size == 2) {
//                        val storedPickId = parts[1].toIntOrNull()
//                        if (storedPickId == pickId) {
//                            findViewById<Button>(R.id.pickValidateButton).visibility = if (value) View.VISIBLE else View.GONE
//                        }
//                    }
//                } else {
//                    // Handling standard product match keys
//                    val parts = prefKey.split("_")
//                    if (parts.size == 2) {
//                        try {
//                            val moveLineId = parts[0].toInt()
//                            val prefPickId = parts[1].toInt()
//                            if (prefPickId == pickId) {
//                                val key = ProductReceiptKey(moveLineId, prefPickId)
//                                tempQuantityMatches[key] = value
//                            }
//                        } catch (e: NumberFormatException) {
//                            Log.e("PickProductsActivity", "Error parsing shared preference key: $prefKey", e)
//                        }
//                    } else {
//                        Log.e("PickProductsActivity", "Incorrectly formatted shared preference key: $prefKey. Expected format: productId_pickId")
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
//            productsAdapter.updateProducts(productsAdapter.moveLines, pickId, quantityMatches)
//        }
//    }
//    override fun onDestroy() {
//        super.onDestroy()
//        coroutineScope.cancel()
//    }
//
//    override fun onProductClick(product: ReceiptMoveLine) {
//        showProductDialog(product)
//    }
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
//        editTextProductLotNumber.setHintTextColor(Color.WHITE)
//        editTextQuantity.setHintTextColor(Color.WHITE)
//
//        // Set values to the TextViews
//        textProductName.text = product.productName
//        textProductQuantity.text = "${product.quantity}"
//        textProductToLocation.text = product.locationDestName
//        textProductLotNumber.text = product.lotName
//
//        // Flags to track if edits were made
//        var quantityChanged = false
//        var lotNameChanged = false
//
//        // Create and show the dialog
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//        dialog.show()
//
//        // Setup visibility based on product tracking type
//        lotNumberLayout.visibility = View.GONE
//        buttonEditLotNumber.visibility = View.GONE
//        buttonEditQuantity.visibility = View.GONE
//
//        coroutineScope.launch {
//            withContext(Dispatchers.Main) {
//                if (product.trackingType == "serial") {
//                    buttonEditQuantity.visibility = View.GONE
//                    if (product.lotName.isNotEmpty()) {
//                        lotNumberLayout.visibility = View.VISIBLE
//                        buttonEditLotNumber.visibility = View.VISIBLE
//                    }
//                } else if (product.trackingType == "lot" && product.lotName.isNotEmpty()) {
//                    lotNumberLayout.visibility = View.VISIBLE
//                    buttonEditLotNumber.visibility = View.VISIBLE
//                    buttonEditQuantity.visibility = View.VISIBLE
//                } else if (product.trackingType == "none") {
//                    buttonEditQuantity.visibility = View.VISIBLE
//                    lotNumberLayout.visibility = View.GONE
//                    buttonEditLotNumber.visibility = View.GONE
//                } else {
//                    lotNumberLayout.visibility = View.VISIBLE
//                    buttonEditLotNumber.visibility = View.GONE
//                    buttonEditQuantity.visibility = View.VISIBLE
//                }
//            }
//        }
//        // Button handlers for editing quantity and lot number
//        buttonEditQuantity.setOnClickListener {
//            editTextQuantity.visibility = View.VISIBLE
//            editTextQuantity.setText(product.quantity.toString())
//            textProductQuantity.visibility = View.GONE
//            quantityChanged = true
//        }
//        buttonEditLotNumber.setOnClickListener {
//            editTextProductLotNumber.visibility = View.VISIBLE
//            editTextProductLotNumber.setText(product.lotName)
//            textProductLotNumber.visibility = View.GONE
//            lotNameChanged = true
//        }
//
//        buttonConfirmQuantity.setOnClickListener {
//            coroutineScope.launch(Dispatchers.IO) { // Start coroutine in IO Dispatcher for network operations
//                try {
//                    when {
//                        quantityChanged && lotNameChanged -> {
//                            val newQuantity = editTextQuantity.text.toString().toFloat().toInt()
//                            odooXmlRpcClient.updateMoveLineQuantityForReceipt(product.id, receiptId, newQuantity)
//                            odooXmlRpcClient.updateMoveLinesForReceipt(product.id, receiptId, editTextProductLotNumber.text.toString())
//                        }
//                        quantityChanged -> {
//                            val newQuantity = editTextQuantity.text.toString().toFloat().toInt()
//                            odooXmlRpcClient.updateMoveLineQuantityForReceipt(product.id, receiptId, newQuantity)
//                        }
//                        lotNameChanged -> {
//                            odooXmlRpcClient.updateMoveLinesForReceipt(product.id, receiptId, editTextProductLotNumber.text.toString())
//                        }
//                    }
//                    // Execute fetching of products on the same background thread
//                    fetchProductsForReceipt(receiptId)
//
//                    withContext(Dispatchers.Main) { // Switch to Main Dispatcher for UI operations
//                        dialog.dismiss()
//                        barcodeInput.setText("")
//                        barcodeInput.requestFocus()
//                    }
//                } catch (e: NumberFormatException) {
//                    withContext(Dispatchers.Main) { // Handle UI updates on the main thread
//                        Toast.makeText(this@ProductsActivity, "Invalid input. Please enter a valid number.", Toast.LENGTH_SHORT).show()
//                    }
//                } catch (e: Exception) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@ProductsActivity, "Error during network operations: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
//
//
//        buttonCancel.setOnClickListener {
//            dialog.dismiss()
//        }
//    }
//
//    private fun captureImage(pickId: Int) {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Capture Image?")
//        builder.setMessage("Would you like to capture an image?")
//        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)
//
//        builder.setNegativeButton("No") { dialog, _ ->
//            vibrateDevice(vibrator)
//            dialog.dismiss()
//        }
//
//        builder.setPositiveButton("Capture Image") { dialog, _ ->
//            vibrateDevice(vibrator)
//            dialog.dismiss()
//            Log.d("CaptureImage", "Opening camera for packId: $pickId")
//            openCamera(pickId)
//        }
//
//        val dialog = builder.create()
//        dialog.show()
//    }
//
//    private fun openCamera(packId: Int) {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),
//                PackProductsActivity.CAMERA_REQUEST_CODE
//            )
//        } else {
//            Log.d("CaptureImage", "Camera permission granted, starting camera intent for packId: $packId")
//            startCameraIntent()
//        }
//    }
//    private fun startCameraIntent() {
//        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        try {
//            if (cameraIntent.resolveActivity(packageManager) != null) {
//                cameraLauncher.launch(cameraIntent)
//            } else {
//                Log.e("CameraIntent", "No application can handle camera intent.")
//                Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            Log.e("CameraIntent", "Failed to start camera intent", e)
//            Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == PackProductsActivity.CAMERA_REQUEST_CODE) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                Log.d("CaptureImage", "Camera permission granted, starting camera intent")
//                startCameraIntent()
//            } else {
//                Toast.makeText(this, "Camera permission is necessary to capture images", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//    private fun vibrateDevice(vibrator: Vibrator?) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
//        } else {
//            @Suppress("DEPRECATION")
//            vibrator?.vibrate(50)
//        }
//    }
//    companion object {
//        private const val CAMERA_REQUEST_CODE = 1001
//    }
//    private inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
//        Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
//        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
//    }
//
//}

class ProductsActivity : AppCompatActivity(), ProductsAdapter.OnProductClickListener {
    private lateinit var productsAdapter: ProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    private var pickSerialNumbers = hashMapOf<Int, MutableList<String>>()
    private var pickLotNumbers = hashMapOf<Int, MutableList<String>>()

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
            coroutineScope.launch { fetchProductsForReceipt(receiptId) }
        } else {
            Log.e("ProductsActivity", "Invalid receipt ID passed to ProductsActivity.")
        }

        findViewById<Button>(R.id.clearButton).setOnClickListener {
            findViewById<EditText>(R.id.barcodeInput).text.clear()
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val imageBitmap = data?.parcelable<Bitmap>("data")
                if (imageBitmap != null) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()
                    val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

                    Log.d("CaptureImage", "Encoded image: $encodedImage")

                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                odooXmlRpcClient.updatePickingImage(receiptId, encodedImage)
                            }
                            Log.d("OdooUpdate", "Image updated successfully on server")

                            // Ensure UI updates are on the main thread
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Image updated successfully on server", Toast.LENGTH_SHORT).show()
                                barcodeInput.setText("")
                                barcodeInput.requestFocus()
                            }
                        } catch (e: Exception) {
                            Log.e("OdooUpdate", "Failed to update image: ${e.localizedMessage}", e)

                            // Ensure UI updates are on the main thread
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Failed to update image.", Toast.LENGTH_SHORT).show()
                                barcodeInput.setText("")
                                barcodeInput.requestFocus()
                            }
                        }
                    }
                } else {
                    Log.e("CaptureImage", "Failed to capture image")
                }
            } else {
                Log.e("CaptureImage", "Camera action was cancelled or failed")
                Toast.makeText(this, "Camera action was cancelled or failed.", Toast.LENGTH_SHORT).show()
            }
        }

        val enterManuallyButton: Button = findViewById(R.id.pickManualEntryButton)
        val confirmButton: Button = findViewById(R.id.confirmButton)
        val clearButton: Button = findViewById(R.id.clearButton)
        val barcodeInput: EditText = findViewById(R.id.barcodeInput)

        enterManuallyButton.setOnClickListener {
            confirmButton.visibility = View.VISIBLE
            clearButton.visibility = View.VISIBLE
            enterManuallyButton.visibility = View.GONE
            barcodeInput.apply {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                isCursorVisible = true
                requestFocus()
                setBackgroundResource(R.drawable.edittext_border)
                hint = "Enter Barcode"
                val layoutParams = this.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                this.layoutParams = layoutParams
            }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_flag_receipt -> {
                showDialogForFlag()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDialogForFlag() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnFlagPick).setOnClickListener {
            flagReceipt()
            dialog.dismiss()
        }

        dialog.show()
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
                    Toast.makeText(this@ProductsActivity, "Receipt flagged", Toast.LENGTH_LONG).show()
                    captureImage(receiptId)
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    Log.e("ProductsActivity", "Failed to fetch buyer details or flag the receipt.")
                    Toast.makeText(this@ProductsActivity, "Failed to flag receipt", Toast.LENGTH_SHORT).show()
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
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
            // Ensure UI updates are on the main thread
            runOnUiThread {
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        } catch (e: MessagingException) {
            Log.e("EmailSender", "Failed to send email.", e)
            // Ensure UI updates are on the main thread
            runOnUiThread {
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
        productsAdapter = ProductsAdapter(emptyList(), mapOf(), receiptId, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productsAdapter
    }

    private fun setupBarcodeVerification(receiptId: Int) {
        fun performBarcodeVerification() {
            val enteredBarcode = barcodeInput.text.toString().trim()
            verifyBarcode(enteredBarcode, receiptId)
            hideKeyboard()
            barcodeInput.setText("")
        }

        confirmButton.setOnClickListener {
            performBarcodeVerification()
        }

        barcodeInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                if (confirmButton.visibility == View.GONE || confirmButton.visibility == View.INVISIBLE) {
                    performBarcodeVerification()
                } else {
                    confirmButton.performClick()
                }
                true
            } else false
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
    }

    private suspend fun fetchProductsForReceipt(pickId: Int) {
        try {
            Log.d("PickProductsActivity", "Fetching products for pick ID: $pickId")
            val fetchedLines = odooXmlRpcClient.fetchReceiptMoveLinesByPickingId(pickId)
            val updatedMoveLinesWithDetails = mutableListOf<ReceiptMoveLine>()

            // Optionally fetch additional package information if required
            odooXmlRpcClient.fetchResultPackagesByPickingId(pickId)

            // Fetch product tracking and expiration details asynchronously
            val fetchJobs = fetchedLines.map { moveLine ->
                coroutineScope.async(Dispatchers.IO) {
                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(moveLine.productName) ?: Pair("none", false)
                    val barcode = barcodeToProductIdMap.filterValues { it == moveLine.productId }.keys.firstOrNull()?.toString()

                    moveLine.copy(
                        trackingType = trackingAndExpiration.first ?: "none",
                        useExpirationDate = trackingAndExpiration.second,
                        barcode = barcode
                    )
                }
            }

            updatedMoveLinesWithDetails.addAll(fetchJobs.awaitAll())

            // Optionally fetch barcodes for all products in the fetched lines
            fetchBarcodesForProducts(fetchedLines)

            // After all async operations complete, update the UI on the main thread
            withContext(Dispatchers.Main) {
                // Call updateUIForProducts instead of directly updating productsAdapter to maintain abstraction
                updateUIForProducts(updatedMoveLinesWithDetails, pickId)
                Log.d("PickProductsActivity", "UI updated with detailed move lines for Pick ID: $pickId")
            }
        } catch (e: Exception) {
            Log.e("PickProductsActivity", "Error fetching move lines or updating UI: ${e.localizedMessage}", e)
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

    private fun verifyBarcode(scannedBarcode: String, receiptId: Int) {
        coroutineScope.launch {
            val productId = barcodeToProductIdMap[scannedBarcode]
            if (productId != null) {
                val productLines = productsAdapter.moveLines.filter { it.productId == productId }.sortedBy { it.id }

                // Check for any confirmed lot lines first to handle them appropriately
                val confirmedLotProductLine = productLines.find { it.trackingType == "lot" && confirmedLines.contains(it.id) }
                if (confirmedLotProductLine != null) {
                    withContext(Dispatchers.Main) {
                        showAddNewLotDialog(confirmedLotProductLine.productName, receiptId, confirmedLotProductLine.productId, confirmedLotProductLine.useExpirationDate)
                    }
                    return@launch
                }

                // Handle other cases with unconfirmed lines
                val nextProductLine = productLines.find { !confirmedLines.contains(it.id) }
                nextProductLine?.let { productLine ->
                    val trackingType = productLine.trackingType
                    withContext(Dispatchers.Main) {
                        when (trackingType) {
                            "serial" -> {
                                promptForSerialNumber(
                                    productLine.productName,
                                    receiptId,
                                    productLine.productId,
                                    productLine.id
                                )
                            }
                            "lot" -> {
                                promptForLotNumber(
                                    productLine.productName,
                                    receiptId,
                                    productLine.productId,
                                    productLine.id
                                )
                            }
                            "none" -> {
                                promptForProductQuantity(
                                    productLine.productName,
                                    productLine.expectedQuantity,
                                    receiptId,
                                    productLine.productId,
                                    productLine.id,
                                    false
                                )
                            }
                            else -> {
                                Log.d("verifyBarcode", "Unhandled tracking type: $trackingType")
                                barcodeInput.setText("")
                                barcodeInput.requestFocus()
                            }
                        }
                    }
                } ?: withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductsActivity, "All items for this product have been processed or no such product found.", Toast.LENGTH_SHORT).show()
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductsActivity, "Barcode not found", Toast.LENGTH_SHORT).show()
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
                }
            }
        }
    }

    private fun showAddNewLotDialog(productName: String, receiptId: Int, productId: Int, usesExpirationDate: Boolean?) {
        // Inflate the custom layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_lot, null)
        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)

        // Set the dynamic parts of the layout
        textDialogMessage.text = getString(R.string.already_confirmed_message, productName)

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
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }
        addNewLotButton.setOnClickListener {
            dialog.dismiss()
            promptForNewLotNumber(productName, receiptId, productId, usesExpirationDate)
        }
        // Display the dialog
        dialog.show()
    }

    private fun promptForNewLotNumber(productName: String, receiptId: Int, productId: Int, usesExpirationDate: Boolean?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)
        editTextLotNumber.setHintTextColor(Color.WHITE)
        textEnterLotMessage.text = getString(R.string.enter_lot_number, productName)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        buttonConfirmLot.setOnClickListener {
            val enteredLotNumber = editTextLotNumber.text.toString().trim()
            if (enteredLotNumber.isNotEmpty()) {
                coroutineScope.launch {
                    val lotList = pickLotNumbers.getOrPut(receiptId) { mutableListOf() }

                    if (!lotList.contains(enteredLotNumber)) {
                        lotList.add(enteredLotNumber)
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            showNewLotQuantityDialog(receiptId, productId, productName, enteredLotNumber, usesExpirationDate)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProductsActivity, "Lot number already entered for this picking operation. Please enter a different number.", Toast.LENGTH_LONG).show()
                            editTextLotNumber.setText("")
                            editTextLotNumber.requestFocus()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
                editTextLotNumber.requestFocus()
            }
        }

        buttonCancelLot.setOnClickListener {
            dialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }

        dialog.show()
        editTextLotNumber.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
    }

    private suspend fun showNewLotQuantityDialog(receiptId: Int, productId: Int, productName: String, lotName: String, usesExpirationDate: Boolean?) {
        // Fetch the latest data first, ensure UI updates post-fetch are on the main thread
        withContext(Dispatchers.IO) {
            fetchProductsForReceipt(receiptId)
        }

        withContext(Dispatchers.Main) {
            val dialogView = LayoutInflater.from(this@ProductsActivity).inflate(R.layout.dialog_lot_quantity_input, null)
            val textEnterLotQuantityMessage = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityMessage)
            val editTextLotQuantity = dialogView.findViewById<EditText>(R.id.editTextLotQuantity)
            val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
            editTextLotQuantity.setHintTextColor(Color.WHITE)

            textEnterLotQuantityMessage.text = getString(R.string.enter_lot_quantity, lotName)

            val dialog = AlertDialog.Builder(this@ProductsActivity)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            buttonConfirmQuantity.setOnClickListener {
                val enteredQuantity = editTextLotQuantity.text.toString().toDoubleOrNull()
                if (enteredQuantity != null) {
                    coroutineScope.launch {
                        if (usesExpirationDate == true) {
                            // No need to call createMoveLineWithExpirationForReceiving here
                            // Just call the promptForNewLotExpirationDate function directly
                            promptForNewLotExpirationDate(productName, receiptId, productId, lotName, enteredQuantity)
                            dialog.dismiss()
                        } else {
                            // Otherwise, create the move line immediately
                            try {
                                val response = odooXmlRpcClient.createMoveLineForReceiving(receiptId, productId, lotName, enteredQuantity)
                                val newLineId = response?.get("line_id") as? Int
                                if (newLineId != null) {
                                    updateProductMatchState(newLineId, receiptId, true)
                                    withContext(Dispatchers.Main) {
                                        dialog.dismiss()
                                        Toast.makeText(this@ProductsActivity, "Lot quantity updated and match state set.", Toast.LENGTH_SHORT).show()
                                        barcodeInput.setText("")
                                        barcodeInput.requestFocus()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@ProductsActivity, "Failed to create new move line or retrieve line ID.", Toast.LENGTH_SHORT).show()
                                        barcodeInput.setText("")
                                        barcodeInput.requestFocus()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Log.e("OdooXmlRpcClient", "Error creating move line for receiving: ${e.localizedMessage}", e)
                                    Toast.makeText(this@ProductsActivity, "Error in creating move line.", Toast.LENGTH_SHORT).show()
                                    barcodeInput.setText("")
                                    barcodeInput.requestFocus()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@ProductsActivity, "Invalid quantity entered. Please try again.", Toast.LENGTH_SHORT).show()
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
                }
            }

            dialog.show()
        }
    }

    private fun promptForNewLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotName: String, quantity: Double) {
        // Move UI-related operations to the main thread
        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                val dialogView = LayoutInflater.from(this@ProductsActivity).inflate(R.layout.dialog_expiration_date, null)

                val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
                textDialogMessage.text = getString(R.string.enter_expiration_date_lot, productName)

                val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
                setupDateInputField(editTextExpirationDate) // Assumes this method is thread-safe or also switched to main thread
                editTextExpirationDate.setHintTextColor(Color.WHITE)

                val dialog = AlertDialog.Builder(this@ProductsActivity)
                    .setView(dialogView)
                    .create()

                var lastClickTime = 0L
                dialogView.findViewById<Button>(R.id.buttonOk).setOnClickListener {
                    if (System.currentTimeMillis() - lastClickTime < 1000) {
                        return@setOnClickListener // Debounce time of 1 second
                    }
                    lastClickTime = System.currentTimeMillis()

                    val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
                    val convertedDate = convertToFullDateTime(enteredExpirationDate) // Ensure this method is thread-safe
                    if (isValidDateFormat(convertedDate)) {
                        // Perform network request on IO Dispatcher
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val response = odooXmlRpcClient.createMoveLineWithExpirationForReceiving(receiptId, productId, lotName, quantity, convertedDate)
                                val newLineId = response?.get("line_id") as? Int
                                if (newLineId != null) {
                                    updateProductMatchState(newLineId, receiptId, matched = true)
                                    confirmedLines.add(newLineId)
                                    fetchProductsForReceipt(receiptId)
                                    barcodeInput.setText("")
                                    barcodeInput.requestFocus()
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@ProductsActivity, "Failed to create new move line or retrieve line ID.", Toast.LENGTH_SHORT).show()
                                        barcodeInput.setText("")
                                        barcodeInput.requestFocus()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Log.e("OdooXmlRpcClient", "Error creating move line with expiration: ${e.localizedMessage}", e)
                                    Toast.makeText(this@ProductsActivity, "Error in creating move line with expiration.", Toast.LENGTH_SHORT).show()
                                    barcodeInput.setText("")
                                    barcodeInput.requestFocus()
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    dialog.dismiss()
                                    barcodeInput.setText("")
                                    barcodeInput.requestFocus()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this@ProductsActivity, "Invalid expiration date entered. Please use the format DD/MM/YYYY", Toast.LENGTH_SHORT).show()
                        barcodeInput.setText("")
                        barcodeInput.requestFocus()
                    }
                }

                dialogView.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
                    dialog.dismiss()
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
                }

                dialog.show()
            }
        }
    }

    private fun promptForLotNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_number_input, null)
        val editTextLotNumber = dialogView.findViewById<EditText>(R.id.editTextLotNumber)
        val textEnterLotMessage = dialogView.findViewById<TextView>(R.id.textEnterLotMessage)
        val buttonCancelLot = dialogView.findViewById<Button>(R.id.buttonCancelLot)
        val buttonConfirmLot = dialogView.findViewById<Button>(R.id.buttonConfirmLot)

        textEnterLotMessage.text = getString(R.string.enter_lot_number_simple, productName)
        editTextLotNumber.setHintTextColor(Color.WHITE)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        buttonConfirmLot.setOnClickListener {
            val enteredLotNumber = editTextLotNumber.text.toString().trim()
            if (enteredLotNumber.isNotEmpty()) {
                coroutineScope.launch {
                    val lotList = pickLotNumbers.getOrPut(receiptId) { mutableListOf() }

                    if (!lotList.contains(enteredLotNumber)) {
                        lotList.add(enteredLotNumber)
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            promptForLotQuantity(productName, receiptId, productId, enteredLotNumber, productsAdapter.moveLines.find { it.productId == productId }?.useExpirationDate == true, lineId)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProductsActivity, "Lot number already entered for this picking operation.", Toast.LENGTH_SHORT).show()
                            editTextLotNumber.setText("")
                            editTextLotNumber.requestFocus()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a lot number.", Toast.LENGTH_SHORT).show()
                editTextLotNumber.requestFocus()
            }
        }

        buttonCancelLot.setOnClickListener {
            dialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }

        dialog.show()
        editTextLotNumber.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTextLotNumber, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun promptForLotQuantity(productName: String, receiptId: Int, productId: Int, lotNumber: String, requiresExpirationDate: Boolean, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_quantity_input, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.textEnterLotQuantityMessage)
        val editTextLotQuantity = dialogView.findViewById<EditText>(R.id.editTextLotQuantity)
        val buttonCancelQuantity = dialogView.findViewById<Button>(R.id.buttonCancelQuantity)
        val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
        editTextLotQuantity.setHintTextColor(Color.WHITE)

        messageTextView.text = getString(R.string.enter_quantity_for_lot, lotNumber)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        buttonConfirmQuantity.setOnClickListener {
            val enteredQuantity = editTextLotQuantity.text.toString().toIntOrNull()
            if (enteredQuantity != null) {
                coroutineScope.launch {
                    if (requiresExpirationDate) {
                        withContext(Dispatchers.Main) {
                            promptForLotExpirationDate(productName, receiptId, productId, lotNumber, enteredQuantity, lineId)
                            dialog.dismiss()
                        }
                    } else {
                        odooXmlRpcClient.updateMoveLineLotAndQuantity(lineId, receiptId, productId, lotNumber, enteredQuantity)
                        confirmedLines.add(lineId)
                        updateProductMatchState(lineId, receiptId, matched = true)
                        fetchProductsForReceipt(receiptId)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProductsActivity, "Quantity updated for lot without expiration date.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            barcodeInput.setText("")
                            barcodeInput.requestFocus()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show()
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        }

        buttonCancelQuantity.setOnClickListener {
            dialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }

        // Ensure the keyboard is shown when the EditText gains focus
        dialog.setOnShowListener {
            editTextLotQuantity.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextLotQuantity, InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.show()
    }

    private fun promptForLotExpirationDate(productName: String, receiptId: Int, productId: Int, lotNumber: String, quantity: Int, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expiration_date, null)

        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
        textDialogMessage.text = getString(R.string.enter_expiration_date_lot, productName)

        val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
        setupDateInputField(editTextExpirationDate)  // Assuming this sets up a listener for proper date formatting

        editTextExpirationDate.setHintTextColor(Color.WHITE)

        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        buttonOk.setOnClickListener {
            val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
            val convertedDate = convertToFullDateTime(enteredExpirationDate)  // Ensure this converts to "yyyy-MM-dd"
            if (isValidDateFormat(convertedDate)) {
                coroutineScope.launch {
                    odooXmlRpcClient.updateMoveLineLotExpiration(lineId, receiptId, productId, lotNumber, quantity, convertedDate).also {
                        updateProductMatchState(lineId, receiptId, matched = true)
                        confirmedLines.add(lineId)
                        fetchProductsForReceipt(receiptId)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProductsActivity, "Lot expiration date updated.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            barcodeInput.setText("")
                            barcodeInput.requestFocus()
                        }
                    }
                }
            } else {
                Toast.makeText(this@ProductsActivity, "Invalid expiration date entered. Please use the format DD/MM/YYYY", Toast.LENGTH_SHORT).show()
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }

        dialog.show()
    }

    private fun promptForSerialNumber(productName: String, receiptId: Int, productId: Int, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
        val productMessage = dialogView.findViewById<TextView>(R.id.ProductMessage)
        serialNumberInput.setHintTextColor(Color.WHITE)
        productMessage.text = getString(R.string.product_message, productName)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
            .create()

        buttonConfirmSN.setOnClickListener {
            val enteredSerialNumber = serialNumberInput.text.toString().trim()
            if (enteredSerialNumber.isNotEmpty()) {
                coroutineScope.launch {
                    val product = productsAdapter.moveLines.find { it.productId == productId }
                    if (product != null) {
                        val serialList = pickSerialNumbers.getOrPut(receiptId) { mutableListOf() }
                        if (!serialList.contains(enteredSerialNumber)) {
                            if (product.useExpirationDate == true) {
                                withContext(Dispatchers.Main) {
                                    dialog.dismiss()
                                    promptForExpirationDate(productName, receiptId, productId, enteredSerialNumber, lineId)
                                }
                            } else {
                                withContext(Dispatchers.IO) {
                                    val updateResult = odooXmlRpcClient.updateMoveLineSerialNumber(lineId, receiptId, productId, enteredSerialNumber)
                                    withContext(Dispatchers.Main) {
                                        if (updateResult) {
                                            serialList.add(enteredSerialNumber)
                                            updateProductMatchState(lineId, receiptId, true)
                                            confirmedLines.add(lineId)
                                            coroutineScope.launch { fetchProductsForReceipt(receiptId) }
                                            Toast.makeText(this@ProductsActivity, "Serial number added for $productName.", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            barcodeInput.setText("")
                                            barcodeInput.requestFocus()
                                        } else {
                                            Toast.makeText(this@ProductsActivity, "Failed to update serial number.", Toast.LENGTH_SHORT).show()
                                            barcodeInput.setText("")
                                            barcodeInput.requestFocus()
                                        }
                                    }
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ProductsActivity, "Serial number already entered for this picking operation.", Toast.LENGTH_SHORT).show()
                                serialNumberInput.setText("")
                                serialNumberInput.requestFocus()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProductsActivity, "Product details not found.", Toast.LENGTH_SHORT).show()
                            barcodeInput.setText("")
                            barcodeInput.requestFocus()
                        }
                    }
                }
            } else {
                Toast.makeText(this@ProductsActivity, "Please enter a serial number", Toast.LENGTH_SHORT).show()
                serialNumberInput.setText("")
                serialNumberInput.requestFocus()
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expiration_date, null)
        val textDialogMessage = dialogView.findViewById<TextView>(R.id.textDialogMessage)
        textDialogMessage.text = getString(R.string.enter_expiration_date, productName)
        val editTextExpirationDate = dialogView.findViewById<EditText>(R.id.editTextExpirationDate)
        setupDateInputField(editTextExpirationDate) // Assumes this method setups date picker or similar

        // Setting the hint color to white
        editTextExpirationDate.setHintTextColor(ContextCompat.getColor(this, android.R.color.white))

        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        buttonOk.setOnClickListener {
            val enteredExpirationDate = editTextExpirationDate.text.toString().trim()
            val convertedDate = convertToFullDateTime(enteredExpirationDate) // Assume this correctly formats the date
            if (isValidDateFormat(convertedDate)) { // Assume this validates the formatted date
                coroutineScope.launch {
                    odooXmlRpcClient.updateMoveLineSerialExpirationDate(lineId, receiptId, productId, serialNumber, convertedDate)
                    updateProductMatchState(lineId, receiptId, matched = true)
                    confirmedLines.add(lineId)
                    fetchProductsForReceipt(receiptId)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProductsActivity, "Expiration date updated and serial number added for $productName.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        barcodeInput.setText("")
                        barcodeInput.requestFocus()
                    }
                }
            } else {
                Toast.makeText(this@ProductsActivity, "Invalid expiration date entered. Please use the format DD/MM/YYYY", Toast.LENGTH_SHORT).show()
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
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
            if (date != null) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
            } else {
                Log.e("Swiib", "Date parsing error: parsed date is null")
                ""
            }
        } catch (e: ParseException) {
            Log.e("Swiib", "Date parsing error: ${e.localizedMessage}")
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
            barcodeInput.setText("")
            barcodeInput.requestFocus()
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
                    coroutineScope.launch {
                        fetchProductsForReceipt(receiptId)
                    }
                }

                // Check if the quantity is correct
                if (enteredQuantity == expectedQuantity) {
                    Toast.makeText(this@ProductsActivity, "Quantity updated for $productName", Toast.LENGTH_SHORT).show()
                    updateProductMatchState(lineId, receiptId, true)
                    confirmedLines.add(lineId)
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
                } else if (!recount) {
                    promptForProductQuantity(productName, expectedQuantity, receiptId, productId, lineId, recount = true)
                } else {
                    handleIncorrectRecount(productName)
                }

                dialog.dismiss() // Dismiss the dialog after confirming
            } else {
                Toast.makeText(this, "Invalid quantity entered", Toast.LENGTH_SHORT).show()
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        }

        // Display the dialog
        dialog.show()
    }

    private fun handleIncorrectRecount(productName: String) {
        val localReceiptId = receiptName  // Copy the mutable property to a local variable

        lifecycleScope.launch(Dispatchers.IO) {
            val buyerDetails = localReceiptId?.let { odooXmlRpcClient.fetchAndLogBuyerDetails(it) }
            if (buyerDetails != null) {
                sendEmailToBuyer(buyerDetails.login, buyerDetails.name, localReceiptId, productName) // Pass the local copy to the function
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductsActivity, "Flagged ${buyerDetails.login}. Email sent.", Toast.LENGTH_SHORT).show()
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductsActivity, "Flagged, but buyer details not found.", Toast.LENGTH_SHORT).show()
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
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
            // Ensure UI updates are on the main thread
            runOnUiThread {
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        } catch (e: MessagingException) {
            Log.e("EmailSender", "Failed to send email.", e)
            // Ensure UI updates are on the main thread
            runOnUiThread {
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        }
    }

    private fun updateProductMatchState(lineId: Int, pickId: Int, matched: Boolean = true) {
        coroutineScope.launch {
            fetchProductsForReceipt(pickId)  // Fetch and wait for completion

            withContext(Dispatchers.Main) {
                val key = ProductReceiptKey(lineId, pickId)
                val productLine = productsAdapter.moveLines.find { it.id == lineId }

                if (productLine != null) {
                    quantityMatches[key] = matched
                    val position = productsAdapter.findProductPositionById(lineId)
                    if (position != -1) {
                        productsAdapter.notifyItemChanged(position)
                    }
                    checkAndToggleValidateButton(pickId)
                    saveMatchStateToPreferences(key, matched)
                } else {
                    Log.e("updateProductMatchState", "No line found for ID $lineId after refreshing data")
                }
            }
        }
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
                            Toast.makeText(applicationContext, "Receipt validated successfully.", Toast.LENGTH_SHORT).show()
                            val mediaPlayer: MediaPlayer? = MediaPlayer.create(applicationContext, R.raw.button_pressed)
                            mediaPlayer?.start()
                            mediaPlayer?.setOnCompletionListener {
                                it.release()
                            }
                            // Redirect to PickActivity upon successful validation
                            val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
                            startActivity(intent)
                            finish()  // Optionally call finish() if you want to remove this activity from the back stack
                            barcodeInput.setText("")
                            barcodeInput.requestFocus()
                        } else {
                            Toast.makeText(applicationContext, "Failed to validate receipt.\nPlease flag or recount quantities. ", Toast.LENGTH_SHORT).show()
                            barcodeInput.setText("")
                            barcodeInput.requestFocus()
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
                        // Play the sound
                        val mediaPlayer: MediaPlayer? = MediaPlayer.create(applicationContext, R.raw.button_pressed)
                        mediaPlayer?.start()
                        mediaPlayer?.setOnCompletionListener {
                            it.release()
                        }
                        val intent = Intent(this@ProductsActivity, ReceiptsActivity::class.java)
                        startActivity(intent)
                        finish()  // Optionally call finish() if you want to remove this activity from the back stack
                        barcodeInput.setText("")
                        barcodeInput.requestFocus()
                    } else {
                        Toast.makeText(applicationContext, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
                        barcodeInput.setText("")
                        barcodeInput.requestFocus()
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
        editTextProductLotNumber.setHintTextColor(Color.WHITE)
        editTextQuantity.setHintTextColor(Color.WHITE)

        // Set values to the TextViews
        textProductName.text = product.productName
        textProductQuantity.text = "${product.quantity}"
        textProductToLocation.text = product.locationDestName
        textProductLotNumber.text = product.lotName

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
                    buttonEditQuantity.visibility = View.VISIBLE
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

        buttonConfirmQuantity.setOnClickListener {
            coroutineScope.launch(Dispatchers.IO) { // Start coroutine in IO Dispatcher for network operations
                try {
                    when {
                        quantityChanged && lotNameChanged -> {
                            val newQuantity = editTextQuantity.text.toString().toFloat().toInt()
                            odooXmlRpcClient.updateMoveLineQuantityForReceipt(product.id, receiptId, newQuantity)
                            odooXmlRpcClient.updateMoveLinesForReceipt(product.id, receiptId, editTextProductLotNumber.text.toString())
                        }
                        quantityChanged -> {
                            val newQuantity = editTextQuantity.text.toString().toFloat().toInt()
                            odooXmlRpcClient.updateMoveLineQuantityForReceipt(product.id, receiptId, newQuantity)
                        }
                        lotNameChanged -> {
                            odooXmlRpcClient.updateMoveLinesForReceipt(product.id, receiptId, editTextProductLotNumber.text.toString())
                        }
                    }
                    // Execute fetching of products on the same background thread
                    fetchProductsForReceipt(receiptId)

                    withContext(Dispatchers.Main) { // Switch to Main Dispatcher for UI operations
                        dialog.dismiss()
                        barcodeInput.setText("")
                        barcodeInput.requestFocus()
                    }
                } catch (e: NumberFormatException) {
                    withContext(Dispatchers.Main) { // Handle UI updates on the main thread
                        Toast.makeText(this@ProductsActivity, "Invalid input. Please enter a valid number.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProductsActivity, "Error during network operations: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun captureImage(pickId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Capture Image?")
        builder.setMessage("Would you like to capture an image?")
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        builder.setNegativeButton("No") { dialog, _ ->
            vibrateDevice(vibrator)
            dialog.dismiss()
        }

        builder.setPositiveButton("Capture Image") { dialog, _ ->
            vibrateDevice(vibrator)
            dialog.dismiss()
            Log.d("CaptureImage", "Opening camera for packId: $pickId")
            openCamera(pickId)
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun openCamera(packId: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            Log.d("CaptureImage", "Camera permission granted, starting camera intent for packId: $packId")
            startCameraIntent()
        }
    }

    private fun startCameraIntent() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            if (cameraIntent.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(cameraIntent)
            } else {
                Log.e("CameraIntent", "No application can handle camera intent.")
                Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("CameraIntent", "Failed to start camera intent", e)
            Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("CaptureImage", "Camera permission granted, starting camera intent")
                startCameraIntent()
            } else {
                Toast.makeText(this, "Camera permission is necessary to capture images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun vibrateDevice(vibrator: Vibrator?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    companion object {
        const val CAMERA_REQUEST_CODE = 1001
    }

    private inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }
}
