package com.example.warehousetet

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
//import org.apache.xmlrpc.client.XmlRpcClient
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class DeliveryOrdersProductsActivity : AppCompatActivity(), DeliveryOrdersProductsAdapter.VerificationListener {
    private lateinit var deliveryOrdersProductsAdapter: DeliveryOrdersProductsAdapter
    private lateinit var validateButton: MaterialButton
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var barcodeInput: EditText
    private var lastScannedBarcode = StringBuilder()
    private var lastKeyTime: Long = 0
    private var isScannerInput = false
    private val usedSerialNumbers = mutableSetOf<String>()
    private var serialNumberToMoveLineIdMap = mutableMapOf<String, Int>()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    private val deliveryOrdersId by lazy { intent.getIntExtra("DELIVERY_ORDERS_ID", -1) }
    private val deliveryOrdersName by lazy { intent.getStringExtra("DELIVERY_ORDERS_NAME") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_orders_products)
        registerBackPressHandler()

        validateButton = findViewById(R.id.validateOperationButton)
        validateButton.visibility = View.INVISIBLE

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
                            odooXmlRpcClient.updatePickingImage(deliveryOrdersId, encodedImage)
                            Log.d("OdooUpdate", "Image updated successfully on server")
                        } catch (e: Exception) {
                            Log.e("OdooUpdate", "Failed to update image: ${e.localizedMessage}", e)
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



        Log.d("DeliveryOrdersProductsActivity", "Activity created with delivery orders ID: $deliveryOrdersId")

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        initializeUI()
        if (deliveryOrdersId != -1) {
            fetchMoveLinesForPickingId()
        } else {
            Log.e("DeliveryOrdersProductsActivity", "Invalid delivery orders ID passed to DeliveryOrdersProductsActivity.")
        }
    }

    override fun onPause() {
        super.onPause()
        saveVerificationState()
    }

    override fun onResume() {
        super.onResume()
        loadVerificationState()
    }

    private fun getSharedPreferences(): SharedPreferences {
        return getSharedPreferences("DeliveryOrderPrefs", Context.MODE_PRIVATE)
    }

    private fun saveVerificationState() {
        val verifiedPackages = deliveryOrdersProductsAdapter.verifiedPackages.joinToString(",")
        val verifiedSerialNumbers = deliveryOrdersProductsAdapter.verifiedSerialNumbers.joinToString(",")
        val verifiedBarcodes = deliveryOrdersProductsAdapter.verifiedBarcodes.joinToString(",")
        val isValidateButtonVisible = validateButton.visibility == View.VISIBLE

        getSharedPreferences().edit().apply {
            putString("verifiedPackages_$deliveryOrdersId", verifiedPackages)
            putString("verifiedSerialNumbers_$deliveryOrdersId", verifiedSerialNumbers)
            putString("verifiedBarcodes_$deliveryOrdersId", verifiedBarcodes)
            putBoolean("validateButtonVisible_$deliveryOrdersId", isValidateButtonVisible)
            apply()
        }
    }

    private fun loadVerificationState() {
        val prefs = getSharedPreferences()

        // Load the verification state
        val verifiedPackages = prefs.getString("verifiedPackages_$deliveryOrdersId", "")!!
            .split(",").filterNot { it.isEmpty() }.map { it.toInt() }.toMutableSet()
        val verifiedSerialNumbers = prefs.getString("verifiedSerialNumbers_$deliveryOrdersId", "")!!
            .split(",").filterNot { it.isEmpty() }.toMutableSet()
        val verifiedBarcodes = prefs.getString("verifiedBarcodes_$deliveryOrdersId", "")!!
            .split(",").filterNot { it.isEmpty() }.toMutableSet()

        // Determine the differences
        val oldVerifiedPackages = deliveryOrdersProductsAdapter.verifiedPackages
        val oldVerifiedSerialNumbers = deliveryOrdersProductsAdapter.verifiedSerialNumbers
        val oldVerifiedBarcodes = deliveryOrdersProductsAdapter.verifiedBarcodes

        deliveryOrdersProductsAdapter.verifiedPackages = verifiedPackages
        deliveryOrdersProductsAdapter.verifiedSerialNumbers = verifiedSerialNumbers
        deliveryOrdersProductsAdapter.verifiedBarcodes = verifiedBarcodes

        validateButton.visibility = if (prefs.getBoolean("validateButtonVisible_$deliveryOrdersId", false)) View.VISIBLE else View.INVISIBLE

        // Notify changes in the adapter
        val sections = deliveryOrdersProductsAdapter.sections
        sections.forEachIndexed { sectionIndex, section ->
            section.moveLines.forEachIndexed { _, moveLine ->
                val isPackageVerified = oldVerifiedPackages.contains(section.packageId) != verifiedPackages.contains(section.packageId)
                val isSerialVerified = oldVerifiedSerialNumbers.contains(moveLine.lotName) != verifiedSerialNumbers.contains(moveLine.lotName)
                val isBarcodeVerified = oldVerifiedBarcodes.contains(moveLine.lineId.toString()) != verifiedBarcodes.contains(moveLine.lineId.toString())
                if (isPackageVerified || isSerialVerified || isBarcodeVerified) {
                    deliveryOrdersProductsAdapter.notifyItemChanged(sectionIndex)
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delivery_orders_products, menu)

        menu?.findItem(R.id.action_flag)?.let { menuItem ->
            val colorDangerRed = ContextCompat.getColor(this, R.color.danger_red)
            val coloredDrawable = menuItem.icon?.mutate()?.apply {
                colorFilter = PorterDuffColorFilter(colorDangerRed, PorterDuff.Mode.SRC_ATOP)
            }
            menuItem.icon = coloredDrawable
        }
        return true
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (isScannerInput) {
                    val scannedPackageBarcode = lastScannedBarcode.toString()
                    verifyBarcode(scannedPackageBarcode)
                    lastScannedBarcode.clear()
                    isScannerInput = false
                }
                return true
            } else {
                val char = event.unicodeChar.toChar()
                if (!Character.isISOControl(char)) {
                    val currentTime = System.currentTimeMillis()
                    isScannerInput = lastKeyTime != 0L && (currentTime - lastKeyTime) < 50
                    lastScannedBarcode.append(char)
                    lastKeyTime = currentTime
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun verifyBarcode(scannedBarcode: String) = lifecycleScope.launch {
        val matchingPackage = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }
            .find { it.resultPackageName == scannedBarcode }

        if (matchingPackage != null) {
            Log.d("DeliveryOrdersProductsActivity", "Package verification successful: Package ID ${matchingPackage.resultPackageId} has been verified.")
            handlePackageVerificationSuccess(matchingPackage)
            barcodeInput.text.clear()
            return@launch
        }

        val matchingMoveLine = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }
            .find { it.resultPackageName == "None" && withContext(Dispatchers.IO) { odooXmlRpcClient.fetchProductBarcodeByName(it.productName) } == scannedBarcode }

        if (matchingMoveLine != null) {
            Log.d("DeliveryOrdersProductsActivity", "Barcode verification successful for product: ${matchingMoveLine.productName}")
            checkProductTrackingAndHandle(matchingMoveLine)
            barcodeInput.text.clear()
        } else {
            Toast.makeText(this@DeliveryOrdersProductsActivity, "Barcode does not match any known package or product, or is not 'Not Packaged'.", Toast.LENGTH_SHORT).show()
            Log.e("DeliveryOrdersProductsActivity", "Barcode verification failed. No matching 'Not Packaged' product found.")
            barcodeInput.selectAll()
        }
    }

    private fun initializeUI() {
        val packName = intent.getStringExtra("DELIVERY_ORDERS_NAME") ?: "DeliveryOrders"
        supportActionBar?.title = packName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        deliveryOrdersProductsAdapter = DeliveryOrdersProductsAdapter(this)
        findViewById<RecyclerView>(R.id.deliveryOrdersProductsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@DeliveryOrdersProductsActivity)
            adapter = deliveryOrdersProductsAdapter
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        validateButton = findViewById(R.id.validateOperationButton)
        barcodeInput = findViewById(R.id.deliveryOrdersBarcodeInput)
        val packConfirmButton = findViewById<Button>(R.id.deliveryOrdersConfirmButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        validateButton.setOnClickListener {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(50)
                }
            }
            Log.d("ValidationAttempt", "Attempting to validate operation for pack ID: $deliveryOrdersId")
            lifecycleScope.launch {
                if (odooXmlRpcClient.validateOperation(deliveryOrdersId, this@DeliveryOrdersProductsActivity)) {
                    Toast.makeText(this@DeliveryOrdersProductsActivity, "Operation validated successfully!", Toast.LENGTH_SHORT).show()
                    MediaPlayer.create(this@DeliveryOrdersProductsActivity, R.raw.validation_sound_effect).apply {
                        start()
                        setOnCompletionListener { it.release() }
                    }
                    startActivity(Intent(this@DeliveryOrdersProductsActivity, DeliveryOrdersActivity::class.java))
                } else {
                    Toast.makeText(this@DeliveryOrdersProductsActivity, "Failed to validate operation.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        packConfirmButton.setOnClickListener {
            val typedBarcode = barcodeInput.text.toString()
            if (typedBarcode.isNotEmpty()) {
                verifyBarcode(typedBarcode)
                barcodeInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter or scan a barcode first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onVerificationStatusChanged(allVerified: Boolean) {
        validateButton.visibility = if (allVerified) View.VISIBLE else View.INVISIBLE
    }

    private fun handlePackageVerificationSuccess(moveLine: MoveLineOutgoing) {
        Log.d("DeliveryOrdersProductsActivity", "Verified package: ${moveLine.resultPackageName}")
        moveLine.resultPackageId?.let { deliveryOrdersProductsAdapter.verifyPackage(it) }
            ?: Log.e("DeliveryOrdersProductsActivity", "Package ID is null, cannot verify package.")
    }

    private fun fetchProductBarcodes(productNames: List<String>): Map<String, String?> = runBlocking {
        productNames.associateWith { productName ->
            withContext(Dispatchers.IO) { odooXmlRpcClient.fetchProductBarcodeByName(productName) }
        }
    }

    private fun fetchMoveLinesForPickingId() = lifecycleScope.launch {
        Log.d("PackProductsActivity", "Fetching move lines for pack ID: $deliveryOrdersId")
        try {
            val fetchedMoveLines = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchMoveLinesByOperationId(deliveryOrdersId)
            }
            Log.d("PackProductsActivity", "Fetched move lines: $fetchedMoveLines")
            Log.d("DeliveryOrdersProductsActivity", "Fetched move lines: ${fetchedMoveLines.map { it.productName + ": " + it.quantity }}")
            updateUIForMoveLines(fetchedMoveLines)
            updateRelevantSerialNumbers(fetchedMoveLines)

            val uniqueProductNames = fetchedMoveLines.map { it.productName }.distinct()
            val barcodes = fetchProductBarcodes(uniqueProductNames)
            Log.d("PackProductsActivity", "Fetched barcodes for all products: ${barcodes.map { "${it.key}: ${it.value}" }.joinToString(", ")}")

        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error fetching move lines for pack: ${e.localizedMessage}")
        }
    }

    private fun updateRelevantSerialNumbers(moveLines: List<MoveLineOutgoing>) {
        serialNumberToMoveLineIdMap.clear()
        moveLines.forEach { moveLine ->
            if (moveLine.lotName.isNotBlank()) {
                serialNumberToMoveLineIdMap[moveLine.lotName] = moveLine.lineId
            }
        }
        Log.d("PackProductsActivity", "Updated serial numbers to move line IDs: ${serialNumberToMoveLineIdMap.entries.joinToString(", ") { "${it.key}: ${it.value}" }}")
    }

    private fun fetchMoveLineIdForSerialNumber(serialNumber: String): Int? {
        return serialNumberToMoveLineIdMap[serialNumber]
    }

    private fun updateUIForMoveLines(moveLines: List<MoveLineOutgoing>) {
        runOnUiThread {
            val groupedByPackage = moveLines.groupBy { it.resultPackageName }.map { (packageName, lines) ->
                PackageSection(packageName, lines.first().resultPackageId, lines.toMutableList())
            }

            val oldSections = deliveryOrdersProductsAdapter.sections
            deliveryOrdersProductsAdapter.sections = groupedByPackage

            // Calculate changes between oldSections and new groupedByPackage
            val oldSize = oldSections.size
            val newSize = groupedByPackage.size

            if (oldSize < newSize) {
                deliveryOrdersProductsAdapter.notifyItemRangeChanged(0, oldSize)
                deliveryOrdersProductsAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
            } else if (oldSize > newSize) {
                deliveryOrdersProductsAdapter.notifyItemRangeChanged(0, newSize)
                deliveryOrdersProductsAdapter.notifyItemRangeRemoved(newSize, oldSize - newSize)
            } else {
                deliveryOrdersProductsAdapter.notifyItemRangeChanged(0, oldSize)
            }
        }
    }


    private fun checkProductTrackingAndHandle(moveLine: MoveLineOutgoing) = lifecycleScope.launch {
        val productName = moveLine.productName
        try {
            val trackingInfo = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductTrackingAndExpirationByName(productName)
            }

            trackingInfo?.let {
                when (it.first) {
                    "none" -> handleNoTracking(moveLine)
                    "serial" -> handleSerialTracking(moveLine)
                    "lot" -> handleLotTracking(moveLine)
                    else -> Log.e("PackProductsActivity", "Unhandled tracking type: ${it.first}")
                }
            } ?: Log.e("PackProductsActivity", "No tracking info found for product: $productName")
        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error checking product tracking: ${e.localizedMessage}", e)
        }
    }

    private fun handleNoTracking(moveLine: MoveLineOutgoing) {
        Log.d("PackProductsActivity", "Handling no tracking for ${moveLine.productName}. Marking as verified.")
        val barcodeIdentifier = moveLine.lineId.toString()

        if (barcodeIdentifier.isNotEmpty()) {
            deliveryOrdersProductsAdapter.addVerifiedBarcode(barcodeIdentifier)
            Log.d("VerifyBarcode", "Marking barcode $barcodeIdentifier as verified without tracking.")
            refreshItemInAdapter(moveLine)
        } else {
            Log.e("PackProductsActivity", "Invalid identifier for moveLine: ${moveLine.productName}")
        }
    }

    private fun handleSerialTracking(moveLine: MoveLineOutgoing) {
        Log.d("PackProductsActivity", "Handling serial number tracking for ${moveLine.productName}.")
        showDialogScanSerialNumber(moveLine)
    }

    private fun handleLotTracking(moveLine: MoveLineOutgoing) {
        Log.d("PackProductsActivity", "Handling lot tracking for ${moveLine.productName}.")
        showDialogScanLotNumber(moveLine)
    }

    private fun showDialogScanSerialNumber(moveLine: MoveLineOutgoing) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_scan_serial, null)
        val serialInput = dialogView.findViewById<EditText>(R.id.serialInput)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.confirmButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        confirmButton.setOnClickListener {
            val enteredSerial = serialInput.text.toString().trim()
            if (enteredSerial.isEmpty()) {
                Toast.makeText(this@DeliveryOrdersProductsActivity, "Please enter a serial number.", Toast.LENGTH_SHORT).show()
                serialInput.requestFocus()
                return@setOnClickListener
            }

            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(this@DeliveryOrdersProductsActivity, "This serial number has already been used. Please enter a different one.", Toast.LENGTH_LONG).show()
                serialInput.requestFocus()
                return@setOnClickListener
            }

            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                usedSerialNumbers.add(enteredSerial)
                deliveryOrdersProductsAdapter.addVerifiedSerialNumber(enteredSerial)
                Log.d("VerifySerial", "Adding serial $enteredSerial to verified list")
                refreshItemInAdapter(moveLine)
                dialog.dismiss()
            } else {
                Toast.makeText(this@DeliveryOrdersProductsActivity, "Invalid serial number entered. Please check and try again.", Toast.LENGTH_LONG).show()
                serialInput.requestFocus()
            }
        }

        dialog.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showDialogScanLotNumber(moveLine: MoveLineOutgoing) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_scan_lot, null)
        val lotInput = dialogView.findViewById<EditText>(R.id.lotInput)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.confirmButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        confirmButton.setOnClickListener {
            val enteredSerial = lotInput.text.toString().trim()
            if (enteredSerial.isEmpty()) {
                Toast.makeText(this@DeliveryOrdersProductsActivity, "Please enter a serial number.", Toast.LENGTH_SHORT).show()
                lotInput.requestFocus()
                return@setOnClickListener
            }

            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(this@DeliveryOrdersProductsActivity, "This serial number has already been used. Please enter a different one.", Toast.LENGTH_LONG).show()
                lotInput.requestFocus()
                return@setOnClickListener
            }

            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                usedSerialNumbers.add(enteredSerial)
                deliveryOrdersProductsAdapter.addVerifiedSerialNumber(enteredSerial)
                Log.d("VerifySerial", "Adding serial $enteredSerial to verified list")
                refreshItemInAdapter(moveLine)
                dialog.dismiss()
            } else {
                Toast.makeText(this@DeliveryOrdersProductsActivity, "Invalid serial number entered. Please check and try again.", Toast.LENGTH_LONG).show()
                lotInput.requestFocus()
            }
        }

        dialog.show()
    }

    private fun refreshItemInAdapter(moveLine: MoveLineOutgoing) {
        var cumulativePosition = 0
        var lineFound = false

        // First pass: Try to find and update the existing item
        deliveryOrdersProductsAdapter.sections.forEachIndexed { sectionIndex, section ->
            val lineIndex = section.moveLines.indexOf(moveLine)
            if (lineIndex != -1) {
                val positionInAdapter = cumulativePosition + lineIndex + sectionIndex // Add sectionIndex to account for header positions
                deliveryOrdersProductsAdapter.notifyItemChanged(positionInAdapter)
                Log.d("AdapterUpdate", "Updated item at adapter position $positionInAdapter as verified.")
                lineFound = true
                return@forEachIndexed
            }
            cumulativePosition += section.moveLines.size + 1 // +1 for the section header
        }

        // If not found, assume the move line might have been added
        if (!lineFound) {
            Log.d("AdapterUpdate", "Move line not found. Assuming data change and refreshing specific changes.")
            cumulativePosition = 0

            deliveryOrdersProductsAdapter.sections.forEachIndexed { sectionIndex, section ->
                val lineIndex = section.moveLines.indexOf(moveLine)
                if (lineIndex != -1) {
                    val positionInAdapter = cumulativePosition + lineIndex + sectionIndex // Add sectionIndex to account for header positions
                    deliveryOrdersProductsAdapter.notifyItemInserted(positionInAdapter)
                    Log.d("AdapterUpdate", "Inserted item at adapter position $positionInAdapter.")
                    lineFound = true
                    return@forEachIndexed
                }
                cumulativePosition += section.moveLines.size + 1 // +1 for the section header
            }
        }

        // If the item is still not found, this implies a significant change in the data structure
        if (!lineFound) {
            Log.d("AdapterUpdate", "Move line still not found. Data structure might have changed significantly.")
            // Handle this case as needed by higher-level logic.
            // Avoiding `notifyDataSetChanged` and assuming higher-level logic handles it.
        }
    }

    //================================================================================================================
    //                                                  FlAG CODE
    //                  Test whether camera opens on another device if it doesn't work on yours
    //================================================================================================================
    private suspend fun sendEmailToBuyer(buyerEmail: String, buyerName: String, packName: String?) {
        withContext(Dispatchers.IO) {
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
                    subject = "Action Required: Discrepancy in Received Quantity for Receipt $packName"
                    setText("""
                    Dear $buyerName,

                    During a recent receipt event, we identified a discrepancy in the quantities received for the following item:

                    - Pack Name: $packName

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
    }




    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        when (item.itemId) {
            android.R.id.home -> {
                registerBackPressHandler()
                vibrateDevice(vibrator)
                return true
            }
            R.id.action_flag -> {
                showFlagDialog()
                vibrateDevice(vibrator)
                return true
            }
            R.id.action_print -> {
                val subMenu = item.subMenu
                if (subMenu == null || subMenu.size() == 0) {
                    Toast.makeText(this, "There Is Nothing To Print Right Now", Toast.LENGTH_LONG).show()
                    vibrateDevice(vibrator)
                    return true
                } else {
                    vibrateDevice(vibrator)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun vibrateDevice(vibrator: Vibrator?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    private fun showFlagDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, findViewById(android.R.id.content), false)
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(false)
        }
        val dialog = dialogBuilder.create()

        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            vibrateDevice(vibrator)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnFlagPick).setOnClickListener {
            vibrateDevice(vibrator)
            lifecycleScope.launch {
                try {
                    val pickId = this@DeliveryOrdersProductsActivity.deliveryOrdersId
                    val pickName = this@DeliveryOrdersProductsActivity.deliveryOrdersName

                    val buyerDetails = withContext(Dispatchers.IO) {
                        odooXmlRpcClient.fetchAndLogBuyerDetails(pickName)
                    }

                    if (buyerDetails != null) {
                        Log.d("PackProductsActivity", "Starting image capture for packId: $pickId")
                        captureImage(pickId)
                        sendEmailToBuyer(buyerDetails.login, buyerDetails.name, pickName)
                        Log.d("PackProductsActivity", "Pack flagged and buyer notified via email.")
                        Toast.makeText(this@DeliveryOrdersProductsActivity, "Pack flagged", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("PackProductsActivity", "Failed to fetch buyer details or flag the pack.")
                        Toast.makeText(this@DeliveryOrdersProductsActivity, "Failed to flag pack", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("PackProductsActivity", "Error in flagging process: ${e.localizedMessage}", e)
                    Toast.makeText(this@DeliveryOrdersProductsActivity, "Error during flagging", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        dialog.show()
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


    /*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageBitmap = data?.parcelable<Bitmap>("data")
            if (imageBitmap != null) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

                Log.d("CaptureImage", "Encoded image: $encodedImage")

                lifecycleScope.launch {
                    try {
                        odooXmlRpcClient.updatePickingImage(deliveryOrdersId, encodedImage)
                        Log.d("OdooUpdate", "Image updated successfully on server")
                    } catch (e: Exception) {
                        Log.e("OdooUpdate", "Failed to update image: ${e.localizedMessage}", e)
                    }
                }
            } else {
                Log.e("CaptureImage", "Failed to capture image")
            }
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode != RESULT_OK) {
            Log.e("CaptureImage", "Camera action was cancelled or failed")
            Toast.makeText(this, "Camera action was cancelled or failed.", Toast.LENGTH_SHORT).show()
        }
    }
    */

    /*
    suspend fun updatePickingImage(packId: Int, encodedImage: String) {
        withContext(Dispatchers.IO) {
            try {
                // Your existing code to update the image on the server
                val client = XmlRpcClient() // Assuming XmlRpcClient is initialized correctly
                val params = listOf(packId, encodedImage)
                client.execute("updatePickingImage", params)
            } catch (e: Exception) {
                Log.e("OdooXmlRpcClient", "Error updating image for picking ID $packId: ${e.localizedMessage}", e)
                throw e
            }
        }
    }

    */


    companion object {
        private const val CAMERA_REQUEST_CODE = 1001
    }


    private inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }


    private fun registerBackPressHandler() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this) {
                finish()
            }
        }
    }
}
