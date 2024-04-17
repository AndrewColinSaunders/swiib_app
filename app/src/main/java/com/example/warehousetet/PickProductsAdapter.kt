//package com.example.warehousetet
//
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.card.MaterialCardView
//
//class PickProductsAdapter(
//    var products: List<Product>,
//    private var quantityMatches: Map<ProductPickKey, Boolean>,
//    private var pickId: Int
//) : RecyclerView.Adapter<PickProductsAdapter.ProductViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_product_item, parent, false)
//        return ProductViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
//        val product = products[position]
//        val key = ProductPickKey(product.id, pickId)
//        val isMatched = quantityMatches[key] ?: false
//        holder.bind(product, isMatched)
//    }
//
//    override fun getItemCount(): Int = products.size
//
//    fun updateProducts(newProducts: List<Product>, newDeliveryOrderId: Int, newQuantityMatches: Map<ProductPickKey, Boolean>) {
//        this.products = newProducts
//        this.pickId = newDeliveryOrderId
//        this.quantityMatches = newQuantityMatches
//        notifyDataSetChanged()
//    }
//
//    fun findProductPositionById(productId: Int): Int {
//        return products.indexOfFirst { it.id == productId }
//    }
//
//    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val nameTextView: TextView = itemView.findViewById(R.id.pickProductNameTextView)
//        private val quantityTextView: TextView = itemView.findViewById(R.id.pickProductQuantityTextView)
////        private val idTextView: TextView = itemView.findViewById(R.id.productIdTextView)
//        private val trackingTypeTextView: TextView = itemView.findViewById(R.id.pickProductTrackingTypeTextView)
//        private val cardView: MaterialCardView = itemView.findViewById(R.id.doProductItemCard) // Assuming you have MaterialCardView as the root of your item layout
//
//        fun bind(product: Product, matches: Boolean) {
//            Log.d("DOProductAdapter", "Binding product: ${product.name}, Matched: $matches")
//            nameTextView.text = product.name
//            quantityTextView.text = "Quantity: ${product.quantity}"
////            idTextView.text = "ID: ${product.id}"
//            trackingTypeTextView.text = "Tracking Type: ${product.trackingType ?: "N/A"}"
//
//
//            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
//            nameTextView.setTextColor(whiteColor)
//            quantityTextView.setTextColor(whiteColor)
////            idTextView.setTextColor(whiteColor)
//            trackingTypeTextView.setTextColor(whiteColor)
//
//            val context = itemView.context
//            cardView.setCardBackgroundColor(
//                if (matches) ContextCompat.getColor(context, R.color.success_green)
//                else ContextCompat.getColor(context, R.color.cardGrey)
//            )
//        }
//    }
//}


package com.example.warehousetet

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class PickProductsAdapter(
    var lines: List<MoveLine>,
    private var quantityMatches: Map<ProductPickKey, Boolean>,
    private var pickId: Int
) : RecyclerView.Adapter<PickProductsAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = lines[position]
        val key = ProductPickKey(product.id, pickId)
        val isMatched = quantityMatches[key] ?: false
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

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.pickProductNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.pickProductQuantityTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.pickProductLocationTextView)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.doProductItemCard) // Assuming you have MaterialCardView as the root of your item layout
        private val pickProductLotView: TextView = itemView.findViewById(R.id.pickProductLotView)
        private val pickProductDestLocationView: TextView = itemView.findViewById(R.id.pickProductDestLocationView)
        fun bind(moveline: MoveLine, matches: Boolean) {
            Log.d("DOProductAdapter", "Binding product: ${moveline.productName}, Matched: $matches")
            nameTextView.text = moveline.productName
            quantityTextView.text = "Quantity: ${moveline.quantity}"
            locationTextView.text = "From Location: ${moveline.locationName}"
            pickProductDestLocationView.text = "To Location: ${moveline.locationDestName}"
            pickProductLotView.text = "Lot/Serial Number: ${moveline.lotName}"

            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
            nameTextView.setTextColor(whiteColor)
            quantityTextView.setTextColor(whiteColor)
            pickProductDestLocationView.setTextColor(whiteColor)
            locationTextView.setTextColor(whiteColor)
            pickProductLotView.setTextColor(whiteColor)

            val context = itemView.context
            cardView.setCardBackgroundColor(
                if (matches) ContextCompat.getColor(context, R.color.success_green)
                else ContextCompat.getColor(context, R.color.cardGrey)
            )
        }
    }
}
