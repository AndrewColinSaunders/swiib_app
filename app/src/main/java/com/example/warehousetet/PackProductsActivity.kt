package com.example.warehousetet

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.print.PrintHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import org.apache.xmlrpc.XmlRpcException
import java.io.File
import java.io.FileOutputStream
import com.google.zxing.common.BitMatrix
import com.google.zxing.MultiFormatWriter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import java.io.ByteArrayOutputStream
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
import android.Manifest
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Parcelable
import android.os.VibrationEffect
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback




class PackProductsActivity : AppCompatActivity(), PackProductsAdapter.VerificationListener {
    private lateinit var packProductsAdapter: PackProductsAdapter
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private val packagedMoveLines = mutableListOf<PackagedMovedLine>()
    private lateinit var barcodeInput: EditText
    private var shouldShowPrinterIcon = false
    private var lastScannedBarcode = StringBuilder()
    private var lastKeyTime: Long = 0
    private var isScannerInput = false
    private val usedSerialNumbers = mutableSetOf<String>()
    private var serialNumberToMoveLineIdMap = mutableMapOf<String, Int>()
    private val packId by lazy { intent.getIntExtra("PACK_ID", -1) }
    private val packName by lazy {intent.getStringExtra("PACK_NAME")}
    private lateinit var validateButton: Button
    private var classLevelMenu: Menu? = null





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pack_activity_products)


        registerBackPressHandler()


        Log.d("PackProductsActivity", "Activity created with pack ID: $packId")

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        initializeUI()
        loadVerificationState()
        loadPackagedIds()
        if (packId != -1) {
            fetchMoveLinesForPickingId()
        } else {
            Log.e("PackProductsActivity", "Invalid pack ID passed to PackProductsActivity.")
        }
    }



    //============================================================================================================
    //                                          Flag and printer icon code
    //============================================================================================================
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pack_products, menu)
        classLevelMenu = menu  // Store the menu reference

        val colorWhite = ContextCompat.getColor(this, android.R.color.white)
        val colorDangerRed = ContextCompat.getColor(this, R.color.danger_red)
        val mode = PorterDuff.Mode.SRC_ATOP

        val printItem = menu?.findItem(R.id.action_print)
        val flagItem = menu?.findItem(R.id.action_flag)

        printItem?.icon?.let {
            val coloredDrawable = it.mutate().apply {
                colorFilter = PorterDuffColorFilter(colorWhite, mode)
            }
            printItem.icon = coloredDrawable
        }

        flagItem?.icon?.let {
            val coloredDrawable = it.mutate().apply {
                colorFilter = PorterDuffColorFilter(colorDangerRed, mode)
            }
            flagItem.icon = coloredDrawable
        }

        return true
    }



    override fun onVerificationStatusChanged(allVerified: Boolean) {
        runOnUiThread {
            validateButton.visibility = if (allVerified) View.VISIBLE else View.GONE
            saveVerificationState(allVerified)
        }
    }


    private fun initializeUI() {
        val packName = intent.getStringExtra("PACK_NAME") ?: "Pack"
        supportActionBar?.title = packName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        validateButton = findViewById<Button>(R.id.validateOperationButton)
        validateButton.visibility = View.GONE
        barcodeInput = findViewById(R.id.packBarcodeInput)
        val packConfirmButton = findViewById<Button>(R.id.packConfirmButton)
        val clearButton = findViewById<Button>(R.id.packClearButton)

        shouldShowPrinterIcon = false

        packProductsAdapter = PackProductsAdapter(emptyList(), packId, packagedMoveLines, this)
        findViewById<RecyclerView>(R.id.packProductsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@PackProductsActivity)
            adapter = packProductsAdapter
        }



        //============================================================================================================
        //                                          Validate Operation
        //============================================================================================================
        validateButton.setOnClickListener {
            // Log the validation attempt
            Log.d("ValidationAttempt", "Attempting to validate operation for pack ID: $packId")

            // Get the Vibrator service from the system
            val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }

            lifecycleScope.launch {
                if (odooXmlRpcClient.validateOperation(packId, this@PackProductsActivity)) {
                    Toast.makeText(this@PackProductsActivity, "Operation validated successfully!", Toast.LENGTH_SHORT).show()

                    //============================================================================================================
                    //                              Plays the sound for the validation button
                    //                        NB!!!! INCLUDE IN EVERY ACTIVITY FOR VALIDATION BUTTON NB!!!!
                    //============================================================================================================
                    MediaPlayer.create(this@PackProductsActivity, R.raw.validation_sound_effect).apply {
                        start() // Start playing the sound
                        setOnCompletionListener {
                            it.release() // Release the MediaPlayer once the sound is done playing
                        }
                    }
                    //============================================================================================================

                    // Navigate to PackActivity if validation is successful
                    val intent = Intent(this@PackProductsActivity, PackActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@PackProductsActivity, "Failed to validate operation.", Toast.LENGTH_SHORT).show()
                }
            }
        }



        //============================================================================================================
        //                       Confirm button for when the user has to type in barcode
        //============================================================================================================
        packConfirmButton.setOnClickListener {
            // Get the Vibrator service from the system
            val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }

            val typedBarcode = barcodeInput.text.toString()
            if (typedBarcode.isNotEmpty()) {
                // Logging the barcode to Logcat
                Log.d("PackProductsActivity", "Scanned barcode: $typedBarcode")

                verifyProductBarcode(typedBarcode)  // Process the scanned barcode
                barcodeInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter or scan a barcode first.", Toast.LENGTH_SHORT).show()
            }
        }


        //============================================================================================================
        //                       Clear button to clear the EditView of text
        //============================================================================================================
        clearButton.setOnClickListener {
            // Get the Vibrator service from the system
            val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }

            barcodeInput.text.clear() // Clear the text in the barcode input EditText
        }
    }



    //============================================================================================================
    //                                          Fetch Products Barcode
    //============================================================================================================
    private fun fetchProductBarcodes(productNames: List<String>): Map<String, String?> = runBlocking {
        productNames.associateWith { productName ->
            withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductBarcodeByName(productName)
            }
        }
    }


    //============================================================================================================
    //                                          Fetch Movelines
    //============================================================================================================
    private fun fetchMoveLinesForPickingId() = lifecycleScope.launch {
        Log.d("PackProductsActivity", "Fetching move lines for pack ID: $packId")
        try {
            val fetchedMoveLines = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchMoveLinesByOperationId(packId)
            }
            Log.d("PackProductsActivity", "Fetched move lines: $fetchedMoveLines")
            Log.d("DeliveryOrdersProductsActivity1234556", "Fetched move lines: ${fetchedMoveLines.map { it.productName + ": " + it.quantity }}")
            updateUIForMoveLines(fetchedMoveLines)
            updateRelevantSerialNumbers(fetchedMoveLines)


            // Extract unique product names
            val uniqueProductNames = fetchedMoveLines.map { it.productName }.distinct()

            // Fetch barcodes for all unique products
            val barcodes = fetchProductBarcodes(uniqueProductNames)

            // Log fetched barcodes for all products
            Log.d("PackProductsActivity", "Fetched barcodes for all products: ${barcodes.map { "${it.key}: ${it.value}" }.joinToString(", ")}")

        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error fetching move lines for pack: ${e.localizedMessage}")
        }
    }




    //============================================================================================================
    //                                          Lot and Serial Number Code
    //============================================================================================================
    private fun updateRelevantSerialNumbers(moveLines: List<MoveLine>) {
        serialNumberToMoveLineIdMap.clear()
        moveLines.forEach { moveLine ->
            if (moveLine.lotName.isNotBlank()) {
                // Map each serial number to its move line ID
                serialNumberToMoveLineIdMap[moveLine.lotName] = moveLine.lineId
            }
        }
        Log.d("PackProductsActivity", "Updated serial numbers to move line IDs: ${serialNumberToMoveLineIdMap.entries.joinToString(", ") { "${it.key}: ${it.value}" }}")
    }

    // Function to fetch the move line ID based on the serial number
    private fun fetchMoveLineIdForSerialNumber(serialNumber: String): Int? {
        return serialNumberToMoveLineIdMap[serialNumber]
    }


    private fun updateUIForMoveLines(moveLines: List<MoveLine>) {
        runOnUiThread {
            packProductsAdapter.moveLines = moveLines
            packProductsAdapter.notifyDataSetChanged()
        }
    }



    //============================================================================================================
    //                                          Scanner Code
    //============================================================================================================
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (isScannerInput) {
                    barcodeInput.setText(lastScannedBarcode.toString())
                    verifyProductBarcode(lastScannedBarcode.toString())
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



    //============================================================================================================
    //                                          Verify Barcode
    //============================================================================================================
    private fun verifyProductBarcode(scannedBarcode: String) = lifecycleScope.launch {
        val matchingMoveLine = packProductsAdapter.moveLines.find { moveLine ->
            val expectedBarcode = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductBarcodeByName(moveLine.productName)
            }
            expectedBarcode == scannedBarcode
        }

        if (matchingMoveLine != null) {
            Log.d("PackProductsActivity", "Barcode verification successful for product: ${matchingMoveLine.productName}")
            checkProductTrackingAndHandle(matchingMoveLine)
            barcodeInput.text.clear()
        } else {
            Toast.makeText(this@PackProductsActivity, "Barcode does not match any product.", Toast.LENGTH_SHORT).show()
            Log.e("PackProductsActivity", "Barcode verification failed. No matching product found.")
            barcodeInput.selectAll()
        }
    }



//    private fun handleVerificationFailure(productName: String?, scannedBarcode: String, expectedBarcode: String?) {
//        Toast.makeText(this, "Barcode mismatch for $productName. Expected: $expectedBarcode, Found: $scannedBarcode", Toast.LENGTH_LONG).show()
//        barcodeInput.selectAll() // Select all text in EditText to facilitate correction
//    }




    //============================================================================================================
    //                                          Handle Tracking Types
    //============================================================================================================
    private fun checkProductTrackingAndHandle(moveLine: MoveLine) = lifecycleScope.launch {
        val productName = moveLine.productName
        try {
            val trackingInfo = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductTrackingAndExpirationByName(productName)
            }

            trackingInfo?.let {
                when (it.first) {
                    "none" -> handleNoTracking(classLevelMenu, moveLine)  // Use the stored menu here
                    "serial" -> handleSerialTracking(classLevelMenu, moveLine)
                    "lot" -> handleLotTracking(classLevelMenu, moveLine)
                    else -> Log.e("PackProductsActivity", "Unhandled tracking type: ${it.first}")
                }
            } ?: Log.e("PackProductsActivity", "No tracking info found for product: $productName")
        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error checking product tracking: ${e.localizedMessage}", e)
        }
    }


    private fun handleNoTracking(menu: Menu?, moveLine: MoveLine) {
        Log.d("PackProductsActivity", "Handling no tracking for ${moveLine.productName}.")

        // Check for the presence of submenus
        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
        if (subMenu != null && subMenu.size() > 0) {
            // Submenus exist, display the quantity dialog
            displayQuantityDialog(moveLine.productName, moveLine.quantity, packId, moveLine.lineId, moveLine)
        } else {
            // No submenus, log the message to inflate the dialog_prepackage_lot.xml
            showPrePackageDialogNoTracking(moveLine)
        }
    }


    private fun handleSerialTracking(menu: Menu?,moveLine: MoveLine) {
        Log.d("PackProductsActivity", "Handling serial number tracking for ${moveLine.productName}.")
        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
        if (subMenu != null && subMenu.size() > 0) {
            showPackageDialogSerial(moveLine)
        } else {
            showPrePackageDialogSerialTracking(moveLine)
        }

    }

    private fun handleLotTracking(menu: Menu?, moveLine: MoveLine) {
        Log.d("PackProductsActivity", "Handling lot tracking for ${moveLine.productName}.")
        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
        if (subMenu != null && subMenu.size() > 0) {
            showPackageDialogLot(moveLine)
        } else {
            showPrePackageDialogLotTracking(moveLine)
        }
    }


    //============================================================================================================
    //                                          Dialog Code Pre-packaged (No Tracking)
    //============================================================================================================
    private fun showPrePackageDialogNoTracking(moveLine: MoveLine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_prepackage_no_tracking, null)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        createNewButton.apply {
            text = "Create New"
            setBackgroundColor(ContextCompat.getColor(context, R.color.success_green))
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                Log.d("PackageDialog", "Attempting to put item in new package with MoveLine ID: ${moveLine.lineId}")
                lifecycleScope.launch {
                    try {
                        Log.d("Check what moveline is being parsed", "moveLine ID: $moveLine")
                        val result = odooXmlRpcClient.putMoveLineInNewPack(moveLine.lineId, this@PackProductsActivity)
                        if (result) {

                            Log.d("PackageDialog", "Successfully put item in new package.")
                            Toast.makeText(context, "Item successfully put into a new package.", Toast.LENGTH_SHORT).show()
                            packagedMoveLines.add(PackagedMovedLine(moveLine.lineId))
                            val index = packProductsAdapter.moveLines.indexOf(moveLine)
                            savePackagedIds()
                            packProductsAdapter.notifyItemChanged(index)
                            checkAllItemsPackaged()// Notify adapter to update this item
                            refreshMenu()
                        } else {
                            Toast.makeText(context, "Failed to put item in new package.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: XmlRpcException) {
                        val message = when {
                            e.message?.contains("AlreadyPacked") == true -> "Cannot put item in package as it is already in another package."
                            else -> "Failed to process the request due to a system error."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        Log.e("PackageDialog", "Error putting item in new package: ${e.message}")
                    }
                    dialog.dismiss()
                }
            }
        }

        notPackagedButton.apply {
            text = "Not Packaged"
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLine.lineId}")

                // Simulate adding a packaged move line
                val packagedMovedLine = PackagedMovedLine(moveLine.lineId)
                packagedMoveLines.add(packagedMovedLine)

                // Update the UI
                val index = packProductsAdapter.moveLines.indexOf(moveLine)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }
        dialog.show()
    }




    //============================================================================================================
    //                                          Dialog Code (No Tracking)
    //============================================================================================================
    private fun showPackageDialogNoTracking(moveLine: MoveLine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package_no_tracking, null)
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val addToPackageButton = dialogView.findViewById<MaterialButton>(R.id.addToPackageButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)



        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        createNewButton.apply {
            text = "Create New"
            setBackgroundColor(ContextCompat.getColor(context, R.color.success_green))
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                Log.d("PackageDialog", "Attempting to put item in new package with MoveLine ID: ${moveLine.lineId}")
                lifecycleScope.launch {
                    try {
                        Log.d("Check what moveline is being parsed", "moveLine ID: $moveLine")
                        val result = odooXmlRpcClient.putMoveLineInNewPack(moveLine.lineId, this@PackProductsActivity)
                        if (result) {
                            Log.d("PackageDialog", "Successfully put item in new package.")
                            Toast.makeText(context, "Item successfully put into a new package.", Toast.LENGTH_SHORT).show()
                            packagedMoveLines.add(PackagedMovedLine(moveLine.lineId))
                            val index = packProductsAdapter.moveLines.indexOf(moveLine)
                            savePackagedIds()
                            packProductsAdapter.notifyItemChanged(index)
                            checkAllItemsPackaged()// Notify adapter to update this item
                            refreshMenu()
                        } else {
                            Toast.makeText(context, "Failed to put item in new package.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: XmlRpcException) {
                        val message = when {
                            e.message?.contains("AlreadyPacked") == true -> "Cannot put item in package as it is already in another package."
                            else -> "Failed to process the request due to a system error."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        Log.e("PackageDialog", "Error putting item in new package: ${e.message}")
                    }
                    dialog.dismiss()
                }
            }
        }


        addToPackageButton.setOnClickListener {
            val packageName = packageInput.text.toString()
            if (packageName.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.setPackageForMoveLine(packId, moveLine.lineId, packageName)
                        withContext(Dispatchers.Main) {
                            if (result) {
                                // Add this line to the list of packaged move lines if not already present
                                if (!packagedMoveLines.any { it.moveLineId == moveLine.lineId }) {
                                    packagedMoveLines.add(PackagedMovedLine(moveLine.lineId))
                                    packageItem(moveLine.lineId) // Update the adapter
                                    checkAllItemsPackaged() // Check if all items are packaged
                                    refreshMenu() // Refresh the menu
                                    //isPrintVisible = true // Set print visibility
                                }
                                // Find the index and notify the adapter
                                val index = packProductsAdapter.moveLines.indexOfFirst { it.lineId == moveLine.lineId }
                                if (index != -1) {
                                    packProductsAdapter.notifyItemChanged(index) // Notify adapter to update this item
                                }
                                addToPackageButton.setBackgroundColor(ContextCompat.getColor(this@PackProductsActivity, R.color.success_green))
                                Toast.makeText(this@PackProductsActivity, "Package set successfully.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this@PackProductsActivity, "Failed to set package for move line.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PackageDialog", "Error occurred: ${e.localizedMessage}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PackProductsActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this@PackProductsActivity, "Please enter a package name.", Toast.LENGTH_SHORT).show()
            }
        }



        notPackagedButton.apply {
            text = "Not Packaged"
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLine.lineId}")

                // Simulate adding a packaged move line
                val packagedMovedLine = PackagedMovedLine(moveLine.lineId)
                packagedMoveLines.add(packagedMovedLine)

                // Update the UI
                val index = packProductsAdapter.moveLines.indexOf(moveLine)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        dialog.show()
    }


    //============================================================================================================
    //                                   Dialog Code Pre-packaged (Serialised)
    //============================================================================================================
    private fun showPrePackageDialogSerialTracking(moveLine: MoveLine) {
        // Inflate your custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_prepackage_serial, null)
        val serialInput = dialogView.findViewById<EditText>(R.id.serialInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)


        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set up the "Create New" button
        createNewButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
            val enteredSerial = serialInput.text.toString().trim()

            // Check if the serial number has already been used
            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "This serial number has already been used. Please enter a different one.",
                    Toast.LENGTH_LONG
                ).show()
                serialInput.requestFocus() // Focus back to the input for correction
                return@setOnClickListener
            }

            // Attempt to get the moveLineId using the entered serial number
            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.putMoveLineInNewPack(
                            moveLineId,
                            this@PackProductsActivity
                        )
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Item successfully put into a new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                packageItem(moveLineId)  // Update the adapter
                                checkAllItemsPackaged()
                                //isPrintVisible = true
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)  // Track the used serial numbers
                                odooXmlRpcClient.updateMoveLineQuantityForReceipt(packId, moveLine.lineId, 1)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to put item in new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Error during packaging: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Invalid serial number entered. Please check and try again.",
                    Toast.LENGTH_LONG
                ).show()
                serialInput.requestFocus()  // Focus back to the input for correction
            }
        }

        // Set up the "Not Packaged" button
        notPackagedButton.apply {
            text = "Not Packaged"
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLine.lineId}")

                // Simulate adding a packaged move line
                val packagedMovedLine = PackagedMovedLine(moveLine.lineId)
                packagedMoveLines.add(packagedMovedLine)

                // Update the UI
                val index = packProductsAdapter.moveLines.indexOf(moveLine)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        // Show the dialog
        dialog.show()
    }


    //============================================================================================================
    //                                   Dialog Code (Serialised)
    //============================================================================================================
    private fun showPackageDialogSerial(moveLine: MoveLine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package_serial, null)
        val serialInput = dialogView.findViewById<EditText>(R.id.serialInput)  // Ensure correct ID
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val addToPackageButton = dialogView.findViewById<MaterialButton>(R.id.addToPackageButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)


        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        createNewButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
            val enteredSerial = serialInput.text.toString().trim()

            // Check if the serial number has already been used
            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "This serial number has already been used. Please enter a different one.",
                    Toast.LENGTH_LONG
                ).show()
                serialInput.requestFocus() // Focus back to the input for correction
                return@setOnClickListener
            }

            // Attempt to get the moveLineId using the entered serial number
            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.putMoveLineInNewPack(
                            moveLineId,
                            this@PackProductsActivity
                        )
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Item successfully put into a new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                packageItem(moveLineId)  // Update the adapter
                                checkAllItemsPackaged()
                                //isPrintVisible = true
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)  // Track the used serial numbers
                                odooXmlRpcClient.updateMoveLineQuantityForReceipt(packId, moveLine.lineId, 1)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to put item in new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Error during packaging: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Invalid serial number entered. Please check and try again.",
                    Toast.LENGTH_LONG
                ).show()
                serialInput.requestFocus()  // Focus back to the input for correction
            }
        }


        addToPackageButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
            val packageName = packageInput.text.toString().trim()
            val enteredSerial = serialInput.text.toString().trim()

            if (packageName.isNotEmpty() && enteredSerial.isNotEmpty()) {
                // Check if the serial number has already been used
                if (usedSerialNumbers.contains(enteredSerial)) {
                    Toast.makeText(
                        this@PackProductsActivity,
                        "This serial number has already been used. Please enter a different one.",
                        Toast.LENGTH_LONG
                    ).show()
                    serialInput.requestFocus() // Focus back to the input for correction
                    return@setOnClickListener
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        // Use the serial number to fetch the move line ID
                        val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
                        if (moveLineId != null) {
                            val result = withContext(Dispatchers.IO) {
                                odooXmlRpcClient.setPackageForMoveLine(
                                    packId,
                                    moveLineId,
                                    packageName
                                )
                            }
                            if (result) {
                                // Update the UI and internal state as needed
                                packageItem(moveLineId)  // Update the adapter
                                checkAllItemsPackaged()
                                //isPrintVisible = true
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)  // Track the used serial numbers

                                addToPackageButton.setBackgroundColor(
                                    ContextCompat.getColor(
                                        this@PackProductsActivity,
                                        R.color.success_green
                                    )
                                )
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Package set successfully for move line ID: $moveLineId.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to set package for move line ID: $moveLineId.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Invalid serial number entered. Please check and try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            serialInput.requestFocus()  // Focus back to the input for correction
                        }
                    } catch (e: Exception) {
                        Log.e("PackageDialog", "Error occurred: ${e.localizedMessage}")
                        Toast.makeText(
                            this@PackProductsActivity,
                            "An error occurred",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Please enter a package name and serial number.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        notPackagedButton.apply {
            text = "Not Packaged"
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLine.lineId}")

                // Simulate adding a packaged move line
                val packagedMovedLine = PackagedMovedLine(moveLine.lineId)
                packagedMoveLines.add(packagedMovedLine)

                // Update the UI
                val index = packProductsAdapter.moveLines.indexOf(moveLine)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        dialog.show()
    }


    //============================================================================================================
    //                                   Dialog Code Pre-Packaged (Lot)
    //============================================================================================================
    private fun showPrePackageDialogLotTracking(moveLine: MoveLine) {
        // Inflate your custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_prepackage_lot, null)
        val lotInput = dialogView.findViewById<EditText>(R.id.lotInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)


        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set up the "Create New" button
        createNewButton.setOnClickListener {
            val enteredSerial = lotInput.text.toString().trim()

            // Check if the serial number has already been used
            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "This serial number has already been used. Please enter a different one.",
                    Toast.LENGTH_LONG
                ).show()
                lotInput.requestFocus() // Focus back to the input for correction
                return@setOnClickListener
            }

            // Attempt to get the moveLineId using the entered serial number
            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.putMoveLineInNewPack(
                            moveLineId,
                            this@PackProductsActivity
                        )
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Item successfully put into a new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                packageItem(moveLineId)  // Update the adapter
                                checkAllItemsPackaged()
                                //isPrintVisible = true
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)  // Track the used serial numbers
                                odooXmlRpcClient.updateMoveLineQuantityForReceipt(packId, moveLine.lineId, 1)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to put item in new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Error during packaging: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Invalid serial number entered. Please check and try again.",
                    Toast.LENGTH_LONG
                ).show()
                lotInput.requestFocus()  // Focus back to the input for correction
            }
        }

        // Set up the "Not Packaged" button
        notPackagedButton.apply {
            text = "Not Packaged"
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLine.lineId}")

                // Simulate adding a packaged move line
                val packagedMovedLine = PackagedMovedLine(moveLine.lineId)
                packagedMoveLines.add(packagedMovedLine)

                // Update the UI
                val index = packProductsAdapter.moveLines.indexOf(moveLine)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        // Show the dialog
        dialog.show()
    }



    //============================================================================================================
    //                                   Dialog Code (Lot)
    //============================================================================================================
    private fun showPackageDialogLot(moveLine: MoveLine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package_lot, null)
        val lotInput = dialogView.findViewById<EditText>(R.id.lotInput)  // Ensure correct ID
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val addToPackageButton = dialogView.findViewById<MaterialButton>(R.id.addToPackageButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        createNewButton.setOnClickListener {
            val enteredSerial = lotInput.text.toString().trim()

            // Check if the serial number has already been used
            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "This serial number has already been used. Please enter a different one.",
                    Toast.LENGTH_LONG
                ).show()
                lotInput.requestFocus() // Focus back to the input for correction
                return@setOnClickListener
            }

            // Attempt to get the moveLineId using the entered serial number
            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.putMoveLineInNewPack(
                            moveLineId,
                            this@PackProductsActivity
                        )
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Item successfully put into a new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                packageItem(moveLineId)  // Update the adapter
                                checkAllItemsPackaged()
                                //isPrintVisible = true
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)  // Track the used serial numbers
                                odooXmlRpcClient.updateMoveLineQuantityForReceipt(packId, moveLine.lineId, 1)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to put item in new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Error during packaging: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Invalid serial number entered. Please check and try again.",
                    Toast.LENGTH_LONG
                ).show()
                lotInput.requestFocus()  // Focus back to the input for correction
            }
        }



        addToPackageButton.setOnClickListener {
            val packageName = packageInput.text.toString().trim()
            val enteredSerial = lotInput.text.toString().trim()

            if (packageName.isNotEmpty() && enteredSerial.isNotEmpty()) {
                // Check if the serial number has already been used
                if (usedSerialNumbers.contains(enteredSerial)) {
                    Toast.makeText(
                        this@PackProductsActivity,
                        "This serial number has already been used. Please enter a different one.",
                        Toast.LENGTH_LONG
                    ).show()
                    lotInput.requestFocus() // Focus back to the input for correction
                    return@setOnClickListener
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        // Use the serial number to fetch the move line ID
                        val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
                        if (moveLineId != null) {
                            val result = withContext(Dispatchers.IO) {
                                odooXmlRpcClient.setPackageForMoveLine(
                                    packId,
                                    moveLineId,
                                    packageName
                                )
                            }
                            if (result) {
                                // Update the UI and internal state as needed
                                packageItem(moveLineId)  // Update the adapter
                                checkAllItemsPackaged()
                                //isPrintVisible = true
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)  // Track the used serial numbers

                                addToPackageButton.setBackgroundColor(
                                    ContextCompat.getColor(
                                        this@PackProductsActivity,
                                        R.color.success_green
                                    )
                                )
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Package set successfully for move line ID: $moveLineId.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to set package for move line ID: $moveLineId.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Invalid serial number entered. Please check and try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            lotInput.requestFocus()  // Focus back to the input for correction
                        }
                    } catch (e: Exception) {
                        Log.e("PackageDialog", "Error occurred: ${e.localizedMessage}")
                        Toast.makeText(
                            this@PackProductsActivity,
                            "An error occurred",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Please enter a package name and serial number.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        notPackagedButton.apply {
            text = "Not Packaged"
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLine.lineId}")

                // Simulate adding a packaged move line
                val packagedMovedLine = PackagedMovedLine(moveLine.lineId)
                packagedMoveLines.add(packagedMovedLine)

                // Update the UI
                val index = packProductsAdapter.moveLines.indexOf(moveLine)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        dialog.show()
    }







    //============================================================================================================
    //                               Save the state of the activity code
    //============================================================================================================

    private fun savePackagedIds() {
        val editor = getSharedPreferences("PackPrefs", MODE_PRIVATE).edit()
        val packagedIds = packagedMoveLines.map { it.moveLineId }.joinToString(",")
        editor.putString("packagedIds", packagedIds)
        editor.apply()
    }

    private fun loadPackagedIds() {
        val prefs = getSharedPreferences("PackPrefs", MODE_PRIVATE)
        val packagedIds = prefs.getString("packagedIds", "") ?: ""
        if (packagedIds.isNotEmpty()) {
            packagedMoveLines.clear()
            packagedMoveLines.addAll(packagedIds.split(",").map { PackagedMovedLine(it.toInt()) })
            updateUIForMoveLines(packProductsAdapter.moveLines) // Refresh UI after loading state
        }
    }


    private fun checkAllItemsPackaged() {
        val allPackaged = packProductsAdapter.moveLines.all { moveLine ->
            packagedMoveLines.any { it.moveLineId == moveLine.lineId }
        }
        findViewById<Button>(R.id.validateOperationButton).visibility = if (allPackaged) View.VISIBLE else View.GONE
        savePackagedIds() // This call can also be removed if no other state needs to be saved when checking all items
    }

    private fun packageItem(moveLineId: Int) {
        val newPackagedMovedLine = PackagedMovedLine(moveLineId)
        packProductsAdapter.addPackagedMoveLine(newPackagedMovedLine)
        savePackagedIds()  // Save state whenever an item's packaged status changes
    }



    private fun saveVerificationState(isAllVerified: Boolean) {
        getSharedPreferences("PackProductPrefs_$packId", Context.MODE_PRIVATE).edit().apply {
            putBoolean("allVerified", isAllVerified)
            apply()
        }
    }


    private fun loadVerificationState() {
        val prefs = getSharedPreferences("PackProductPrefs_$packId", Context.MODE_PRIVATE)
        val allVerified = prefs.getBoolean("allVerified", false)
        validateButton.visibility = if (allVerified) View.VISIBLE else View.GONE
    }




    private fun refreshMenu() {
        invalidateOptionsMenu()
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                return true
            }
            R.id.action_flag -> {
                showFlagDialog()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Vibrate for 50 milliseconds on pre-Oreo devices
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
                return true
            }
            R.id.action_print -> {
                val subMenu = item.subMenu
                if (subMenu == null || subMenu.size() == 0) {
                    Toast.makeText(this, "There Is Nothing To Print Right Now", Toast.LENGTH_LONG).show()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        // Vibrate for 50 milliseconds on pre-Oreo devices
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(50)
                    }
                    return true
                } else {
                    // Add vibration on opening the submenu
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        // Vibrate for 50 milliseconds on pre-Oreo devices
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(50)
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }




    private fun showFlagDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, null)
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(true)  // Prevent dialog from being dismissed by back press or outside touches
        }
        val dialog = dialogBuilder.create()

        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)


        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
            dialog.dismiss()  // Dismiss the dialog when "Cancel" is clicked
        }

        dialogView.findViewById<Button>(R.id.btnFlagPick).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
            lifecycleScope.launch {
                try {
                    val pickId = this@PackProductsActivity.packId
                    val pickName = this@PackProductsActivity.packName ?: run {
                        Log.e("PackProductsActivity", "Pack name is null")
                        Toast.makeText(this@PackProductsActivity, "Invalid pack details", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val buyerDetails = withContext(Dispatchers.IO) {
                        odooXmlRpcClient.fetchAndLogBuyerDetails(pickName)
                    }

                    if (buyerDetails != null) {
                        // Capture image before sending the email
                        captureImage(packId)

                        // Ensure email is sent after image capture dialog completion
                        sendEmailToBuyer(buyerDetails.login, buyerDetails.name, packName)
                        Log.d("PackProductsActivity", "Pack flagged and buyer notified via email.")
                        Toast.makeText(this@PackProductsActivity, "Pack flagged", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("PackProductsActivity", "Failed to fetch buyer details or flag the pack.")
                        Toast.makeText(this@PackProductsActivity, "Failed to flag pack", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("PackProductsActivity", "Error in flagging process: ${e.localizedMessage}", e)
                    Toast.makeText(this@PackProductsActivity, "Error during flagging", Toast.LENGTH_SHORT).show()
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
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)


        // No button - just dismiss the dialog
        builder.setNegativeButton("No") { dialog, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
            dialog.dismiss()
        }

        // Capture Image button - open the camera
        builder.setPositiveButton("Capture Image") { dialog, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 50 milliseconds on pre-Oreo devices
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
            dialog.dismiss()
            openCamera(packId)  // Pass packId to ensure it is available after capturing the image
        }

        val dialog = builder.create()
        dialog.show()
    }
    private fun openCamera(packId: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            startCameraIntent()
        }
    }

    private fun startCameraIntent() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            if (cameraIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
            } else {
                // Log and toast that no application can handle the camera intent
                Log.e("CameraIntent", "No application can handle camera intent.")
                Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Log the exception details if any error occurs during the process
            Log.e("CameraIntent", "Failed to start camera intent", e)
            Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startCameraIntent()
            } else {
                Toast.makeText(this, "Camera permission is necessary to capture images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageBitmap = intent.parcelable<Bitmap>("data")


            if (imageBitmap != null) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

                Log.d("CaptureImage", "Encoded image: $encodedImage") // Log the encoded string or its length

                lifecycleScope.launch {
                    try {
                        val updateResult = odooXmlRpcClient.updatePickingImage(packId, encodedImage)
                        Log.d("OdooUpdate", "Update result: $updateResult") // Log the result from the server
                    } catch (e: Exception) {
                        Log.e("OdooUpdate", "Failed to update image: ${e.localizedMessage}", e)
                    }
                }
            } else {
                Log.e("CaptureImage", "Failed to capture image")
            }
        }
    }




    //============================================================================================================
    //             Code to display the quantity the user has to pack fro the products (no Tracking)
    //============================================================================================================
    private fun displayQuantityDialog(productName: String, expectedQuantity: Double, packId: Int, lineId: Int, moveLine: MoveLine) {
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
            // Pass the moveLine to the state update function and open the next dialog
            showPackageDialogNoTracking(moveLine)
            alertDialog.dismiss()  // Close the dialog after confirmation
        }

        buttonCancel.setOnClickListener {
            alertDialog.dismiss()  // Close the dialog when cancel is clicked
        }
    }


    //============================================================================================================
    //             Code to display the quantity the user has to pack fro the products (Serialised)
    //============================================================================================================
    /*
    private fun displayQuantityDialogSerial(productName: String, moveLine: MoveLine) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_confirmation, null)
        val textViewConfirmation = dialogView.findViewById<TextView>(R.id.ConfirmationTextView)
        val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirm)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        // Use lotName as the serial number and assume it's a unique count per MoveLine
        val serialNumber = moveLine.lotName
        val fullText = "Confirm the serial number $serialNumber for $productName has been picked."

        val spannableString = SpannableString(fullText)

        // Styling for serialNumber (lotName)
        val numberStart = fullText.indexOf(serialNumber)
        val numberEnd = numberStart + serialNumber.length
        spannableString.setSpan(StyleSpan(Typeface.BOLD), numberStart, numberEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.1f), numberStart, numberEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

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
            // Handle confirmation and proceed with specific serial number handling
            showPackageDialogSerial(moveLine)
            alertDialog.dismiss()  // Close the dialog after confirmation
        }

        buttonCancel.setOnClickListener {
            alertDialog.dismiss()  // Close the dialog when cancel is clicked
        }
    }

    */




    //============================================================================================================
    //                                      Printer Code
    //============================================================================================================
    private fun printPackage(packageName: String) {
        createAndPrintBarcode(packageName)
    }

    private fun createAndPrintBarcode(packageName: String) {
        val barcodeBitmap = createBarcodeImage(packageName)
        barcodeBitmap?.let {
            val fileName = "package_barcode.png"
            saveBitmapToFile(it, fileName)
            printBarcode(fileName)
        }
    }

    private fun createBarcodeImage(packageName: String): Bitmap? {
        return try {
            // Define the dimensions of the barcode itself
            val barcodeWidth = 1000
            val barcodeHeight = 400

            // Generating the barcode
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                packageName,
                BarcodeFormat.CODE_128,
                barcodeWidth,
                barcodeHeight,
                hashMapOf(EncodeHintType.MARGIN to 10)
            )

            // Creating a larger bitmap to include the text beneath the barcode
            val totalHeight = barcodeHeight + 100 // Additional space for the text
            val bitmap = Bitmap.createBitmap(barcodeWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Paint for the barcode
            val paint = Paint().apply {
                color = Color.BLACK
            }

            // Draw the barcode pixels on the new bitmap
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    paint.color = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    canvas.drawRect(x.toFloat(), y.toFloat(), x + 1.toFloat(), y + 1.toFloat(), paint)
                }
            }

            // Adding the text under the barcode
            paint.color = Color.BLACK
            paint.textSize = 40f // Set the text size as needed
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(packageName, barcodeWidth / 2f, barcodeHeight + 60f, paint) // Adjust text position as needed

            bitmap
        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error generating barcode: ${e.localizedMessage}")
            null
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, fileName: String) {
        val file = File(filesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun printBarcode(fileName: String) {
        val filePath = File(filesDir, fileName).absolutePath
        val bitmap = BitmapFactory.decodeFile(filePath)
        val printHelper = PrintHelper(this).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap("Print Job - Package Barcode", bitmap)
    }

    //============================================================================================================
    //                          Submenu for printer (user selects what packages to print code)
    //============================================================================================================
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
        subMenu?.clear()
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)


        val addedPackageNames = mutableSetOf<String>()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val packages = odooXmlRpcClient.fetchResultPackagesByPickingId(packId)
                withContext(Dispatchers.Main) {
                    packages.forEach { packageInfo ->
                        if (!addedPackageNames.contains(packageInfo.name)) {
                            subMenu?.add(Menu.NONE, packageInfo.id, Menu.NONE, packageInfo.name)?.setOnMenuItemClickListener {
                                printPackage(packageInfo.name)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    // Vibrate for 50 milliseconds on Android Oreo (API 26) and above
                                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    // Vibrate for 50 milliseconds on pre-Oreo devices
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(50)
                                }
                                true
                            }
                            addedPackageNames.add(packageInfo.name)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PackageMenu", "Error fetching packages: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PackProductsActivity, "Failed to fetch packages", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    //============================================================================================================
    //                        Androids built in back button at the bottom of the screen
    //                             NB!!!!    INCLUDE IN EVERY ACTIVITY    NB!!!!
    //============================================================================================================
    private fun registerBackPressHandler() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                // Back is pressed... Finishing the activity
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this /* lifecycle owner */) {
                // Back is pressed... Finishing the activity
                finish()
            }
        }
    }


    private inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }


}

