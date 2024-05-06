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


package com.example.warehousetet

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Define the PickAdapter class, which extends RecyclerView.Adapter
class PickAdapter(
    private var picks: List<Pick>, // List of Pick items
    private val onPickClicked: (Pick) -> Unit // Lambda function to handle click events
) : RecyclerView.Adapter<PickAdapter.ViewHolder>() {

    private var filteredPicks: List<Pick> = ArrayList(picks)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for a delivery order item view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pick_item, parent, false)
        return ViewHolder(view, onPickClicked)
    }


    fun filter(query: String) {
        Log.d("PickAdapter", "Current query: $query")
        filteredPicks = if (query.isEmpty()) {
            picks
        } else {
            picks.filter {
                val matches = it.name.contains(query, ignoreCase = true)
                Log.d("PickAdapter", "Testing '${it.name}' against '$query': $matches")
                matches
            }
        }
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int = filteredPicks.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredPicks[position])
    }

    // ViewHolder class to hold references to views within each item
    inner class ViewHolder(itemView: View, private val onPickClicked: (Pick) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val pickNameTextView: TextView = itemView.findViewById(R.id.pickNameTextView)
        private val pickDateTextView: TextView = itemView.findViewById(R.id.pickDateTextView)
        private val pickOriginTextView: TextView = itemView.findViewById(R.id.pickOriginTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPickClicked(filteredPicks[position])  // Use filteredPicks here
                }
            }
        }


        fun bind(pick: Pick) {
            pickNameTextView.text = pick.name
            pickDateTextView.text = pick.date
            pickOriginTextView.text = pick.origin
        }
    }

    fun updateDeliveryOrders(newPicks: List<Pick>) {
        picks = newPicks
        filteredPicks = ArrayList(picks)
        notifyDataSetChanged()
    }
}
