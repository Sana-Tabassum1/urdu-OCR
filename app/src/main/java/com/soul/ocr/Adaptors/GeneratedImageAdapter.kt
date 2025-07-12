package com.soul.ocr.Adaptors

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.soul.ocr.R

class GeneratedImageAdapter(
    private val imageList: List<Bitmap>,
    private val onSaveClick: (Bitmap) -> Unit
) : RecyclerView.Adapter<GeneratedImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val generatedImageView: ImageView = itemView.findViewById(R.id.generatedImageView)
        val btnSave: ImageButton = itemView.findViewById(R.id.btnSave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_generated_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = imageList.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val bitmap = imageList[position]
        holder.generatedImageView.setImageBitmap(bitmap)
        holder.btnSave.setOnClickListener {
            onSaveClick(bitmap)
        }
    }
}