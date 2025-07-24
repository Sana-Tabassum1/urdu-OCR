package com.urduocr.scanner.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.urduocr.scanner.databinding.ItemCroppreviewImageBinding

class CropPreviewImageAdapter(
    private val imageList: MutableList<Bitmap>,
    private val onCropClick: (Int, Bitmap) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<CropPreviewImageAdapter.CropViewHolder>() {

    inner class CropViewHolder(val binding: ItemCroppreviewImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CropViewHolder {
        val binding = ItemCroppreviewImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CropViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CropViewHolder, position: Int) {
        val bitmap = imageList[position]
        holder.binding.cropitemImage.setImageBitmap(bitmap)

        holder.binding.btnCrop.setOnClickListener {
            onCropClick(position, bitmap)
        }

        holder.binding.btnCross.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = imageList.size

    fun updateList(newList: List<Bitmap>) {
        imageList.clear()
        imageList.addAll(newList)
        notifyDataSetChanged()
    }
}
