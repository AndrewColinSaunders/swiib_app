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
    var moveLineOutGoings: List<MoveLineOutGoing>,
    private var packId: Int,  // This packId is being used so please ignore the warning. Being used in PackProductsActivity
    private var packagedMoveLines: MutableList<PackagedMovedLine>,
    private val verificationListener: VerificationListener
) : RecyclerView.Adapter<PackProductsAdapter.MoveLineViewHolder>() {

    //var selectedMoveLineId: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveLineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pack_product_item, parent, false)
        return MoveLineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoveLineViewHolder, position: Int) {
        val moveLine = moveLineOutGoings[position]
        val isPackaged = packagedMoveLines.any { it.moveLineId == moveLine.lineId }
        holder.bind(moveLine, isPackaged)
        checkAllVerified()
    }

    override fun getItemCount(): Int = moveLineOutGoings.size


    class MoveLineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.packProductNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.packProductQuantityTextView)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.packProductItemCard)

        fun bind(moveLineOutGoing: MoveLineOutGoing, isPackaged: Boolean) {
            Log.d("ViewHolderLog", "Binding move line: ${moveLineOutGoing.productName}, Packaged: $isPackaged")
            nameTextView.text = moveLineOutGoing.productName
            quantityTextView.text = itemView.context.getString(R.string.quantity_text, moveLineOutGoing.quantity.toString())
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
            val index = moveLineOutGoings.indexOfFirst { it.lineId == packagedMovedLine.moveLineId }
            if (index != -1) {
                notifyItemChanged(index)
            }
        }
        checkAllVerified()
    }

    private fun checkAllVerified() {
        val allVerified = moveLineOutGoings.all { moveLine ->
            packagedMoveLines.any { it.moveLineId == moveLine.lineId }
        }
        verificationListener.onVerificationStatusChanged(allVerified)
    }

    interface VerificationListener {
        fun onVerificationStatusChanged(allVerified: Boolean)
    }


}

