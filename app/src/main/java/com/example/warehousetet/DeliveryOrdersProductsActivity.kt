package com.example.warehousetet

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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



class DeliveryOrdersProductsActivity : AppCompatActivity(), DeliveryOrdersProductsAdapter.VerificationListener {
    private lateinit var deliveryOrdersProductsAdapter: DeliveryOrdersProductsAdapter
    private lateinit var validateButton: MaterialButton
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private var currentProductName: String? = null
    private val deliveryOrdersMoveLines = mutableListOf<DeliveryOrdersMovedLine>()
    private lateinit var barcodeInput: EditText
    private var shouldShowPrinterIcon = false
    private var lastScannedBarcode = StringBuilder()
    private var lastKeyTime: Long = 0
    private var isScannerInput = false




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
                    verifyPackageBarcode(scannedPackageBarcode)
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
                verifyPackageBarcode(typedBarcode)
                barcodeInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter or scan a barcode first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onVerificationStatusChanged(allVerified: Boolean) {
        validateButton.visibility = if (allVerified) View.VISIBLE else View.INVISIBLE
    }

    private fun verifyPackageBarcode(scannedBarcode: String) = lifecycleScope.launch {
        val matchingPackage = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }
            .find { it.resultPackageName == scannedBarcode }

        if (matchingPackage != null) {
            Log.d("DeliveryOrdersProductsActivity", "Package verification successful: Package ID ${matchingPackage.resultPackageId} has been verified.")
            handlePackageVerificationSuccess(matchingPackage)
            barcodeInput.text.clear()
        } else {
            Toast.makeText(this@DeliveryOrdersProductsActivity, "No matching package found.", Toast.LENGTH_SHORT).show()
            barcodeInput.selectAll()
        }
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


    private fun updateUIForMoveLines(moveLines: List<MoveLine>) {
        runOnUiThread {
            val groupedByPackage = moveLines.groupBy { it.resultPackageName }.map { (packageName, lines) ->
                PackageSection(packageName, lines.first().resultPackageId, lines.toMutableList())
            }
            deliveryOrdersProductsAdapter.sections = groupedByPackage
            deliveryOrdersProductsAdapter.notifyDataSetChanged()
        }
    }


    /*
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
        val matchingMoveLine = deliveryOrdersProductsAdapter.sections.flatMap { it.moveLines }
            .find { it.resultPackageName == scannedBarcode }

        if (matchingMoveLine != null) {
            Log.d("PackProductsActivity", "Barcode verification successful for product: ${matchingMoveLine.productName}")
            checkProductTrackingAndHandle(matchingMoveLine)
            barcodeInput.text.clear()
        } else {
            Toast.makeText(this@DeliveryOrdersProductsActivity, "Barcode does not match any product.", Toast.LENGTH_SHORT).show()
            barcodeInput.selectAll()
        }
    }



    private fun handleVerificationFailure(productName: String?, scannedBarcode: String, expectedBarcode: String?) {
        Toast.makeText(this, "Barcode mismatch for $productName. Expected: $expectedBarcode, Found: $scannedBarcode", Toast.LENGTH_LONG).show()
        barcodeInput.selectAll() // Select all text in EditText to facilitate correction
    }


     */


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
    }


    private fun handleSerialTracking(moveLine: MoveLine) {
        // Implement logic for products tracked by serial number
        Log.d("PackProductsActivity", "Handling serial number tracking for ${moveLine.productName}.")
    }

    private fun handleLotTracking(moveLine: MoveLine) {
        // Implement logic for products tracked by lot
        Log.d("PackProductsActivity", "Handling lot tracking for ${moveLine.productName}.")
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