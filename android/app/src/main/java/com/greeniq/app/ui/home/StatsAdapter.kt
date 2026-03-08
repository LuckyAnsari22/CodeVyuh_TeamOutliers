package com.greeniq.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.greeniq.app.R

class StatsAdapter(private val stats: List<Pair<String, String>>) :
    RecyclerView.Adapter<StatsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tvStatNumber)
        val tvLabel: TextView = view.findViewById(R.id.tvStatLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stat_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (number, label) = stats[position]
        holder.tvNumber.text = number
        holder.tvLabel.text = label
    }

    override fun getItemCount() = stats.size
}
