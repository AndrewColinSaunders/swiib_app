//package com.example.warehousetet
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.RecyclerView
//
//class ReceiptsAdapter(
//    receipts: List<Receipt>,
//    private val onReceiptClicked: (Receipt) -> Unit
//) : RecyclerView.Adapter<ReceiptsAdapter.ViewHolder>() {
//
//    private var receipts: MutableList<Receipt> = receipts.toMutableList()
//    private var filteredReceipts: MutableList<Receipt> = receipts.toMutableList()
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false)
//        return ViewHolder(view, onReceiptClicked)
//    }
//
//    override fun getItemCount(): Int = filteredReceipts.size
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(filteredReceipts[position])
//    }
//
//    fun filter(query: String) {
//        val result = if (query.isEmpty()) {
//            receipts.toList() // Make a copy of the original list
//        } else {
//            receipts.filter { it.name.contains(query, ignoreCase = true) }
//        }
//
//        val diffCallback = ReceiptDiffCallback(filteredReceipts, result)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//
//        filteredReceipts.clear()
//        filteredReceipts.addAll(result)
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    fun updateReceipts(newReceipts: List<Receipt>) {
//        val diffCallback = ReceiptDiffCallback(receipts, newReceipts)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//
//        receipts.clear()
//        receipts.addAll(newReceipts)
//        filteredReceipts.clear()
//        filteredReceipts.addAll(newReceipts)
//
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    inner class ViewHolder(itemView: View, private val onReceiptClicked: (Receipt) -> Unit) : RecyclerView.ViewHolder(itemView) {
//        private val receiptNameTextView: TextView = itemView.findViewById(R.id.receiptNameTextView)
//        private val receiptDateTextView: TextView = itemView.findViewById(R.id.receiptDateTextView)
//        private val receiptOriginTextView: TextView = itemView.findViewById(R.id.receiptOriginTextView)
//
//        init {
//            itemView.setOnClickListener {
//                val position = adapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    onReceiptClicked(filteredReceipts[position])
//                }
//            }
//        }
//
//        fun bind(receipt: Receipt) {
//            receiptNameTextView.text = receipt.name
//            receiptDateTextView.text = receipt.date
//            receiptOriginTextView.text = receipt.origin
//        }
//    }
//}
//
//class ReceiptDiffCallback(
//    private val oldList: List<Receipt>,
//    private val newList: List<Receipt>
//) : DiffUtil.Callback() {
//
//    override fun getOldListSize(): Int = oldList.size
//
//    override fun getNewListSize(): Int = newList.size
//
//    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        // Assuming each receipt has a unique ID or other unique identifier
//        return oldList[oldItemPosition].id == newList[newItemPosition].id
//    }
//
//    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        // Compare the contents of old and new items
//        return oldList[oldItemPosition] == newList[newItemPosition]
//    }
//}



//package com.example.warehousetet
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.RecyclerView
//
//class ReceiptsAdapter(
//    receipts: List<Receipt>,
//    private val onReceiptClicked: (Receipt) -> Unit
//) : RecyclerView.Adapter<ReceiptsAdapter.ViewHolder>() {
//
//    private var receipts: MutableList<Receipt> = receipts.toMutableList()
//    private var filteredReceipts: MutableList<Receipt> = receipts.toMutableList()
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false)
//        return ViewHolder(view, onReceiptClicked)
//    }
//
//    override fun getItemCount(): Int = filteredReceipts.size
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(filteredReceipts[position])
//    }
//
//    fun filter(query: String) {
//        val result = if (query.isEmpty()) {
//            receipts.toList() // Make a copy of the original list
//        } else {
//            receipts.filter { it.name.contains(query, ignoreCase = true) }
//        }
//
//        val diffCallback = ReceiptDiffCallback(filteredReceipts, result)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//
//        filteredReceipts.clear()
//        filteredReceipts.addAll(result)
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    fun updateReceipts(newReceipts: List<Receipt>) {
//        val diffCallback = ReceiptDiffCallback(receipts, newReceipts)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//
//        receipts.clear()
//        receipts.addAll(newReceipts)
//        filteredReceipts.clear()
//        filteredReceipts.addAll(newReceipts)
//
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    inner class ViewHolder(itemView: View, private val onReceiptClicked: (Receipt) -> Unit) : RecyclerView.ViewHolder(itemView) {
//        private val receiptNameTextView: TextView = itemView.findViewById(R.id.receiptNameTextView)
//        private val receiptDateTextView: TextView = itemView.findViewById(R.id.receiptDateTextView)
//        private val receiptOriginTextView: TextView = itemView.findViewById(R.id.receiptOriginTextView)
//
//        init {
//            itemView.setOnClickListener {
//                val position = adapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    onReceiptClicked(filteredReceipts[position])
//                }
//            }
//        }
//
//        fun bind(receipt: Receipt) {
//            receiptNameTextView.text = receipt.name
//            receiptDateTextView.text = receipt.date
//            receiptOriginTextView.text = receipt.origin
//        }
//    }
//}
//
//class ReceiptDiffCallback(
//    private val oldList: List<Receipt>,
//    private val newList: List<Receipt>
//) : DiffUtil.Callback() {
//
//    override fun getOldListSize(): Int = oldList.size
//
//    override fun getNewListSize(): Int = newList.size
//
//    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        return oldList[oldItemPosition].id == newList[newItemPosition].id
//    }
//
//    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        return oldList[oldItemPosition] == newList[newItemPosition]
//    }
//}



package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class ReceiptsAdapter(
    receipts: List<Receipt>,
    private val onReceiptClicked: (Receipt) -> Unit
) : RecyclerView.Adapter<ReceiptsAdapter.ViewHolder>() {

    private var receipts: MutableList<Receipt> = receipts.toMutableList()
    private var filteredReceipts: MutableList<Receipt> = receipts.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false)
        return ViewHolder(view, onReceiptClicked)
    }

    override fun getItemCount(): Int = filteredReceipts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredReceipts[position])
    }

    fun filter(query: String) {
        val result = if (query.isEmpty()) {
            receipts.toList() // Make a copy of the original list
        } else {
            receipts.filter { it.name.contains(query, ignoreCase = true) }
        }

        val diffCallback = ReceiptDiffCallback(filteredReceipts, result)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        filteredReceipts.clear()
        filteredReceipts.addAll(result)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateReceipts(newReceipts: List<Receipt>) {
        val diffCallback = ReceiptDiffCallback(receipts, newReceipts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        receipts.clear()
        receipts.addAll(newReceipts)
        filteredReceipts.clear()
        filteredReceipts.addAll(newReceipts)

        diffResult.dispatchUpdatesTo(this)
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

class ReceiptDiffCallback(
    private val oldList: List<Receipt>,
    private val newList: List<Receipt>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
