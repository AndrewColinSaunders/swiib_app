package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Define the PickAdapter class, which extends RecyclerView.Adapter
    class PickAdapter(
    private var picks: List<Pick>, // List of Pick items
    private val onDeliveryOrderClicked: (Pick) -> Unit // Lambda function to handle click events
    ) : RecyclerView.Adapter<PickAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Inflate the layout for a delivery order item view
            val view = LayoutInflater.from(parent.context).inflate(R.layout.delivery_order_item, parent, false)
            return ViewHolder(view, onDeliveryOrderClicked)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(picks[position])
        }

        override fun getItemCount(): Int = picks.size

        // ViewHolder class to hold references to views within each item
        inner class ViewHolder(itemView: View, private val onDeliveryOrderClicked: (Pick) -> Unit) : RecyclerView.ViewHolder(itemView) {
            private val deliveryOrderNameTextView: TextView = itemView.findViewById(R.id.deliveryOrderNameTextView)
            private val deliveryOrderDateTextView: TextView = itemView.findViewById(R.id.deliveryOrderDateTextView)
            private val deliveryOrderOriginTextView: TextView = itemView.findViewById(R.id.deliveryOrderOriginTextView)

            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onDeliveryOrderClicked(picks[position])
                    }
                }
            }

            fun bind(pick: Pick) {
                deliveryOrderNameTextView.text = pick.name
                deliveryOrderDateTextView.text = pick.date
                deliveryOrderOriginTextView.text = pick.origin
            }
        }

        fun updateDeliveryOrders(newPicks: List<Pick>) {
            picks = newPicks
            notifyDataSetChanged()
        }
    }
