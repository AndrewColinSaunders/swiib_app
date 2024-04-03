//package com.example.warehousetet
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.card.MaterialCardView
//
//class PickProductsAdapter(
//        var products: List<Product>,
//        private  var quantityMatches: Map<ProductDOKey, Boolean>,
//        private  var deliveryOrderId: Int
//) : RecyclerView.Adapter<PickProductsAdapter.ProductViewHolder>() {
//
////    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
////        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_product_item, parent, false)
////        return ProductViewHolder(view)
////    }
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickProductsAdapter.ProductViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_product_item, parent, false)
//        return PickProductsAdapter.ProductViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ProductsAdapter.ProductViewHolder, position: Int) {
//        val product = products[position]
//        val key = ProductDOKey(product.id, deliveryOrderId)
//        val isMatched = quantityMatches[key] ?: false
//        holder.bind(product, isMatched)
//    }
//
//    override fun getItemCount(): Int = products.size
//
////    fun updateProducts(newProducts: List<Product>, newDeliveryOrderId: Int, newQuantityMatches: Map<ProductDOKey, Boolean>) {
////        products = newProducts
////        notifyDataSetChanged()
////    }
//
//    fun updateProducts(newProducts: List<Product>, newDeliveryOrderId: Int, newQuantityMatches: Map<ProductDOKey, Boolean>) {
//        this.products = newProducts
//        this.deliveryOrderId = newDeliveryOrderId
//        this.quantityMatches = newQuantityMatches
//        notifyDataSetChanged()
//    }
//    fun findProductPositionById(productId: Int): Int {
//        return products.indexOfFirst { it.id == productId }
//    }
////    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
////        private val nameTextView: TextView = itemView.findViewById(R.id.doProductNameTextView)
////        private val quantityTextView: TextView = itemView.findViewById(R.id.doProductQuantityTextView)
////
////        fun bind(product: Product) {
////            nameTextView.text = product.name
////            quantityTextView.text = "Quantity: ${product.quantity}"
////        }
////    }
//class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//    private val nameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
//    private val quantityTextView: TextView = itemView.findViewById(R.id.productQuantityTextView)
//    private val idTextView: TextView = itemView.findViewById(R.id.productIdTextView)
//    private val trackingTypeTextView: TextView = itemView.findViewById(R.id.productTrackingTypeTextView)
//    private val cardView: MaterialCardView = itemView.findViewById(R.id.productItemCard) // Assuming you have MaterialCardView as the root of your item layout
//
//    fun bind(product: Product, matches: Boolean) {
//        nameTextView.text = product.name
//        quantityTextView.text = "Quantity: ${product.quantity}"
//        idTextView.text = "ID: ${product.id}"
//        trackingTypeTextView.text = "Tracking Type: ${product.trackingType ?: "N/A"}"
//
//        // Set text color to white for all TextViews
//        val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white) // or R.color.white if you have it defined
//        nameTextView.setTextColor(whiteColor)
//        quantityTextView.setTextColor(whiteColor)
//        idTextView.setTextColor(whiteColor)
//        trackingTypeTextView.setTextColor(whiteColor)
//
//        val context = itemView.context // Use itemView's context to ensure correct resource access
//        cardView.setCardBackgroundColor(
//            if (matches) ContextCompat.getColor(context, R.color.success_green) // Use your success_green color
//            else ContextCompat.getColor(context, R.color.cardGrey) // Default or original card background color
//        )
//
//    }
//}
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
    var products: List<Product>,
    private var quantityMatches: Map<ProductDOKey, Boolean>,
    private var deliveryOrderId: Int
) : RecyclerView.Adapter<PickProductsAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        val key = ProductDOKey(product.id, deliveryOrderId)
        val isMatched = quantityMatches[key] ?: false
        holder.bind(product, isMatched)
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>, newDeliveryOrderId: Int, newQuantityMatches: Map<ProductDOKey, Boolean>) {
        this.products = newProducts
        this.deliveryOrderId = newDeliveryOrderId
        this.quantityMatches = newQuantityMatches
        notifyDataSetChanged()
    }

    fun findProductPositionById(productId: Int): Int {
        return products.indexOfFirst { it.id == productId }
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.doProductNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.doProductQuantityTextView)
//        private val idTextView: TextView = itemView.findViewById(R.id.productIdTextView)
//        private val trackingTypeTextView: TextView = itemView.findViewById(R.id.productTrackingTypeTextView)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.doProductItemCard) // Assuming you have MaterialCardView as the root of your item layout

        fun bind(product: Product, matches: Boolean) {
            Log.d("DOProductAdapter", "Binding product: ${product.name}, Matched: $matches")
            nameTextView.text = product.name
            quantityTextView.text = "Quantity: ${product.quantity}"
//            idTextView.text = "ID: ${product.id}"
//            trackingTypeTextView.text = "Tracking Type: ${product.trackingType ?: "N/A"}"

            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
            nameTextView.setTextColor(whiteColor)
            quantityTextView.setTextColor(whiteColor)
//            idTextView.setTextColor(whiteColor)
//            trackingTypeTextView.setTextColor(whiteColor)

            val context = itemView.context
            cardView.setCardBackgroundColor(
                if (matches) ContextCompat.getColor(context, R.color.success_green)
                else ContextCompat.getColor(context, R.color.cardGrey)
            )
        }
    }
}
