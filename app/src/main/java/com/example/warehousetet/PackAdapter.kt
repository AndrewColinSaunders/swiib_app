package com.example.warehousetet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Define the PickAdapter class, which extends RecyclerView.Adapter
class PackAdapter(
    private var packs: List<Pack>, // List of Pick items
    private val onPackClicked: (Pack) -> Unit // Lambda function to handle click events
) : RecyclerView.Adapter<PackAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for a delivery order item view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pack_item, parent, false)
        return ViewHolder(view, onPackClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(packs[position])
    }

    override fun getItemCount(): Int = packs.size

    // ViewHolder class to hold references to views within each item
    inner class ViewHolder(itemView: View, private val onPackClicked: (Pack) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val packNameTextView: TextView = itemView.findViewById(R.id.packNameTextView)
        private val packDateTextView: TextView = itemView.findViewById(R.id.packDateTextView)
        private val packOriginTextView: TextView = itemView.findViewById(R.id.packOriginTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPackClicked(packs[position])
                }
            }
        }

        fun bind(pack: Pack) {
            packNameTextView.text = pack.name
            packDateTextView.text = pack.date
            packOriginTextView.text = pack.origin
        }
    }

    fun updatePack(newPacks: List<Pack>) {
        packs = newPacks
        notifyDataSetChanged()
    }
}
