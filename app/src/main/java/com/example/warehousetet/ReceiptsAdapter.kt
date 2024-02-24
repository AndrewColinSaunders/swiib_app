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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false)
        return ViewHolder(view, onReceiptClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(receipts[position])
    }

    override fun getItemCount(): Int = receipts.size

    inner class ViewHolder(itemView: View, private val onReceiptClicked: (Receipt) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val receiptNameTextView: TextView = itemView.findViewById(R.id.receiptNameTextView)
        private val receiptDateTextView: TextView = itemView.findViewById(R.id.receiptDateTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onReceiptClicked(receipts[position])
                }
            }
        }

        fun bind(receipt: Receipt) {
            receiptNameTextView.text = receipt.name
            receiptDateTextView.text = receipt.date
        }
    }
    fun updateReceipts(newReceipts: List<Receipt>) {
        receipts = newReceipts
        notifyDataSetChanged()
    }
}
