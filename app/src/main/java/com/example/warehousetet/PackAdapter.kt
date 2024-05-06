//package com.example.warehousetet
//
//import android.content.Intent
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.cardview.widget.CardView
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//
//class PackAdapter :
//    ListAdapter<InternalTransfers, PackAdapter.PackViewHolder>(DiffCallback()) {
//
//    // Store the full list for resetting later
//    private var fullList: List<InternalTransfers> = emptyList()
//
//    fun filter(searchQuery: String) {
//        val filteredList = if (searchQuery.isEmpty()) {
//            fullList.filterNot {
//                it.transferName.contains("INT", ignoreCase = true) ||
//                        it.transferName.contains("PICK", ignoreCase = true)
//            }
//        } else {
//            fullList.filter {
//                it.sourceDocument.equals(searchQuery, ignoreCase = true)
//            }
//        }
//        submitList(filteredList)
//    }
//
//    fun submitFilteredPacks(list: List<InternalTransfers>) {
//        fullList = ArrayList(list) // Update the full list
//        val filteredList = list.filterNot {
//            it.transferName.contains("INT", ignoreCase = true) ||
//                    it.transferName.contains("PICK", ignoreCase = true)
//        }
//        submitList(filteredList)
//    }
//
//    // Reset list to the initial filtered state
//    fun resetList() {
//        submitList(fullList.filterNot {
//            it.transferName.contains("INT", ignoreCase = true) ||
//                    it.transferName.contains("PICK", ignoreCase = true)
//        })
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_internal_transfer, parent, false)
//        return PackViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: PackViewHolder, position: Int) {
//        val internalTransfer = getItem(position)
//        holder.bind(internalTransfer)
//    }
//
//    inner class PackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val cardView: CardView = itemView.findViewById(R.id.card_view)
//        private val transferNameTextView: TextView = itemView.findViewById(R.id.textView_transfer_title)
//        private val transferDateTextView: TextView = itemView.findViewById(R.id.textView_transferDate)
//
//        init {
//            cardView.setOnClickListener {
//                val context = it.context
//                val internalTransfer = getItem(adapterPosition)
//                val intent = Intent(context, IntTransferProductsActivity::class.java).apply {
//                    putParcelableArrayListExtra("EXTRA_PRODUCTS", ArrayList(internalTransfer.productDetails))
//                }
//                context.startActivity(intent)
//            }
//        }
//
//        fun bind(internalTransfer: InternalTransfers) {
//            transferNameTextView.text = internalTransfer.transferName
//            transferDateTextView.text = "Date: ${internalTransfer.transferDate}"
//            itemView.findViewById<TextView>(R.id.textView_sourceDocument).text = "Source Document: ${internalTransfer.sourceDocument}"
//        }
//    }
//
//    class DiffCallback : DiffUtil.ItemCallback<InternalTransfers>() {
//        override fun areItemsTheSame(oldItem: InternalTransfers, newItem: InternalTransfers): Boolean = oldItem.id == newItem.id
//        override fun areContentsTheSame(oldItem: InternalTransfers, newItem: InternalTransfers): Boolean = oldItem == newItem
//    }
//}
