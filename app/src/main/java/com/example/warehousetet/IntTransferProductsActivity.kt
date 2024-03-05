package com.example.warehousetet

import IntTransferProducts
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class IntTransferProductsActivity : AppCompatActivity() {
    private lateinit var barcodeInput: EditText
    private lateinit var recyclerView: RecyclerView
    private var products: ArrayList<IntTransferProducts> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_int_products)

        // Starting the ClearStateService
        startService(Intent(this, ClearStateService::class.java))

        initializeViews()
        configureBarcodeInput()
    }

    private fun initializeViews() {
        barcodeInput = findViewById(R.id.barcodeInput)
        recyclerView = findViewById(R.id.recyclerView_internal_transfers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        products = intent.getParcelableArrayListExtra("EXTRA_PRODUCTS") ?: arrayListOf()
        loadScannedState()

        recyclerView.adapter = IntTransferProductsAdapter(products)
    }

    private fun configureBarcodeInput() {
        barcodeInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val enteredBarcode = barcodeInput.text.toString().trim()
                if (enteredBarcode.isNotEmpty()) {
                    verifyBarcode(enteredBarcode)
                    barcodeInput.text.clear()
                }
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
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
                if (enteredQuantity != null && enteredQuantity == product.quantity) {
                    product.isScanned = true
                    recyclerView.adapter?.notifyItemChanged(productIndex)
                    saveScannedState()
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
        scannedProductsPrefs.putStringSet("ScannedBarcodes", scannedBarcodes).apply()
    }

    private fun loadScannedState() {
        val scannedProductsPrefs = getSharedPreferences("ScannedProductsPrefs", Context.MODE_PRIVATE)
        val scannedBarcodes = scannedProductsPrefs.getStringSet("ScannedBarcodes", emptySet())
        products.forEach { product ->
            product.isScanned = scannedBarcodes?.contains(product.barcode) == true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveScannedState()
    }
}

