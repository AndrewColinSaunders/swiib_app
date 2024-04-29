package com.example.warehousetet

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class DeliveryOrdersProductsAdapter : RecyclerView.Adapter<DeliveryOrdersProductsAdapter.GroupViewHolder>() {
    var sections: List<PackageSection> = listOf()

    override fun getItemCount(): Int = sections.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.group_layout, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        private val itemsRecyclerView: RecyclerView = view.findViewById(R.id.itemsRecyclerView)

        fun bind(section: PackageSection) {
            headerTextView.text = section.packageName
            setupRecyclerView(section.moveLines)
        }

        private fun setupRecyclerView(moveLines: List<MoveLine>) {
            if (itemsRecyclerView.adapter == null) { // Setup RecyclerView only once
                itemsRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                itemsRecyclerView.adapter = ItemAdapter(moveLines)
            } else {
                (itemsRecyclerView.adapter as ItemAdapter).updateData(moveLines)
            }
        }
    }

    class ItemAdapter(private var items: List<MoveLine>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
        fun updateData(newItems: List<MoveLine>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.delivery_orders_products_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(items[position])

        }

        override fun getItemCount() = items.size

        class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val nameTextView: TextView = view.findViewById(R.id.deliveryOrdersProductNameTextView)
            private val quantityTextView: TextView = view.findViewById(R.id.deliveryOrdersProductQuantityTextView)
            private val cardView: MaterialCardView = view as MaterialCardView

            fun bind(moveLine: MoveLine) {
                nameTextView.text = moveLine.productName
                quantityTextView.text = "Quantity: ${moveLine.quantity}"
                val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
                nameTextView.setTextColor(whiteColor)
                quantityTextView.setTextColor(whiteColor)

            }
        }
    }
}
