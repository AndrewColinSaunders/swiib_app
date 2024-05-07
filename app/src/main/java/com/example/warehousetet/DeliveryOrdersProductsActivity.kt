package com.example.warehousetet

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.media.MediaPlayer
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*




class DeliveryOrdersProductsActivity : AppCompatActivity(), DeliveryOrdersProductsAdapter.VerificationListener {
    private lateinit var deliveryOrdersProductsAdapter: DeliveryOrdersProductsAdapter
    private lateinit var validateButton: MaterialButton
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private var currentProductName: String? = null
    private val deliveryOrdersMoveLines = mutableListOf<DeliveryOrdersMovedLine>()
    private lateinit var barcodeInput: EditText
    private var lastScannedBarcode = StringBuilder()
    private var lastKeyTime: Long = 0
    private var isScannerInput = false
    private val usedSerialNumbers = mutableSetOf<String>()
    private var serialNumberToMoveLineIdMap = mutableMapOf<String, Int>()
    private val verifiedSerialNumbers = mutableSetOf<String>()




    private val deliveryOrdersId by lazy { intent.getIntExtra("DELIVERY_ORDERS_ID", -1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_orders_products)

        validateButton = findViewById(R.id.validateOperationButton)
        validateButton.visibility = View.INVISIBLE


        Log.d("DeliveryOrdersProductsActivity", "Activity created with delivery orders ID: $deliveryOrdersId")

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        initializeUI()
        if (deliveryOrdersId != -1) {
            fetchMoveLinesForPickingId()
        } else {
            Log.d("deliveryOrdersId", "Delivery Order Id: $deliveryOrdersId")
            Log.e("DeliveryOrdersProductsActivity", "Invalid delivery orders ID passed to DeliveryOrdersProductsActivity.")
        }
    }


    //Nothing changing here=============================================================================================================================
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delivery_orders_products, menu)
        val flagItem = menu?.findItem(R.id.action_flag)
        flagItem?.icon?.mutate()?.setColorFilter(ContextCompat.getColor(this, R.color.danger_red), PorterDuff.Mode.SRC_ATOP)
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
        // First, attempt to verify as a package barcode
        val matchingPackage = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }
            .find { it.resultPackageName == scannedBarcode }

        if (matchingPackage != null) {
            Log.d("DeliveryOrdersProductsActivity", "Package verification successful: Package ID ${matchingPackage.resultPackageId} has been verified.")
            handlePackageVerificationSuccess(matchingPackage)
            barcodeInput.text.clear()
            return@launch  // Exit the function after successful package verification
        }

        // If no package is found, check as a product barcode but only if it is "Not Packaged"
        val allMoveLines = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }
        val matchingMoveLine = allMoveLines.find { moveLine ->
            moveLine.resultPackageName == "None" && withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductBarcodeByName(moveLine.productName)
            } == scannedBarcode
        }

        if (matchingMoveLine != null) {
            Log.d("DeliveryOrdersProductsActivity", "Barcode verification successful for product: ${matchingMoveLine.productName}")
            checkProductTrackingAndHandle(matchingMoveLine)  // Adjust this if you don't need package handling logic here
            barcodeInput.text.clear()
        } else {
            Toast.makeText(this@DeliveryOrdersProductsActivity, "Barcode does not match any known package or product, or is not 'Not Packaged'.", Toast.LENGTH_SHORT).show()
            Log.e("DeliveryOrdersProductsActivity", "Barcode verification failed. No matching 'Not Packaged' product found.")
            barcodeInput.selectAll()
        }
    }




    private fun initializeUI() {

        deliveryOrdersProductsAdapter = DeliveryOrdersProductsAdapter(this)
        findViewById<RecyclerView>(R.id.deliveryOrdersProductsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@DeliveryOrdersProductsActivity)
            adapter = deliveryOrdersProductsAdapter
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val validateButton = findViewById<Button>(R.id.validateOperationButton)
        barcodeInput = findViewById(R.id.deliveryOrdersBarcodeInput)
        val packConfirmButton = findViewById<Button>(R.id.deliveryOrdersConfirmButton)


        validateButton.setOnClickListener {
            Log.d("ValidationAttempt", "Attempting to validate operation for pack ID: $deliveryOrdersId")
            lifecycleScope.launch {
                if (odooXmlRpcClient.validateOperation(deliveryOrdersId, this@DeliveryOrdersProductsActivity)) {
                    Toast.makeText(this@DeliveryOrdersProductsActivity, "Operation validated successfully!", Toast.LENGTH_SHORT).show()


                    //============================================================================================================
                    //                              Plays the sound for the validation button
                    //                        NB!!!! INCLUDE IN EVERY ACTIVITY FOR VALIDATION BUTTON NB!!!!
                    //============================================================================================================
                    MediaPlayer.create(this@DeliveryOrdersProductsActivity, R.raw.validation_sound_effect).apply {
                        start() // Start playing the sound
                        setOnCompletionListener {
                            it.release() // Release the MediaPlayer once the sound is done playing
                        }
                    }
                    //============================================================================================================


                    // Navigate to PackActivity if validation is successful
                    val intent = Intent(this@DeliveryOrdersProductsActivity, DeliveryOrdersActivity::class.java)
                    startActivity(intent)
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


    private fun handlePackageVerificationSuccess(moveLine: MoveLine) {
        Log.d("DeliveryOrdersProductsActivity", "Verified package: ${moveLine.resultPackageName}")
        moveLine.resultPackageId?.let { packageId ->
            deliveryOrdersProductsAdapter.verifyPackage(packageId)
        } ?: Log.e("DeliveryOrdersProductsActivity", "Package ID is null, cannot verify package.")
    }



    private fun fetchProductBarcodes(productNames: List<String>): Map<String, String?> = runBlocking {
        productNames.associateWith { productName ->
            withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductBarcodeByName(productName)
            }
        }
    }

    private fun fetchMoveLinesForPickingId() = lifecycleScope.launch {
        Log.d("PackProductsActivity", "Fetching move lines for pack ID: $deliveryOrdersId")
        try {
            val fetchedMoveLines = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchMoveLinesByOperationId(deliveryOrdersId)
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
            val groupedByPackage = moveLines.groupBy { it.resultPackageName }.map { (packageName, lines) ->
                PackageSection(packageName, lines.first().resultPackageId, lines.toMutableList())
            }
            deliveryOrdersProductsAdapter.sections = groupedByPackage
            deliveryOrdersProductsAdapter.notifyDataSetChanged()
        }
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
        Log.d("PackProductsActivity", "Handling no tracking for ${moveLine.productName}. Marking as verified.")

        // Since addVerifiedBarcode now expects a String, convert lineId to String directly.
        val barcodeIdentifier = moveLine.lineId.toString()

        // Check if the barcode identifier is not empty.
        if (barcodeIdentifier.isNotEmpty()) {
            deliveryOrdersProductsAdapter.addVerifiedBarcode(barcodeIdentifier)  // This expects a String.
            Log.d("VerifyBarcode", "Marking barcode $barcodeIdentifier as verified without tracking.")

            // Find the index of the current moveLine in the flat list of all moveLines.
            val index = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }.indexOf(moveLine)
            if (index != -1) {
                deliveryOrdersProductsAdapter.notifyItemChanged(index)
                Log.d("AdapterUpdate", "Updated item at index $index as verified.")
            } else {
                // If the moveLine wasn't found, force a full refresh of the adapter.
                deliveryOrdersProductsAdapter.notifyDataSetChanged()
                Log.d("AdapterUpdate", "Full adapter refresh triggered.")
            }
        } else {
            Log.e("PackProductsActivity", "Invalid identifier for moveLine: ${moveLine.productName}")
        }
    }


    private fun handleSerialTracking(moveLine: MoveLine) {
        // Implement logic for products tracked by serial number
        Log.d("PackProductsActivity", "Handling serial number tracking for ${moveLine.productName}.")
        showDialogScanSerialNumber(moveLine)
    }

    private fun handleLotTracking(moveLine: MoveLine) {
        // Implement logic for products tracked by lot
        Log.d("PackProductsActivity", "Handling lot tracking for ${moveLine.productName}.")
        showDialogScanLotNumber(moveLine)
    }

    private fun showDialogScanSerialNumber(moveLine: MoveLine) {
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
                serialInput.requestFocus() // Focus back to the input for correction
                return@setOnClickListener
            }

            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                usedSerialNumbers.add(enteredSerial)

                // Add serial number to verified list using the adapter's method
                deliveryOrdersProductsAdapter.addVerifiedSerialNumber(enteredSerial)

                Log.d("VerifySerial", "Adding serial $enteredSerial to verified list")

                // Calculate the index to refresh the correct item
                val index = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }.indexOfFirst { it.lineId == moveLineId }
                if (index != -1) {
                    deliveryOrdersProductsAdapter.notifyItemChanged(index)

                    // Log before updating adapter to see what data is being pushed
                    Log.d("AdapterUpdate", "Updating adapter at index $index with new verified serials: ${deliveryOrdersProductsAdapter.verifiedSerialNumbers}")
                }

                Log.d("SerialVerification", "Serial number $enteredSerial has been successfully verified.")
                Toast.makeText(this@DeliveryOrdersProductsActivity, "Serial number verified successfully.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this@DeliveryOrdersProductsActivity, "Invalid serial number entered. Please check and try again.", Toast.LENGTH_LONG).show()
                serialInput.requestFocus()  // Focus back to the input for correction
            }
        }

        dialog.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showDialogScanLotNumber(moveLine: MoveLine) {
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
                lotInput.requestFocus() // Focus back to the input for correction
                return@setOnClickListener
            }

            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                usedSerialNumbers.add(enteredSerial)

                // Add serial number to verified list using the adapter's method
                deliveryOrdersProductsAdapter.addVerifiedSerialNumber(enteredSerial)

                Log.d("VerifySerial", "Adding serial $enteredSerial to verified list")

                // Calculate the index to refresh the correct item
                val index = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }.indexOfFirst { it.lineId == moveLineId }
                if (index != -1) {
                    deliveryOrdersProductsAdapter.notifyItemChanged(index)

                    // Log before updating adapter to see what data is being pushed
                    Log.d("AdapterUpdate", "Updating adapter at index $index with new verified serials: ${deliveryOrdersProductsAdapter.verifiedSerialNumbers}")
                }

                Log.d("SerialVerification", "Serial number $enteredSerial has been successfully verified.")
                Toast.makeText(this@DeliveryOrdersProductsActivity, "Serial number verified successfully.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this@DeliveryOrdersProductsActivity, "Invalid serial number entered. Please check and try again.", Toast.LENGTH_LONG).show()
                lotInput.requestFocus()  // Focus back to the input for correction
            }
        }

        dialog.show()
    }


    //============================================================================================================
    //                        Androids built in back button at the bottom of the screen
    //                             NB!!!!    INCLUDE IN EVERY ACTIVITY    NB!!!!
    //============================================================================================================

    override fun onBackPressed() {
        super.onBackPressed()
        // Create an Intent to start DeliveryOrdersActivity
        val intent = Intent(this, DeliveryOrdersActivity::class.java)
        startActivity(intent)
        finish()  // Optional: Call finish() if you do not want to return to this activity
    }

}