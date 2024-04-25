package com.example.warehousetet

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class DeliveryOrdersProductsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var sections: List<PackageSection> = listOf()

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    override fun getItemCount(): Int {
        var count = 0
        sections.forEach {
            count += 1 // for the header
            count += it.moveLines.size // for items
        }
        return count
    }

    override fun getItemViewType(position: Int): Int {
        var cumulatedSize = 0
        for (section in sections) {
            if (position == cumulatedSize) {
                return TYPE_HEADER
            }
            cumulatedSize += 1 + section.moveLines.size
            if (position < cumulatedSize) {
                return TYPE_ITEM
            }
        }
        throw IllegalArgumentException("Invalid position")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val headerView = inflater.inflate(R.layout.package_header, parent, false)
            HeaderViewHolder(headerView)
        } else {
            val itemView = inflater.inflate(R.layout.delivery_orders_products_item, parent, false)
            ItemViewHolder(itemView)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val type = getItemViewType(position)
        var cumulatedSize = 0
        for (section in sections) {
            if (type == TYPE_HEADER && position == cumulatedSize) {
                (holder as HeaderViewHolder).bind(section)
                break
            }
            cumulatedSize += 1
            if (type == TYPE_ITEM && position < cumulatedSize + section.moveLines.size) {
                (holder as ItemViewHolder).bind(section.moveLines[position - cumulatedSize])
                break
            }
            cumulatedSize += section.moveLines.size
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val packageNameTextView: TextView = view.findViewById(R.id.packageNameTextView)

        fun bind(section: PackageSection) {
            packageNameTextView.text = section.packageName
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.findViewById(R.id.deliveryOrdersProductNameTextView)
        private val quantityTextView: TextView = view.findViewById(R.id.deliveryOrdersProductQuantityTextView)
        private val cardView: MaterialCardView = view as MaterialCardView  // Assuming the whole view is the card

        fun bind(moveLine: MoveLine) {
            nameTextView.text = moveLine.productName
            quantityTextView.text = "Quantity: ${moveLine.quantity}"
            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
            nameTextView.setTextColor(whiteColor)
            quantityTextView.setTextColor(whiteColor)

            val backgroundColor = ContextCompat.getColor(itemView.context, R.color.cardGrey)
            cardView.setCardBackgroundColor(backgroundColor)
        }
    }

}
