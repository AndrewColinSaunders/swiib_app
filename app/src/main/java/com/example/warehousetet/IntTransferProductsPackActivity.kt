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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

class IntTransferProductsPackActivity : AppCompatActivity() {
    private lateinit var barcodeInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var successActionButton: Button
    private var products: ArrayList<IntTransferProducts> = arrayListOf()
    private var productPackKeys = ProductPackKey(packID = mutableListOf())
    private lateinit var transferName: String
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)
    private var packId: Int = 0 // Initialize picking ID
    private var allItemsShouldBeGreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_int_transfer_products_pack)

        // Initialize components after setContentView
        initializeComponents()

        // Check if all items should be green and make necessary UI updates
        productPackKeys = fetchProductPackKeys(this)
        allItemsShouldBeGreen = productPackKeys.packID.contains(packId)

        if (allItemsShouldBeGreen) {
            makeCardViewsGreenAndButtonVisible()
        }
    }

    private fun initializeComponents() {
        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
        transferName = intent.getStringExtra("EXTRA_TRANSFER_NAME") ?: ""
        packId = intent.getIntExtra("EXTRA_TRANSFER_ID", -1)

        Log.d("IntTransferProductsPackActivity", "Received transfer name: $transferName")
        Log.d("IntTransferProductsPackActivity", "Received packing ID: $packId")

        barcodeInput = findViewById(R.id.barcodeInput)
        recyclerView = findViewById(R.id.recyclerView_internal_transfers)
        successActionButton = findViewById(R.id.successActionButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        products = intent.getParcelableArrayListExtra("EXTRA_PRODUCTS") ?: arrayListOf()
        recyclerView.adapter = IntTransferProductsPackAdapter(products, allItemsShouldBeGreen)

        configureBarcodeInput()
        configureSuccessActionButton()
    }


    fun fetchProductPackKeys(context: Context): ProductPackKey {
        val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("internalTransferKeys", null)

        return if (json != null) {
            val type = object : TypeToken<MutableList<Int>>() {}.type
            val packId: MutableList<Int> = gson.fromJson(json, type)
            ProductPackKey(packId)
        } else {
            // Return an empty ProductInternalTransferKey if nothing was found
            ProductPackKey(mutableListOf())
        }
    }

    private fun makeCardViewsGreenAndButtonVisible() {
        products.forEach { it.isScanned = true }
        recyclerView.adapter?.notifyDataSetChanged()
        successActionButton.visibility = View.VISIBLE
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
                if (enteredQuantity != null && enteredQuantity > 0) {
                    products[productIndex].isScanned = true
                    // Update UI immediately
                    (recyclerView.adapter as? IntTransferProductsPackAdapter)?.notifyItemChanged(productIndex)
                    showSuccessActionButton()

                    // Append internalTransferId to the list and save/update as needed
                    appendPackIdAndSave(packId)
                } else {
                    Toast.makeText(this, "Quantity mismatch.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun appendPackIdAndSave(id: Int) {
        // Check if the ID is already in the list to avoid duplicates
        if (!productPackKeys.packID.contains(id)) {
            productPackKeys.packID.add(id)
            saveProductPackKeys()
        }
    }

    private fun saveProductPackKeys() {
        // Assuming you're using SharedPreferences to persist the data
        val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(productPackKeys.packID)
        editor.putString("internalTransferKeys", json)
        editor.apply()
    }


    private fun showSuccessActionButton() {
        successActionButton.visibility = View.VISIBLE
    }

    private fun configureSuccessActionButton() {
        successActionButton.setOnClickListener {
            if (transferName.isNotEmpty()) {
                activityScope.launch {
                    val validationSuccess = withContext(Dispatchers.IO) {
                        odooXmlRpcClient.validateOperation(this@IntTransferProductsPackActivity, packId)
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
        val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("InstantRefreshRequested", true).apply()
        // If you want to clear all previous activities on the stack and bring PackActivity to the top
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish() // Optional: if you want to remove the current activity from the stack
    }
}
