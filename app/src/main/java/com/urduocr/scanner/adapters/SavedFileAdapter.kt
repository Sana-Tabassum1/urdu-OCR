package com.urduocr.scanner.adapters

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.ItemFileHeaderBinding
import com.urduocr.scanner.databinding.ItemSavedFileBinding
import java.io.File

class SavedFileAdapter(
    private val context: Context,
    var fileList: List<FileListItem>,
    private val listener: FileAdapterListener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_FILE = 1
    }

    inner class FileViewHolder(val binding: ItemSavedFileBinding) : RecyclerView.ViewHolder(binding.root)
    inner class HeaderViewHolder(val binding: ItemFileHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    interface FileAdapterListener {
        fun onItemSelectionChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (fileList[position]) {
            is FileListItem.Header -> VIEW_TYPE_HEADER
            is FileListItem.FileItem -> VIEW_TYPE_FILE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemFileHeaderBinding.inflate(LayoutInflater.from(context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemSavedFileBinding.inflate(LayoutInflater.from(context), parent, false)
            FileViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = fileList[position]) {
            is FileListItem.Header -> {
                val headerHolder = holder as HeaderViewHolder
                headerHolder.binding.headerText.text = item.title
            }

            is FileListItem.FileItem -> {
                val fileHolder = holder as FileViewHolder
                val file = item.file
                val fileObj = File(file.path)

                when {
                    file.name.endsWith(".png", true) -> {
                        fileHolder.binding.ivFileIcon.setImageResource(R.drawable.png)
                    }
                    file.name.endsWith(".pdf", true) -> {
                        fileHolder.binding.ivFileIcon.setImageResource(R.drawable.pdf)
                    }
                    file.name.endsWith(".txt", true) -> {
                        fileHolder.binding.ivFileIcon.setImageResource(R.drawable.txt)
                    }
                }

                fileHolder.binding.tvFileName.text = file.name

                val lastModified = fileObj.lastModified()
                val time = DateFormat.format("hh:mm a", lastModified).toString()
                val diff = System.currentTimeMillis() - lastModified
                val hours = diff / (1000 * 60 * 60)
                val dateLabel = when {
                    hours < 24 -> "Today"
                    hours in 24..48 -> "Yesterday"
                    else -> DateFormat.format("dd MMM yyyy", lastModified).toString()
                }

                val sizeInBytes = fileObj.length()
                val sizeFormatted = if (sizeInBytes < 1024 * 1024) {
                    String.format("%.1f KB", sizeInBytes / 1024f)
                } else {
                    String.format("%.1f MB", sizeInBytes / (1024f * 1024f))
                }

                fileHolder.binding.tvTimeSize.text = "$dateLabel - $time\n$sizeFormatted"

                fileHolder.binding.textViewFile.setOnClickListener {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            fileObj
                        )

                        val mimeType = when {
                            file.name.endsWith(".pdf") -> "application/pdf"
                            file.name.endsWith(".txt") -> "text/plain"
                            file.name.endsWith(".png") -> "image/png"
                            else -> "*/*"
                        }

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(Intent.createChooser(intent, "Open with"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Can't open this file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                fileHolder.binding.root.setOnClickListener {
                    if (file.isSelected || isAnyItemSelected()) {
                        file.isSelected = !file.isSelected
                        notifyItemChanged(position)
                        listener?.onItemSelectionChanged()
                    }
                }

                fileHolder.binding.root.setOnLongClickListener {
                    file.isSelected = !file.isSelected
                    notifyItemChanged(position)
                    listener?.onItemSelectionChanged()
                    true
                }

                fileHolder.binding.root.alpha = if (file.isSelected) 0.5f else 1.0f
                fileHolder.binding.root.setBackgroundResource(
                    if (file.isSelected) R.drawable.bg_file_selected
                    else R.drawable.bg_file_unselected
                )
            }
        }
    }

    fun clearSelection() {
        fileList.forEach {
            if (it is FileListItem.FileItem && it.file.isSelected) {
                it.file.isSelected = false
            }
        }
        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int {
        return fileList.count { it is FileListItem.FileItem && it.file.isSelected }
    }

    fun isAnyItemSelected(): Boolean {
        return fileList.any { it is FileListItem.FileItem && it.file.isSelected }
    }
    fun getSelectedFiles(): List<FileListItem.FileItem> {
        return fileList.filterIsInstance<FileListItem.FileItem>().filter { it.file.isSelected }
    }


    fun clearSelections() {
        fileList.filterIsInstance<FileListItem.FileItem>()
            .forEach { it.file.isSelected = false }
        notifyDataSetChanged()
    }

    fun updateList(newList: List<FileListItem>) {
        // ✅ Clear selection on update
        newList.filterIsInstance<FileListItem.FileItem>()
            .forEach { it.file.isSelected = false }

        fileList = newList.toMutableList()
        notifyDataSetChanged()
    }





    override fun getItemCount(): Int = fileList.size
}
