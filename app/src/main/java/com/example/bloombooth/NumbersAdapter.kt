package com.example.bloombooth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NumbersAdapter(
    private val numbersList: List<Int>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<NumbersAdapter.NumberViewHolder>() {

    inner class NumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberTextView: TextView = itemView.findViewById(R.id.boothCnt)

        init {
            itemView.setOnClickListener {
                onItemClick(numbersList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_number, parent, false)
        return NumberViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        holder.numberTextView.text = numbersList[position].toString()
    }

    override fun getItemCount(): Int = numbersList.size
}
