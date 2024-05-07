//package com.example.warehousetet
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//
//class ReceiptsAdapter(
//    private var receipts: List<Receipt>,
//    private val onReceiptClicked: (Receipt) -> Unit // Lambda function to handle click events on Receipt items
//) : RecyclerView.Adapter<ReceiptsAdapter.ViewHolder>() {
//
//    // This function is called when the RecyclerView needs a new ViewHolder instance.
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        // Inflate the layout for a single item view and return a new ViewHolder instance.
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false)
//        return ViewHolder(view, onReceiptClicked) // Create and return a new ViewHolder instance
//    }
//
//    // This function is called to bind data to a ViewHolder at a specific position.
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(receipts[position]) // Bind data to the ViewHolder at the specified position
//    }
//
//    // This function returns the total number of items in the list.
//    override fun getItemCount(): Int = receipts.size
//
//    // Inner ViewHolder class to hold references to views within each RecyclerView item.
//    inner class ViewHolder(itemView: View, private val onReceiptClicked: (Receipt) -> Unit) : RecyclerView.ViewHolder(itemView) {
//        // References to TextViews within the layout
//        private val receiptNameTextView: TextView = itemView.findViewById(R.id.receiptNameTextView)
//        private val receiptDateTextView: TextView = itemView.findViewById(R.id.receiptDateTextView)
//        private val receiptOriginTextView: TextView = itemView.findViewById(R.id.receiptOriginTextView)
//
//        // Initialization block where click listener is set up for the itemView
//        init {
//            itemView.setOnClickListener {
//                val position = adapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    // Call the lambda function to handle click event on the receipt item
//                    onReceiptClicked(receipts[position])
//                }
//            }
//        }
//
//        // Function to bind data to the ViewHolder
//        fun bind(receipt: Receipt) {
//            // Set the text of TextViews to display receipt data
//            receiptNameTextView.text = receipt.name
//            receiptDateTextView.text = receipt.date
//            receiptOriginTextView.text = receipt.origin
//        }
//    }
//
//    // Function to update the list of receipts and notify the adapter of the change
//    fun updateReceipts(newReceipts: List<Receipt>) {
//        receipts = newReceipts // Update the list of receipts
//        notifyDataSetChanged() // Notify the adapter that the data set has changed
//    }
//
//
//}
package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReceiptsAdapter(
    private var receipts: List<Receipt>,
    private val onReceiptClicked: (Receipt) -> Unit
) : RecyclerView.Adapter<ReceiptsAdapter.ViewHolder>() {

    private var filteredReceipts: List<Receipt> = ArrayList(receipts)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false)
        return ViewHolder(view, onReceiptClicked)
    }

    override fun getItemCount(): Int = filteredReceipts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredReceipts[position])
    }

    fun filter(query: String) {
        filteredReceipts = if (query.isEmpty()) {
            receipts
        } else {
            receipts.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateReceipts(newReceipts: List<Receipt>) {
        receipts = newReceipts
        filteredReceipts = newReceipts
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View, private val onReceiptClicked: (Receipt) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val receiptNameTextView: TextView = itemView.findViewById(R.id.receiptNameTextView)
        private val receiptDateTextView: TextView = itemView.findViewById(R.id.receiptDateTextView)
        private val receiptOriginTextView: TextView = itemView.findViewById(R.id.receiptOriginTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onReceiptClicked(filteredReceipts[position])
                }
            }
        }

        fun bind(receipt: Receipt) {
            receiptNameTextView.text = receipt.name
            receiptDateTextView.text = receipt.date
            receiptOriginTextView.text = receipt.origin
        }
    }
}



