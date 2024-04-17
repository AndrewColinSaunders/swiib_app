package com.example.warehousetet

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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
import org.apache.xmlrpc.XmlRpcException

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
                if (odooXmlRpcClient.validateOperation(packId)) {
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

        addToPackageButton.apply {
            text = "Add to Package"
            setBackgroundColor(ContextCompat.getColor(context, R.color.toDoBlue))
            setOnClickListener {
                // Implement add to existing package logic here
                dialog.dismiss()
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
    }
}
