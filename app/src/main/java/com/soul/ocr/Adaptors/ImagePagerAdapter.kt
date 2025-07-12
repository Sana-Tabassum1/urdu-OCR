package com.soul.ocr.Adaptors

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soul.ocr.databinding.ItemPagerImageBinding

class ImagePagerAdapter(
    private val images: List<Bitmap>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemPagerImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemPagerImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val bitmap = images[position]
        holder.binding.imageView.setImageBitmap(bitmap)
    }

    override fun getItemCount(): Int = images.size
}
