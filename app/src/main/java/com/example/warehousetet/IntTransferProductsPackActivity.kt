package com.example.warehousetet

import IntTransferProducts
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class IntTransferProductsPackActivity : AppCompatActivity() {
    private lateinit var barcodeInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var successActionButton: Button
    private var products: ArrayList<IntTransferProducts> = arrayListOf()
    private var productPackKeys = ProductPackKey(sourceDocuments = mutableListOf())
    private lateinit var transferName: String
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)
    private var pickingId: Int = 225 // Initialize picking ID

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_int_transfer_products_pack)
        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
        transferName = intent.getStringExtra("EXTRA_TRANSFER_NAME") ?: ""
        pickingId = intent.getIntExtra("EXTRA_TRANSFER_ID", -1) // Corrected key

        Log.d("IntTransferProductsActivity", "Received transfer name: $transferName")
        Log.d("IntTransferProductsActivity", "Received picking ID: $pickingId")

        initializeViews()
        configureBarcodeInput()
        loadScannedState()
        loadButtonState()
        configureSuccessActionButton()
    }

    private fun initializeViews() {
        barcodeInput = findViewById(R.id.barcodeInput)
        recyclerView = findViewById(R.id.recyclerView_internal_transfers)
        successActionButton = findViewById(R.id.successActionButton)
        recyclerView.layoutManager = LinearLayoutManager(this)
        products = intent.getParcelableArrayListExtra("EXTRA_PRODUCTS") ?: arrayListOf()
        recyclerView.adapter = IntTransferProductsPackAdapter(products, productPackKeys.sourceDocuments)
    }

    private fun configureBarcodeInput() {
        barcodeInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val enteredBarcode = barcodeInput.text.toString().trim()
                if (enteredBarcode.isNotEmpty()) {
                    verifyBarcode(enteredBarcode)
                    barcodeInput.text.clear()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
                true
            } else false
        }
    }

    private fun verifyBarcode(scannedBarcode: String) {
        val matchedProductIndex = products.indexOfFirst { it.barcode == scannedBarcode }
        if (matchedProductIndex != -1) {
            promptForQuantity(matchedProductIndex)
        } else {
            Toast.makeText(this, "No matching product found for the entered barcode.", Toast.LENGTH_LONG).show()
        }
    }

    private fun promptForQuantity(productIndex: Int) {
        val product = products[productIndex]
        val input = EditText(this).apply { inputType = EditorInfo.TYPE_CLASS_NUMBER }
        AlertDialog.Builder(this)
            .setTitle("Enter Quantity")
            .setMessage("Enter the quantity for ${product.name}:")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val enteredQuantity = input.text.toString().toDoubleOrNull()
                if (enteredQuantity != null && enteredQuantity > 0) { // Adjusted for a realistic check
                    if (!productPackKeys.sourceDocuments.contains(product.sourceDocument)) {
                        productPackKeys.sourceDocuments.add(product.sourceDocument)
                        showSuccessActionButton()
                    }
                    recyclerView.adapter?.notifyItemChanged(productIndex)
                    Toast.makeText(this, "Quantity verified and product scanned.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Quantity mismatch.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveScannedState() {
        val scannedProductsPrefs = getSharedPreferences("ScannedProductsPrefs", Context.MODE_PRIVATE).edit()
        val scannedBarcodes = products.filter { it.isScanned }.mapNotNull { it.barcode }.toSet()
        scannedProductsPrefs.putStringSet("ScannedBarcodes", scannedBarcodes)
            .putBoolean("ButtonVisible", successActionButton.visibility == View.VISIBLE)
            .apply()
    }

    private fun loadScannedState() {
        val scannedProductsPrefs = getSharedPreferences("ScannedProductsPrefs", Context.MODE_PRIVATE)
        val scannedBarcodes = scannedProductsPrefs.getStringSet("ScannedBarcodes", emptySet())
        products.forEach { product ->
            product.isScanned = scannedBarcodes?.contains(product.barcode) == true
        }
    }


    private fun loadButtonState() {
        val prefs = getSharedPreferences("ScannedProductsPrefs", Context.MODE_PRIVATE)
        val isButtonVisible = prefs.getBoolean("ButtonVisible", false)
        successActionButton.visibility = if (isButtonVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun showSuccessActionButton() {
        successActionButton.visibility = View.VISIBLE
    }

    private fun configureSuccessActionButton() {
        successActionButton.setOnClickListener {
            if (transferName.isNotEmpty()) {
                activityScope.launch {
                    val validationSuccess = withContext(Dispatchers.IO) {
                        odooXmlRpcClient.changePackState(this@IntTransferProductsPackActivity, pickingId)
                    }
                    if (validationSuccess) {
                        // If validation is successful, redirect back to PickActivity
                        navigateBackToPackActivity()
                    } else {
                        // Handle validation failure as needed
                        Toast.makeText(this@IntTransferProductsPackActivity, "Failed to validate picking.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@IntTransferProductsPackActivity, "No picking ID found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateBackToPackActivity() {
        val intent = Intent(this, PackActivity::class.java)
        // If you want to clear all previous activities on the stack and bring PackActivity to the top
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish() // Optional: if you want to remove the current activity from the stack
    }

    private fun removeCurrentTransferSourceDocument() {
        productPackKeys.sourceDocuments.remove(transferName)
    }

    private fun clearScannedState() {
        val intent = Intent(this, ClearStateService::class.java).apply {
            action = "com.example.warehousetet.CLEAR_SCANNED_STATE"
        }
        startService(intent)
    }


}
