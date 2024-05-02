package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class DeliveryOrdersProductsAdapter(private val verificationListener: VerificationListener) : RecyclerView.Adapter<DeliveryOrdersProductsAdapter.GroupViewHolder>() {
    var sections: List<PackageSection> = listOf()
    private val verifiedPackages = mutableSetOf<Int>() // Tracks verified package IDs

    override fun getItemCount(): Int = sections.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.group_layout, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val section = sections[position]
        holder.bind(section, verifiedPackages.contains(section.packageId))
    }

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        private val itemsRecyclerView: RecyclerView = view.findViewById(R.id.itemsRecyclerView)
        private val cardView: CardView = view.findViewById(R.id.groupCardView) // Ensure this matches the XML

        fun bind(section: PackageSection, isVerified: Boolean) {
            headerTextView.text = section.packageName
            if (itemsRecyclerView.adapter == null) {
                itemsRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                itemsRecyclerView.adapter = ItemAdapter(section.moveLines, isVerified)
            } else {
                (itemsRecyclerView.adapter as ItemAdapter).updateData(section.moveLines, isVerified)
            }
            // Update the background of the LinearLayout as you have it set up
            val backgroundResource = if (isVerified) R.drawable.bordered_background_success
            else R.drawable.bordered_background
            itemView.findViewById<LinearLayout>(R.id.linearLayoutInsideCardView).setBackgroundResource(backgroundResource)
        }
    }

    fun verifyPackage(packageId: Int) {
        verifiedPackages.add(packageId)
        notifyDataSetChanged()
        verificationListener.onVerificationStatusChanged(areAllPackagesVerified())
    }

    private fun areAllPackagesVerified(): Boolean {
        return sections.all { verifiedPackages.contains(it.packageId) }
    }

    interface VerificationListener {
        fun onVerificationStatusChanged(allVerified: Boolean)
    }

    class ItemAdapter(private var items: List<MoveLine>, private var isVerified: Boolean) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
        fun updateData(newItems: List<MoveLine>, newIsVerified: Boolean) {
            items = newItems
            isVerified = newIsVerified
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.delivery_orders_products_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(items[position], isVerified)
        }

        override fun getItemCount() = items.size

        class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val nameTextView: TextView = view.findViewById(R.id.deliveryOrdersProductNameTextView)
            private val quantityTextView: TextView = view.findViewById(R.id.deliveryOrdersProductQuantityTextView)
            private val cardView: MaterialCardView = view as MaterialCardView

            fun bind(moveLine: MoveLine, isVerified: Boolean) {
                nameTextView.text = moveLine.productName
                quantityTextView.text = "Quantity: ${moveLine.quantity}"
                val textColor = ContextCompat.getColor(itemView.context, android.R.color.white)
                nameTextView.setTextColor(textColor)
                quantityTextView.setTextColor(textColor)
                if (isVerified) {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.success_green))
                } else {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.cardGrey))
                }
            }
        }
    }
}
