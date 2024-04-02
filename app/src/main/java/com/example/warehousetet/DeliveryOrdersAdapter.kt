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


class DeliveryOrdersAdapter(private val context: Context, private val listener: OnInternalTransferSelectedListener) :
    ListAdapter<DeliveryOrders, DeliveryOrdersAdapter.DeliveryOrdersViewHolder>(DiffCallback()){

    private var fullList: List<DeliveryOrders> = emptyList()
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

    fun submitFilteredPicks(list: List<DeliveryOrders>) {
        fullList = ArrayList(list) // Update the full list
        val filteredList = list.filterNot { transfer ->
            transfer.orderName.contains("INT", ignoreCase = true) || transfer.orderName.contains("PACK", ignoreCase = true)
        }
        submitList(filteredList)
    }

    fun resetList() {
        submitList(fullList.filterNot {
            it.orderName.contains("INT", ignoreCase = true) || it.orderName.contains("PACK", ignoreCase = true)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryOrdersViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_internal_transfer, parent, false)
        vibrator = parent.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        return DeliveryOrdersViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeliveryOrdersViewHolder, position: Int) {
        val deliveryOrder = getItem(position)
        holder.bind(deliveryOrder)
    }

    inner class DeliveryOrdersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_view)
        private val transferNameTextView: TextView = itemView.findViewById(R.id.textView_transfer_title)
        private val transferDateTextView: TextView = itemView.findViewById(R.id.textView_transferDate)

        init {
            cardView.setOnClickListener {
                val deliveryOrder = getItem(adapterPosition)
                val intent = Intent(context, IntTransferProductsDeliveryOrdersActivity::class.java).apply {
                    putExtra("EXTRA_TRANSFER_ID", deliveryOrder.id)
                    putExtra("EXTRA_TRANSFER_NAME", deliveryOrder.orderName)
                    putExtra("EXTRA_SOURCE_DOCUMENT", deliveryOrder.sourceDocument)
                    putParcelableArrayListExtra("EXTRA_PRODUCTS", ArrayList(deliveryOrder.productDetails))
                }
                context.startActivity(intent)
                listener.onInternalTransferFinish()
            }
        }

        fun bind(deliveryOrder: DeliveryOrders) {
            transferNameTextView.text = deliveryOrder.orderName
            transferDateTextView.text = "Date: ${deliveryOrder.deliveryDate}"
            itemView.findViewById<TextView>(R.id.textView_sourceDocument).text = "Source Document: ${deliveryOrder.sourceDocument}"
        }

        private fun triggerHapticFeedback() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(50)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DeliveryOrders>() {
        override fun areItemsTheSame(oldItem: DeliveryOrders, newItem: DeliveryOrders): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DeliveryOrders, newItem: DeliveryOrders): Boolean = oldItem == newItem
    }
}
