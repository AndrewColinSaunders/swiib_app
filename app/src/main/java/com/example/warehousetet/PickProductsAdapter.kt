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
//    var lines: List<MoveLine>,
//    private var quantityMatches: Map<ProductPickKey, Boolean>,
//    private var trackingTypes: Map<Int, String>,
//    private var pickId: Int,
//    private val listener: OnProductClickListener
//) : RecyclerView.Adapter<PickProductsAdapter.ProductViewHolder>() {
//
//    interface OnProductClickListener {
//        fun onProductClick(product: MoveLine)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_product_item, parent, false)
//        return ProductViewHolder(view, listener)
//    }
//
//    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
//        val product = lines[position]
//        val key = ProductPickKey(product.id, pickId)
//        val isMatched = quantityMatches[key] ?: false
//        val trackingType = trackingTypes[product.productId] ?: "none"
//        holder.bind(product, isMatched)
//    }
//
//    override fun getItemCount(): Int = lines.size
//
//    fun updateProducts(newProducts: List<MoveLine>, newDeliveryOrderId: Int, newQuantityMatches: Map<ProductPickKey, Boolean>) {
//        this.lines = newProducts
//        this.pickId = newDeliveryOrderId
//        this.quantityMatches = newQuantityMatches
//        notifyDataSetChanged()
//    }
//
//    fun findProductPositionById(productId: Int): Int {
//        return lines.indexOfFirst { it.id == productId }
//    }
//
//    class ProductViewHolder(itemView: View, private val listener: OnProductClickListener) : RecyclerView.ViewHolder(itemView) {
//        private val nameTextView: TextView = itemView.findViewById(R.id.pickProductNameTextView)
//        private val quantityTextView: TextView = itemView.findViewById(R.id.pickProductQuantityTextView)
//        private val locationTextView: TextView = itemView.findViewById(R.id.pickProductLocationTextView)
//        private val cardView: MaterialCardView = itemView.findViewById(R.id.doProductItemCard)
//        private val pickProductLotView: TextView = itemView.findViewById(R.id.pickProductLotView)
//        private val pickProductDestLocationView: TextView = itemView.findViewById(R.id.pickProductDestLocationView)
//
//        fun bind(moveline: MoveLine, matches: Boolean ) {
//            nameTextView.text = moveline.productName
//            quantityTextView.text = "Quantity: ${moveline.quantity}"
//            locationTextView.text = "From Location: ${moveline.locationName}"
//            pickProductDestLocationView.text = "To Location: ${moveline.locationDestName}"
//            pickProductLotView.text = "Lot/Serial Number: ${moveline.lotName}"
//
//            pickProductLotView.visibility = if (moveline.trackingType == "none" || moveline.lotName.isEmpty()) View.GONE else View.VISIBLE
//
//            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
//            nameTextView.setTextColor(whiteColor)
//            quantityTextView.setTextColor(whiteColor)
//            locationTextView.setTextColor(whiteColor)
//            pickProductLotView.setTextColor(whiteColor)
//            pickProductDestLocationView.setTextColor(whiteColor)
//
//            cardView.setCardBackgroundColor(
//                if (matches) ContextCompat.getColor(itemView.context, R.color.success_green)
//                else ContextCompat.getColor(itemView.context, R.color.cardGrey)
//            )
//
//            if (matches) {
//                itemView.setOnClickListener {
//                    listener.onProductClick(moveline)
//                }
//                itemView.isClickable = true
//            } else {
//                itemView.setOnClickListener(null)
//                itemView.isClickable = false
//            }
//        }
//    }
//}



//
//
//package com.example.warehousetet
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.card.MaterialCardView
//
//class PickProductsAdapter(
//    var lines: List<MoveLine>,
//    private var quantityMatches: Map<ProductPickKey, Boolean>,
//    private var trackingTypes: Map<Int, String>,
//    private var pickId: Int,
//    private val listener: OnProductClickListener
//) : RecyclerView.Adapter<PickProductsAdapter.ProductViewHolder>() {
//
//    interface OnProductClickListener {
//        fun onProductClick(product: MoveLine)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_product_item, parent, false)
//        return ProductViewHolder(view, listener)
//    }
//
//    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
//        val product = lines[position]
//        val key = ProductPickKey(product.id, pickId)
//        val isMatched = quantityMatches[key] ?: false
//        holder.bind(product, isMatched)
//    }
//
//    override fun getItemCount(): Int = lines.size
//
//    fun updateProducts(newProducts: List<MoveLine>, newPickId: Int, newQuantityMatches: Map<ProductPickKey, Boolean>) {
//        val diffCallback = MoveLineDiffCallback(lines, newProducts)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//
//        lines = newProducts
//        pickId = newPickId
//        quantityMatches = newQuantityMatches
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    fun findProductPositionById(productId: Int): Int {
//        return lines.indexOfFirst { it.id == productId }
//    }
//
//    class ProductViewHolder(itemView: View, private val listener: OnProductClickListener) : RecyclerView.ViewHolder(itemView) {
//        private val nameTextView: TextView = itemView.findViewById(R.id.pickProductNameTextView)
//        private val quantityTextView: TextView = itemView.findViewById(R.id.pickProductQuantityTextView)
//        private val locationTextView: TextView = itemView.findViewById(R.id.pickProductLocationTextView)
//        private val cardView: MaterialCardView = itemView.findViewById(R.id.doProductItemCard)
//        private val pickProductLotView: TextView = itemView.findViewById(R.id.pickProductLotView)
//        private val pickProductDestLocationView: TextView = itemView.findViewById(R.id.pickProductDestLocationView)
//
//        fun bind(moveline: MoveLine, matches: Boolean) {
//            nameTextView.text = moveline.productName
//            quantityTextView.text = "Quantity: ${moveline.quantity}"
//            locationTextView.text = "From Location: ${moveline.locationName}"
//            pickProductDestLocationView.text = "To Location: ${moveline.locationDestName}"
//            pickProductLotView.text = "Lot/Serial Number: ${moveline.lotName}"
//
//            pickProductLotView.visibility = if (moveline.trackingType == "none" || moveline.lotName.isEmpty()) View.GONE else View.VISIBLE
//
//            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
//            nameTextView.setTextColor(whiteColor)
//            quantityTextView.setTextColor(whiteColor)
//            locationTextView.setTextColor(whiteColor)
//            pickProductLotView.setTextColor(whiteColor)
//            pickProductDestLocationView.setTextColor(whiteColor)
//
//            cardView.setCardBackgroundColor(
//                if (matches) ContextCompat.getColor(itemView.context, R.color.success_green)
//                else ContextCompat.getColor(itemView.context, R.color.cardGrey)
//            )
//
//            if (matches) {
//                itemView.setOnClickListener {
//                    listener.onProductClick(moveline)
//                }
//                itemView.isClickable = true
//            } else {
//                itemView.setOnClickListener(null)
//                itemView.isClickable = false
//            }
//        }
//    }
//}
//
//class MoveLineDiffCallback(
//    private val oldList: List<MoveLine>,
//    private val newList: List<MoveLine>
//) : DiffUtil.Callback() {
//    override fun getOldListSize(): Int = oldList.size
//    override fun getNewListSize(): Int = newList.size
//
//    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
//        oldList[oldItemPosition].id == newList[newItemPosition].id
//
//    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
//        oldList[oldItemPosition] == newList[newItemPosition]
//}
//



package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class PickProductsAdapter(
    var lines: List<MoveLine>,
    private var quantityMatches: Map<ProductPickKey, Boolean>,
    private var trackingTypes: Map<Int, String>,
    private var pickId: Int,
    private val listener: OnProductClickListener
) : RecyclerView.Adapter<PickProductsAdapter.ProductViewHolder>() {

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
        holder.bind(product, isMatched)
    }

    override fun getItemCount(): Int = lines.size

    fun updateProducts(newProducts: List<MoveLine>, newPickId: Int, newQuantityMatches: Map<ProductPickKey, Boolean>) {
        val diffCallback = MoveLineDiffCallback(lines, newProducts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        lines = newProducts
        pickId = newPickId
        quantityMatches = newQuantityMatches
        diffResult.dispatchUpdatesTo(this)
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

        fun bind(moveline: MoveLine, matches: Boolean) {
            val context = itemView.context
            nameTextView.text = moveline.productName
            quantityTextView.text = context.getString(R.string.pick_product_quantity_text, moveline.quantity)
            locationTextView.text = context.getString(R.string.pick_product_from_location_text, moveline.locationName)
            pickProductDestLocationView.text = context.getString(R.string.pick_product_to_location_text, moveline.locationDestName)
            pickProductLotView.text = context.getString(R.string.pick_product_lot_serial_number_text, moveline.lotName)

            pickProductLotView.visibility = if (moveline.trackingType == "none" || moveline.lotName.isEmpty()) View.GONE else View.VISIBLE

            val whiteColor = ContextCompat.getColor(context, android.R.color.white)
            nameTextView.setTextColor(whiteColor)
            quantityTextView.setTextColor(whiteColor)
            locationTextView.setTextColor(whiteColor)
            pickProductLotView.setTextColor(whiteColor)
            pickProductDestLocationView.setTextColor(whiteColor)

            cardView.setCardBackgroundColor(
                if (matches) ContextCompat.getColor(context, R.color.success_green)
                else ContextCompat.getColor(context, R.color.cardGrey)
            )

            if (matches) {
                itemView.setOnClickListener {
                    listener.onProductClick(moveline)
                }
                itemView.isClickable = true
            } else {
                itemView.setOnClickListener(null)
                itemView.isClickable = false
            }
        }
    }
}

class MoveLineDiffCallback(
    private val oldList: List<MoveLine>,
    private val newList: List<MoveLine>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}
