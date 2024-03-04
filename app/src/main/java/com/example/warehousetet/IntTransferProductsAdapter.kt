package com.example.warehousetet

import IntTransferProducts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IntTransferProductsAdapter(private val productList: List<IntTransferProducts>) :
    RecyclerView.Adapter<IntTransferProductsAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.findViewById(R.id.textView_productName)
        private val quantityTextView: TextView = view.findViewById(R.id.textView_quantity)
        private val transferDateTextView: TextView = view.findViewById(R.id.textView_transferDate)

        fun bind(product: IntTransferProducts) {
            nameTextView.text = product.name
            quantityTextView.text = "Quantity: ${product.quantity}"
            transferDateTextView.text = "Transfer Date: ${product.transferDate}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_int_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size
}