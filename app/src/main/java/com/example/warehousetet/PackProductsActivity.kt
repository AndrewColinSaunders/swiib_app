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
    private var lastScannedBarcode: String = ""
    private var currentProductName: String? = null
    private val packagedMoveLines = mutableListOf<PackagedMovedLine>()
    private lateinit var barcodeInput: EditText


    private val packId by lazy { intent.getIntExtra("PACK_ID", -1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pack_activity_products)

        loadPackagedIds()

        Log.d("PackProductsActivity", "Activity created with pack ID: $packId")

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        initializeUI()
        if (packId != -1) {
            fetchMoveLinesForPickingId()
        } else {
            Log.e("PackProductsActivity", "Invalid pack ID passed to PackProductsActivity.")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pack_products, menu)
        menu?.findItem(R.id.action_print)?.icon?.mutate()?.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_ATOP)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
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
                verifyProductBarcode(currentProductName, typedBarcode)
                barcodeInput.text.clear()  // Clear the barcode input after processing
            }
        }
    }

    private fun fetchMoveLinesForPickingId() = lifecycleScope.launch {
        Log.d("PackProductsActivity", "Fetching move lines for pack ID: $packId")
        try {
            val fetchedMoveLines = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchMoveLinesByPickingId(packId)
            }
            Log.d("PackProductsActivity", "Fetched move lines: $fetchedMoveLines")
            updateUIForMoveLines(fetchedMoveLines)
            fetchedMoveLines.forEach { moveLine ->
                Log.d("MoveLineLog", "MoveLine ID: ${moveLine.id}")
                currentProductName = moveLine.productName
                checkProductTracking(moveLine.productName)
            }
        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error fetching move lines for pack: ${e.localizedMessage}")
        }
    }

    private fun updateUIForMoveLines(moveLines: List<MoveLine>) {
        runOnUiThread {
            packProductsAdapter.moveLines = moveLines
            packProductsAdapter.notifyDataSetChanged()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
            if (lastScannedBarcode.isNotEmpty()) {
                verifyProductBarcode(currentProductName, lastScannedBarcode)
                lastScannedBarcode = ""  // Reset the barcode after verification
                return true
            }
        } else if (event.action == KeyEvent.ACTION_DOWN && Character.isDigit(event.unicodeChar.toChar())) {
            lastScannedBarcode += event.unicodeChar.toChar()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun verifyProductBarcode(productName: String?, scannedBarcode: String) = lifecycleScope.launch {
        if (productName != null) {
            val expectedBarcode = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductBarcodeByName(productName)
            }
            if (expectedBarcode != null && expectedBarcode == scannedBarcode) {
                Log.d("PackProductsActivity", "Barcode verification successful for product: $productName")
                val moveLine = packProductsAdapter.moveLines.find { it.productName == productName }
                if (moveLine != null) {
                    val quantity = moveLine.quantity
                    showInformationDialog(quantity, moveLine)  // Pass the moveLine to the dialog
                }
            } else {
                Log.e("PackProductsActivity", "Barcode verification failed for product: $productName. Expected: $expectedBarcode, Scanned: $scannedBarcode")
            }
        }
    }

    private fun checkProductTracking(productName: String) = lifecycleScope.launch {
        try {
            val trackingInfo = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductTrackingAndExpirationByName(productName)
            }

            trackingInfo?.let {
                when (it.first) {
                    "no tracking" -> Log.d("PackProductsActivity", "Product has no tracking.")
                    "Serialised" -> Log.d("PackProductsActivity", "Product is tracked by serial number.")
                    "By lot" -> Log.d("PackProductsActivity", "Product is tracked by lot.")
                    else -> Log.d("PackProductsActivity", "Product tracking type: ${it.first}")
                }
            } ?: Log.e("PackProductsActivity", "No tracking info found for product: $productName")
        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error checking product tracking: ${e.localizedMessage}", e)
        }
    }

    private fun showPackageDialog(moveLine: MoveLine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package, null)
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
                Log.d("PackageDialog", "Attempting to put item in new package with MoveLine ID: ${moveLine.id}")
                lifecycleScope.launch {
                    try {
                        val result = odooXmlRpcClient.actionPutInPack(moveLine.id, this@PackProductsActivity)
                        if (result) {
                            Log.d("PackageDialog", "Successfully put item in new package.")
                            Toast.makeText(context, "Item successfully put into a new package.", Toast.LENGTH_SHORT).show()
                            packagedMoveLines.add(PackagedMovedLine(moveLine.id))
                            val index = packProductsAdapter.moveLines.indexOf(moveLine)
                            savePackagedIds()
                            packProductsAdapter.notifyItemChanged(index)
                            checkAllItemsPackaged()// Notify adapter to update this item
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
            lifecycleScope.launch(Dispatchers.IO) {  // Use Dispatchers.IO to enforce background execution
                try {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    Log.e("PackageDialog", "Error occurred: ${e.localizedMessage}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PackProductsActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        dialog.show()
    }


    private fun showInformationDialog(quantity: Double, moveLine: MoveLine) {
        AlertDialog.Builder(this)
            .setTitle("Product Packing")
            .setMessage("Please pack $quantity units of the selected product as instructed.")
            .setPositiveButton("OK") { dialog, which ->
                showPackageDialog(moveLine)  // Pass the moveLine here
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
            packagedMoveLines.any { it.moveLineId == moveLine.id }
        }
        findViewById<Button>(R.id.validateOperationButton).visibility = if (allPackaged) View.VISIBLE else View.GONE
        savePackagedIds() // This call can also be removed if no other state needs to be saved when checking all items
    }
}
