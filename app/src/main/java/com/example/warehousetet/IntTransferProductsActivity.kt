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

class IntTransferProductsActivity : AppCompatActivity() {
    private lateinit var barcodeInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var successActionButton: Button
    private var products: ArrayList<IntTransferProducts> = arrayListOf()
    private var productPickKeys = ProductPickKey(sourceDocuments = mutableListOf())
    private lateinit var transferName: String
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_int_products)
        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
        transferName = intent.getStringExtra("EXTRA_TRANSFER_NAME") ?: ""

        Log.d("IntTransferProductsActivity", "Received transfer name: $transferName")
        initializeViews()
        configureBarcodeInput()
        loadVerifiedSourceDocuments()
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
        recyclerView.adapter = IntTransferProductsAdapter(products, productPickKeys.sourceDocuments)
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
                    if (!productPickKeys.sourceDocuments.contains(product.sourceDocument)) {
                        productPickKeys.sourceDocuments.add(product.sourceDocument)
                        saveVerifiedSourceDocuments()
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

    private fun saveVerifiedSourceDocuments() {
        val prefs = getSharedPreferences("VerifiedSourceDocumentsPrefs", Context.MODE_PRIVATE).edit()
        prefs.putStringSet("VerifiedDocuments", productPickKeys.sourceDocuments.toSet()).apply()
    }

    private fun loadVerifiedSourceDocuments() {
        val prefs = getSharedPreferences("VerifiedSourceDocumentsPrefs", Context.MODE_PRIVATE)
        val verifiedDocuments = prefs.getStringSet("VerifiedDocuments", emptySet())
        productPickKeys.sourceDocuments.clear()
        productPickKeys.sourceDocuments.addAll(verifiedDocuments!!)
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
            // Your existing operations
            removeCurrentTransferSourceDocument()
            clearScannedState()
            Log.d("YourActivity", "Transfer Name: $transferName")

            if (transferName.isNotEmpty()) {
                activityScope.launch {
                    withContext(Dispatchers.IO) {
                        odooXmlRpcClient.changePickState(this@IntTransferProductsActivity, transferName)
                    }
                    // After changePickState completes
                    // Assuming you handle UI feedback within changePickState itself or want to do something right after
                    Log.d("YourActivity", "Picking attempt made for transfer name: $transferName")
                    // Optionally, navigate to another activity or update UI based on the outcome here
                }
            } else {
                Log.e("YourActivity", "No transfer name found")
                Toast.makeText(this@IntTransferProductsActivity, "No transfer name found", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun removeCurrentTransferSourceDocument() {
        productPickKeys.sourceDocuments.remove(transferName)
    }

    private fun clearScannedState() {
        val intent = Intent(this, ClearStateService::class.java).apply {
            action = "com.example.warehousetet.CLEAR_SCANNED_STATE"
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel() // Ensure coroutine scope is cancelled to avoid leaks
        saveScannedState()
        saveVerifiedSourceDocuments()
    }
}
