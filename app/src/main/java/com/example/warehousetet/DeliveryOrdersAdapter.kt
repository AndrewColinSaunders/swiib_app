package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

//class DeliveryOrdersAdapter(
//    private var deliveryOrders: List<DeliveryOrders>,
//    private val onDeliveryOrdersClicked: (DeliveryOrders) -> Unit
//) : RecyclerView.Adapter<DeliveryOrdersAdapter.ViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.delivery_orders_item, parent, false)
//        return ViewHolder(view, onDeliveryOrdersClicked)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(deliveryOrders[position])
//    }
//
//    override fun getItemCount(): Int = deliveryOrders.size
//
//    inner class ViewHolder(itemView: View, private val onDeliveryOrdersClicked: (DeliveryOrders) -> Unit) : RecyclerView.ViewHolder(itemView) {
//        private val deliveryOrdersNameTextView: TextView = itemView.findViewById(R.id.deliveryOrdersNameTextView)
//        private val deliveryOrdersDateTextView: TextView = itemView.findViewById(R.id.deliveryOrdersDateTextView)
//        private val deliveryOrdersOriginTextView: TextView = itemView.findViewById(R.id.deliveryOrdersOriginTextView)
//
//        init {
//            itemView.setOnClickListener {
//                val position = adapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    onDeliveryOrdersClicked(deliveryOrders[position])
//                }
//            }
//        }
//
//        fun bind(deliveryOrders: DeliveryOrders) {
//            deliveryOrdersNameTextView.text = deliveryOrders.name
//            deliveryOrdersDateTextView.text = deliveryOrders.date
//            deliveryOrdersOriginTextView.text = deliveryOrders.origin
//        }
//    }
//
//    fun updateDeliveryOrders(newDeliveryOrders: List<DeliveryOrders>) {
//        val diffCallback = DeliveryOrdersDiffCallback(deliveryOrders, newDeliveryOrders)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//
//        deliveryOrders = newDeliveryOrders
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    class DeliveryOrdersDiffCallback(
//        private val oldList: List<DeliveryOrders>,
//        private val newList: List<DeliveryOrders>
//    ) : DiffUtil.Callback() {
//
//        override fun getOldListSize(): Int = oldList.size
//
//        override fun getNewListSize(): Int = newList.size
//
//        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//            return oldList[oldItemPosition].id == newList[newItemPosition].id
//        }
//
//        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//            return oldList[oldItemPosition] == newList[newItemPosition]
//        }
//    }
//}

class DeliveryOrdersAdapter(
    private var deliveryOrders: List<DeliveryOrders>,
    private val onDeliveryOrdersClicked: (DeliveryOrders) -> Unit
) : RecyclerView.Adapter<DeliveryOrdersAdapter.ViewHolder>() {

    private var filteredDeliveryOrders: List<DeliveryOrders> = ArrayList(deliveryOrders)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.delivery_orders_item, parent, false)
        return ViewHolder(view, onDeliveryOrdersClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredDeliveryOrders[position])
    }

    override fun getItemCount(): Int = filteredDeliveryOrders.size

    fun filter(query: String) {
        val oldList = filteredDeliveryOrders
        filteredDeliveryOrders = if (query.isEmpty()) {
            deliveryOrders
        } else {
            deliveryOrders.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = filteredDeliveryOrders.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition] == filteredDeliveryOrders[newItemPosition]

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition].name == filteredDeliveryOrders[newItemPosition].name
        }).dispatchUpdatesTo(this)
    }

    fun updateDeliveryOrders(newDeliveryOrders: List<DeliveryOrders>) {
        val diffCallback = DeliveryOrdersDiffCallback(deliveryOrders, newDeliveryOrders)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        deliveryOrders = newDeliveryOrders
        filteredDeliveryOrders = newDeliveryOrders
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(itemView: View, private val onDeliveryOrdersClicked: (DeliveryOrders) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val deliveryOrdersNameTextView: TextView = itemView.findViewById(R.id.deliveryOrdersNameTextView)
        private val deliveryOrdersDateTextView: TextView = itemView.findViewById(R.id.deliveryOrdersDateTextView)
        private val deliveryOrdersOriginTextView: TextView = itemView.findViewById(R.id.deliveryOrdersOriginTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeliveryOrdersClicked(filteredDeliveryOrders[position])
                }
            }
        }

        fun bind(deliveryOrders: DeliveryOrders) {
            deliveryOrdersNameTextView.text = deliveryOrders.name
            deliveryOrdersDateTextView.text = deliveryOrders.date
            deliveryOrdersOriginTextView.text = deliveryOrders.origin
        }
    }

    class DeliveryOrdersDiffCallback(
        private val oldList: List<DeliveryOrders>,
        private val newList: List<DeliveryOrders>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

