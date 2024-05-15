//package com.example.warehousetet
//
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.card.MaterialCardView
//
//class ProductsAdapter(
//    var moveLines: List<ReceiptMoveLine>,
//    private var quantityMatches: Map<ProductReceiptKey, Boolean>,
//    private var receiptId: Int,
//    private val listener: OnProductClickListener
//) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {
//
//    interface OnProductClickListener {
//        fun onProductClick(product: ReceiptMoveLine)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
//        return ProductViewHolder(view, listener)
//    }
//
//    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
//        val product = moveLines[position]
//        val key = ProductReceiptKey(product.id, receiptId)
//        val isMatched = quantityMatches[key] ?: false
//        holder.bind(product, isMatched)
//    }
//
//    override fun getItemCount(): Int = moveLines.size
//
//    fun updateProducts(newProducts: List<ReceiptMoveLine>, newReceiptId: Int, newQuantityMatches: Map<ProductReceiptKey, Boolean>) {
//        this.moveLines = newProducts
//        this.receiptId = newReceiptId
//        this.quantityMatches = newQuantityMatches
//        notifyDataSetChanged()
//    }
//
//    fun findProductPositionById(productId: Int): Int {
//        return moveLines.indexOfFirst { it.id == productId }
//    }
//
//    class ProductViewHolder(itemView: View, private val listener: ProductsAdapter.OnProductClickListener) : RecyclerView.ViewHolder(itemView) {
//        private val nameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
//        private val quantityTextView: TextView = itemView.findViewById(R.id.productQuantityTextView)
//        private val productQuantityTextView: TextView = itemView.findViewById(R.id.productQuantityTextView)
//        private val productDestinationLocationTextView: TextView = itemView.findViewById(R.id.productDestinationLocationTextView)
//        private val productLotSerialNumberTextView: TextView = itemView.findViewById(R.id.productLotSerialNumberTextView)
//        private val productUomQtyTextView: TextView = itemView.findViewById(R.id.productUomQtyTextView)
//        private val productTotalQuantTextView: TextView = itemView.findViewById(R.id.productTotalQuantTextView)
//
//        private val cardView: MaterialCardView = itemView.findViewById(R.id.productItemCard) // Assuming you have MaterialCardView as the root of your item layout
//
//    fun bind(moveLine: ReceiptMoveLine, matches: Boolean) {
//        nameTextView.text = moveLine.productName
//        productQuantityTextView.text = "Quantity: ${moveLine.quantity}"
//        productDestinationLocationTextView.text = "To: ${moveLine.locationDestName}"
//        productLotSerialNumberTextView.text = "Lot/Serial number: ${moveLine.lotName}"
//        productUomQtyTextView.text = "Expected Quantity: ${moveLine.expectedQuantity}"
//        productTotalQuantTextView.text = "Total Current Quantity: ${moveLine.totalQuantity}"
//
//        productLotSerialNumberTextView.visibility = if (moveLine.trackingType == "none" || moveLine.lotName.isEmpty()) View.GONE else View.VISIBLE
//
//        // Set text color to white for all TextViews
//        val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
//        nameTextView.setTextColor(whiteColor)
//        productQuantityTextView.setTextColor(whiteColor)
//        productDestinationLocationTextView.setTextColor(whiteColor)
//        productLotSerialNumberTextView.setTextColor(whiteColor)
//        productUomQtyTextView.setTextColor(whiteColor)
//        productTotalQuantTextView.setTextColor(whiteColor)
//
//        val context = itemView.context
//        cardView.setCardBackgroundColor(
//            if (matches) ContextCompat.getColor(context, R.color.success_green)
//            else ContextCompat.getColor(context, R.color.cardGrey)
//        )
//
//        if (matches) {
//            itemView.setOnClickListener {
//                listener.onProductClick(moveLine)
//            }
//        } else {
//            itemView.setOnClickListener(null)
//            itemView.isClickable = false
//        }
//    }
//
//    }
//}
//
//
//
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

class ProductsAdapter(
    var moveLines: List<ReceiptMoveLine>,
    private var quantityMatches: Map<ProductReceiptKey, Boolean>,
    private var receiptId: Int,
    private val listener: OnProductClickListener
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    interface OnProductClickListener {
        fun onProductClick(product: ReceiptMoveLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = moveLines[position]
        val key = ProductReceiptKey(product.id, receiptId)
        val isMatched = quantityMatches[key] ?: false
        holder.bind(product, isMatched)
    }

    override fun getItemCount(): Int = moveLines.size

    fun updateProducts(newProducts: List<ReceiptMoveLine>, newReceiptId: Int, newQuantityMatches: Map<ProductReceiptKey, Boolean>) {
        val diffCallback = ProductDiffCallback(moveLines, newProducts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        moveLines = newProducts
        receiptId = newReceiptId
        quantityMatches = newQuantityMatches
        diffResult.dispatchUpdatesTo(this)
    }

    fun findProductPositionById(productId: Int): Int {
        return moveLines.indexOfFirst { it.id == productId }
    }

    class ProductViewHolder(itemView: View, private val listener: OnProductClickListener) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        private val productQuantityTextView: TextView = itemView.findViewById(R.id.productQuantityTextView)
        private val productDestinationLocationTextView: TextView = itemView.findViewById(R.id.productDestinationLocationTextView)
        private val productLotSerialNumberTextView: TextView = itemView.findViewById(R.id.productLotSerialNumberTextView)
        private val productUomQtyTextView: TextView = itemView.findViewById(R.id.productUomQtyTextView)
        private val productTotalQuantTextView: TextView = itemView.findViewById(R.id.productTotalQuantTextView)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.productItemCard)

        fun bind(moveLine: ReceiptMoveLine, matches: Boolean) {
            nameTextView.text = moveLine.productName
            productQuantityTextView.text = itemView.context.getString(R.string.quantity_text, moveLine.quantity)
            productDestinationLocationTextView.text = itemView.context.getString(R.string.destination_text, moveLine.locationDestName)
            productLotSerialNumberTextView.text = itemView.context.getString(R.string.lot_serial_text, moveLine.lotName)
            productUomQtyTextView.text = itemView.context.getString(R.string.expected_quantity_text, moveLine.expectedQuantity)
            productTotalQuantTextView.text = itemView.context.getString(R.string.total_quantity_text, moveLine.totalQuantity)

            productLotSerialNumberTextView.visibility = if (moveLine.trackingType == "none" || moveLine.lotName.isEmpty()) View.GONE else View.VISIBLE
            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
            nameTextView.setTextColor(whiteColor)
            productQuantityTextView.setTextColor(whiteColor)
            productDestinationLocationTextView.setTextColor(whiteColor)
            productLotSerialNumberTextView.setTextColor(whiteColor)
            productUomQtyTextView.setTextColor(whiteColor)
            productTotalQuantTextView.setTextColor(whiteColor)

            val context = itemView.context
            cardView.setCardBackgroundColor(if (matches) ContextCompat.getColor(context, R.color.success_green) else ContextCompat.getColor(context, R.color.cardGrey))

            itemView.setOnClickListener {
                if (matches) {
                    listener.onProductClick(moveLine)
                }
            }
        }

    }
}

class ProductDiffCallback(
    private val oldList: List<ReceiptMoveLine>,
    private val newList: List<ReceiptMoveLine>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}
