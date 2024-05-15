package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class DeliveryOrdersProductsAdapter(private val verificationListener: VerificationListener) : RecyclerView.Adapter<DeliveryOrdersProductsAdapter.GroupViewHolder>() {
    var sections: List<PackageSection> = listOf()
    var verifiedPackages = mutableSetOf<Int>()
    var verifiedSerialNumbers = mutableSetOf<String>()
    var verifiedBarcodes = mutableSetOf<String>()

    override fun getItemCount(): Int = sections.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.group_layout, parent, false)
        return GroupViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val section = sections[position]
        val isVerified = isSectionVerified(section)
        holder.bind(section, isVerified)
    }

    private fun isSectionVerified(section: PackageSection): Boolean {
        return verifiedPackages.contains(section.packageId) ||
                section.moveLines.all { verifiedSerialNumbers.contains(it.lotName) || verifiedBarcodes.contains(it.lineId.toString()) } &&
                section.moveLines.any { it.resultPackageName == "None" }
    }

    class GroupViewHolder(view: View, private val adapter: DeliveryOrdersProductsAdapter) : RecyclerView.ViewHolder(view) {
        private val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        private val itemsRecyclerView: RecyclerView = view.findViewById(R.id.itemsRecyclerView)
        private val cardView: LinearLayout = view.findViewById(R.id.linearLayoutInsideCardView)

        fun bind(section: PackageSection, isVerified: Boolean) {
            headerTextView.text = if (section.moveLines.any { it.resultPackageName == "None" }) "Not Packaged" else section.packageName
            itemsRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            itemsRecyclerView.adapter = ItemAdapter(section.moveLines, isVerified, adapter.verifiedSerialNumbers, adapter.verifiedBarcodes)
            val backgroundResource = if (isVerified) R.drawable.bordered_background_success else R.drawable.bordered_background
            cardView.setBackgroundResource(backgroundResource)
        }
    }

    fun verifyPackage(packageId: Int) {
        verifiedPackages.add(packageId)
        notifyItemRangeChanged(0, sections.size)
        verificationListener.onVerificationStatusChanged(areAllPackagesVerified())
    }

    private fun areAllPackagesVerified(): Boolean {
        return sections.all { isSectionVerified(it) }
    }

    fun addVerifiedSerialNumber(serialNumber: String) {
        verifiedSerialNumbers.add(serialNumber)
        notifyItemRangeChanged(0, sections.size)
        verificationListener.onVerificationStatusChanged(areAllPackagesVerified())
    }

    fun addVerifiedBarcode(barcodeIdentifier: String) {
        verifiedBarcodes.add(barcodeIdentifier)
        notifyItemRangeChanged(0, sections.size)
        verificationListener.onVerificationStatusChanged(areAllPackagesVerified())
    }

    interface VerificationListener {
        fun onVerificationStatusChanged(allVerified: Boolean)
    }

    class ItemAdapter(
        private val items: List<MoveLineOutgoing>,
        private val isVerified: Boolean,
        private val verifiedSerialNumbers: Set<String>,
        private val verifiedBarcodes: Set<String>
    ) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.delivery_orders_products_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val moveLine = items[position]
            val isItemVerified = isVerified || verifiedSerialNumbers.contains(moveLine.lotName) || verifiedBarcodes.contains(moveLine.lineId.toString())
            holder.bind(moveLine, isItemVerified)
        }

        override fun getItemCount(): Int = items.size

        class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val nameTextView: TextView = view.findViewById(R.id.deliveryOrdersProductNameTextView)
            private val quantityTextView: TextView = view.findViewById(R.id.deliveryOrdersProductQuantityTextView)
            private val cardView: MaterialCardView = view as MaterialCardView

            fun bind(moveLine: MoveLineOutgoing, isVerified: Boolean) {
                nameTextView.text = moveLine.productName
                quantityTextView.text = itemView.context.getString(R.string.quantity_text, moveLine.quantity.toString())
                nameTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                quantityTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                cardView.setCardBackgroundColor(if (isVerified) ContextCompat.getColor(itemView.context, R.color.success_green) else ContextCompat.getColor(itemView.context, R.color.cardGrey))
            }
        }
    }
}
