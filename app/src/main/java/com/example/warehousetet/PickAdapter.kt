//package com.example.warehousetet
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//
//// Define the PickAdapter class, which extends RecyclerView.Adapter
//    class PickAdapter(
//    private var picks: List<Pick>, // List of Pick items
//    private val onPickClicked: (Pick) -> Unit // Lambda function to handle click events
//    ) : RecyclerView.Adapter<PickAdapter.ViewHolder>() {
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//            // Inflate the layout for a delivery order item view
//            val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_item, parent, false)
//            return ViewHolder(view, onPickClicked)
//        }
//
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//            holder.bind(picks[position])
//        }
//
//        override fun getItemCount(): Int = picks.size
//
//        // ViewHolder class to hold references to views within each item
//        inner class ViewHolder(itemView: View, private val onPickClicked: (Pick) -> Unit) : RecyclerView.ViewHolder(itemView) {
//            private val pickNameTextView: TextView = itemView.findViewById(R.id.pickNameTextView)
//            private val pickDateTextView: TextView = itemView.findViewById(R.id.pickDateTextView)
//            private val pickOriginTextView: TextView = itemView.findViewById(R.id.pickOriginTextView)
//
//            init {
//                itemView.setOnClickListener {
//                    val position = adapterPosition
//                    if (position != RecyclerView.NO_POSITION) {
//                        onPickClicked(picks[position])
//                    }
//                }
//            }
//
//            fun bind(pick: Pick) {
//                pickNameTextView.text = pick.name
//                pickDateTextView.text = pick.date
//                pickOriginTextView.text = pick.origin
//            }
//        }
//
//        fun updateDeliveryOrders(newPicks: List<Pick>) {
//            picks = newPicks
//            notifyDataSetChanged()
//        }
//    }
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.warehousetet.Pick
import com.example.warehousetet.R

class PickAdapter(
    private var picks: List<Pick>,
    private val onPickClicked: (Pick) -> Unit
) : RecyclerView.Adapter<PickAdapter.ViewHolder>() {

    private var filteredPicks: List<Pick> = ArrayList(picks)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_item, parent, false)
        return ViewHolder(view, onPickClicked)
    }

    override fun getItemCount(): Int = filteredPicks.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredPicks[position])
    }

    fun filter(query: String) {
        val oldList = filteredPicks
        filteredPicks = if (query.isEmpty()) {
            picks
        } else {
            picks.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = filteredPicks.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition] == filteredPicks[newItemPosition]

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition].name == filteredPicks[newItemPosition].name
        }).dispatchUpdatesTo(this)
    }

    fun updatePicks(newPicks: List<Pick>) {
        val diffCallback = DiffUtilCallback(picks, newPicks)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        picks = newPicks
        filteredPicks = newPicks
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(itemView: View, private val onPickClicked: (Pick) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val pickNameTextView: TextView = itemView.findViewById(R.id.pickNameTextView)
        private val pickDateTextView: TextView = itemView.findViewById(R.id.pickDateTextView)
        private val pickOriginTextView: TextView = itemView.findViewById(R.id.pickOriginTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPickClicked(filteredPicks[position])
                }
            }
        }

        fun bind(pick: Pick) {
            pickNameTextView.text = pick.name
            pickDateTextView.text = pick.date
            pickOriginTextView.text = pick.origin
        }
    }
}

class DiffUtilCallback(
    private val oldList: List<Pick>,
    private val newList: List<Pick>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}
