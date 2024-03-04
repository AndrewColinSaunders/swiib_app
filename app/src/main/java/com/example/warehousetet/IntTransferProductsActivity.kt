package com.example.warehousetet

import IntTransferProducts
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class IntTransferProductsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_int_products)

        val products: ArrayList<IntTransferProducts> = intent.getParcelableArrayListExtra<IntTransferProducts>("EXTRA_PRODUCTS") ?: arrayListOf()

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView_internal_transfers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = IntTransferProductsAdapter(products)
    }
}