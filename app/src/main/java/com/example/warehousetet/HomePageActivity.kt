package com.example.warehousetet

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomePageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        setupOptionButton(R.id.btnReceipt, "Receipt")
        setupOptionButton(R.id.btnInternalTransfers, "Internal Transfers")
        setupOptionButton(R.id.btnPick, "Pick")
        setupOptionButton(R.id.btnPack, "Pack")
        setupOptionButton(R.id.btnDeliveryOrders, "Delivery Orders")
        setupOptionButton(R.id.btnReturns, "Returns")

        findViewById<Button>(R.id.btnReceipt).setOnClickListener {
            Intent(this, ReceiptsActivity::class.java).also { startActivity(it) }
        }

    }

    private fun setupOptionButton(buttonId: Int, optionName: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            Toast.makeText(this, "$optionName clicked", Toast.LENGTH_SHORT).show()
            // Here you can start new activities or perform navigation based on the option
        }
    }
}
