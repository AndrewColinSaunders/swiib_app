//package com.example.warehousetet
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.PorterDuff
//import android.graphics.Typeface
//import android.os.Bundle
//import android.text.SpannableString
//import android.text.Spanned
//import android.text.style.RelativeSizeSpan
//import android.text.style.StyleSpan
//import android.util.Log
//import android.view.KeyEvent
//import android.view.LayoutInflater
//import android.view.MenuItem
//import android.view.View
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.joinAll
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
//    val lotQuantities: MutableMap<ProductPickKey, Double> = mutableMapOf()
//    private var quantityMatches = mutableMapOf<ProductPickKey, Boolean>()
//    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
//    // Assuming this is declared at the class level
//    private val accumulatedQuantities: MutableMap<Int, Double> = mutableMapOf()
//    private val confirmedLines = mutableSetOf<Int>()
//    private var pickId: Int = -1
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
////        val pickId = intent.getIntExtra("PICK_ID", -1)
//         pickId = intent.getIntExtra("PICK_ID", -1)
//        val pickName = intent.getStringExtra("PICK_NAME") ?: "Pick"
////        val titleTextView: TextView = findViewById(R.id.pickProductsTitleTextView)
////        titleTextView.text = pickName
//
//        val locationName = intent.getStringExtra("LOCATION")
////        val locationTextView: TextView = findViewById(R.id.sourceLocationId)
////        locationTextView.text = locationName
//
//        destLocationName = intent.getStringExtra("DEST_LOCATION")
////        val destLocationTextView: TextView = findViewById(R.id.destinationLocationId)
////        destLocationTextView.text = destLocationName
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
////        findViewById<Button>(R.id.resetButton).setOnClickListener {
////            resetMatchStates(pickId)  // Call the reset function when the button is clicked
////        }
//
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        setupBarcodeVerification(pickId)
//        restoreButtonVisibility(pickId)
//        loadMatchStatesFromPreferences(pickId)
//        restoreButtonVisibility(pickId)
//    }
//    override fun onResume() {
//        super.onResume()
//        // Restore visibility state whenever the activity resumes
//        restoreButtonVisibility(pickId)
//    }
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
//            try {
//                Log.d("PickProductsActivity", "Fetching products for pick ID: $pickId")
//                val fetchedLines = odooXmlRpcClient.fetchMoveLinesByPickingId(pickId)
//                val updatedMoveLinesWithDetails = mutableListOf<MoveLine>()
//                odooXmlRpcClient.fetchResultPackagesByPickingId(pickId)
//                fetchedLines.forEach { moveLine ->
//                    coroutineScope.launch(Dispatchers.IO) {
//                        // Assuming fetchProductTrackingAndExpirationByName can be used for delivery orders as well
//                        val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(moveLine.productName) ?: Pair("none", false)
//                        val barcode = barcodeToProductIdMap.filterValues { it == moveLine.productId }.keys.firstOrNull()
//                        Log.d("PickProductsActivity", "Product: ${moveLine.productName}, Uses Expiration Date: ${trackingAndExpiration.second}")
////                        val barcode = fetchBarcodesForProducts(fetchedLines)
//
//                        // Update move line details to include tracking type, expiration date, and barcode
//                        val updatedMoveLine = moveLine.copy(
//
//                            barcode = barcode.toString()
//                        )
//                        synchronized(updatedMoveLinesWithDetails) {
//                            updatedMoveLinesWithDetails.add(updatedMoveLine)
//                        }
//                    }
//                }
//                fetchBarcodesForProducts(fetchedLines)
//
//                coroutineScope.launch(Dispatchers.Main) {
//                    // Ensure all operations are completed
//                    updatedMoveLinesWithDetails.forEach {
//                        joinAll()
//                    }
//                    updateUIForProducts(fetchedLines, pickId)
//                    Log.d("PickProductsActivity", "UI updated with detailed move lines for Pick")
//                }
//            } catch (e: Exception) {
//                Log.e("PickProductsActivity", "Error fetching move lines or updating UI: ${e.localizedMessage}", e)
//            }
//        }
//    }
//
//    private fun updateUIForProducts(moveLines: List<MoveLine>, pickId: Int) {
//        // Explicitly defining types in the map creation
//        val newQuantityMatches: MutableMap<ProductPickKey, Boolean> = moveLines.associateBy({ ProductPickKey(it.id, pickId) }, { moveLine ->
//            quantityMatches.getOrDefault(ProductPickKey(moveLine.id, pickId), false)
//        }).toMutableMap()
//
//        // Update the quantityMatches and the adapter for RecyclerView
//        quantityMatches.clear()
//        quantityMatches.putAll(newQuantityMatches)
//        pickProductsAdapter.updateProducts(moveLines, pickId, quantityMatches)
//        Log.d("PickProductsActivity", "Adapter updated with new products and match states.")
//    }
//
//    private fun fetchBarcodesForProducts(moveLine: List<MoveLine>) {
//        moveLine.forEach { moveLine ->
//            coroutineScope.launch {
//                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(moveLine.productName)
//                barcode?.let {
//                    // Assuming barcodeToProductIdMap should map barcode to product ID
//                    synchronized(this@PickProductsActivity) { // Ensure to use your current activity or context correctly
//                        barcodeToProductIdMap[barcode] = moveLine.productId
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
//
//    private fun hideKeyboard() {
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(barcodeInput.windowToken, 0)
//    }
//
//    private fun verifyBarcode(scannedBarcode: String, pickId: Int) {
//        coroutineScope.launch {
//            val productId = barcodeToProductIdMap[scannedBarcode]
//            val packagingProductInfo = odooXmlRpcClient.fetchProductIdFromPackagingBarcode(scannedBarcode)
//
//            if (productId != null) {
//                val productLines = pickProductsAdapter.lines.filter { it.productId == productId }.sortedBy { it.id }
//                val nextProductLine = productLines.find { !confirmedLines.contains(it.id) }
//
//                nextProductLine?.let { productLine ->
//                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(productLine.productName)
//                    val trackingType = trackingAndExpiration?.first ?: "none"
//
//                    withContext(Dispatchers.Main) {
//                        showSourceLocationDialog(productLine.productName, productLine.quantity, pickId, productLine.id, productLine.locationName, trackingType,productLine.productId, productLine.locationId, productLine.lotName)
//                    }
//                }
//            } else if (packagingProductInfo != null) {
//                Log.d("verifyBarcode", "Found barcode in product.packaging model. Product ID: ${packagingProductInfo.first}, Quantity: ${packagingProductInfo.second}")
//            } else {
//                withContext(Dispatchers.Main) {
//                    Log.d("verifyBarcode", "Barcode not found in product.template or product.packaging models")
//                }
//            }
//        }
//    }
//
////    private fun showSourceLocationDialog(
////        productName: String, quantity: Double, pickId: Int, lineId: Int,
////        locationName: String, trackingType: String, productId: Int,
////        locationId: Int, lotName: String
////    ) {
////        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_scan_source_location, null)
////        val sourceLocationInput = dialogView.findViewById<EditText>(R.id.sourceLocationInput)
////        val locationNameTextView = dialogView.findViewById<TextView>(R.id.locationNameTextView)
////        sourceLocationInput.setHintTextColor(Color.WHITE)
////        locationNameTextView.text = locationName
////
////        val dialog = AlertDialog.Builder(this)
////            .setView(dialogView)
////            .setCancelable(false) // Disables dismiss by clicking outside or pressing back
////            .create()
////
////        dialogView.findViewById<Button>(R.id.confirmButton).setOnClickListener {
////            val enteredLocation = sourceLocationInput.text.toString()
////            if (enteredLocation.isNotEmpty()) {
////                coroutineScope.launch {
////                    if (enteredLocation == locationName) {
////                        // If the entered location matches the expected location
////                        withContext(Dispatchers.Main) {
////                            showGreenToast("Source location confirmed: $enteredLocation")
////                            dialog.dismiss()
////                            when (trackingType) {
////                                "serial" -> promptForSerialNumber(productName, pickId, productId, locationId, lineId)
////                                "lot" -> promptConfirmLotQuantity(productName, lotName, quantity, locationName, lineId, pickId)
////                                "none" -> displayQuantityDialog(productName, quantity, pickId, lineId)
////                                else -> Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
////                            }
////                        }
////                    } else {
////                        // If the entered location does not match
////                        withContext(Dispatchers.Main) {
////                            showRedToast("Incorrect location, please re-enter.")
////                            sourceLocationInput.text.clear() // Clear the input for re-entry
////                        }
////                    }
////                }
////            } else {
////                showRedToast("Please enter a source location.")
////            }
////        }
////        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
////            dialog.dismiss()
////        }
////
////        dialog.show() // Display the dialog
////    }
//
//    private fun showSourceLocationDialog(
//        productName: String, quantity: Double, pickId: Int, lineId: Int,
//        locationName: String, trackingType: String, productId: Int,
//        locationId: Int, lotName: String
//    ) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_scan_source_location, null)
//        val sourceLocationInput = dialogView.findViewById<EditText>(R.id.sourceLocationInput)
//        val locationNameTextView = dialogView.findViewById<TextView>(R.id.locationNameTextView)
//        locationNameTextView.text = locationName
//
//        sourceLocationInput.setHintTextColor(Color.WHITE)
//        sourceLocationInput.setOnEditorActionListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
//                dialogView.findViewById<Button>(R.id.confirmButton).performClick()
//                true
//            } else {
//                false
//            }
//        }
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false) // Disables dismiss by clicking outside or pressing back
//            .create()
//
//        dialogView.findViewById<Button>(R.id.confirmButton).setOnClickListener {
//            val enteredLocation = sourceLocationInput.text.toString()
//            if (enteredLocation.isNotEmpty()) {
//                coroutineScope.launch {
//                    if (enteredLocation == locationName) {
//                        // If the entered location matches the expected location
//                        withContext(Dispatchers.Main) {
//                            showGreenToast("Source location confirmed: $enteredLocation")
//                            dialog.dismiss()
//                            when (trackingType) {
//                                "serial" -> promptForSerialNumber(productName, pickId, productId, locationId, lineId)
//                                "lot" -> promptConfirmLotQuantity(productName, lotName, quantity, locationName, lineId, pickId)
//                                "none" -> displayQuantityDialog(productName, quantity, pickId, lineId)
//                                else -> Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
//                            }
//                        }
//                    } else {
//                        // If the entered location does not match
//                        withContext(Dispatchers.Main) {
//                            showRedToast("Incorrect location, please re-enter.")
//                            sourceLocationInput.text.clear() // Clear the input for re-entry
//                        }
//                    }
//                }
//            } else {
//                showRedToast("Please enter a source location.")
//            }
//        }
//
//        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialog.show() // Display the dialog
//        sourceLocationInput.requestFocus() // Autofocus on the source location input field when the dialog shows
//    }
//
//
//    private fun displayQuantityDialog(productName: String, expectedQuantity: Double, pickId: Int, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_confirmation, null)
//        val textViewConfirmation = dialogView.findViewById<TextView>(R.id.ConfirmationTextView)
//        val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirmQuantity)
//        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
//
//        val fullText = "Confirm the quantity of $expectedQuantity for $productName has been picked."
//        val spannableString = SpannableString(fullText)
//
//        // Styling for expectedQuantity
//        val quantityStart = fullText.indexOf("$expectedQuantity")
//        val quantityEnd = quantityStart + "$expectedQuantity".length
//        spannableString.setSpan(StyleSpan(Typeface.BOLD), quantityStart, quantityEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//        spannableString.setSpan(RelativeSizeSpan(1.1f), quantityStart, quantityEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // 10% larger
//
//        // Styling for productName
//        val productNameStart = fullText.indexOf("$productName")
//        val productNameEnd = productNameStart + "$productName".length
//        spannableString.setSpan(StyleSpan(Typeface.BOLD), productNameStart, productNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//        spannableString.setSpan(RelativeSizeSpan(1.1f), productNameStart, productNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // 10% larger
//
//        textViewConfirmation.text = spannableString
//
//        val alertDialog = AlertDialog.Builder(this).apply {
//            setView(dialogView)
//            create()
//        }.show()
//
//        buttonConfirm.setOnClickListener {
//            // Pass the lineId to match state update function
//            updateProductMatchState(lineId, pickId)
//            confirmedLines.add(lineId)
//            alertDialog.dismiss()  // Close the dialog after confirmation
//        }
//
//        buttonCancel.setOnClickListener {
//            alertDialog.dismiss()  // Close the dialog when cancel is clicked
//        }
//    }
//
//    private fun promptConfirmLotQuantity(productName: String, lotName: String, quantity: Double, locationName: String, lineId: Int, pickId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_quantity, null)
//        dialogView.findViewById<TextView>(R.id.textProductInfo).text = "Product: $productName"
//        dialogView.findViewById<TextView>(R.id.textQuantityInfo).text = "Quantity: $quantity"
//        dialogView.findViewById<TextView>(R.id.textLotInfo).text = "LOT: $lotName"
//        dialogView.findViewById<TextView>(R.id.textSourceLocationInfo).text = "Pick From: $locationName"
//
//        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
//        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
//        val alertDialog = AlertDialog.Builder(this)
//
//            .setView(dialogView)
//            .create()
//
//        confirmButton.setOnClickListener {
//            confirmLotNumber(productName, quantity, lotName, lineId, pickId)
//            showGreenToast("Quantity confirmed for $productName.")
//            alertDialog.dismiss()  // Close the dialog after confirmation
//        }
//        buttonCancel.setOnClickListener {
//            alertDialog.dismiss()  // Close the dialog when cancel is clicked
//        }
//
//        alertDialog.show()
//    }
//
////    private fun confirmLotNumber(productName: String, quantity: Double, lotName: String, lineId: Int, pickId: Int) {
////        // Inflate the custom layout for the dialog
////        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_entry, null)
////        val textProductInfo: TextView = dialogView.findViewById(R.id.textProductInfo)
////        val textLotInfo: TextView = dialogView.findViewById(R.id.textLotInfo)
////        val editText: EditText = dialogView.findViewById(R.id.editLotNumber)
////        editText.setHintTextColor(Color.WHITE)
////        val textQuantityInfo: TextView = dialogView.findViewById(R.id.textQuantityInfo)
////        val confirmButton: Button = dialogView.findViewById(R.id.confirmButton)
////        val buttonCancel: Button = dialogView.findViewById(R.id.buttonCancel)
////
////        // Set the texts for TextViews
////        textProductInfo.text = "Product: $productName"
////        textQuantityInfo.text = "Quantity: $quantity"
////        textLotInfo.text = "LOT: $lotName"
////
////        // Configure and show AlertDialog
////        val alertDialog = AlertDialog.Builder(this).apply {
////            setView(dialogView)
////            create()
////        }.show()
////
////        // Set up the confirm button action
////        confirmButton.setOnClickListener {
////            val enteredLotNumber = editText.text.toString()
////            if (enteredLotNumber == lotName) {
////                showGreenToast("Correct LOT number confirmed for $productName.")
////                updateProductMatchState(lineId, pickId)
////                confirmedLines.add(lineId)
////                alertDialog.dismiss()  // Close the dialog when the correct number is confirmed
////            } else {
////                showRedToast("Incorrect LOT number entered.")
////            }
////        }
////        buttonCancel.setOnClickListener {
////            alertDialog.dismiss()  // Close the dialog when cancel is clicked
////        }
////
////        // Automatically show keyboard when dialog appears
////        editText.requestFocus()
////        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
////    }
//    private fun confirmLotNumber(productName: String, quantity: Double, lotName: String, lineId: Int, pickId: Int) {
//        // Inflate the custom layout for the dialog
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_entry, null)
//        val textProductInfo: TextView = dialogView.findViewById(R.id.textProductInfo)
//        val textLotInfo: TextView = dialogView.findViewById(R.id.textLotInfo)
//        val editText: EditText = dialogView.findViewById(R.id.editLotNumber)
//        editText.setHintTextColor(Color.WHITE)
//        val textQuantityInfo: TextView = dialogView.findViewById(R.id.textQuantityInfo)
//        val confirmButton: Button = dialogView.findViewById(R.id.confirmButton)
//        val buttonCancel: Button = dialogView.findViewById(R.id.buttonCancel)
//
//        // Set the texts for TextViews
//        textProductInfo.text = "Product: $productName"
//        textQuantityInfo.text = "Quantity: $quantity"
//        textLotInfo.text = "LOT: $lotName"
//
//        // Configure and show AlertDialog
//        val alertDialog = AlertDialog.Builder(this).apply {
//            setView(dialogView)
//            setCancelable(false)
//            create()
//        }.show()
//
//        // Set up the confirm button action
//        confirmButton.setOnClickListener {
//            val enteredLotNumber = editText.text.toString()
//            if (enteredLotNumber == lotName) {
//                showGreenToast("Correct LOT number confirmed for $productName.")
//                updateProductMatchState(lineId, pickId)
//                confirmedLines.add(lineId)
//                alertDialog.dismiss()  // Close the dialog when the correct number is confirmed
//            } else {
//                showRedToast("Incorrect LOT number entered.")
//            }
//        }
//        buttonCancel.setOnClickListener {
//            alertDialog.dismiss()  // Close the dialog when cancel is clicked
//        }
//
//        // Set up the editor action listener for the enter key
//        editText.setOnEditorActionListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
//                confirmButton.performClick()
//                true
//            } else {
//                false
//            }
//        }
//
//        // Automatically show keyboard when dialog appears
//        editText.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//    }
//
//
////    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int, locationId: Int, lineId: Int) {
////        // Inflate the layout for the dialog
////        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
////        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
////        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
////        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
////
////        serialNumberInput.setHintTextColor(Color.WHITE)
////
////        // Set up the editor action listener for the serial number input
////        serialNumberInput.setOnEditorActionListener { v, actionId, event ->
////            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
////                buttonConfirmSN.performClick()
////                true
////            } else {
////                false
////            }
////        }
////
////        // Configure the dialog
////        val dialog = AlertDialog.Builder(this)
////            .setView(dialogView)
////            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
////            .create()
////
////        // Set up the confirm button action
////        buttonConfirmSN.setOnClickListener {
////            val enteredSerialNumber = serialNumberInput.text.toString().trim()
////            if (enteredSerialNumber.isNotEmpty()) {
////                coroutineScope.launch {
////                    val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
////                    val serialList = productSerialNumbers.getOrPut(ProductPickKey(productId, pickId)) { mutableListOf() }
////
////                    if (serialNumbers?.contains(enteredSerialNumber) == true) {
////                        serialList.add(enteredSerialNumber)
////                        odooXmlRpcClient.updateMoveLinesForPick(lineId, pickId, enteredSerialNumber, productId)
////                        withContext(Dispatchers.Main) {
////                            updateProductMatchState(lineId, pickId)
////                            confirmedLines.add(lineId)
////                            showGreenToast("Serial number added for $productName.")
////                            dialog.dismiss()
////                            fetchProductsForPick(pickId)
////                        }
////                    } else {
////                        withContext(Dispatchers.Main) {
////                            showRedToast("Serial number does not exist. Please enter a valid serial number.")
////                            promptForSerialNumber(productName, pickId, productId, locationId, lineId) // Re-prompt
////                        }
////                    }
////                }
////            } else {
////                showRedToast("Please enter a serial number.")
////                promptForSerialNumber(productName, pickId, productId, locationId, lineId) // Re-prompt
////            }
////        }
////
////        // Set up the cancel button action
////        buttonCancelSN.setOnClickListener {
////            dialog.dismiss()  // Dismiss the dialog
////        }
////
////        dialog.show()
////
////        // Request focus and show keyboard for the serial number input
////        serialNumberInput.requestFocus()
////        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
////    }
//
////    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int, locationId: Int, lineId: Int) {
////        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
////        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
////        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
////        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
////
////        serialNumberInput.setHintTextColor(Color.WHITE)
////
////        // Set up the editor action listener for the serial number input
////        serialNumberInput.setOnEditorActionListener { v, actionId, event ->
////            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
////                buttonConfirmSN.performClick()
////                true
////            } else {
////                false
////            }
////        }
////
////        // Access SharedPreferences to retrieve saved serial numbers
////        val sharedPrefs = getSharedPreferences("SerialNumbers", Context.MODE_PRIVATE)
////        val savedSerials = sharedPrefs.getStringSet("Serials_$productId", mutableSetOf()) ?: mutableSetOf()
////
////        // Configure the dialog
////        val dialog = AlertDialog.Builder(this)
////            .setView(dialogView)
////            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
////            .create()
////
////        // Set up the confirm button action
////        buttonConfirmSN.setOnClickListener {
////            val enteredSerialNumber = serialNumberInput.text.toString().trim()
////            if (enteredSerialNumber.isNotEmpty()) {
////                coroutineScope.launch {
////                    if (savedSerials.contains(enteredSerialNumber)) {
////                        withContext(Dispatchers.Main) {
////                            showRedToast("Serial number already added. Please enter a different serial number.")
////                            serialNumberInput.setText("")
////                            serialNumberInput.requestFocus()
////                        }
////                    } else {
////                        val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
////                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
////                            savedSerials.add(enteredSerialNumber)
////                            sharedPrefs.edit().putStringSet("Serials_$productId", savedSerials).apply()
////                            odooXmlRpcClient.updateMoveLinesForPick(lineId, pickId, enteredSerialNumber, productId)
////                            withContext(Dispatchers.Main) {
////                                updateProductMatchState(lineId, pickId)
////                                confirmedLines.add(lineId)
////                                showGreenToast("Serial number added for $productName.")
////                                dialog.dismiss()
////                                fetchProductsForPick(pickId)
////                            }
////                        } else {
////                            withContext(Dispatchers.Main) {
////                                showRedToast("Serial number does not exist. Please enter a valid serial number.")
////                                serialNumberInput.setText("")
////                                serialNumberInput.requestFocus()
////                            }
////                        }
////                    }
////                }
////            } else {
////                showRedToast("Please enter a serial number.")
////                serialNumberInput.setText("")
////                serialNumberInput.requestFocus()
////            }
////        }
////
////        // Set up the cancel button action
////        buttonCancelSN.setOnClickListener {
////            dialog.dismiss()  // Dismiss the dialog
////        }
////
////        dialog.show()
////
////        // Request focus and show keyboard for the serial number input
////        serialNumberInput.requestFocus()
////        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
////    }
//
//    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int, locationId: Int, lineId: Int) {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
//        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
//        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
//        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
//
//        serialNumberInput.setHintTextColor(Color.WHITE)
//
//        // Set up the editor action listener for the serial number input
//        serialNumberInput.setOnEditorActionListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                buttonConfirmSN.performClick()
//                true
//            } else {
//                false
//            }
//        }
//
//        // Access SharedPreferences to retrieve saved serial numbers, using both pickId and productId in the key
//        val sharedPrefs = getSharedPreferences("SerialNumbers", Context.MODE_PRIVATE)
//        val key = "Serials_${pickId}_$productId"
//        val savedSerials = sharedPrefs.getStringSet(key, mutableSetOf()) ?: mutableSetOf()
//
//        // Configure the dialog
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
//            .create()
//
//        // Set up the confirm button action
//        buttonConfirmSN.setOnClickListener {
//            val enteredSerialNumber = serialNumberInput.text.toString().trim()
//            if (enteredSerialNumber.isNotEmpty()) {
//                coroutineScope.launch {
//                    if (savedSerials.contains(enteredSerialNumber)) {
//                        withContext(Dispatchers.Main) {
//                            showRedToast("Serial number already added. Please enter a different serial number.")
//                            serialNumberInput.setText("")
//                            serialNumberInput.requestFocus()
//                        }
//                    } else {
//                        val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
//
//                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
//                            savedSerials.add(enteredSerialNumber)
//                            sharedPrefs.edit().putStringSet(key, savedSerials).apply()
//                            odooXmlRpcClient.updateMoveLinesForPick(lineId, pickId, enteredSerialNumber, productId)
//                            withContext(Dispatchers.Main) {
//                                updateProductMatchState(lineId, pickId)
//                                confirmedLines.add(lineId)
//                                showGreenToast("Serial number added for $productName.")
//                                dialog.dismiss()
//                                fetchProductsForPick(pickId)
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                showRedToast("Serial number does not exist. Please enter a valid serial number.")
//                                serialNumberInput.setText("")
//                                serialNumberInput.requestFocus()
//                            }
//                        }
//                    }
//                }
//            } else {
//                showRedToast("Please enter a serial number.")
//                serialNumberInput.setText("")
//                serialNumberInput.requestFocus()
//            }
//        }
//
//        // Set up the cancel button action
//        buttonCancelSN.setOnClickListener {
//            dialog.dismiss()  // Dismiss the dialog
//        }
//
//        dialog.show()
//
//        // Request focus and show keyboard for the serial number input
//        serialNumberInput.requestFocus()
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(serialNumberInput, InputMethodManager.SHOW_IMPLICIT)
//    }
//
//
////    private fun promptForProductQuantity(
////        productName: String,
////        expectedQuantity: Double,
////        pickId: Int,
////        productId: Int,
////        recount: Boolean = false
////    ) {
////        val layout = LinearLayout(this).apply {
////            orientation = LinearLayout.VERTICAL
////            layoutParams = LinearLayout.LayoutParams(
////                LinearLayout.LayoutParams.MATCH_PARENT,
////                LinearLayout.LayoutParams.WRAP_CONTENT
////            )
////            setPadding(16, 8, 16, 16) // Standard padding
////        }
////
////        val quantityEditText = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
////            hint = "Enter product quantity"
////        }
////        layout.addView(quantityEditText)
////
////        val subheading = TextView(this).apply {
////            text = "Store To Location"
////            textSize = 14f
////            setTypeface(null, Typeface.BOLD)
////            layoutParams = LinearLayout.LayoutParams(
////                LinearLayout.LayoutParams.WRAP_CONTENT,
////                LinearLayout.LayoutParams.WRAP_CONTENT
////            ).apply {
////                setMargins(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
////            }
////        }
////        layout.addView(subheading)
////
////        val storeToLocationEditText = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Store to location"
////            setText(destLocationName)
////        }
////        layout.addView(storeToLocationEditText)
////
////        AlertDialog.Builder(this)
////            .setTitle(if (recount) "Recount Required" else "Enter Quantity")
////            .setMessage("Enter the exact quantity for $productName.")
////            .setView(layout)
////            .setPositiveButton("OK") { _, _ ->
////                val enteredQuantity = quantityEditText.text.toString().toDoubleOrNull()
////                val enteredLocation = storeToLocationEditText.text.toString()
////
////                if (enteredQuantity != null) {
////                    coroutineScope.launch(Dispatchers.IO) {
////                        // Update the database and state
////                        val successfulUpdate = odooXmlRpcClient.createStockMoveLineForUntrackedProduct(pickId, productId, enteredQuantity, enteredLocation)
////                        withContext(Dispatchers.Main) {
////                            if (successfulUpdate) {
//////                                updateProductMatchState(productId, pickId, enteredQuantity == expectedQuantity)
////                                fetchProductsForPick(pickId) // Refetch and update UI after state change
////                            } else {
////                                showRedToast("Failed to update product data.")
////                            }
////                        }
////                    }
////                } else if (!recount) {
////                    promptForProductQuantity(productName, expectedQuantity, pickId, productId, true)
////                }
////            }
////            .setNegativeButton("Cancel", null)
////            .show()
////    }
//
////    private fun promptForLotNumber(productName: String, pickId: Int, productId: Int) {
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
////        val lotNumberInput = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Enter lot number"
////        }
////        container.addView(lotNumberInput)
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
////        val storeToInput = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_TEXT
////            hint = "Store to"
////            setText(destLocationName) // Set the default text to the EditText
////
////            // Adding a focus change listener instead
////            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
////                // Clear the text when the EditText gains focus if it contains the default location name
////                if (hasFocus && text.toString() == destLocationName) {
////                    setText("")
////                }
////            }
////        }
////        container.addView(storeToInput)
////
////        val dialogBuilder = AlertDialog.Builder(this)
////            .setTitle("Enter lot Number")
////            .setMessage("Enter the lot number for $productName.")
////            .setView(container)
////            .setNegativeButton("Cancel", null)
////
////        val dialog = dialogBuilder.create()
////
////        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
////            val enteredLotNumber = lotNumberInput.text.toString().trim()
////            val enteredStoreTo = storeToInput.text.toString().trim() // This captures the user input but does not change destLocationName
////
////            if (enteredLotNumber.isNotEmpty()) {
////                coroutineScope.launch {
////                    val lotNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)
//////                    val key = ProductPickKey(productId, pickId)
//////                    val serialList = productSerialNumbers.getOrPut(key) { mutableListOf() }
////                    if (lotNumbers?.contains(enteredLotNumber) == true) {
////                        coroutineScope.launch {
////                            withContext(Dispatchers.Main) {
////                                // Assume promptForLotQuantity is correctly defined elsewhere to handle these parameters
////                                promptForLotQuantity(productName, pickId, productId, enteredLotNumber, enteredStoreTo)
////                            }
////                        }
////                        withContext(Dispatchers.Main) {
////                            showGreenToast("lot number added for $productName.")
////                        }
////                    } else {
////                        // Serial number does not exist, notify and prompt again
////                        withContext(Dispatchers.Main) {
////                            showRedToast("Serial number does not exist. Please enter a valid lot number.")
////                            promptForLotNumber(productName, pickId, productId) // Re-prompt
////                        }
////                    }
////                }
////            } else {
////                showRedToast("Please enter a lot number")
////                promptForLotNumber(productName, pickId, productId) // Re-prompt
////            }
////        }
////
////        dialog.show()
////        // Show keyboard
////        lotNumberInput.requestFocus()
////        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.showSoftInput(lotNumberInput, InputMethodManager.SHOW_IMPLICIT)
////    }
//
//
////    private fun promptForLotQuantity(productName: String, pickId: Int, productId: Int, lotNumber: String, location: String) {
////        val editText = EditText(this).apply {
////            inputType = InputType.TYPE_CLASS_NUMBER
////            hint = "Enter quantity for lot $lotNumber"
////        }
////
////        val dialog = AlertDialog.Builder(this)
////            .setTitle("Enter Lot Quantity")
////            .setMessage("Enter the quantity for the lot $lotNumber of $productName.")
////            .setView(editText)
////            .setPositiveButton("OK") { _, _ ->
////                val enteredQuantity = editText.text.toString().toIntOrNull()
////                if (enteredQuantity != null) {
////                    coroutineScope.launch {
////                        odooXmlRpcClient.updateMoveLinesWithoutExpirationWithLot(pickId, productId, lotNumber, enteredQuantity, location)
////                        withContext(Dispatchers.Main) {
////                            showGreenToast("Quantity $enteredQuantity added for lot $lotNumber of $productName.")
////                            // Assume we accumulate lot quantities in a different function
////                        }
////                    }
////                } else {
////                    showRedToast("Invalid quantity entered.")
////                }
////            }
////            .setNegativeButton("Cancel") { dialog, _ ->
////                dialog.dismiss()
////            }
////            .create()
////
////        dialog.setOnShowListener {
////            editText.requestFocus()
////            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
////        }
////
////        dialog.show()
////    }
//
//
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
////        lineId: Int,
////        pickId: Int,
////        matched: Boolean = true,
////        serialNumbers: MutableList<String>? = null,
////    ) {
////        val key = ProductPickKey(lineId, pickId)
////        val productLine = pickProductsAdapter.lines.find { it.id == lineId }
////
////        if (productLine != null) {
////            quantityMatches[key] = matched
////
////            // Refresh the UI to reflect the updated state
////            runOnUiThread {
////                val position = pickProductsAdapter.findProductPositionById(lineId)
////                if (position != -1) {
////                    pickProductsAdapter.notifyItemChanged(position)
////                }
////            }
////        } else {
////            Log.e("updateProductMatchState", "No line found for ID $lineId")
////        }
////
////        saveMatchStateToPreferences(key, quantityMatches[key] == true)
////    }
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
//                checkAndToggleValidateButton(pickId)
//            }
//        } else {
//            Log.e("updateProductMatchState", "No line found for ID $lineId")
//        }
//
//        saveMatchStateToPreferences(key, quantityMatches[key] == true)
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
//                            Toast.makeText(applicationContext, "Picking validated successfully.", Toast.LENGTH_SHORT).show()
//                            // Redirect to PickActivity upon successful validation
//                            val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
//                            startActivity(intent)
//                            finish()  // Optionally call finish() if you want to remove this activity from the back stack
//                        } else {
//                            Toast.makeText(applicationContext, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            }
//        }
//    }
////    private fun checkAndToggleValidateButton(pickId: Int) {
////        val allMatched = quantityMatches.filterKeys { it.pickId == pickId }.all { it.value }
////        val validateButton = findViewById<Button>(R.id.pickValidateButton)
////
////        validateButton.visibility = if (allMatched) View.VISIBLE else View.GONE
////        saveButtonVisibilityState(pickId, allMatched)
////
////        if (allMatched) {
////            validateButton.setOnClickListener {
////                coroutineScope.launch {
////                    odooXmlRpcClient.validateOperation(pickId)  // Call validateOperation but ignore its return value
////                    runOnUiThread {
////                        Toast.makeText(applicationContext, "Picking validated successfully.", Toast.LENGTH_SHORT).show()
////                        val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
////                        startActivity(intent)
////                        finish()  // Remove this activity from the back stack
////                    }
////                }
////            }
////        }
////    }
//
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
//                        val intent = Intent(this@PickProductsActivity, PickActivity::class.java)
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
//    private fun checkAllProductsMatched(pickId: Int): Boolean {
//        // Filter the quantityMatches for the current receiptId
//        return quantityMatches.filter { it.key.pickId == pickId }.all { it.value }
//    }
//
//    private fun saveMatchStateToPreferences(key: ProductPickKey, matched: Boolean) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        with(sharedPref.edit()) {
//            putBoolean("${key.moveLineId}_${key.pickId}", matched)
//            apply()
//        }
//    }
//
////    private fun loadMatchStatesFromPreferences(pickId: Int) {
////        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
////        val tempQuantityMatches = mutableMapOf<ProductPickKey, Boolean>()
////
////        sharedPref.all.forEach { (prefKey, value) ->
////            if (value is Boolean) {
////                val parts = prefKey.split("_")
////                if (parts.size == 2) {
////                    try {
////                        val productId = parts[0].toInt()
////                        val prefPickId = parts[1].toInt()
////                        if (prefPickId == pickId) {
////                            val key = ProductPickKey(productId, prefPickId)
////                            tempQuantityMatches[key] = value
////                        }
////                    } catch (e: NumberFormatException) {
////                        Log.e("PickProductsActivity", "Error parsing shared preference key: $prefKey", e)
////                    }
////                } else {
////                    // This is a better place to log a detailed message about the formatting issue
////                    Log.e("PickProductsActivity", "Incorrectly formatted shared preference key: $prefKey. Expected format: productId_pickId")
////                }
////            }
////
////        }
////
////        quantityMatches.clear()
////        quantityMatches.putAll(tempQuantityMatches)
////
////        // Now update the adapter with the loaded match states
////        runOnUiThread {
////            pickProductsAdapter.updateProducts(pickProductsAdapter.lines, pickId, quantityMatches)
////        }
////    }
//    private fun loadMatchStatesFromPreferences(pickId: Int) {
//        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//        val tempQuantityMatches = mutableMapOf<ProductPickKey, Boolean>()
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
//                                val key = ProductPickKey(moveLineId, prefPickId)
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
//            pickProductsAdapter.updateProducts(pickProductsAdapter.lines, pickId, quantityMatches)
//        }
//    }
//
//    private fun resetMatchStates(pickId: Int) {
//        // Reset all in-memory data structures
//        quantityMatches.keys.filter { it.pickId == pickId }.forEach {
//            quantityMatches[it] = false
//            accumulatedQuantities[it.moveLineId] = 0.0
//            lotQuantities[it] = 0.0
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
//    internal interface OnProductClickListener {
//        fun onProductClick(product: MoveLine?)
//    }
//
//
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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class PickProductsActivity : AppCompatActivity(), PickProductsAdapter.OnProductClickListener{
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


        pickId = intent.getIntExtra("PICK_ID", -1)
        pickName = intent.getStringExtra("PICK_NAME") ?: "Pick"


        val locationName = intent.getStringExtra("LOCATION")


        destLocationName = intent.getStringExtra("DEST_LOCATION")


        supportActionBar?.title = pickName
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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupBarcodeVerification(pickId)
//        restoreButtonVisibility(pickId)
        loadMatchStatesFromPreferences(pickId)
        restoreButtonVisibility(pickId)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.flag_menu_item, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed() // This will handle the back action
                return true
            }
            R.id.action_flag -> {
                showFlagDialog()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFlagDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, null)
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(false)  // Prevent dialog from being dismissed by back press or outside touches
        }
        val dialog = dialogBuilder.create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()  // Dismiss the dialog when "Cancel" is clicked
        }

        dialogView.findViewById<Button>(R.id.btnFlagPick).setOnClickListener {
            coroutineScope.launch {
                try {
                    val pickId = this@PickProductsActivity.pickId
                    val pickName = this@PickProductsActivity.pickName ?: run {
                        withContext(Dispatchers.Main) {
                            Log.e("PickProductsActivity", "Pick name is null")
                            showRedToast("Invalid pick details")
                        }
                        return@launch
                    }

                    val buyerDetails = odooXmlRpcClient.fetchAndLogBuyerDetails(pickName)
                    if (buyerDetails != null) {
                        withContext(Dispatchers.Main) {
                            // Capture image before sending the email
                            captureImage(pickId)
                        }

                        // Ensure email is sent after image capture dialog completion
                        sendEmailToBuyer(buyerDetails.login, buyerDetails.name, pickName)
                        withContext(Dispatchers.Main) {
                            Log.d("PickProductsActivity", "Pick flagged and buyer notified via email.")
                            showRedToast("Pick flagged")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Log.e("PickProductsActivity", "Failed to fetch buyer details or flag the pick.")
                            showRedToast("Failed to flag pick")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("PickProductsActivity", "Error in flagging process: ${e.localizedMessage}", e)
                        showRedToast("Error during flagging")
                    }
                }
                dialog.dismiss()  // Dismiss the dialog once the operations are complete
            }
        }

        dialog.show()  // Show the dialog
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 1001
    }

    private fun captureImage(pickId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Capture Image?")
        builder.setMessage("Would you like to capture an image?")

        // No button - just dismiss the dialog
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        // Capture Image button - open the camera
        builder.setPositiveButton("Capture Image") { dialog, _ ->
            dialog.dismiss()
            openCamera(pickId)  // Pass pickId to ensure it is available after capturing the image
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun openCamera(pickId: Int) {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

                Log.d("CaptureImage", "Encoded image: $encodedImage") // Log the encoded string or its length

                coroutineScope.launch {
                    val updateResult = odooXmlRpcClient.updatePickingImage(pickId, encodedImage)
                    Log.d("OdooUpdate", "Update result: $updateResult") // Log the result from the server
                }
            } else {
                Log.e("CaptureImage", "Failed to capture image")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Restore visibility state whenever the activity resumes
        restoreButtonVisibility(pickId)
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.pickProductsRecyclerView)
        // Ensure you are passing all necessary parameters in the correct order
        // The parameters should be (List<MoveLine>, Map<ProductPickKey, Boolean>, Map<Int, String>, Int, OnProductClickListener)
        pickProductsAdapter = PickProductsAdapter(
            emptyList(),                    // List of MoveLine, initially empty
            quantityMatches,                // Map of quantity matches
            mapOf(),                        // Empty map for tracking types initially
            pickId,                         // Pick ID
            this                            // Listener, 'this' refers to PickProductsActivity implementing OnProductClickListener
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = pickProductsAdapter
    }


    override fun onProductClick(product: MoveLine) {
        showProductDialog(product)
    }


    private fun showProductDialog(product: MoveLine) {
        // Inflate the custom layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_product_details, null)
    
        // Retrieve all the TextViews from the inflated layout
        val textProductName = dialogView.findViewById<TextView>(R.id.textProductName)
        val textProductQuantity = dialogView.findViewById<TextView>(R.id.textProductQuantity)
        val textProductFromLocation = dialogView.findViewById<TextView>(R.id.textProductFromLocation)
        val textProductToLocation = dialogView.findViewById<TextView>(R.id.textProductToLocationHeading)
        val textProductLotNumber = dialogView.findViewById<TextView>(R.id.textProductLotNumber)
        val editTextProductLotNumber = dialogView.findViewById<EditText>(R.id.editTextProductLotNumber)
        val lotNumberLayout = dialogView.findViewById<LinearLayout>(R.id.lotNumberLayout) // Reference to the LinearLayout
        val buttonEditLotNumber = dialogView.findViewById<ImageButton>(R.id.buttonEditLotNumber)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val buttonConfirmQuantity = dialogView.findViewById<Button>(R.id.buttonConfirmSN) // Ensure this ID is correct
    
        // Set values to the TextViews
        textProductName.text = product.productName
        textProductQuantity.text = "Quantity: ${product.quantity}"
        textProductFromLocation.text = "From Location: ${product.locationName}"
        textProductToLocation.text = "To Location: ${product.locationDestName}"
        textProductLotNumber.text = "${product.lotName}"
    
        // Determine the product's tracking type
        coroutineScope.launch {
            val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(product.productName)
            val trackingType = trackingAndExpiration?.first ?: "none"

            withContext(Dispatchers.Main) {
                // Toggle the visibility for editing the lot number based on tracking type
                if (trackingType == "serial" && product.lotName.isNotEmpty()) {
                    lotNumberLayout.visibility = View.VISIBLE
                    buttonEditLotNumber.visibility = View.VISIBLE
                    buttonEditLotNumber.setOnClickListener {
                        if (editTextProductLotNumber.visibility == View.GONE) {
                            editTextProductLotNumber.visibility = View.VISIBLE
                            editTextProductLotNumber.setText(product.lotName)
                            textProductLotNumber.visibility = View.GONE
                        } else {
                            editTextProductLotNumber.visibility = View.GONE
                            textProductLotNumber.visibility = View.VISIBLE
                            textProductLotNumber.text = "${editTextProductLotNumber.text}"
                        }
                    }
                } else if (trackingType == "none") {
                    lotNumberLayout.visibility = View.GONE
                } else {
                    lotNumberLayout.visibility = View.VISIBLE
                    buttonEditLotNumber.visibility = View.GONE
                }
            }
        }
    
        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

    buttonConfirmQuantity.setOnClickListener {
        if (editTextProductLotNumber.visibility == View.VISIBLE) {
            val enteredSerialNumber = editTextProductLotNumber.text.toString()
            product.lotName = enteredSerialNumber
            textProductLotNumber.text = enteredSerialNumber

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    odooXmlRpcClient.updateMoveLinesForPick(product.id, pickId, enteredSerialNumber, product.productId)
                    withContext(Dispatchers.Main) {
                        Log.d("UpdateProduct", "Successfully updated move line for product ID: ${product.id}")
                        fetchProductsForPick(pickId)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("UpdateProduct", "Failed to update move line: ${e.localizedMessage}")
                        // Handle errors, possibly showing a dialog to the user or retrying the operation
                    }
                }
            }
        }
        dialog.dismiss()
    }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
    
        dialog.show()
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

                // Fetch additional package information if required
                odooXmlRpcClient.fetchResultPackagesByPickingId(pickId)

                val fetchJobs = fetchedLines.map { moveLine ->
                    coroutineScope.async(Dispatchers.IO) {
                        val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(moveLine.productName) ?: Pair("none", false)
                        val barcode = barcodeToProductIdMap.filterValues { it == moveLine.productId }.keys.firstOrNull()?.toString()

                        moveLine.copy(
                            barcode = barcode,
                            trackingType = trackingAndExpiration?.first ?: "none"
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
                        showSourceLocationDialog(productLine.productName, productLine.quantity, pickId, productLine.id, productLine.locationName, trackingType,productLine.productId, productLine.locationId, productLine.lotName)
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

    private fun showSourceLocationDialog(
        productName: String, quantity: Double, pickId: Int, lineId: Int,
        locationName: String, trackingType: String, productId: Int,
        locationId: Int, lotName: String
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_scan_source_location, null)
        val sourceLocationInput = dialogView.findViewById<EditText>(R.id.sourceLocationInput)
        val locationNameTextView = dialogView.findViewById<TextView>(R.id.locationNameTextView)
        locationNameTextView.text = locationName

        sourceLocationInput.setHintTextColor(Color.WHITE)
        sourceLocationInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                dialogView.findViewById<Button>(R.id.confirmButton).performClick()
                true
            } else {
                false
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Disables dismiss by clicking outside or pressing back
            .create()

        dialogView.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            val enteredLocation = sourceLocationInput.text.toString()
            if (enteredLocation.isNotEmpty()) {
                coroutineScope.launch {
                    if (enteredLocation == locationName) {
                        // If the entered location matches the expected location
                        withContext(Dispatchers.Main) {
                            showGreenToast("Source location confirmed: $enteredLocation")
                            dialog.dismiss()
                            when (trackingType) {
                                "serial" -> promptForSerialNumber(productName, pickId, productId, locationId, lineId)
                                "lot" -> promptConfirmLotQuantity(productName, lotName, quantity, locationName, lineId, pickId)
                                "none" -> displayQuantityDialog(productName, quantity, pickId, lineId)
                                else -> Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
                            }
                        }
                    } else {
                        // If the entered location does not match
                        withContext(Dispatchers.Main) {
                            showRedToast("Incorrect location, please re-enter.")
                            sourceLocationInput.text.clear() // Clear the input for re-entry
                        }
                    }
                }
            } else {
                showRedToast("Please enter a source location.")
            }
        }

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show() // Display the dialog
        sourceLocationInput.requestFocus() // Autofocus on the source location input field when the dialog shows
    }

    private fun displayQuantityDialog(productName: String, expectedQuantity: Double, pickId: Int, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_confirmation, null)
        val textViewConfirmation = dialogView.findViewById<TextView>(R.id.ConfirmationTextView)
        val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirm)
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

    private fun promptConfirmLotQuantity(productName: String, lotName: String, quantity: Double, locationName: String, lineId: Int, pickId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_quantity, null)
        dialogView.findViewById<TextView>(R.id.textProductInfo).text = "Product: $productName"
        dialogView.findViewById<TextView>(R.id.textQuantityInfo).text = "Quantity: $quantity"
        dialogView.findViewById<TextView>(R.id.textLotInfo).text = "LOT: $lotName"
        dialogView.findViewById<TextView>(R.id.textSourceLocationInfo).text = "Pick From: $locationName"

        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val alertDialog = AlertDialog.Builder(this)

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
        editText.setHintTextColor(Color.WHITE)
        val textQuantityInfo: TextView = dialogView.findViewById(R.id.textQuantityInfo)
        val confirmButton: Button = dialogView.findViewById(R.id.confirmButton)
        val buttonCancel: Button = dialogView.findViewById(R.id.buttonCancel)

        // Set the texts for TextViews
        textProductInfo.text = "Product: $productName"
        textQuantityInfo.text = "Quantity: $quantity"
        textLotInfo.text = "LOT: $lotName"

        // Configure and show AlertDialog
        val alertDialog = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(false)
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

        // Set up the editor action listener for the enter key
        editText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                confirmButton.performClick()
                true
            } else {
                false
            }
        }

        // Automatically show keyboard when dialog appears
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int, locationId: Int, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)

        serialNumberInput.setHintTextColor(Color.WHITE)

        // Set up the editor action listener for the serial number input
        serialNumberInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                buttonConfirmSN.performClick()
                true
            } else {
                false
            }
        }

        // Access SharedPreferences to retrieve saved serial numbers, using both pickId and productId in the key
        val sharedPrefs = getSharedPreferences("SerialNumbers", Context.MODE_PRIVATE)
        val key = "Serials_${pickId}_$productId"
        val savedSerials = sharedPrefs.getStringSet(key, mutableSetOf()) ?: mutableSetOf()

        // Configure the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)  // Disable dismissing the dialog by pressing back or clicking outside
            .create()

        // Set up the confirm button action
        buttonConfirmSN.setOnClickListener {
            val enteredSerialNumber = serialNumberInput.text.toString().trim()
            if (enteredSerialNumber.isNotEmpty()) {
                coroutineScope.launch {
                    if (savedSerials.contains(enteredSerialNumber)) {
                        withContext(Dispatchers.Main) {
                            showRedToast("Serial number already added. Please enter a different serial number.")
                            serialNumberInput.setText("")
                            serialNumberInput.requestFocus()
                        }
                    } else {
                        val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)

                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
                            savedSerials.add(enteredSerialNumber)
                            sharedPrefs.edit().putStringSet(key, savedSerials).apply()
                            odooXmlRpcClient.updateMoveLinesForPick(lineId, pickId, enteredSerialNumber, productId)
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
                                serialNumberInput.setText("")
                                serialNumberInput.requestFocus()
                            }
                        }
                    }
                }
            } else {
                showRedToast("Please enter a serial number.")
                serialNumberInput.setText("")
                serialNumberInput.requestFocus()
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

    private fun sendEmailToBuyer(buyerEmail: String, buyerName: String, pickName: String?) {
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

                - Receipt Name: $pickName

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

    private fun saveMatchStateToPreferences(key: ProductPickKey, matched: Boolean) {
        val sharedPref = getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("${key.moveLineId}_${key.pickId}", matched)
            apply()
        }
    }

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
}


