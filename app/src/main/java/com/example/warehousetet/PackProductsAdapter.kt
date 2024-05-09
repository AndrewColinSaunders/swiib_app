package com.example.warehousetet

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class PackProductsAdapter(
    public var moveLines: List<MoveLine>,
    private var packId: Int,
    var packagedMoveLines: MutableList<PackagedMovedLine>,
    private val verificationListener: VerificationListener
) : RecyclerView.Adapter<PackProductsAdapter.MoveLineViewHolder>() {

    var selectedMoveLineId: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveLineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pack_product_item, parent, false)
        return MoveLineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoveLineViewHolder, position: Int) {
        val moveLine = moveLines[position]
        val isPackaged = packagedMoveLines.any { it.moveLineId == moveLine.lineId }
        holder.bind(moveLine, isPackaged)
        checkAllVerified()
    }

    override fun getItemCount(): Int = moveLines.size

    fun updateMoveLines(newMoveLines: List<MoveLine>, newPackId: Int) {
        moveLines = newMoveLines
        packId = newPackId
        notifyDataSetChanged()
        checkAllVerified()
    }

    class MoveLineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.packProductNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.packProductQuantityTextView)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.packProductItemCard)

        fun bind(moveLine: MoveLine, isPackaged: Boolean) {
            Log.d("ViewHolderLog", "Binding move line: ${moveLine.productName}, Packaged: $isPackaged")
            nameTextView.text = moveLine.productName
            quantityTextView.text = "Quantity: ${moveLine.quantity}"
            val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
            nameTextView.setTextColor(whiteColor)
            quantityTextView.setTextColor(whiteColor)
            val backgroundColor = if (isPackaged) {
                ContextCompat.getColor(itemView.context, R.color.success_green)
            } else {
                ContextCompat.getColor(itemView.context, R.color.cardGrey)
            }
            cardView.setCardBackgroundColor(backgroundColor)
        }

    }

    fun addPackagedMoveLine(packagedMovedLine: PackagedMovedLine) {
        if (!packagedMoveLines.any { it.moveLineId == packagedMovedLine.moveLineId }) {
            packagedMoveLines.add(packagedMovedLine)
            val index = moveLines.indexOfFirst { it.lineId == packagedMovedLine.moveLineId }
            if (index != -1) {
                notifyItemChanged(index)
            }
        }
        checkAllVerified()
    }

    private fun checkAllVerified() {
        val allVerified = moveLines.all { moveLine ->
            packagedMoveLines.any { it.moveLineId == moveLine.lineId }
        }
        verificationListener.onVerificationStatusChanged(allVerified)
    }

    interface VerificationListener {
        fun onVerificationStatusChanged(allVerified: Boolean)
    }
}
