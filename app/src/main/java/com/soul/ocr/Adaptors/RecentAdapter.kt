package com.soul.ocr.Adaptors

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.soul.ocr.ModelClass.FileListItem
import com.soul.ocr.R
import com.soul.ocr.databinding.ItemSavedFileBinding
import java.io.File

class RecentAdapter(
    private val context: Context,
    var fileList: List<FileListItem.FileItem> // ⬅️ Only FileItems now
) : RecyclerView.Adapter<RecentAdapter.FileViewHolder>() {

    inner class FileViewHolder(val binding: ItemSavedFileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemSavedFileBinding.inflate(LayoutInflater.from(context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val filePath = fileList[position].file.path
        val file = File(filePath)

        holder.binding.tvFileName.text = file.name

        val lastModified = file.lastModified()
        val time = android.text.format.DateFormat.format("hh:mm a", lastModified).toString()
        val diff = System.currentTimeMillis() - lastModified
        val hours = diff / (1000 * 60 * 60)

        val dateLabel = when {
            hours < 24 -> "Today"
            hours in 24..48 -> "Yesterday"
            else -> android.text.format.DateFormat.format("dd MMM yyyy", lastModified).toString()
        }

        val sizeInBytes = file.length()
        val sizeFormatted = if (sizeInBytes < 1024 * 1024) {
            String.format("%.1f KB", sizeInBytes / 1024f)
        } else {
            String.format("%.1f MB", sizeInBytes / (1024f * 1024f))
        }

        holder.binding.tvTimeSize.text = "$dateLabel - $time\n$sizeFormatted"

        when {
            file.name.endsWith(".pdf", ignoreCase = true) -> {
                holder.binding.ivFileIcon.setImageResource(R.drawable.pdf)
            }
            file.name.endsWith(".txt", ignoreCase = true) -> {
                holder.binding.ivFileIcon.setImageResource(R.drawable.txt)
            }
            file.name.endsWith(".png", ignoreCase = true)
                    || file.name.endsWith(".jpg", ignoreCase = true)
                    || file.name.endsWith(".jpeg", ignoreCase = true) -> {
                holder.binding.ivFileIcon.setImageResource(R.drawable.png)
            }
            else -> {
                // holder.binding.ivFileIcon.setImageResource(R.drawable.ic_unknown_file)
            }
        }

        holder.binding.textViewFile.setOnClickListener {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    file
                )

                val mimeType = when {
                    file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    file.name.endsWith(".txt", ignoreCase = true) -> "text/plain"
                    file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                    file.name.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
                    else -> "*/*"
                }

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Can't open this file: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
    fun updateList(newList: List<FileListItem.FileItem>) {
        fileList = newList
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int = fileList.size
}
