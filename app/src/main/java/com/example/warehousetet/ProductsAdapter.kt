package com.example.warehousetet

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductsAdapter(private var products: List<Product>) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.productQuantityTextView)

        fun bind(product: Product) {
            nameTextView.text = product.name
            quantityTextView.text = "Quantity: ${product.quantity}"

            // Set click listener
            itemView.setOnClickListener {
                val context = it.context
                val intent = Intent(context, ProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_NAME", product.name)
                    putExtra("PRODUCT_QUANTITY", product.quantity)
                }
                context.startActivity(intent)
            }
        }
    }

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
