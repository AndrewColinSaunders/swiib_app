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


class PickAdapter(private val context: Context, private val listener: OnInternalTransferSelectedListener) :
    ListAdapter<InternalTransfers, PickAdapter.PickViewHolder>(DiffCallback()){

    private var fullList: List<InternalTransfers> = emptyList()
    private lateinit var vibrator: Vibrator


    fun filter(searchQuery: String) {
        val filteredList = if (searchQuery.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                it.sourceDocument.equals(searchQuery, ignoreCase = true)
            }
        }
        submitList(filteredList)
    }

    fun submitFilteredPicks(list: List<InternalTransfers>) {
        fullList = ArrayList(list) // Update the full list
        val filteredList = list.filterNot { transfer ->
            transfer.transferName.contains("INT", ignoreCase = true) || transfer.transferName.contains("PACK", ignoreCase = true)
        }
        submitList(filteredList)
    }

    fun resetList() {
        submitList(fullList.filterNot {
            it.transferName.contains("INT", ignoreCase = true) || it.transferName.contains("PACK", ignoreCase = true)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_internal_transfer, parent, false)
        vibrator = parent.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        return PickViewHolder(view)
    }

    override fun onBindViewHolder(holder: PickViewHolder, position: Int) {
        val internalTransfer = getItem(position)
        holder.bind(internalTransfer)
    }

    inner class PickViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_view)
        private val transferNameTextView: TextView = itemView.findViewById(R.id.textView_transfer_title)
        private val transferDateTextView: TextView = itemView.findViewById(R.id.textView_transferDate)

        init {
            cardView.setOnClickListener {
                val internalTransfer = getItem(adapterPosition)
                val intent = Intent(context, IntTransferProductsPickActivity::class.java).apply {
                    putExtra("EXTRA_TRANSFER_ID", internalTransfer.id)
                    putExtra("EXTRA_TRANSFER_NAME", internalTransfer.transferName)
                    putExtra("EXTRA_SOURCE_DOCUMENT", internalTransfer.sourceDocument)
                    putParcelableArrayListExtra("EXTRA_PRODUCTS", ArrayList(internalTransfer.productDetails))
                }
                context.startActivity(intent)
                listener.onInternalTransferFinish()
            }
        }

        fun bind(internalTransfer: InternalTransfers) {
            transferNameTextView.text = internalTransfer.transferName
            transferDateTextView.text = "Date: ${internalTransfer.transferDate}"
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
