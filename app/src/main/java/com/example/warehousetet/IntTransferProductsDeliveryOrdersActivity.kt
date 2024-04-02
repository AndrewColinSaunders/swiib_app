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

class IntTransferProductsDeliveryOrdersActivity : AppCompatActivity() {
    private lateinit var barcodeInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var successActionButton: Button
    private var products: ArrayList<IntTransferProducts> = arrayListOf()
    private var productDeliveryOrderKeys = ProductDeliveryOrderKey(deliveryOrderID = mutableListOf())
    private lateinit var transferName: String
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var credentialManager: CredentialManager
    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)
    private var deliveryOrderId: Int = 0 // Initialize picking ID
    private var allItemsShouldBeGreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_int_transfer_products_delivery_orders)

        // Initialize components after setContentView
        initializeComponents()

        // Check if all items should be green and make necessary UI updates
        productDeliveryOrderKeys = fetchDeliveryOrderKeys(this)
        allItemsShouldBeGreen = productDeliveryOrderKeys.deliveryOrderID.contains(deliveryOrderId)

        if (allItemsShouldBeGreen) {
            makeCardViewsGreenAndButtonVisible()
        }
    }

    private fun initializeComponents() {
        credentialManager = CredentialManager(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager)
        transferName = intent.getStringExtra("EXTRA_TRANSFER_NAME") ?: ""
        deliveryOrderId = intent.getIntExtra("EXTRA_TRANSFER_ID", -1)

        Log.d("IntTransferProductsDeliveryOrdersActivity", "Received transfer name: $transferName")
        Log.d("IntTransferProductsDeliveryOrdersActivity", "Received delivery order ID: $deliveryOrderId")

        barcodeInput = findViewById(R.id.barcodeInput)
        recyclerView = findViewById(R.id.recyclerView_internal_transfers)
        successActionButton = findViewById(R.id.successActionButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        products = intent.getParcelableArrayListExtra("EXTRA_PRODUCTS") ?: arrayListOf()
        recyclerView.adapter = IntTransferProductsDeliveryOrdersAdapter(products, allItemsShouldBeGreen)

        configureBarcodeInput()
        configureSuccessActionButton()
    }


    fun fetchDeliveryOrderKeys(context: Context): ProductDeliveryOrderKey {
        val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("internalTransferKeys", null)

        return if (json != null) {
            val type = object : TypeToken<MutableList<Int>>() {}.type
            val deliveryOrderId: MutableList<Int> = gson.fromJson(json, type)
            ProductDeliveryOrderKey(deliveryOrderId)
        } else {
            // Return an empty ProductInternalTransferKey if nothing was found
            ProductDeliveryOrderKey(mutableListOf())
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
                    (recyclerView.adapter as? IntTransferProductsDeliveryOrdersAdapter)?.notifyItemChanged(productIndex)
                    showSuccessActionButton()

                    // Append internalTransferId to the list and save/update as needed
                    appendDeliveryOrderIdAndSave(deliveryOrderId)
                } else {
                    Toast.makeText(this, "Quantity mismatch.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun appendDeliveryOrderIdAndSave(id: Int) {
        // Check if the ID is already in the list to avoid duplicates
        if (!productDeliveryOrderKeys.deliveryOrderID.contains(id)) {
            productDeliveryOrderKeys.deliveryOrderID.add(id)
            saveProductDeliveryOrderKeys()
        }
    }

    private fun saveProductDeliveryOrderKeys() {
        // Assuming you're using SharedPreferences to persist the data
        val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(productDeliveryOrderKeys.deliveryOrderID)
        editor.putString("deliveryOrderKeys", json)
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
                        odooXmlRpcClient.validateOperation(this@IntTransferProductsDeliveryOrdersActivity, deliveryOrderId)
                    }
                    if (validationSuccess) {
                        // If validation is successful, redirect back to PickActivity
                        navigateBackToDeliveryOrdersActivity()
                    } else {
                        // Handle validation failure as needed
                        Toast.makeText(this@IntTransferProductsDeliveryOrdersActivity, "Failed to validate delivery order.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@IntTransferProductsDeliveryOrdersActivity, "No delivery order ID found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateBackToDeliveryOrdersActivity() {
        val intent = Intent(this, DeliveryOrdersActivity::class.java)
        val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("InstantRefreshRequested", true).apply()
        // If you want to clear all previous activities on the stack and bring PackActivity to the top
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish() // Optional: if you want to remove the current activity from the stack
    }
}
