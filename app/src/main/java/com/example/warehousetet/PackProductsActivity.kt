package com.example.warehousetet

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.print.PrintHelper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.xmlrpc.XmlRpcException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class PackProductsActivity : AppCompatActivity(), PackProductsAdapter.VerificationListener {
    private lateinit var packProductsAdapter: PackProductsAdapter
    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private val packagedMoveLines = mutableListOf<PackagedMovedLine>()
    private lateinit var barcodeInput: EditText
    private var shouldShowPrinterIcon = false
    private var lastScannedBarcode = StringBuilder()
    private var lastKeyTime: Long = 0
    private var isScannerInput = false
    private val usedSerialNumbers = mutableSetOf<String>()
    private var serialNumberToMoveLineIdMap = mutableMapOf<String, Int>()
    private val packId by lazy { intent.getIntExtra("PACK_ID", -1) }
    private val packName by lazy {intent.getStringExtra("PACK_NAME")}
    private lateinit var validateButton: Button
    private var classLevelMenu: Menu? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pack_activity_products)

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val imageBitmap = data?.parcelable<Bitmap>("data")
                if (imageBitmap != null) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()
                    val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

                    Log.d("CaptureImage", "Encoded image: $encodedImage")

                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                odooXmlRpcClient.updatePickingImage(packId, encodedImage)
                            }
                            Log.d("OdooUpdate", "Image updated successfully on server")
                        } catch (e: Exception) {
                            Log.e("OdooUpdate", "Failed to update image: ${e.localizedMessage}", e)
                        }
                    }
                } else {
                    Log.e("CaptureImage", "Failed to capture image")
                }
            } else {
                Log.e("CaptureImage", "Camera action was cancelled or failed")
                Toast.makeText(this, "Camera action was cancelled or failed.", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d("PackProductsActivity", "Activity created with pack ID: $packId")

        odooXmlRpcClient = OdooXmlRpcClient(CredentialManager(this))
        initializeUI()
        loadVerificationState()
        loadPackagedIds()
        if (packId != -1) {
            fetchMoveLinesForPickingId()
        } else {
            Log.e("PackProductsActivity", "Invalid pack ID passed to PackProductsActivity.")
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pack_products, menu)
        return true
    }

    override fun onVerificationStatusChanged(allVerified: Boolean) {
        runOnUiThread {
            validateButton.visibility = if (allVerified) View.VISIBLE else View.GONE
            saveVerificationState(allVerified)
        }
    }

    private fun initializeUI() {
        val packName = intent.getStringExtra("PACK_NAME") ?: "Pack"
        supportActionBar?.title = packName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        validateButton = findViewById(R.id.validateOperationButton)
        validateButton.visibility = View.GONE
        barcodeInput = findViewById(R.id.packBarcodeInput)
        val packConfirmButton = findViewById<Button>(R.id.packConfirmButton)
        val clearButton = findViewById<Button>(R.id.packClearButton)

        shouldShowPrinterIcon = false

        packProductsAdapter = PackProductsAdapter(emptyList(), packId, packagedMoveLines, this)
        findViewById<RecyclerView>(R.id.packProductsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@PackProductsActivity)
            adapter = packProductsAdapter
        }

        validateButton.setOnClickListener {
            Log.d("ValidationAttempt", "Attempting to validate operation for pack ID: $packId")

            val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)
            vibrateDevice(vibrator)

            lifecycleScope.launch {
                if (odooXmlRpcClient.validateOperationDO(packId, this@PackProductsActivity)) {
                    Toast.makeText(this@PackProductsActivity, "Operation validated successfully!", Toast.LENGTH_SHORT).show()

                    MediaPlayer.create(this@PackProductsActivity, R.raw.validation_sound_effect).apply {
                        start()
                        setOnCompletionListener {
                            it.release()
                        }
                    }

                    val intent = Intent(this@PackProductsActivity, PackActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@PackProductsActivity, "Failed to validate operation.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        packConfirmButton.setOnClickListener {
            val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)
            vibrateDevice(vibrator)

            val typedBarcode = barcodeInput.text.toString()
            if (typedBarcode.isNotEmpty()) {
                Log.d("PackProductsActivity", "Scanned barcode: $typedBarcode")

                verifyProductBarcode(typedBarcode)
                barcodeInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter or scan a barcode first.", Toast.LENGTH_SHORT).show()
            }
        }

        clearButton.setOnClickListener {
            val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)
            vibrateDevice(vibrator)

            barcodeInput.text.clear()
        }
    }

    private fun fetchProductBarcodes(productNames: List<String>): Map<String, String?> = runBlocking {
        productNames.associateWith { productName ->
            withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductBarcodeByName(productName)
            }
        }
    }

    private fun fetchMoveLinesForPickingId() = lifecycleScope.launch {
        Log.d("PackProductsActivity", "Fetching move lines for pack ID: $packId")
        try {
            val fetchedMoveLines = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchMoveLinesByOperationId(packId)
            }
            Log.d("PackProductsActivity", "Fetched move lines: $fetchedMoveLines")
            Log.d("DeliveryOrdersProductsActivity1234556", "Fetched move lines: ${fetchedMoveLines.map { it.productName + ": " + it.quantity }}")
            updateUIForMoveLines(fetchedMoveLines)
            updateRelevantSerialNumbers(fetchedMoveLines)

            val uniqueProductNames = fetchedMoveLines.map { it.productName }.distinct()
            val barcodes = fetchProductBarcodes(uniqueProductNames)

            Log.d("PackProductsActivity", "Fetched barcodes for all products: ${barcodes.map { "${it.key}: ${it.value}" }.joinToString(", ")}")
        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error fetching move lines for pack: ${e.localizedMessage}")
        }
    }

    private fun updateRelevantSerialNumbers(moveLineOutGoings: List<MoveLineOutGoing>) {
        serialNumberToMoveLineIdMap.clear()
        moveLineOutGoings.forEach { moveLine ->
            if (moveLine.lotName.isNotBlank()) {
                serialNumberToMoveLineIdMap[moveLine.lotName] = moveLine.lineId
            }
        }
        Log.d("PackProductsActivity", "Updated serial numbers to move line IDs: ${serialNumberToMoveLineIdMap.entries.joinToString(", ") { "${it.key}: ${it.value}" }}")
    }

    private fun fetchMoveLineIdForSerialNumber(serialNumber: String): Int? {
        return serialNumberToMoveLineIdMap[serialNumber]
    }

    private fun updateUIForMoveLines(moveLineOutGoings: List<MoveLineOutGoing>) {
        runOnUiThread {
            val diffCallback = MoveLineDiffCallback(packProductsAdapter.moveLineOutGoings, moveLineOutGoings)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            packProductsAdapter.moveLineOutGoings = moveLineOutGoings
            diffResult.dispatchUpdatesTo(packProductsAdapter)
        }
    }

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
        val matchingMoveLine = packProductsAdapter.moveLineOutGoings.find { moveLine ->
            val expectedBarcode = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductBarcodeByName(moveLine.productName)
            }
            expectedBarcode == scannedBarcode
        }

        if (matchingMoveLine != null) {
            Log.d("PackProductsActivity", "Barcode verification successful for product: ${matchingMoveLine.productName}")
            checkProductTrackingAndHandle(matchingMoveLine)
            barcodeInput.text.clear()
        } else {
            Toast.makeText(this@PackProductsActivity, "Barcode does not match any product.", Toast.LENGTH_SHORT).show()
            Log.e("PackProductsActivity", "Barcode verification failed. No matching product found.")
            barcodeInput.selectAll()
        }
    }

    private fun checkProductTrackingAndHandle(moveLineOutGoing: MoveLineOutGoing) = lifecycleScope.launch {
        val productName = moveLineOutGoing.productName
        try {
            val trackingInfo = withContext(Dispatchers.IO) {
                odooXmlRpcClient.fetchProductTrackingAndExpirationByName(productName)
            }

            trackingInfo?.let {
                when (it.first) {
                    "none" -> handleNoTracking(classLevelMenu, moveLineOutGoing)
                    "serial" -> handleSerialTracking(classLevelMenu, moveLineOutGoing)
                    "lot" -> handleLotTracking(classLevelMenu, moveLineOutGoing)
                    else -> Log.e("PackProductsActivity", "Unhandled tracking type: ${it.first}")
                }
            } ?: Log.e("PackProductsActivity", "No tracking info found for product: $productName")
        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error checking product tracking: ${e.localizedMessage}", e)
        }
    }

    private fun handleNoTracking(menu: Menu?, moveLineOutGoing: MoveLineOutGoing) {
        Log.d("PackProductsActivity", "Handling no tracking for ${moveLineOutGoing.productName}.")

        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
        if (subMenu != null && subMenu.size() > 0) {
            displayQuantityDialog(moveLineOutGoing.productName, moveLineOutGoing.quantity, moveLineOutGoing)
        } else {
            showPrePackageDialogNoTracking(moveLineOutGoing)
        }
    }

    private fun handleSerialTracking(menu: Menu?, moveLineOutGoing: MoveLineOutGoing) {
        Log.d("PackProductsActivity", "Handling serial number tracking for ${moveLineOutGoing.productName}.")
        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
        if (subMenu != null && subMenu.size() > 0) {
            showPackageDialogSerial(moveLineOutGoing)
        } else {
            showPrePackageDialogSerialTracking(moveLineOutGoing)
        }
    }

    private fun handleLotTracking(menu: Menu?, moveLineOutGoing: MoveLineOutGoing) {
        Log.d("PackProductsActivity", "Handling lot tracking for ${moveLineOutGoing.productName}.")
        val subMenu = menu?.findItem(R.id.action_print)?.subMenu
        if (subMenu != null && subMenu.size() > 0) {
            showPackageDialogLot(moveLineOutGoing)
        } else {
            showPrePackageDialogLotTracking(moveLineOutGoing)
        }
    }

    private fun showPrePackageDialogNoTracking(moveLineOutGoing: MoveLineOutGoing) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_prepackage_no_tracking, null)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        createNewButton.apply {
            text = getString(R.string.create_new)
            setBackgroundColor(ContextCompat.getColor(context, R.color.success_green))
            setOnClickListener {
                vibrateDevice(vibrator)
                Log.d("PackageDialog", "Attempting to put item in new package with MoveLine ID: ${moveLineOutGoing.lineId}")
                lifecycleScope.launch {
                    try {
                        Log.d("Check what moveline is being parsed", "moveLine ID: $moveLineOutGoing")
                        val result = withContext(Dispatchers.IO) {
                            odooXmlRpcClient.putMoveLineInNewPack(moveLineOutGoing.lineId, this@PackProductsActivity)
                        }
                        if (result) {
                            Log.d("PackageDialog", "Successfully put item in new package.")
                            Toast.makeText(context, "Item successfully put into a new package.", Toast.LENGTH_SHORT).show()
                            packagedMoveLines.add(PackagedMovedLine(moveLineOutGoing.lineId))
                            val index = packProductsAdapter.moveLineOutGoings.indexOf(moveLineOutGoing)
                            savePackagedIds()
                            packProductsAdapter.notifyItemChanged(index)
                            checkAllItemsPackaged()
                            refreshMenu()
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

        notPackagedButton.apply {
            text = getString(R.string.not_packaged)
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                vibrateDevice(vibrator)
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLineOutGoing.lineId}")

                val packagedMovedLine = PackagedMovedLine(moveLineOutGoing.lineId)
                packagedMoveLines.add(packagedMovedLine)

                val index = packProductsAdapter.moveLineOutGoings.indexOf(moveLineOutGoing)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showPackageDialogNoTracking(moveLineOutGoing: MoveLineOutGoing) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package_no_tracking, null)
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val addToPackageButton = dialogView.findViewById<MaterialButton>(R.id.addToPackageButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        createNewButton.apply {
            text = getString(R.string.create_new)
            setBackgroundColor(ContextCompat.getColor(context, R.color.success_green))
            setOnClickListener {
                vibrateDevice(vibrator)
                Log.d("PackageDialog", "Attempting to put item in new package with MoveLine ID: ${moveLineOutGoing.lineId}")
                lifecycleScope.launch {
                    try {
                        Log.d("Check what moveline is being parsed", "moveLine ID: $moveLineOutGoing")
                        val result = withContext(Dispatchers.IO) {
                            odooXmlRpcClient.putMoveLineInNewPack(moveLineOutGoing.lineId, this@PackProductsActivity)
                        }
                        if (result) {
                            Log.d("PackageDialog", "Successfully put item in new package.")
                            Toast.makeText(context, "Item successfully put into a new package.", Toast.LENGTH_SHORT).show()
                            packagedMoveLines.add(PackagedMovedLine(moveLineOutGoing.lineId))
                            val index = packProductsAdapter.moveLineOutGoings.indexOf(moveLineOutGoing)
                            savePackagedIds()
                            packProductsAdapter.notifyItemChanged(index)
                            checkAllItemsPackaged()
                            refreshMenu()
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
            val packageName = packageInput.text.toString()
            if (packageName.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.setPackageForMoveLine(packId, moveLineOutGoing.lineId, packageName)
                        withContext(Dispatchers.Main) {
                            if (result) {
                                if (!packagedMoveLines.any { it.moveLineId == moveLineOutGoing.lineId }) {
                                    packagedMoveLines.add(PackagedMovedLine(moveLineOutGoing.lineId))
                                    packageItem(moveLineOutGoing.lineId)
                                    checkAllItemsPackaged()
                                    refreshMenu()
                                }
                                val index = packProductsAdapter.moveLineOutGoings.indexOfFirst { it.lineId == moveLineOutGoing.lineId }
                                if (index != -1) {
                                    packProductsAdapter.notifyItemChanged(index)
                                }
                                addToPackageButton.setBackgroundColor(ContextCompat.getColor(this@PackProductsActivity, R.color.success_green))
                                Toast.makeText(this@PackProductsActivity, "Package set successfully.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this@PackProductsActivity, "Failed to set package for move line.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PackageDialog", "Error occurred: ${e.localizedMessage}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PackProductsActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this@PackProductsActivity, "Please enter a package name.", Toast.LENGTH_SHORT).show()
            }
        }

        notPackagedButton.apply {
            text = getString(R.string.not_packaged)
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                vibrateDevice(vibrator)
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLineOutGoing.lineId}")

                val packagedMovedLine = PackagedMovedLine(moveLineOutGoing.lineId)
                packagedMoveLines.add(packagedMovedLine)

                val index = packProductsAdapter.moveLineOutGoings.indexOf(moveLineOutGoing)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showPrePackageDialogSerialTracking(moveLineOutGoing: MoveLineOutGoing) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_prepackage_serial, findViewById(android.R.id.content), false)
        val serialInput = dialogView.findViewById<EditText>(R.id.serialInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun isSerialInputValid(): Boolean {
            val enteredSerial = serialInput.text.toString().trim()
            return if (enteredSerial.isEmpty()) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Please enter a serial number.",
                    Toast.LENGTH_SHORT
                ).show()
                serialInput.requestFocus()
                false
            } else {
                true
            }
        }

        createNewButton.setOnClickListener {
            if (!isSerialInputValid()) return@setOnClickListener

            vibrateDevice(vibrator)
            val enteredSerial = serialInput.text.toString().trim()

            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "This serial number has already been used. Please enter a different one.",
                    Toast.LENGTH_LONG
                ).show()
                serialInput.requestFocus()
                return@setOnClickListener
            }

            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.putMoveLineInNewPack(moveLineId, this@PackProductsActivity)
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Item successfully put into a new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                packageItem(moveLineId)
                                checkAllItemsPackaged()
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)
                                odooXmlRpcClient.updateMoveLineQuantityForReceipt(packId, moveLineOutGoing.lineId, 1)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to put item in new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Error during packaging: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Invalid serial number entered. Please check and try again.",
                    Toast.LENGTH_LONG
                ).show()
                serialInput.requestFocus()
            }
        }

        notPackagedButton.apply {
            text = getString(R.string.not_packaged)
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (!isSerialInputValid()) return@setOnClickListener

                vibrateDevice(vibrator)
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLineOutGoing.lineId}")

                val packagedMovedLine = PackagedMovedLine(moveLineOutGoing.lineId)
                packagedMoveLines.add(packagedMovedLine)

                val index = packProductsAdapter.moveLineOutGoings.indexOf(moveLineOutGoing)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showPackageDialogSerial(moveLineOutGoing: MoveLineOutGoing) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package_serial, null)
        val serialInput = dialogView.findViewById<EditText>(R.id.serialInput)
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val addToPackageButton = dialogView.findViewById<MaterialButton>(R.id.addToPackageButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun isSerialInputValid(): Boolean {
            val enteredSerial = serialInput.text.toString().trim()
            return if (enteredSerial.isEmpty()) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Please enter a serial number.",
                    Toast.LENGTH_SHORT
                ).show()
                serialInput.requestFocus()
                false
            } else {
                true
            }
        }

        createNewButton.setOnClickListener {
            if (!isSerialInputValid()) return@setOnClickListener

            vibrateDevice(vibrator)
            val enteredSerial = serialInput.text.toString().trim()

            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "This serial number has already been used. Please enter a different one.",
                    Toast.LENGTH_LONG
                ).show()
                serialInput.requestFocus()
                return@setOnClickListener
            }

            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.putMoveLineInNewPack(moveLineId, this@PackProductsActivity)
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Item successfully put into a new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                packageItem(moveLineId)
                                checkAllItemsPackaged()
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)
                                odooXmlRpcClient.updateMoveLineQuantityForReceipt(packId, moveLineOutGoing.lineId, 1)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to put item in new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Error during packaging: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Invalid serial number entered. Please check and try again.",
                    Toast.LENGTH_LONG
                ).show()
                serialInput.requestFocus()
            }
        }

        addToPackageButton.setOnClickListener {
            if (!isSerialInputValid()) return@setOnClickListener

            vibrateDevice(vibrator)
            val packageName = packageInput.text.toString().trim()
            val enteredSerial = serialInput.text.toString().trim()

            if (packageName.isNotEmpty() && enteredSerial.isNotEmpty()) {
                if (usedSerialNumbers.contains(enteredSerial)) {
                    Toast.makeText(
                        this@PackProductsActivity,
                        "This serial number has already been used. Please enter a different one.",
                        Toast.LENGTH_LONG
                    ).show()
                    serialInput.requestFocus()
                    return@setOnClickListener
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
                        if (moveLineId != null) {
                            val result = withContext(Dispatchers.IO) {
                                odooXmlRpcClient.setPackageForMoveLine(packId, moveLineId, packageName)
                            }
                            if (result) {
                                packageItem(moveLineId)
                                checkAllItemsPackaged()
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)

                                addToPackageButton.setBackgroundColor(ContextCompat.getColor(this@PackProductsActivity, R.color.success_green))
                                Toast.makeText(this@PackProductsActivity, "Package set successfully for move line ID: $moveLineId.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this@PackProductsActivity, "Failed to set package for move line ID: $moveLineId.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@PackProductsActivity, "Invalid serial number entered. Please check and try again.", Toast.LENGTH_LONG).show()
                            serialInput.requestFocus()
                        }
                    } catch (e: Exception) {
                        Log.e("PackageDialog", "Error occurred: ${e.localizedMessage}")
                        Toast.makeText(this@PackProductsActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@PackProductsActivity, "Please enter a package name and serial number.", Toast.LENGTH_SHORT).show()
            }
        }

        notPackagedButton.apply {
            text = getString(R.string.not_packaged)
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (!isSerialInputValid()) return@setOnClickListener

                vibrateDevice(vibrator)
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLineOutGoing.lineId}")

                val packagedMovedLine = PackagedMovedLine(moveLineOutGoing.lineId)
                packagedMoveLines.add(packagedMovedLine)

                val index = packProductsAdapter.moveLineOutGoings.indexOf(moveLineOutGoing)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showPrePackageDialogLotTracking(moveLineOutGoing: MoveLineOutGoing) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_prepackage_lot, null)
        val lotInput = dialogView.findViewById<EditText>(R.id.lotInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun isLotInputValid(): Boolean {
            val enteredLot = lotInput.text.toString().trim()
            return if (enteredLot.isEmpty()) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Please enter a lot number.",
                    Toast.LENGTH_SHORT
                ).show()
                lotInput.requestFocus()
                false
            } else {
                true
            }
        }

        createNewButton.setOnClickListener {
            if (!isLotInputValid()) return@setOnClickListener

            val enteredSerial = lotInput.text.toString().trim()

            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "This serial number has already been used. Please enter a different one.",
                    Toast.LENGTH_LONG
                ).show()
                lotInput.requestFocus()
                return@setOnClickListener
            }

            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.putMoveLineInNewPack(moveLineId, this@PackProductsActivity)
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Item successfully put into a new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                packageItem(moveLineId)
                                checkAllItemsPackaged()
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)
                                odooXmlRpcClient.updateMoveLineQuantityForReceipt(packId, moveLineOutGoing.lineId, 1)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to put item in new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Error during packaging: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Invalid serial number entered. Please check and try again.",
                    Toast.LENGTH_LONG
                ).show()
                lotInput.requestFocus()
            }
        }

        notPackagedButton.apply {
            text = getString(R.string.not_packaged)
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (!isLotInputValid()) return@setOnClickListener

                vibrateDevice(vibrator)
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLineOutGoing.lineId}")

                val packagedMovedLine = PackagedMovedLine(moveLineOutGoing.lineId)
                packagedMoveLines.add(packagedMovedLine)

                val index = packProductsAdapter.moveLineOutGoings.indexOf(moveLineOutGoing)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showPackageDialogLot(moveLineOutGoing: MoveLineOutGoing) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_package_lot, null)
        val lotInput = dialogView.findViewById<EditText>(R.id.lotInput)
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        val createNewButton = dialogView.findViewById<MaterialButton>(R.id.createNewButton)
        val addToPackageButton = dialogView.findViewById<MaterialButton>(R.id.addToPackageButton)
        val notPackagedButton = dialogView.findViewById<MaterialButton>(R.id.notPackagedButton)
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun isLotInputValid(): Boolean {
            val enteredLot = lotInput.text.toString().trim()
            return if (enteredLot.isEmpty()) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Please enter a lot number.",
                    Toast.LENGTH_SHORT
                ).show()
                lotInput.requestFocus()
                false
            } else {
                true
            }
        }

        createNewButton.setOnClickListener {
            if (!isLotInputValid()) return@setOnClickListener

            val enteredSerial = lotInput.text.toString().trim()

            if (usedSerialNumbers.contains(enteredSerial)) {
                Toast.makeText(
                    this@PackProductsActivity,
                    "This serial number has already been used. Please enter a different one.",
                    Toast.LENGTH_LONG
                ).show()
                lotInput.requestFocus()
                return@setOnClickListener
            }

            val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
            if (moveLineId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = odooXmlRpcClient.putMoveLineInNewPack(moveLineId, this@PackProductsActivity)
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Item successfully put into a new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                packageItem(moveLineId)
                                checkAllItemsPackaged()
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)
                                odooXmlRpcClient.updateMoveLineQuantityForReceipt(packId, moveLineOutGoing.lineId, 1)
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    this@PackProductsActivity,
                                    "Failed to put item in new package.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PackProductsActivity,
                                "Error during packaging: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@PackProductsActivity,
                    "Invalid serial number entered. Please check and try again.",
                    Toast.LENGTH_LONG
                ).show()
                lotInput.requestFocus()
            }
        }

        addToPackageButton.setOnClickListener {
            if (!isLotInputValid()) return@setOnClickListener

            val packageName = packageInput.text.toString().trim()
            val enteredSerial = lotInput.text.toString().trim()

            if (packageName.isNotEmpty()) {
                if (usedSerialNumbers.contains(enteredSerial)) {
                    Toast.makeText(
                        this@PackProductsActivity,
                        "This serial number has already been used. Please enter a different one.",
                        Toast.LENGTH_LONG
                    ).show()
                    lotInput.requestFocus()
                    return@setOnClickListener
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        val moveLineId = fetchMoveLineIdForSerialNumber(enteredSerial)
                        if (moveLineId != null) {
                            val result = withContext(Dispatchers.IO) {
                                odooXmlRpcClient.setPackageForMoveLine(packId, moveLineId, packageName)
                            }
                            if (result) {
                                packageItem(moveLineId)
                                checkAllItemsPackaged()
                                refreshMenu()
                                usedSerialNumbers.add(enteredSerial)

                                addToPackageButton.setBackgroundColor(ContextCompat.getColor(this@PackProductsActivity, R.color.success_green))
                                Toast.makeText(this@PackProductsActivity, "Package set successfully for move line ID: $moveLineId.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this@PackProductsActivity, "Failed to set package for move line ID: $moveLineId.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@PackProductsActivity, "Invalid serial number entered. Please check and try again.", Toast.LENGTH_LONG).show()
                            lotInput.requestFocus()
                        }
                    } catch (e: Exception) {
                        Log.e("PackageDialog", "Error occurred: ${e.localizedMessage}")
                        Toast.makeText(this@PackProductsActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@PackProductsActivity, "Please enter a package name.", Toast.LENGTH_SHORT).show()
                packageInput.requestFocus()
            }
        }

        notPackagedButton.apply {
            text = getString(R.string.not_packaged)
            setBackgroundColor(ContextCompat.getColor(context, R.color.cardGrey))
            setOnClickListener {
                if (!isLotInputValid()) return@setOnClickListener

                vibrateDevice(vibrator)
                Log.d("PackageDialog", "Marking as not packaged for MoveLine ID: ${moveLineOutGoing.lineId}")

                val packagedMovedLine = PackagedMovedLine(moveLineOutGoing.lineId)
                packagedMoveLines.add(packagedMovedLine)

                val index = packProductsAdapter.moveLineOutGoings.indexOf(moveLineOutGoing)
                savePackagedIds()
                packProductsAdapter.notifyItemChanged(index)
                checkAllItemsPackaged()
                refreshMenu()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun savePackagedIds() {
        val editor = getSharedPreferences("PackPrefs", MODE_PRIVATE).edit()
        val packagedIds = packagedMoveLines.map { it.moveLineId }.joinToString(",")
        editor.putString("packagedIds", packagedIds)
        editor.apply()
    }

    private fun loadPackagedIds() {
        val prefs = getSharedPreferences("PackPrefs", MODE_PRIVATE)
        val packagedIds = prefs.getString("packagedIds", "") ?: ""
        if (packagedIds.isNotEmpty()) {
            packagedMoveLines.clear()
            packagedMoveLines.addAll(packagedIds.split(",").map { PackagedMovedLine(it.toInt()) })
            updateUIForMoveLines(packProductsAdapter.moveLineOutGoings)
        }
    }

    private fun checkAllItemsPackaged() {
        val allPackaged = packProductsAdapter.moveLineOutGoings.all { moveLine ->
            packagedMoveLines.any { it.moveLineId == moveLine.lineId }
        }
        findViewById<Button>(R.id.validateOperationButton).visibility = if (allPackaged) View.VISIBLE else View.GONE
        savePackagedIds()
    }

    private fun packageItem(moveLineId: Int) {
        val newPackagedMovedLine = PackagedMovedLine(moveLineId)
        packProductsAdapter.addPackagedMoveLine(newPackagedMovedLine)
        savePackagedIds()
    }

    private fun saveVerificationState(isAllVerified: Boolean) {
        getSharedPreferences("PackProductPrefs_$packId", Context.MODE_PRIVATE).edit().apply {
            putBoolean("allVerified", isAllVerified)
            apply()
        }
    }

    private fun loadVerificationState() {
        val prefs = getSharedPreferences("PackProductPrefs_$packId", Context.MODE_PRIVATE)
        val allVerified = prefs.getBoolean("allVerified", false)
        validateButton.visibility = if (allVerified) View.VISIBLE else View.GONE
    }

    private fun refreshMenu() {
        invalidateOptionsMenu()
    }

    private suspend fun sendEmailToBuyer(buyerEmail: String, buyerName: String, packName: String?) {
        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.smtp.host", "mail.dattec.co.za")
                put("mail.smtp.socketFactory.port", "465")
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.auth", "true")
                put("mail.smtp.port", "465")
            }

            val session = Session.getDefaultInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication("info@dattec.co.za", "0s3*X4n)#m,z")
                }
            })

            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress("info@dattec.co.za"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(buyerEmail))
                    subject = "Action Required: Discrepancy in Received Quantity for Receipt $packName"
                    setText("""
                    Dear $buyerName,

                    During a recent receipt event, we identified a discrepancy in the quantities received for the following item:

                    - Pack Name: $packName

                    The recorded quantity does not match the expected quantity as per our purchase order. This discrepancy requires your immediate attention and action.

                    Please review the receipt and product details at your earliest convenience and undertake the necessary steps to rectify this discrepancy. It is crucial to address these issues promptly to maintain accurate inventory records and ensure operational efficiency.

                    Thank you for your prompt attention to this matter.

                    Best regards,
                    The Swiib Team
                """.trimIndent())
                }
                Transport.send(message)
                Log.d("EmailSender", "Email sent successfully to $buyerEmail.")
            } catch (e: MessagingException) {
                Log.e("EmailSender", "Failed to send email.", e)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                vibrateDevice(vibrator)
                return true
            }
            R.id.action_flag -> {
                showFlagDialog()
                vibrateDevice(vibrator)
                return true
            }
            R.id.action_print -> {
                val subMenu = item.subMenu
                if (subMenu == null || subMenu.size() == 0) {
                    Toast.makeText(this, "There Is Nothing To Print Right Now", Toast.LENGTH_LONG).show()
                    vibrateDevice(vibrator)
                    return true
                } else {
                    vibrateDevice(vibrator)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun vibrateDevice(vibrator: Vibrator?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    private fun showFlagDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_pick, findViewById(android.R.id.content), false)
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(false)
        }
        val dialog = dialogBuilder.create()

        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            vibrateDevice(vibrator)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnFlagPick).setOnClickListener {
            vibrateDevice(vibrator)
            lifecycleScope.launch {
                try {
                    val pickId = this@PackProductsActivity.packId
                    val pickName = this@PackProductsActivity.packName ?: run {
                        Log.e("PackProductsActivity", "Pack name is null")
                        Toast.makeText(this@PackProductsActivity, "Invalid pack details", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val buyerDetails = withContext(Dispatchers.IO) {
                        odooXmlRpcClient.fetchAndLogBuyerDetails(pickName)
                    }

                    if (buyerDetails != null) {
                        Log.d("PackProductsActivity", "Starting image capture for packId: $pickId")
                        captureImage(pickId)
                        sendEmailToBuyer(buyerDetails.login, buyerDetails.name, pickName)
                        Log.d("PackProductsActivity", "Pack flagged and buyer notified via email.")
                        Toast.makeText(this@PackProductsActivity, "Pack flagged", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("PackProductsActivity", "Failed to fetch buyer details or flag the pack.")
                        Toast.makeText(this@PackProductsActivity, "Failed to flag pack", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("PackProductsActivity", "Error in flagging process: ${e.localizedMessage}", e)
                    Toast.makeText(this@PackProductsActivity, "Error during flagging", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun captureImage(pickId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Capture Image?")
        builder.setMessage("Would you like to capture an image?")
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)

        builder.setNegativeButton("No") { dialog, _ ->
            vibrateDevice(vibrator)
            dialog.dismiss()
        }

        builder.setPositiveButton("Capture Image") { dialog, _ ->
            vibrateDevice(vibrator)
            dialog.dismiss()
            Log.d("CaptureImage", "Opening camera for packId: $pickId")
            openCamera(pickId)
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun openCamera(packId: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            Log.d("CaptureImage", "Camera permission granted, starting camera intent for packId: $packId")
            startCameraIntent()
        }
    }
    companion object {
        private const val CAMERA_REQUEST_CODE = 1001
    }

    private fun startCameraIntent() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            if (cameraIntent.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(cameraIntent)
            } else {
                Log.e("CameraIntent", "No application can handle camera intent.")
                Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("CameraIntent", "Failed to start camera intent", e)
            Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("CaptureImage", "Camera permission granted, starting camera intent")
                startCameraIntent()
            } else {
                Toast.makeText(this, "Camera permission is necessary to capture images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayQuantityDialog(
        productName: String,
        expectedQuantity: Double,
        moveLineOutGoing: MoveLineOutGoing
    ) {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.dialog_quantity_confirmation,
            findViewById(android.R.id.content),
            false
        )
        val textViewConfirmation = dialogView.findViewById<TextView>(R.id.ConfirmationTextView)
        val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirm)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        val fullText = "Confirm the quantity of $expectedQuantity for $productName has been picked."
        val spannableString = SpannableString(fullText)

        val quantityStart = fullText.indexOf("$expectedQuantity")
        val quantityEnd = quantityStart + "$expectedQuantity".length
        spannableString.setSpan(StyleSpan(Typeface.BOLD), quantityStart, quantityEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.1f), quantityStart, quantityEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val productNameStart = fullText.indexOf(productName)
        val productNameEnd = productNameStart + productName.length
        spannableString.setSpan(StyleSpan(Typeface.BOLD), productNameStart, productNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.1f), productNameStart, productNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textViewConfirmation.text = spannableString

        val alertDialog = AlertDialog.Builder(this).apply {
            setView(dialogView)
            create()
        }.show()

        buttonConfirm.setOnClickListener {
            showPackageDialogNoTracking(moveLineOutGoing)
            alertDialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun printPackage(packageName: String) {
        createAndPrintBarcode(packageName)
    }

    private fun createAndPrintBarcode(packageName: String) {
        val barcodeBitmap = createBarcodeImage(packageName)
        barcodeBitmap?.let {
            saveBitmapToFile(it)
            printBarcode()
        }
    }

    private fun createBarcodeImage(packageName: String): Bitmap? {
        return try {
            val barcodeWidth = 1000
            val barcodeHeight = 400

            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                packageName,
                BarcodeFormat.CODE_128,
                barcodeWidth,
                barcodeHeight,
                hashMapOf(EncodeHintType.MARGIN to 10)
            )

            val totalHeight = barcodeHeight + 100
            val bitmap = Bitmap.createBitmap(barcodeWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint().apply {
                color = Color.BLACK
            }

            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    paint.color = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    canvas.drawRect(x.toFloat(), y.toFloat(), x + 1.toFloat(), y + 1.toFloat(), paint)
                }
            }

            paint.color = Color.BLACK
            paint.textSize = 40f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(packageName, barcodeWidth / 2f, barcodeHeight + 60f, paint)

            bitmap
        } catch (e: Exception) {
            Log.e("PackProductsActivity", "Error generating barcode: ${e.localizedMessage}")
            null
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap) {
        val fileName = "package_barcode.png"
        val file = File(filesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun printBarcode() {
        val fileName = "package_barcode.png"
        val filePath = File(filesDir, fileName).absolutePath
        val bitmap = BitmapFactory.decodeFile(filePath)
        val printHelper = PrintHelper(this).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap("Print Job - Package Barcode", bitmap)
    }


//override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
//    super.onPrepareOptionsMenu(menu)
//    val subMenu = menu?.findItem(R.id.menu_more)?.subMenu
//    subMenu?.clear()
//
//    // Add static menu items
//    subMenu?.add(Menu.NONE, R.id.action_flag, Menu.NONE, "Flag")
//    val printItem = subMenu?.add(Menu.NONE, R.id.action_print, Menu.NONE, "Print Package Labels")
//
//    printItem?.setOnMenuItemClickListener {
//        showPackageDialog()
//        true // Return true to indicate that the menu item click has been handled
//    }
//
//    return true
//}
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        val subMenu = menu?.findItem(R.id.menu_more)?.subMenu
        subMenu?.clear()

        // Define the SpannableString with color span for "Flag"
        val flagTitle = SpannableString("Flag").apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@PackProductsActivity, R.color.danger_red)), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Add static menu items with styled text
        subMenu?.add(Menu.NONE, R.id.action_flag, Menu.NONE, flagTitle)

        // Adding "Print Package Labels" item with a click listener
        val printItem = subMenu?.add(Menu.NONE, R.id.action_print, Menu.NONE, "Print Package Labels")
        printItem?.setOnMenuItemClickListener {
            showPackageDialog()
            true  // Return true to indicate that the menu item click has been handled
        }

        return true
    }


    private fun showPackageDialog() {
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)
        val packageNames = mutableListOf<String>()  // To hold package names

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val packages = odooXmlRpcClient.fetchResultPackagesByPickingId(packId)
                withContext(Dispatchers.Main) {
                    packages.forEach { packageInfo ->
                        packageNames.add(packageInfo.name)
                    }
                    displayPackagesDialog(packageNames, vibrator)
                }
            } catch (e: Exception) {
                Log.e("PackageMenu", "Error fetching packages: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PackProductsActivity, "Failed to fetch packages", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun displayPackagesDialog(packageNames: List<String>, vibrator: Vibrator?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_packages, null)
        val layout = dialogView.findViewById<LinearLayout>(R.id.linear_layout_packages)

        packageNames.forEach { packageName ->
            // Inflate CardView for each package item
            val cardView = LayoutInflater.from(this).inflate(R.layout.package_item, layout, false) as CardView
            val textView = cardView.findViewById<TextView>(R.id.package_item_text_view)
            textView.text = packageName
            textView.setOnClickListener {
                printPackage(packageName)
                vibrateDevice(vibrator)
            }
            layout.addView(cardView)
        }

        // Initialize the dialog before setting up the button listener
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Show the dialog before setting its width to 90% of the screen
        dialog.show()

        // Get the current window of the dialog and set the width to 90% of the screen
        val window = dialog.window
        val params = window?.attributes
        params?.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        window?.attributes = params

        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancelQuantity)
        cancelButton.setOnClickListener {
            // Properly dismiss the dialog
            dialog.dismiss()
        }
    }

    private inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }

    class MoveLineDiffCallback(
        private val oldList: List<MoveLineOutGoing>,
        private val newList: List<MoveLineOutGoing>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].lineId == newList[newItemPosition].lineId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

}
