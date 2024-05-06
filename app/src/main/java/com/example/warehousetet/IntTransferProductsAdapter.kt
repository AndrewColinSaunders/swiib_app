
package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class IntTransferProductsAdapter(
    var lines: List<MoveLine>,
    private var quantityMatches: Map<ProductPickKey, Boolean>,
    private var trackingTypes: Map<Int, String>,
    private var pickId: Int,
    private val listener: OnProductClickListener
) : RecyclerView.Adapter<IntTransferProductsAdapter.ProductViewHolder>() {

    interface OnProductClickListener {
        fun onProductClick(product: MoveLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_product_item, parent, false)
        return ProductViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = lines[position]
        val key = ProductPickKey(product.id, pickId)
        val isMatched = quantityMatches[key] ?: false
        val trackingType = trackingTypes[product.productId] ?: "none"
        holder.bind(product, isMatched)
    }

    override fun getItemCount(): Int = lines.size

    fun updateProducts(newProducts: List<MoveLine>, newDeliveryOrderId: Int, newQuantityMatches: Map<ProductPickKey, Boolean>) {
        this.lines = newProducts
        this.pickId = newDeliveryOrderId
        this.quantityMatches = newQuantityMatches
        notifyDataSetChanged()
    }



    fun findProductPositionById(productId: Int): Int {
        return lines.indexOfFirst { it.id == productId }
    }

    class ProductViewHolder(itemView: View, private val listener: OnProductClickListener) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.pickProductNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.pickProductQuantityTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.pickProductLocationTextView)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.doProductItemCard)
        private val pickProductLotView: TextView = itemView.findViewById(R.id.pickProductLotView)
        private val pickProductDestLocationView: TextView = itemView.findViewById(R.id.pickProductDestLocationView)

        fun bind(moveLine: MoveLine, matches: Boolean) {
            nameTextView.text = moveLine.productName
            quantityTextView.text = "Quantity: ${moveLine.quantity}"
            locationTextView.text = "From Location: ${moveLine.locationName}"
            pickProductDestLocationView.text = "To Location: ${moveLine.locationDestName}"
            pickProductLotView.text = "Lot/Serial Number: ${moveLine.lotName}"

            pickProductLotView.visibility = if (moveLine.trackingType == "none" || moveLine.lotName.isEmpty()) View.GONE else View.VISIBLE

            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
            nameTextView.setTextColor(whiteColor)
            quantityTextView.setTextColor(whiteColor)
            locationTextView.setTextColor(whiteColor)
            pickProductLotView.setTextColor(whiteColor)
            pickProductDestLocationView.setTextColor(whiteColor)

            cardView.setCardBackgroundColor(
                if (matches) ContextCompat.getColor(itemView.context, R.color.success_green)
                else ContextCompat.getColor(itemView.context, R.color.cardGrey)
            )

            // Set click listeners based on matches
            if (matches) {
                itemView.setOnClickListener {
                    listener.onProductClick(moveLine)
                }
                itemView.isClickable = true
            } else {
                itemView.setOnClickListener(null)
                itemView.isClickable = false
            }
        }
    }

}
