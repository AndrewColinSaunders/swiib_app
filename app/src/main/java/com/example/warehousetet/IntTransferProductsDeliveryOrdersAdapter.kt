package com.example.warehousetet

import IntTransferProducts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class IntTransferProductsDeliveryOrdersAdapter(
    private val productList: List<IntTransferProducts>,
    private val allItemsShouldBeGreen: Boolean
) : RecyclerView.Adapter<IntTransferProductsDeliveryOrdersAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_int_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.bind(
            product,
            product.isScanned || allItemsShouldBeGreen
        ) // Adjust based on isVerified property.
    }

    override fun getItemCount(): Int = productList.size

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.findViewById(R.id.textView_productName)
        private val quantityTextView: TextView = view.findViewById(R.id.textView_quantity)
        private val transferDateTextView: TextView = view.findViewById(R.id.textView_transferDate)
        private val cardView: CardView = view as CardView

        fun bind(product: IntTransferProducts, isVerified: Boolean) {
            nameTextView.text = product.name
            quantityTextView.text = "Quantity: ${product.quantity}"
            transferDateTextView.text = "Transfer Date: ${product.transferDate}"

            cardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    cardView.context,
                    if (isVerified) R.color.success_green else R.color.white
                )
            )
        }
    }
}