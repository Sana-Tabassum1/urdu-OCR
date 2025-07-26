package com.urduocr.scanner.adapters

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.ItemSavedFileBinding
import com.urduocr.scanner.models.FileListItem
import java.io.File

class RecentAdapter(
    private val context: Context,
    private var fileList: List<FileListItem.FileItem>,
    private val listener: FileAdapterListener? // ✅ NEW: to notify activity/fragment
) : RecyclerView.Adapter<RecentAdapter.FileViewHolder>() {

    private var isSelectionMode = false

    inner class FileViewHolder(val binding: ItemSavedFileBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface FileAdapterListener {
        fun onItemSelectionChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemSavedFileBinding.inflate(LayoutInflater.from(context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = fileList[position]
        val file = File(item.file.path)

        holder.binding.tvFileName.text = file.name

        // ✅ Date + size
        val formattedDate = DateFormat.format("dd/MM/yyyy", file.lastModified()).toString()
        val sizeInBytes = file.length()
        val sizeFormatted = if (sizeInBytes < 1024 * 1024) {
            String.format("%.1f KB", sizeInBytes / 1024f)
        } else {
            String.format("%.1f MB", sizeInBytes / (1024f * 1024f))
        }
        holder.binding.tvTimeSize.text = "SIZE: $sizeFormatted    DATE: $formattedDate"

        // ✅ Set icon
        when {
            file.name.endsWith(".pdf", true) -> holder.binding.ivFileIcon.setImageResource(R.drawable.pdf)
            file.name.endsWith(".txt", true) -> holder.binding.ivFileIcon.setImageResource(R.drawable.txt)
            file.name.endsWith(".png", true) ||
                    file.name.endsWith(".jpg", true) ||
                    file.name.endsWith(".jpeg", true) -> holder.binding.ivFileIcon.setImageResource(R.drawable.png)
        }

        // ✅ Show selection icon only if selection mode is active
        holder.binding.checkBoxSelect.visibility =
            if (isSelectionMode) View.VISIBLE else View.GONE

        if (isSelectionMode) {
            holder.binding.checkBoxSelect.setImageResource(
                if (item.file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
            )
        }

        // ✅ Click → toggle selection
        holder.binding.root.setOnClickListener {
            if (isSelectionMode) {
                item.file.isSelected = !item.file.isSelected

                if (getSelectedCount() == 0) {
                    isSelectionMode = false // Exit mode if no selection left
                }

                notifyDataSetChanged()
                listener?.onItemSelectionChanged()
            } else {
                openFile(file)
            }
        }

        // ✅ Long click → start selection mode
        holder.binding.root.setOnLongClickListener {
            isSelectionMode = true
            item.file.isSelected = true
            notifyDataSetChanged()
            listener?.onItemSelectionChanged()
            true
        }

        // ✅ Popup menu (3-dot)
        holder.binding.menuButton.setOnClickListener { view ->
            val popupView =
                LayoutInflater.from(context).inflate(R.layout.custom_file_popup, null)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.elevation = 10f
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true

            popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
                isSelectionMode = true
                item.file.isSelected = true
                notifyDataSetChanged()
                listener?.onItemSelectionChanged()
                popupWindow.dismiss()
            }

            popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
                Toast.makeText(context, "Copy clicked", Toast.LENGTH_SHORT).show()
                popupWindow.dismiss()
            }

            popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
                Toast.makeText(context, "Cut clicked", Toast.LENGTH_SHORT).show()
                popupWindow.dismiss()
            }

            popupView.findViewById<LinearLayout>(R.id.menuPaste).setOnClickListener {
                Toast.makeText(context, "Paste clicked", Toast.LENGTH_SHORT).show()
                popupWindow.dismiss()
            }

            popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
                Toast.makeText(context, "Delete clicked", Toast.LENGTH_SHORT).show()
                popupWindow.dismiss()
            }

            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupWidth = popupView.measuredWidth
            popupWindow.showAsDropDown(view, view.width - popupWidth, 0)
        }
    }

    // ✅ Open file
    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val mimeType = when {
                file.name.endsWith(".pdf", true) -> "application/pdf"
                file.name.endsWith(".txt", true) -> "text/plain"
                file.name.endsWith(".png", true) -> "image/png"
                file.name.endsWith(".jpg", true) -> "image/jpeg"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "Can't open: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ✅ Utility Methods
    fun getSelectedCount(): Int {
        return fileList.count { it.file.isSelected }
    }

    fun getSelectedFiles(): List<FileListItem.FileItem> {
        return fileList.filter { it.file.isSelected }
    }

    fun clearSelection() {
        isSelectionMode = false
        fileList.forEach { it.file.isSelected = false }
        notifyDataSetChanged()
    }

    fun updateList(newList: List<FileListItem.FileItem>) {
        newList.forEach { it.file.isSelected = false }
        fileList = newList
        isSelectionMode = false
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = fileList.size
}
