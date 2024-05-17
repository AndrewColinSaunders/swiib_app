
package com.example.warehousetet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
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

class IntTransferProductsActivity : AppCompatActivity(), IntTransferProductsAdapter.OnProductClickListener{
    private lateinit var intTransferProductsAdapter: IntTransferProductsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    //    private var productBarcodes = hashMapOf<String, String>()
    private var quantityMatches = mutableMapOf<ProductPickKey, Boolean>()
    private var barcodeToProductIdMap = mutableMapOf<String, Int>()
    // Assuming this is declared at the class level
    private val confirmedLines = mutableSetOf<Int>()
    private var transferId: Int = -1

    private var transferName: String? = null
    private var destLocationName: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pick_activity_products)

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))

        barcodeInput = findViewById(R.id.pickBarcodeInput)
        confirmButton = findViewById(R.id.pickConfirmButton)


        transferId = intent.getIntExtra("TRANSFER_ID", -1)
        transferName = intent.getStringExtra("TRANSFER_NAME") ?: "Transfer"


//        val locationName = intent.getStringExtra("LOCATION")


        destLocationName = intent.getStringExtra("DEST_LOCATION")


        supportActionBar?.title = transferName
        setupRecyclerView()

        if (transferId != -1) {
            fetchProductsForPick(transferId)
            coroutineScope.launch {
                try {
//                    displayLocationsForPick(pickId)
                } catch (e: Exception) {
                    Log.e("IntTransferProductsActivity", "Error displaying locations for pick: ${e.message}")
                }
            }
        } else {
            Log.e("IntTransferProductsActivity", "Invalid delivery order ID passed to IntTransferProductsActivity.")
        }


        findViewById<Button>(R.id.pickClearButton).setOnClickListener {
            findViewById<EditText>(R.id.pickBarcodeInput).text.clear()
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                handleCameraResult(result.data)
            } else {
                Toast.makeText(this, "Image capture failed or cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

        val enterManuallyButton: Button = findViewById(R.id.pickManualEntryButton)
        val confirmButton: Button = findViewById(R.id.pickConfirmButton)
        val clearButton: Button = findViewById(R.id.pickClearButton)
        val barcodeInput: EditText = findViewById(R.id.pickBarcodeInput)

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
        setupBarcodeVerification(transferId)
        restoreButtonVisibility(transferId)
        loadMatchStatesFromPreferences(transferId)
        restoreButtonVisibility(transferId)
    }
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.flag_menu_item, menu)
//        return true
//    }
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> {
//                onBackPressedDispatcher.onBackPressed()
//                return true
//            }
//            R.id.action_flag -> {
//                showFlagDialog()
//                return true  // Ensure to return true after handling the action
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_products_activity, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_flag_receipt -> {  // Updated ID
                showFlagDialog()
                true  // Ensure to return true after handling the action
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        // Find the nested "Flag" menu item using its ID.
        val menuItem = menu.findItem(R.id.action_flag_receipt)
        // Create a SpannableString with the title of the menu item.
        val spanString = SpannableString(menuItem.title).apply {
            // Apply a red color span.
            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@IntTransferProductsActivity, R.color.danger_red)), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            // You can also make the text bold if desired.
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }
        // Set the modified SpannableString back as the title of the menu item.
        menuItem.title = spanString
        return true
    }


    private fun showFlagDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, findViewById(android.R.id.content), false)
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
                    val pickName = this@IntTransferProductsActivity.transferName ?: run {
                        withContext(Dispatchers.Main) {
                            Log.e("PickProductsActivity", "Pick name is null")
                            Toast.makeText(this@IntTransferProductsActivity, "Invalid pick details", Toast.LENGTH_SHORT).show()
                            barcodeInput.setText("")
                            barcodeInput.requestFocus()
                        }
                        return@launch
                    }

                    val buyerDetails = odooXmlRpcClient.fetchAndLogBuyerDetails(pickName)
                    if (buyerDetails != null) {
                        withContext(Dispatchers.Main) {
                            // Capture image before sending the email
                            captureImage()
                        }

                        // Ensure email is sent after image capture dialog completion
                        sendEmailToBuyer(buyerDetails.login, buyerDetails.name, pickName)
                        withContext(Dispatchers.Main) {
                            Log.d("PickProductsActivity", "Pick flagged and buyer notified via email.")
                            Toast.makeText(this@IntTransferProductsActivity, "Pick flagged", Toast.LENGTH_SHORT).show()
                            barcodeInput.setText("")
                            barcodeInput.requestFocus()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Log.e("PickProductsActivity", "Failed to fetch buyer details or flag the pick.")
                            Toast.makeText(this@IntTransferProductsActivity, "Failed to flag pick", Toast.LENGTH_SHORT).show()
                            barcodeInput.setText("")
                            barcodeInput.requestFocus()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("PickProductsActivity", "Error in flagging process: ${e.localizedMessage}", e)
                        Toast.makeText(this@IntTransferProductsActivity, "Error during flagging", Toast.LENGTH_SHORT).show()
                        barcodeInput.setText("")
                        barcodeInput.requestFocus()
                    }
                }
                dialog.dismiss()  // Dismiss the dialog once the operations are complete
            }
        }

        dialog.show()  // Show the dialog
    }

//    companion object {
//        const val CAMERA_REQUEST_CODE = 1001
//    }
    private fun captureImage() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Capture Image?")
        builder.setMessage("Would you like to capture an image?")

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        builder.setPositiveButton("Capture Image") { dialog, _ ->
            dialog.dismiss()
            openCamera()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }
    }

    @Suppress("DEPRECATION")
    private fun handleCameraResult(data: Intent?) {
        val imageBitmap = data?.extras?.get("data") as? Bitmap
        if (imageBitmap != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

            Log.d("CaptureImage", "Encoded image: $encodedImage")

            coroutineScope.launch {
                val updateResult = odooXmlRpcClient.updatePickingImage(transferId, encodedImage)
                Log.d("OdooUpdate", "Update result: $updateResult")
                barcodeInput.setText("")
                barcodeInput.requestFocus()
            }
        } else {
            Log.e("CaptureImage", "Failed to capture image")
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }
    }

//    private fun captureImage() {
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
//            openCamera()  // Pass pickId to ensure it is available after capturing the image
//        }
//        dialog.show()
//    }
//
//    private fun openCamera() {
//        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (cameraIntent.resolveActivity(packageManager) != null) {
//            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
//        } else {
//            Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
//        }
//    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
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
//                    val updateResult = odooXmlRpcClient.updatePickingImage(transferId, encodedImage)
//                    Log.d("OdooUpdate", "Update result: $updateResult") // Log the result from the server
//                }
//            } else {
//                Log.e("CaptureImage", "Failed to capture image")
//            }
//        }
//    }

    override fun onResume() {
        super.onResume()
        // Restore visibility state whenever the activity resumes
        restoreButtonVisibility(transferId)
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.pickProductsRecyclerView)
        // Ensure you are passing all necessary parameters in the correct order
        // The parameters should be (List<MoveLine>, Map<ProductPickKey, Boolean>, Map<Int, String>, Int, OnProductClickListener)
        intTransferProductsAdapter = IntTransferProductsAdapter(
            emptyList(),                    // List of MoveLine, initially empty
            quantityMatches,                // Map of quantity matches
            mapOf(),                        // Empty map for tracking types initially
            transferId,                         // Pick ID
            this                            // Listener, 'this' refers to PickProductsActivity implementing OnProductClickListener
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = intTransferProductsAdapter
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
        textProductQuantity.text = getString(R.string.product_quantity, product.quantity)
        textProductFromLocation.text = getString(R.string.from_location, product.locationName)
        textProductToLocation.text = getString(R.string.to_location, product.locationDestName)
        textProductLotNumber.text = product.lotName

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
                        odooXmlRpcClient.updateMoveLinesForPick(product.id, transferId, enteredSerialNumber, product.productId)
                        withContext(Dispatchers.Main) {
                            Log.d("UpdateProduct", "Successfully updated move line for product ID: ${product.productId}")
                            fetchProductsForPick(transferId)

                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("UpdateProduct", "Failed to update move line: ${e.localizedMessage}")

                        }
                    }
                }
            }
            dialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }

        dialog.show()
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
                            trackingType = trackingAndExpiration.first ?: "none"
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
        intTransferProductsAdapter.updateProducts(moveLines, pickId, quantityMatches)
        Log.d("PickProductsActivity", "Adapter updated with new products and match states.")
    }

    private fun fetchBarcodesForProducts(moveLines: List<MoveLine>) {
        moveLines.forEach { moveLine ->
            coroutineScope.launch {
                val barcode = odooXmlRpcClient.fetchProductBarcodeByName(moveLine.productName)
                barcode?.let {
                    // Assuming barcodeToProductIdMap should map barcode to product ID
                    synchronized(this@IntTransferProductsActivity) { // Ensure to use your current activity or context correctly
                        barcodeToProductIdMap[barcode] = moveLine.productId
                    }
                }
            }
        }
    }

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
    private fun setupBarcodeVerification(pickId: Int) {
        fun performBarcodeVerification() {
            val enteredBarcode = barcodeInput.text.toString().trim()
            verifyBarcode(enteredBarcode, pickId)
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

    private fun verifyBarcode(scannedBarcode: String, pickId: Int) {
        coroutineScope.launch {
            val productId = barcodeToProductIdMap[scannedBarcode]
            val packagingProductInfo = odooXmlRpcClient.fetchProductIdFromPackagingBarcode(scannedBarcode)

            if (productId != null) {
                val productLines = intTransferProductsAdapter.lines.filter { it.productId == productId }.sortedBy { it.id }
                val nextProductLine = productLines.find { !confirmedLines.contains(it.id) }

                nextProductLine?.let { productLine ->
                    val trackingAndExpiration = odooXmlRpcClient.fetchProductTrackingAndExpirationByName(productLine.productName)
                    val trackingType = trackingAndExpiration?.first ?: "none"

                    withContext(Dispatchers.Main) {
                        showSourceLocationDialog(productLine.productName, productLine.quantity, pickId, productLine.id, productLine.locationName, trackingType,productLine.productId, productLine.lotName)
                    }
                }
            } else if (packagingProductInfo != null) {
                Log.d("verifyBarcode", "Found barcode in product.packaging model. Product ID: ${packagingProductInfo.first}, Quantity: ${packagingProductInfo.second}")
            } else {
                withContext(Dispatchers.Main) {
                    Log.d("verifyBarcode", "Barcode not found in product.template or product.packaging models")
                    barcodeInput.setText("")
                    barcodeInput.requestFocus()
                }
            }
        }
    }

    private fun showSourceLocationDialog(
        productName: String, quantity: Double, pickId: Int, lineId: Int,
        locationName: String, trackingType: String, productId: Int,
        lotName: String
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_scan_source_location, null)
        val sourceLocationInput = dialogView.findViewById<EditText>(R.id.sourceLocationInput)
        val locationNameTextView = dialogView.findViewById<TextView>(R.id.locationNameTextView)
        locationNameTextView.text = locationName

        sourceLocationInput.setHintTextColor(Color.WHITE)
        sourceLocationInput.setOnEditorActionListener { _, actionId, event ->
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
                            Toast.makeText(this@IntTransferProductsActivity, "Source location confirmed: $enteredLocation", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            when (trackingType) {
                                "serial" -> promptForSerialNumber(productName, pickId, productId, lineId)
                                "lot" -> promptConfirmLotQuantity(productName, lotName, quantity, locationName, lineId, pickId)
                                "none" -> displayQuantityDialog(productName, quantity, pickId, lineId)
                                else -> Log.d("verifyBarcode", "Tracking Type: Other - $trackingType")
                            }
                        }
                    } else {
                        // If the entered location does not match
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@IntTransferProductsActivity, "Incorrect location, please re-enter.", Toast.LENGTH_SHORT).show()
                            sourceLocationInput.text.clear() // Clear the input for re-entry
                        }
                    }
                }
            } else {
                Toast.makeText(this@IntTransferProductsActivity, "Please enter a source location.", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }

        dialog.show() // Display the dialog
        sourceLocationInput.requestFocus() // Autofocus on the source location input field when the dialog shows
    }

//    private fun displayQuantityDialog(productName: String, expectedQuantity: Double, pickId: Int, lineId: Int) {
//
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_confirmation, null)
//        val textViewConfirmation = dialogView.findViewById<TextView>(R.id.ConfirmationTextView)
//        val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirm)
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
//        val productNameStart = fullText.indexOf(productName)
//        val productNameEnd = productNameStart + productName.length
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

    private fun displayQuantityDialog(productName: String, expectedQuantity: Double, pickId: Int, lineId: Int) {
        val rootView = findViewById<ViewGroup>(android.R.id.content)  // Use root view to resolve layout parameters
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_confirmation, rootView, false)
        val textViewConfirmation = dialogView.findViewById<TextView>(R.id.ConfirmationTextView)
        val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirm)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        val fullText = getString(R.string.confirm_quantity_message, expectedQuantity, productName)
        val spannableString = SpannableString(fullText)

        // Styling for expectedQuantity
        val quantityStart = fullText.indexOf("$expectedQuantity")
        val quantityEnd = quantityStart + "$expectedQuantity".length
        spannableString.setSpan(StyleSpan(Typeface.BOLD), quantityStart, quantityEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.1f), quantityStart, quantityEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Styling for productName
        val productNameStart = fullText.indexOf(productName)
        val productNameEnd = productNameStart + productName.length
        spannableString.setSpan(StyleSpan(Typeface.BOLD), productNameStart, productNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.1f), productNameStart, productNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textViewConfirmation.text = spannableString

        val alertDialog = AlertDialog.Builder(this).apply {
            setView(dialogView)
            create()
        }.show()

        buttonConfirm.setOnClickListener {
            updateProductMatchState(lineId, pickId)
            confirmedLines.add(lineId)
            alertDialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }

        buttonCancel.setOnClickListener {
            alertDialog.dismiss()
            barcodeInput.setText("")
            barcodeInput.requestFocus()
        }
    }


    private fun promptConfirmLotQuantity(productName: String, lotName: String, quantity: Double, locationName: String, lineId: Int, pickId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_quantity, null)
        dialogView.findViewById<TextView>(R.id.textProductInfo).text = getString(R.string.product_message, productName)
        dialogView.findViewById<TextView>(R.id.textQuantityInfo).text = getString(R.string.product_quantity, quantity)
        dialogView.findViewById<TextView>(R.id.textLotInfo).text = getString(R.string.lot_info, lotName)
        dialogView.findViewById<TextView>(R.id.textSourceLocationInfo).text = getString(R.string.source_location_info, locationName)

        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val alertDialog = AlertDialog.Builder(this)

            .setView(dialogView)
            .create()

        confirmButton.setOnClickListener {
            confirmLotNumber(productName, quantity, lotName, lineId, pickId)
            Toast.makeText(this@IntTransferProductsActivity, "Quantity confirmed for $productName.", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()  // Close the dialog after confirmation
            barcodeInput.requestFocus()
        }
        buttonCancel.setOnClickListener {
            alertDialog.dismiss()
            barcodeInput.requestFocus()// Close the dialog when cancel is clicked
        }

        alertDialog.show()
    }

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
//        textProductInfo.text = getString(R.string.product_message, productName)
//        textQuantityInfo.text = getString(R.string.product_quantity, quantity)
//        textLotInfo.text = getString(R.string.lot_info, lotName)
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

    private fun confirmLotNumber(productName: String, quantity: Double, lotName: String, lineId: Int, pickId: Int) {
        val rootView = findViewById<ViewGroup>(android.R.id.content)  // Use root view to resolve layout parameters
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lot_entry, rootView, false)
        val textProductInfo: TextView = dialogView.findViewById(R.id.textProductInfo)
        val textLotInfo: TextView = dialogView.findViewById(R.id.textLotInfo)
        val editText: EditText = dialogView.findViewById(R.id.editLotNumber)
        editText.setHintTextColor(Color.WHITE)
        val textQuantityInfo: TextView = dialogView.findViewById(R.id.textQuantityInfo)
        val confirmButton: Button = dialogView.findViewById(R.id.confirmButton)
        val buttonCancel: Button = dialogView.findViewById(R.id.buttonCancel)

        // Set the texts for TextViews using string resources
        textProductInfo.text = getString(R.string.product_message, productName)
        textQuantityInfo.text = getString(R.string.product_quantity, quantity)
        textLotInfo.text = getString(R.string.lot_info, lotName)

        val alertDialog = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(false)
            create()
        }.show()

        confirmButton.setOnClickListener {
            val enteredLotNumber = editText.text.toString()
            if (enteredLotNumber == lotName) {
                Toast.makeText(this@IntTransferProductsActivity, getString(R.string.correct_lot_number, productName), Toast.LENGTH_SHORT).show()
                updateProductMatchState(lineId, pickId)
                confirmedLines.add(lineId)
                alertDialog.dismiss()
                barcodeInput.requestFocus()
            } else {
                Toast.makeText(this@IntTransferProductsActivity, getString(R.string.incorrect_lot_number), Toast.LENGTH_SHORT).show()
                barcodeInput.requestFocus()
            }
        }
        buttonCancel.setOnClickListener {
            alertDialog.dismiss()
            barcodeInput.requestFocus()
        }

        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                confirmButton.performClick()
                true
            } else {
                false
            }
        }

        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }


    private fun promptForSerialNumber(productName: String, pickId: Int, productId: Int, lineId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_number_input, null)
        val serialNumberInput = dialogView.findViewById<EditText>(R.id.serialNumberInput)
        val buttonConfirmSN = dialogView.findViewById<Button>(R.id.buttonConfirmSN)
        val buttonCancelSN = dialogView.findViewById<Button>(R.id.buttonCancelSN)
        val messageTextView = dialogView.findViewById<TextView>(R.id.ProductMessage)
        messageTextView.text = getString(R.string.product_message, productName)
        serialNumberInput.setHintTextColor(Color.WHITE)

        // Set up the editor action listener for the serial number input
        serialNumberInput.setOnEditorActionListener { _, actionId, event ->
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
                            Toast.makeText(this@IntTransferProductsActivity, "Serial number already added. Please enter a different serial number.", Toast.LENGTH_SHORT).show()
                            serialNumberInput.setText("")
                            serialNumberInput.requestFocus()
                        }
                    } else {
                        val serialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId)

                        if (serialNumbers?.contains(enteredSerialNumber) == true) {
                            // Create a new HashSet based on the existing savedSerials set
                            val newSavedSerials = HashSet(savedSerials)
                            newSavedSerials.add(enteredSerialNumber)

                            // Save the new set to SharedPreferences
                            sharedPrefs.edit().putStringSet(key, newSavedSerials).apply()

                            odooXmlRpcClient.updateMoveLinesForPick(lineId, pickId, enteredSerialNumber, productId)

                            withContext(Dispatchers.Main) {
                                updateProductMatchState(lineId, pickId)
                                confirmedLines.add(lineId)
                                Toast.makeText(this@IntTransferProductsActivity, "Serial number added for $productName.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                fetchProductsForPick(pickId)
                                barcodeInput.requestFocus()
                            }
                        }
                         else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@IntTransferProductsActivity, "Serial number does not exist. Please enter a valid serial number.", Toast.LENGTH_SHORT).show()
                                serialNumberInput.setText("")
                                serialNumberInput.requestFocus()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this@IntTransferProductsActivity, "Please enter a serial number.", Toast.LENGTH_SHORT).show()
                serialNumberInput.setText("")
                serialNumberInput.requestFocus()
            }
        }

        // Set up the cancel button action
        buttonCancelSN.setOnClickListener {
            dialog.dismiss()  // Dismiss the dialog
            barcodeInput.requestFocus()
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
            barcodeInput.requestFocus()
        } catch (e: MessagingException) {
            Log.e("EmailSender", "Failed to send email.", e)
            barcodeInput.requestFocus()
        }
    }

    private fun updateProductMatchState(
        lineId: Int,
        pickId: Int,
        matched: Boolean = true,
    ) {
        val key = ProductPickKey(lineId, pickId)
        val productLine = intTransferProductsAdapter.lines.find { it.id == lineId }

        if (productLine != null) {
            quantityMatches[key] = matched

            // Refresh the UI to reflect the updated state
            runOnUiThread {
                val position = intTransferProductsAdapter.findProductPositionById(lineId)
                if (position != -1) {
                    intTransferProductsAdapter.notifyItemChanged(position)
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
                            val mediaPlayer: MediaPlayer? = MediaPlayer.create(applicationContext, R.raw.button_pressed)
                            mediaPlayer?.start()
                            mediaPlayer?.setOnCompletionListener {
                                it.release()
                            }
                            // Redirect to PickActivity upon successful validation
                            val intent = Intent(this@IntTransferProductsActivity, InternalTransfersActivity::class.java)
                            startActivity(intent)
                            finish()  // Optionally call finish() if you want to remove this activity from the back stack
                        } else {
                            Toast.makeText(applicationContext, "Failed to validate picking.\nPlease flag or recount quantities", Toast.LENGTH_SHORT).show()
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
                        val mediaPlayer: MediaPlayer? = MediaPlayer.create(applicationContext, R.raw.button_pressed)
                        mediaPlayer?.start()
                        mediaPlayer?.setOnCompletionListener {
                            it.release()
                        }
                        val intent = Intent(this@IntTransferProductsActivity, InternalTransfersActivity::class.java)
                        startActivity(intent)
                        finish()  // Optionally call finish() if you want to remove this activity from the back stack
                    } else {
                        Toast.makeText(applicationContext, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
                        barcodeInput.requestFocus()
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
            intTransferProductsAdapter.updateProducts(intTransferProductsAdapter.lines, pickId, quantityMatches)
        }
    }
}
