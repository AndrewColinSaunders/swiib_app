package com.example.warehousetet

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
        return InternalTransferViewHolder(view, parent.context)
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

    inner class InternalTransferViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_view)
        private val transferNameTextView: TextView = itemView.findViewById(R.id.textView_transfer_title)
        private val transferDateTextView: TextView = itemView.findViewById(R.id.textView_transferDate)
        private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        init {
            cardView.setOnClickListener {
                val internalTransfer = getItem(adapterPosition)
                val intent = Intent(context, IntTransferProductsPickActivity::class.java).apply {
                    putParcelableArrayListExtra("EXTRA_PRODUCTS", ArrayList(internalTransfer.productDetails))
                }
                context.startActivity(intent)
                triggerHapticFeedback()
            }
        }

        fun bind(internalTransfer: InternalTransfers) {
            transferNameTextView.text = internalTransfer.transferName
            transferDateTextView.text = "Transfer Date: ${internalTransfer.transferDate}"
            itemView.findViewById<TextView>(R.id.textView_sourceDocument).text = "Source Document: ${internalTransfer.sourceDocument}"
        }

        private fun triggerHapticFeedback() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(50)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InternalTransfers>() {
        override fun areItemsTheSame(oldItem: InternalTransfers, newItem: InternalTransfers): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: InternalTransfers, newItem: InternalTransfers): Boolean = oldItem == newItem
    }
}
