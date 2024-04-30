package com.example.warehousetet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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



class PackProductsActivity : AppCompatActivity() {
    private lateinit var packProductsAdapter: PackProductsAdapter
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private var currentProductName: String? = null
    private val packagedMoveLines = mutableListOf<PackagedMovedLine>()
    private lateinit var barcodeInput: EditText
    private var shouldShowPrinterIcon = false
    private var lastScannedBarcode = StringBuilder()
    private var lastKeyTime: Long = 0
    private var isScannerInput = false
    private var isPrintVisible = true
    private var relevantSerialNumbers = mutableListOf<String>()
    private val usedSerialNumbers = mutableSetOf<String>()
    private var serialNumberToMoveLineIdMap = mutableMapOf<String, Int>()



    private val packId by lazy { intent.getIntExtra("PACK_ID", -1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pack_activity_products)

        shouldShowPrinterIcon = false

        loadPackagedIds()
        //loadPrintIconVisibility()

        Log.d("PackProductsActivity", "Activity created with pack ID: $packId")

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        initializeUI()
        if (packId != -1) {
            fetchMoveLinesForPickingId()
        } else {
            Log.e("PackProductsActivity", "Invalid pack ID passed to PackProductsActivity.")
        }
    }


    //Nothing changing here=============================================================================================================================
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pack_products, menu)
        val printItem = menu?.findItem(R.id.action_print)
        val flagItem = menu?.findItem(R.id.action_flag)
        printItem?.icon?.mutate()?.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_ATOP)
        flagItem?.icon?.mutate()?.setColorFilter(ContextCompat.getColor(this, R.color.danger_red), PorterDuff.Mode.SRC_ATOP)


        printItem?.isVisible = isPrintVisible  // Control visibility based on your variable
        return true
    }


    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
        val printItem = menu?.findItem(R.id.action_print)
        subMenu?.clear()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val packages =  odooXmlRpcClient.fetchResultPackagesByPickingId(packId)
                withContext(Dispatchers.Main) {
                    packages.forEach { packageInfo ->
                        subMenu?.add(Menu.NONE, packageInfo.id, Menu.NONE, packageInfo.name)?.setOnMenuItemClickListener {
                            printPackage(packageInfo.name)
                            true
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

//
//    private fun togglePrinterIconVisibility(show: Boolean) {
//        shouldShowPrinterIcon = show
//        invalidateOptionsMenu() // Force menu to update
//    }



// Printer code =====================================================================================================
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




    private fun initializeUI() {
        val packName = intent.getStringExtra("PACK_NAME") ?: "Pack"
        supportActionBar?.title = packName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val validateButton = findViewById<Button>(R.id.validateOperationButton)
        barcodeInput = findViewById(R.id.packBarcodeInput)
        val packConfirmButton = findViewById<Button>(R.id.packConfirmButton)

        shouldShowPrinterIcon = false

        packProductsAdapter = PackProductsAdapter(emptyList(), packId, packagedMoveLines)
        findViewById<RecyclerView>(R.id.packProductsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@PackProductsActivity)
            adapter = packProductsAdapter
        }

        validateButton.setOnClickListener {
            Log.d("ValidationAttempt", "Attempting to validate operation for pack ID: $packId")
            lifecycleScope.launch {
                if (odooXmlRpcClient.validateOperation(packId, this@PackProductsActivity)) {
                    Toast.makeText(this@PackProductsActivity, "Operation validated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PackProductsActivity, "Failed to validate operation.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        packConfirmButton.setOnClickListener {
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




    }

    //=================================================================================================================================================

    private fun fetchProductBarcodes(productNames: List<String>): Map<String, String?> = runBlocking {
        productNames.associateWith { productName ->
            withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductBarcodeByName(productName)
            }
        }
    }

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






    private fun handleVerificationFailure(productName: String?, scannedBarcode: String, expectedBarcode: String?) {
        Toast.makeText(this, "Barcode mismatch for $productName. Expected: $expectedBarcode, Found: $scannedBarcode", Toast.LENGTH_LONG).show()
        barcodeInput.selectAll() // Select all text in EditText to facilitate correction
    }


    private fun checkProductTrackingAndHandle(moveLine: MoveLine) = lifecycleScope.launch {
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

    private fun handleNoTracking(moveLine: MoveLine) {
        // Log the handling action
        Log.d("PackProductsActivity", "Handling no tracking for ${moveLine.productName}.")

        // Call the showInformationDialog to display the packing instructions
        showInformationDialog(moveLine.quantity, moveLine)
    }


    private fun handleSerialTracking(moveLine: MoveLine) {
        // Implement logic for products tracked by serial number
        Log.d("PackProductsActivity", "Handling serial number tracking for ${moveLine.productName}.")
        showPackageDialogSerial(moveLine)
    }

    private fun handleLotTracking(moveLine: MoveLine) {
        // Implement logic for products tracked by lot
        Log.d("PackProductsActivity", "Handling lot tracking for ${moveLine.productName}.")
        showPackageDialogSerial(moveLine)
    }


    private fun showPackageDialogNoTracking(moveLine: MoveLine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package_no_tracking, null)
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val addToPackageButton = dialogView.findViewById<MaterialButton>(R.id.addToPackageButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Package Options")
            .setView(dialogView)
            .create()

        createNewButton.apply {
            text = "Create New"
            setBackgroundColor(ContextCompat.getColor(context, R.color.success_green))
            setOnClickListener {
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
                            isPrintVisible = true
                            refreshMenu()
                            //savePrintIconVisibility()
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
                                    (packagedMoveLines as MutableList).add(PackagedMovedLine(moveLine.lineId))
                                }
                                // Find the index and notify the adapter
                                val index = packProductsAdapter.moveLines.indexOfFirst { it.lineId == moveLine.lineId }
                                if (index != -1) {
                                    packProductsAdapter.notifyItemChanged(index)  // Notify adapter to update this item
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

        dialog.show()
    }

    private fun showPackageDialogSerial(moveLine: MoveLine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package_serial, null)
        val serialInput = dialogView.findViewById<EditText>(R.id.serialInput)  // Ensure correct ID
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val addToPackageButton = dialogView.findViewById<MaterialButton>(R.id.addToPackageButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Package Options")
            .setView(dialogView)
            .create()

        createNewButton.setOnClickListener {
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
                                isPrintVisible = true
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)  // Track the used serial numbers
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
        dialog.show()
    }


        private fun filterRelevantSerialNumbers(allSerialNumbers: List<String>, lotName: String) {
        relevantSerialNumbers.clear()
        relevantSerialNumbers.addAll(allSerialNumbers.filter { serial -> serial.contains(lotName) })
    }

    private fun fetchAndFilterSerialNumbers(productId: Int, lotName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allSerialNumbers = odooXmlRpcClient.fetchLotAndSerialNumbersByProductId(productId) ?: listOf()
                withContext(Dispatchers.Main) {
                    filterRelevantSerialNumbers(allSerialNumbers, lotName)
                    // You may log or update UI here if needed
                }
            } catch (e: Exception) {
                Log.e("PackProductsActivity", "Error fetching serial numbers for product ID: $productId", e)
            }
        }
    }


    private fun showInformationDialog(quantity: Double, moveLine: MoveLine) {
        AlertDialog.Builder(this)
            .setTitle("Product Packing")
            .setMessage("Please pack $quantity units of the selected product as instructed.")
            .setPositiveButton("OK") { dialog, which ->
                showPackageDialogNoTracking(moveLine)  // Pass the moveLine here
            }
            .create()
            .show()
    }

    private fun savePackagedIds() {
        val editor = getSharedPreferences("PackPrefs", MODE_PRIVATE).edit()
        val packagedIds = packagedMoveLines.map { it.moveLineId }.joinToString(",")
        editor.putString("packagedIds", packagedIds)
        editor.apply()
    }

    private fun loadPackagedIds() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("PackPrefs", MODE_PRIVATE)
            val packagedIds = withContext(Dispatchers.IO) {
                val prefs = getSharedPreferences("PackPrefs", MODE_PRIVATE)
                prefs.getString("packagedIds", "") ?: ""
            }

            if (packagedIds.isNotEmpty()) {
                packagedMoveLines.clear()
                packagedMoveLines.addAll(packagedIds.split(",").map { PackagedMovedLine(it.toInt()) })
                updateUIForMoveLines(packProductsAdapter.moveLines) // Update the UI once data is loaded

            }
        }
    }

    private fun checkAllItemsPackaged() {
        val allPackaged = packProductsAdapter.moveLines.all { moveLine ->
            packagedMoveLines.any { it.moveLineId == moveLine.lineId }
        }
        findViewById<Button>(R.id.validateOperationButton).visibility = if (allPackaged) View.VISIBLE else View.GONE
        savePackagedIds() // This call can also be removed if no other state needs to be saved when checking all items
    }

    fun packageItem(moveLineId: Int) {
        val newPackagedMovedLine = PackagedMovedLine(moveLineId)
        packProductsAdapter.addPackagedMoveLine(newPackagedMovedLine)
    }


//
//    private fun savePrintIconVisibility() {
//        val editor = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
//        editor.putBoolean("isPrintVisible", isPrintVisible)
//        editor.apply()
//    }

//
//    private fun loadPrintIconVisibility() {
//        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
//        isPrintVisible = prefs.getBoolean("isPrintVisible", false) // Default to false if not found
//    }

    private fun refreshMenu() {
        invalidateOptionsMenu()
    }
}