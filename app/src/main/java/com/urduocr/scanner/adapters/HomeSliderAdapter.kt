package com.urduocr.scanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.urduocr.scanner.R
import com.urduocr.scanner.models.SliderItem

class HomeSliderAdapter(private val list: List<SliderItem>) : RecyclerView.Adapter<HomeSliderAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sliderImage: ImageView = itemView.findViewById(R.id.sliderImage)
        val sliderText: TextView = itemView.findViewById(R.id.sliderText)
        val sliderSubText: TextView = itemView.findViewById(R.id.sliderSubText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_slider, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.sliderImage.setImageResource(item.imageRes)
        holder.sliderText.text = item.text
        holder.sliderSubText.text = item.detail
    }
}
