package com.example.warehousetet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class IntTransferProductsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_int_products)

        val productList = listOf(
            IntTransferProducts("Chair", 35.0, "2024-02-27 (Today)"),
            // Add more products as needed
        )

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView_internal_transfers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = IntTransferProductsAdapter(productList)
    }

}