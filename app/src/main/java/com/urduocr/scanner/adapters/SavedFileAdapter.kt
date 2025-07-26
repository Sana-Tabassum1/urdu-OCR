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
import com.urduocr.scanner.databinding.ItemFileHeaderBinding
import com.urduocr.scanner.databinding.ItemSavedFileBinding
import com.urduocr.scanner.models.FileListItem
import java.io.File

class SavedFileAdapter(
    private val context: Context,
    private var fileList: List<FileListItem>,
    private val listener: FileAdapterListener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_FILE = 1
    }

    private var isSelectionMode = false

    inner class FileViewHolder(val binding: ItemSavedFileBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HeaderViewHolder(val binding: ItemFileHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

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
            val binding =
                ItemFileHeaderBinding.inflate(LayoutInflater.from(context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding =
                ItemSavedFileBinding.inflate(LayoutInflater.from(context), parent, false)
            FileViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = fileList[position]) {
            is FileListItem.Header -> {
                (holder as HeaderViewHolder).binding.headerText.text = item.title
            }

            is FileListItem.FileItem -> {
                val fileHolder = holder as FileViewHolder
                val file = item.file
                val fileObj = File(file.path)

                // File icon
                when {
                    file.name.endsWith(".png", true) -> fileHolder.binding.ivFileIcon.setImageResource(R.drawable.png)
                    file.name.endsWith(".pdf", true) -> fileHolder.binding.ivFileIcon.setImageResource(R.drawable.pdf)
                    file.name.endsWith(".txt", true) -> fileHolder.binding.ivFileIcon.setImageResource(R.drawable.txt)
                }

                fileHolder.binding.tvFileName.text = file.name

                // Date + Size
                val formattedDate =
                    DateFormat.format("dd/MM/yyyy", fileObj.lastModified()).toString()
                val sizeInBytes = fileObj.length()
                val sizeFormatted = if (sizeInBytes < 1024 * 1024) {
                    String.format("%.1f KB", sizeInBytes / 1024f)
                } else {
                    String.format("%.1f MB", sizeInBytes / (1024f * 1024f))
                }
                fileHolder.binding.tvTimeSize.text =
                    "SIZE: $sizeFormatted    DATE: $formattedDate"

                // ✅ Show selection icon only in selection mode
                fileHolder.binding.checkBoxSelect.visibility =
                    if (isSelectionMode) View.VISIBLE else View.GONE

                if (isSelectionMode) {
                    fileHolder.binding.checkBoxSelect.setImageResource(
                        if (file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
                    )
                }

                // ✅ Click to toggle selection
                fileHolder.binding.root.setOnClickListener {
                    if (isSelectionMode) {
                        file.isSelected = !file.isSelected

                        // 🔹 If no items are selected now → exit selection mode
                        if (getSelectedCount() == 0) {
                            isSelectionMode = false
                        }

                        notifyDataSetChanged()
                        listener?.onItemSelectionChanged()
                    } else {
                        openFile(fileObj, file.name)
                    }
                }

                // ✅ Long click → enable selection mode
                fileHolder.binding.root.setOnLongClickListener {
                    isSelectionMode = true
                    file.isSelected = true
                    notifyDataSetChanged()
                    listener?.onItemSelectionChanged()
                    true
                }

                // ✅ Menu button logic
                fileHolder.binding.menuButton.setOnClickListener { view ->
                    val inflater = LayoutInflater.from(context)
                    val popupView = inflater.inflate(R.layout.custom_file_popup, null)
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
                        file.isSelected = true
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
        }
    }

    private fun openFile(fileObj: File, name: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                fileObj
            )
            val mimeType = when {
                name.endsWith(".pdf") -> "application/pdf"
                name.endsWith(".txt") -> "text/plain"
                name.endsWith(".png") -> "image/png"
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

    // ✅ Utility
    fun getSelectedCount(): Int {
        return fileList.count { it is FileListItem.FileItem && it.file.isSelected }
    }

    fun clearSelection() {
        isSelectionMode = false
        fileList.filterIsInstance<FileListItem.FileItem>().forEach { it.file.isSelected = false }
        notifyDataSetChanged()
    }

    fun getSelectedFiles(): List<FileListItem.FileItem> {
        return fileList.filterIsInstance<FileListItem.FileItem>().filter { it.file.isSelected }
    }

    fun updateList(newList: List<FileListItem>) {
        newList.filterIsInstance<FileListItem.FileItem>().forEach { it.file.isSelected = false }
        fileList = newList
        isSelectionMode = false
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = fileList.size
}
