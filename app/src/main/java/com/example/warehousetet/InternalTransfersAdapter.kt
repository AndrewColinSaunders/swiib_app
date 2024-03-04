package com.example.warehousetet

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class InternalTransfersAdapter :
    ListAdapter<InternalTransfers, InternalTransfersAdapter.InternalTransferViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InternalTransferViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_internal_transfer, parent, false)
        return InternalTransferViewHolder(view)
    }

    override fun onBindViewHolder(holder: InternalTransferViewHolder, position: Int) {
        val internalTransfer = getItem(position)
        holder.bind(internalTransfer)
    }

    // Correctly placed method for filtering and submitting the list
    fun submitFilteredInternalTransfers(list: List<InternalTransfers>) {
        val filteredList = list.filterNot { transfer ->
            transfer.transferName.contains("PICK", ignoreCase = true) || transfer.transferName.contains("PACK", ignoreCase = true)
        }
        submitList(filteredList)
    }

    inner class InternalTransferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_view)
        private val transferNameTextView: TextView = itemView.findViewById(R.id.textView_transfer_title)
        private val transferDateTextView: TextView = itemView.findViewById(R.id.textView_transferDate)
        private val productDetailsTextView: TextView = itemView.findViewById(R.id.textView_productDetails)

        init {
            cardView.setOnClickListener {
                val context = it.context
                val internalTransfer = getItem(adapterPosition)
                val intent = Intent(context, IntTransferProductsActivity::class.java).apply {
                    putParcelableArrayListExtra("EXTRA_PRODUCTS", ArrayList(internalTransfer.productDetails))
                }
                context.startActivity(intent)
            }
        }

        fun bind(internalTransfer: InternalTransfers) {
            transferNameTextView.text = internalTransfer.transferName
            transferDateTextView.text = "Transfer Date: ${internalTransfer.transferDate}"
            val productDetailsFormatted = internalTransfer.productDetails.joinToString(", ") {
                "${it.name} (${it.quantity})"
            }
            productDetailsTextView.text = "Product Details: $productDetailsFormatted"
            itemView.findViewById<TextView>(R.id.textView_sourceDocument).text = "Source Document: ${internalTransfer.sourceDocument}"
        }

    }

    class DiffCallback : DiffUtil.ItemCallback<InternalTransfers>() {
        override fun areItemsTheSame(oldItem: InternalTransfers, newItem: InternalTransfers): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: InternalTransfers, newItem: InternalTransfers): Boolean = oldItem == newItem
    }
}
