package com.urduocr.scanner.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.text.format.DateFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.ItemSavedFileBinding
import com.urduocr.scanner.interfaces.OnFileActionListener
import com.urduocr.scanner.models.FileListItem
import java.io.File

class RecentAdapter(
    private val context: Context,
    private var fileList: List<FileListItem.FileItem>,
    private val listener: FileAdapterListener?,
    var fileActionListener: OnFileActionListener? = null
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

        with(holder.binding) {
            // File info setup
            tvFileName.text = file.name

            // Show relative time (e.g., "2 hours ago")
            val timeAgo = getTimeAgo(item.file.lastModified)
            val sizeInBytes = file.length()
            val sizeFormatted = if (sizeInBytes < 1024 * 1024) {
                String.format("%.1f KB", sizeInBytes / 1024f)
            } else {
                String.format("%.1f MB", sizeInBytes / (1024f * 1024f))
            }
            tvTimeSize.text = "SIZE: $sizeFormatted    DATE: $timeAgo"

            // File icon
            when {
                file.name.endsWith(".pdf", true) -> ivFileIcon.setImageResource(R.drawable.pdf)
                file.name.endsWith(".txt", true) -> ivFileIcon.setImageResource(R.drawable.txt)
                file.name.endsWith(".png", true) ||
                        file.name.endsWith(".jpg", true) ||
                        file.name.endsWith(".jpeg", true) -> ivFileIcon.setImageResource(R.drawable.png)
            }
//
//            // Selection mode UI
//            checkBoxSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
//            if (isSelectionMode) {
//                checkBoxSelect.setImageResource(
//                    if (item.file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
//                )
//            }
//
//            // Menu button visibility
//            menuButton.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

//            // Click listeners
//            root.setOnClickListener {
//                if (isSelectionMode) {
//                    item.file.isSelected = !item.file.isSelected
//                    if (getSelectedCount() == 0) isSelectionMode = false
//                    notifyDataSetChanged()
//                    listener?.onItemSelectionChanged()
//                } else {
//                    openFile(file)
//                }
//            }
//
//            root.setOnLongClickListener {
//                if (!isSelectionMode) {
//                    isSelectionMode = true
//                    item.file.isSelected = true
//                    notifyDataSetChanged()
//                    listener?.onItemSelectionChanged()
//                }
//                true
//            }
//
//            // Popup menu
//            menuButton.setOnClickListener { view ->
//                showFilePopupMenu(view, item, file)
//            }
        }
    }

//    private fun showFilePopupMenu(anchorView: View, item: FileListItem.FileItem, file: File) {
//        val popupView = LayoutInflater.from(context).inflate(R.layout.custom_file_popup, null)
//        val popupWindow = PopupWindow(
//            popupView,
//            (150 * context.resources.displayMetrics.density).toInt(),
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            true
//        ).apply {
////            elevation = 10f
//            isOutsideTouchable = true
//            isFocusable = true
//        }
//
//        // Position calculation
//        val location = IntArray(2)
//        anchorView.getLocationOnScreen(location)
//        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
//        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
//        val popupHeight = popupView.measuredHeight
//
//        val x = location[0] + anchorView.width - popupWindow.width
//        val y = if (location[1] + anchorView.height + popupHeight <= screenHeight) {
//            location[1] + anchorView.height
//        } else {
//            location[1] - popupHeight
//        }
//
//        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x.coerceAtLeast(0), y)
//
//        // Menu item click listeners
//        popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
//            isSelectionMode = true
//            item.file.isSelected = true
//            notifyDataSetChanged()
//            listener?.onItemSelectionChanged()
//            popupWindow.dismiss()
//        }
//
//        popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
//            fileActionListener?.onCopy(file)
//            popupWindow.dismiss()
//            clearSelection()
//        }
//
//        popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
//            fileActionListener?.onCut(file)
//            popupWindow.dismiss()
//            clearSelection()
//        }
//
//        popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
//            showDeleteConfirmation(file) {
//                fileActionListener?.onDelete(file)
//            }
//            popupWindow.dismiss()
//        }
//
//        popupView.findViewById<LinearLayout>(R.id.menushare).setOnClickListener {
//            shareFile(file)
//            popupWindow.dismiss()
//        }
//    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
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

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = when {
                    file.name.endsWith(".pdf", true) -> "application/pdf"
                    file.name.endsWith(".txt", true) -> "text/plain"
                    file.name.endsWith(".png", true) -> "image/png"
                    file.name.endsWith(".jpg", true) -> "image/jpeg"
                    else -> "*/*"
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share file via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteConfirmation(file: File, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Utility methods
    fun getSelectedCount(): Int = fileList.count { it.file.isSelected }
    fun getSelectedFiles(): List<FileListItem.FileItem> = fileList.filter { it.file.isSelected }
    fun clearSelection() {
        isSelectionMode = false
        fileList.forEach { it.file.isSelected = false }
        notifyDataSetChanged()
    }
    fun updateList(newList: List<FileListItem.FileItem>) {
        fileList = newList
        isSelectionMode = false
        notifyDataSetChanged()
    }

    private fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timestamp

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
            else -> DateFormat.format("dd MMM yyyy", timestamp).toString()
        }
    }
    override fun getItemCount(): Int = fileList.size
}