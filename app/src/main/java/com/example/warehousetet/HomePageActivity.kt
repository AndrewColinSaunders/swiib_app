package com.example.warehousetet

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomePageActivity : AppCompatActivity() {

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_home_page)
//
//        setupOptionButton(R.id.btnReceipt, "Receipt")
//        setupOptionButton(R.id.btnInternalTransfers, "Internal Transfers")
//        setupOptionButton(R.id.btnPick, "Pick")
//        setupOptionButton(R.id.btnPack, "Pack")
//        setupOptionButton(R.id.btnDeliveryOrders, "Delivery Orders")
//        setupOptionButton(R.id.btnReturns, "Returns")
//
//        // Modified ClickListener for btnReceipt with haptic feedback
//        findViewById<Button>(R.id.btnReceipt).setOnClickListener {
//            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
//            Intent(this, ReceiptsActivity::class.java).also { startActivity(it) }
//        }
//
//        // Modified ClickListener for btnInternalTransfers with haptic feedback
//        findViewById<Button>(R.id.btnInternalTransfers).setOnClickListener {
//            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
//            Intent(this, InternalTransfersActivity::class.java).also { startActivity(it) }
//        }
//
//        // Modified ClickListener for btnInternalTransfers with haptic feedback
//        findViewById<Button>(R.id.btnPick).setOnClickListener {
//            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
//            Intent(this, PickActivity::class.java).also { startActivity(it) }
//        }
//
//        // Modified ClickListener for btnInternalTransfers with haptic feedback
//        findViewById<Button>(R.id.btnPack).setOnClickListener {
//            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
//            Intent(this, PackActivity::class.java).also { startActivity(it) }
//        }
//
//    }

    //current
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)


//    // Set ActionBar background color
//    supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#212d40"))) // Use the color #212d40
//
//    // Change the status bar color to match the ActionBar
//    window.statusBarColor = ContextCompat.getColor(this, R.color.cardGrey) // Use the color defined as cardGrey

        setupOptionButton(R.id.btnReceipt, "Receipt")
        setupOptionButton(R.id.btnInternalTransfers, "Internal Transfers")
        setupOptionButton(R.id.btnPick, "Pick")
        setupOptionButton(R.id.btnPack, "Pack")
        setupOptionButton(R.id.btnDeliveryOrders, "Delivery Orders")
        setupOptionButton(R.id.btnReturns, "Returns")





        // ClickListeners for buttons
        findViewById<Button>(R.id.btnReceipt).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Intent(this, ReceiptsActivity::class.java).also { startActivity(it) }
        }


        findViewById<Button>(R.id.btnInternalTransfers).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Intent(this, InternalTransfersActivity::class.java).also { startActivity(it) }
        }

        findViewById<Button>(R.id.btnPick).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Intent(this, PickActivity::class.java).also { startActivity(it) }
        }

        findViewById<Button>(R.id.btnPack).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Intent(this, PackActivity::class.java).also { startActivity(it) }
        }

        findViewById<Button>(R.id.btnDeliveryOrders).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Intent(this, DeliveryOrdersActivity::class.java).also { startActivity(it) }
        }

    }


        //    private fun setupOptionButton(buttonId: Int, optionName: String) {
    //        findViewById<Button>(buttonId).setOnClickListener {
    //            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) // Added haptic feedback
    //            Toast.makeText(this, "$optionName clicked", Toast.LENGTH_SHORT).show()
    //            // Here you can start new activities or perform navigation based on the option
    //        }
    //    }
        private fun setupOptionButton(buttonId: Int, optionName: String) {
            // Find the button by its ID
            val button = findViewById<Button>(buttonId)

            // Set the button's background color to #BDBDBD
            val color = Color.parseColor("#212d40") // Parse the color string
            button.backgroundTintList = ColorStateList.valueOf(color) // Apply the color

            // Set a click listener for the button
            button.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                Toast.makeText(this, "$optionName clicked", Toast.LENGTH_SHORT).show()
                // Add additional logic here for navigating to different activities
        }
    }
}
