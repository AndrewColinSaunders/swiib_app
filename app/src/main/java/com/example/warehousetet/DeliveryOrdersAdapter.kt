package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Define the PickAdapter class, which extends RecyclerView.Adapter
class DeliveryOrdersAdapter(
    private var deliveryOrders: List<DeliveryOrders>, // List of Pick items
    private val onDeliveryOrdersClicked: (DeliveryOrders) -> Unit // Lambda function to handle click events
) : RecyclerView.Adapter<DeliveryOrdersAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for a delivery order item view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.delivery_orders_item, parent, false)
        return ViewHolder(view, onDeliveryOrdersClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(deliveryOrders[position])
    }

    override fun getItemCount(): Int = deliveryOrders.size

    // ViewHolder class to hold references to views within each item
    inner class ViewHolder(itemView: View, private val onDeliveryOrdersClicked: (DeliveryOrders) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val deliveryOrdersNameTextView: TextView = itemView.findViewById(R.id.deliveryOrdersNameTextView)
        private val deliveryOrdersDateTextView: TextView = itemView.findViewById(R.id.deliveryOrdersDateTextView)
        private val deliveryOrdersOriginTextView: TextView = itemView.findViewById(R.id.deliveryOrdersOriginTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeliveryOrdersClicked(deliveryOrders[position])
                }
            }
        }

        fun bind(deliveryOrders: DeliveryOrders) {
            deliveryOrdersNameTextView.text = deliveryOrders.name
            deliveryOrdersDateTextView.text = deliveryOrders.date
            deliveryOrdersOriginTextView.text = deliveryOrders.origin
        }
    }

    fun updateDeliveryOrders(newDeliveryOrders: List<DeliveryOrders>) {
        deliveryOrders = newDeliveryOrders
        notifyDataSetChanged()
    }
}
