//package com.example.warehousetet
//
//import android.graphics.BitmapFactory
//import android.os.Bundle
//import android.text.InputType
//import android.util.Base64
//import android.util.Log
//import android.view.KeyEvent
//import android.view.LayoutInflater
//import android.view.View
//import android.view.inputmethod.EditorInfo
//import android.widget.*
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class ProductDetailActivity : AppCompatActivity() {
//
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var productNameTextView: TextView
//    private lateinit var productQuantityTextView: TextView
//    private lateinit var productImageView: ImageView
//    private lateinit var barcodeInput: EditText
//    private lateinit var confirmButton: Button
//
//    private var currentProductBarcode: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_product_detail)
//
//        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
//
//        productNameTextView = findViewById(R.id.productDetailNameTextView)
//        productQuantityTextView = findViewById(R.id.productDetailQuantityTextView)
//        productImageView = findViewById(R.id.productImageView)
//        barcodeInput = findViewById(R.id.barcodeInput)
//        confirmButton = findViewById(R.id.confirmButton) // Make sure you have this button in your layout
//
//        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "Unknown Product"
//        val productQuantity = intent.getDoubleExtra("PRODUCT_QUANTITY", 0.0)
//
//        fetchAndDisplayProductImage(productName)
//        fetchProductBarcode(productName)
//
//        productNameTextView.text = productName
//        productQuantityTextView.text = "Quantity: $productQuantity"
//
//        setupBarcodeInputListener()
//        setupConfirmButton()
//    }
//
//    private fun setupBarcodeInputListener() {
//        barcodeInput.setOnEditorActionListener { _, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE ||
//                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                confirmButton.performClick()
//                true
//            } else {
//                false
//            }
//        }
//    }
//
//    private fun setupConfirmButton() {
//        confirmButton.setOnClickListener {
//            val barcode = barcodeInput.text.toString().trim()
//            verifyBarcode(barcode)
//        }
//    }
//
//    private fun verifyBarcode(scannedBarcode: String) {
//        if (scannedBarcode == currentProductBarcode) {
//            showSuccessDialog()
//        } else {
//            showIncorrectBarcodeDialog()
//        }
//    }
//
//    private fun showSuccessDialog() {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null)
//        val quantityInput = EditText(this).apply {
//            inputType = InputType.TYPE_CLASS_NUMBER
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                topMargin = (16 * resources.displayMetrics.density).toInt() // Converts 16dp to pixels
//            }
//            hint = "Enter quantity"
//        }
//
//        val container = dialogView.findViewById<LinearLayout>(R.id.dialogContainer) // Assuming your dialog_success.xml has a LinearLayout with this ID
//        container.addView(quantityInput) // Add the EditText dynamically to your dialog layout
//
//        // Create the AlertDialog without showing it yet
//        val alertDialog = AlertDialog.Builder(this).apply {
//            setView(dialogView)
//            setCancelable(false)
//            setPositiveButton("Confirm", null) // Set to null initially to override the click behavior later
//        }.create()
//
//        alertDialog.setOnShowListener {
//            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
//                val enteredQuantity = quantityInput.text.toString().trim()
//                // Handle the entered quantity here
//                Log.d("ProductDetailActivity", "Quantity entered: $enteredQuantity")
//                alertDialog.dismiss()
//            }
//        }
//
//        // Set a listener on the quantityInput to listen for the Enter key press
//        quantityInput.setOnEditorActionListener { _, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE ||
//                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.performClick()
//                true
//            } else {
//                false
//            }
//        }
//
//        alertDialog.show()
//    }
//
//    private fun showIncorrectBarcodeDialog() {
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_incorrect_barcode, null)
//
//        AlertDialog.Builder(this).apply {
//            setView(dialogView)
//            setPositiveButton("Try Again") { _, _ ->
//                barcodeInput.text.clear()
//                barcodeInput.requestFocus()
//            }
//            setNegativeButton("Cancel", null)
//            create()
//            show()
//        }
//    }
//
//        private fun fetchAndDisplayProductImage(productName: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//            val imageBase64 = odooXmlRpcClient.fetchProductImageByName(productName)
//            if (imageBase64 != null) {
//                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
//                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//                withContext(Dispatchers.Main) {
//                    productImageView.setImageBitmap(decodedImage)
//                }
//            }
//        }
//    }
//
//        private fun fetchProductBarcode(productName: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//            currentProductBarcode = odooXmlRpcClient.fetchProductBarcodeByName(productName)
//        }
//    }
//}
