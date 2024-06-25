//package com.example.warehousetet
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.RecyclerView
//
//class InternalTransfersAdapter(
//    private var transfers: List<InternalTransfers>,
//    private val onTransferClicked: (InternalTransfers) -> Unit
//) : RecyclerView.Adapter<InternalTransfersAdapter.ViewHolder>() {
//
//    private var filteredTransfers: List<InternalTransfers> = ArrayList(transfers)
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_item, parent, false)
//        return ViewHolder(view, onTransferClicked)
//    }
//
//    override fun getItemCount(): Int = filteredTransfers.size
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(filteredTransfers[position])
//    }
//
//    fun filter(query: String) {
//        val oldFilteredTransfers = filteredTransfers
//        filteredTransfers = if (query.isEmpty()) transfers else transfers.filter {
//            it.name.contains(query, ignoreCase = true)
//        }
//        updateList(oldFilteredTransfers, filteredTransfers)
//    }
//
//    fun updateTransfers(newTransfers: List<InternalTransfers>) {
//        val oldTransfers = transfers
//        transfers = newTransfers
//        filteredTransfers = newTransfers
//        updateList(oldTransfers, newTransfers)
//    }
//
//    private fun updateList(oldList: List<InternalTransfers>, newList: List<InternalTransfers>) {
//        val diffCallback = TransfersDiffCallback(oldList, newList)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    inner class ViewHolder(itemView: View, private val onTransferClicked: (InternalTransfers) -> Unit) : RecyclerView.ViewHolder(itemView) {
//        private val nameTextView: TextView = itemView.findViewById(R.id.pickNameTextView)
//        private val dateTextView: TextView = itemView.findViewById(R.id.pickDateTextView)
//        private val originTextView: TextView = itemView.findViewById(R.id.pickOriginTextView)
//
//        init {
//            itemView.setOnClickListener {
//                val position = adapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    onTransferClicked(filteredTransfers[position])
//                }
//            }
//        }
//
//        fun bind(transfer: InternalTransfers) {
//            nameTextView.text = transfer.name
//            dateTextView.text = transfer.date
//            originTextView.text = transfer.origin
//        }
//    }
//}
//
//class TransfersDiffCallback(
//    private val oldList: List<InternalTransfers>,
//    private val newList: List<InternalTransfers>
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


package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class InternalTransfersAdapter(
    private var transfers: List<InternalTransfers>,
    private val onTransferClicked: (InternalTransfers) -> Unit
) : RecyclerView.Adapter<InternalTransfersAdapter.ViewHolder>() {

    private var filteredTransfers: List<InternalTransfers> = ArrayList(transfers)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_item, parent, false)
        return ViewHolder(view, onTransferClicked)
    }

    override fun getItemCount(): Int = filteredTransfers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredTransfers[position])
    }

    fun filter(query: String) {
        val oldFilteredTransfers = filteredTransfers
        filteredTransfers = if (query.isEmpty()) transfers else transfers.filter {
            it.name.contains(query, ignoreCase = true)
        }
        updateList(oldFilteredTransfers, filteredTransfers)
    }

    fun updateTransfers(newTransfers: List<InternalTransfers>) {
        val oldTransfers = transfers
        transfers = newTransfers
        filteredTransfers = newTransfers
        updateList(oldTransfers, newTransfers)
    }

    private fun updateList(oldList: List<InternalTransfers>, newList: List<InternalTransfers>) {
        val diffCallback = TransfersDiffCallback(oldList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(itemView: View, private val onTransferClicked: (InternalTransfers) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.pickNameTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.pickDateTextView)
        private val originTextView: TextView = itemView.findViewById(R.id.pickOriginTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTransferClicked(filteredTransfers[position])
                }
            }
        }

        fun bind(transfer: InternalTransfers) {
            nameTextView.text = transfer.name
            dateTextView.text = transfer.date
            originTextView.text = transfer.origin
        }
    }
}

class TransfersDiffCallback(
    private val oldList: List<InternalTransfers>,
    private val newList: List<InternalTransfers>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}
