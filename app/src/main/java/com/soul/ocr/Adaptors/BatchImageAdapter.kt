package com.soul.ocr.Adaptors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soul.ocr.databinding.BatchImageItemBinding
import android.graphics.Bitmap


class BatchImageAdapter(
    private val images: List<Bitmap>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<BatchImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: BatchImageItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = BatchImageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val bitmap = images[position]
        holder.binding.itemImage.setImageBitmap(bitmap)

        // ❌ Cross icon pe click listener
        holder.binding.btnRemove.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount(): Int = images.size

}
