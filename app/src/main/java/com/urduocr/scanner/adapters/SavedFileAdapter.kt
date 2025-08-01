package com.urduocr.scanner.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.text.format.DateFormat
import android.util.TypedValue
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.ItemFileHeaderBinding
import com.urduocr.scanner.databinding.ItemSavedFileBinding
import com.urduocr.scanner.interfaces.OnFileActionListener
import com.urduocr.scanner.models.FileListItem
import java.io.File

class SavedFileAdapter(
    private val context: Context,
    private var fileList: List<FileListItem>,
    var listener: OnSelectionChangedListener? = null,
    var fileActionListener: OnFileActionListener? = null,
    var onFolderClick: ((File) -> Unit)? = null

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_FILE = 1
    }

    interface OnSelectionChangedListener {
        fun onItemSelectionChanged()
    }

    private var isSelectionMode = false

    inner class FileViewHolder(val binding: ItemSavedFileBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HeaderViewHolder(val binding: ItemFileHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)


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
                    file.isFolder -> fileHolder.binding.ivFileIcon.setImageResource(R.drawable.saved_folder)
                    file.name.endsWith(
                        ".png",
                        true
                    ) -> fileHolder.binding.ivFileIcon.setImageResource(R.drawable.png)

                    file.name.endsWith(
                        ".pdf",
                        true
                    ) -> fileHolder.binding.ivFileIcon.setImageResource(R.drawable.pdf)

                    file.name.endsWith(
                        ".txt",
                        true
                    ) -> fileHolder.binding.ivFileIcon.setImageResource(R.drawable.txt)
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
                val selectedCount = fileList.count { it is FileListItem.FileItem && it.file.isSelected }



                fileHolder.binding.menuButton.visibility = if (isSelectionMode) {
                    View.GONE
                } else {
                    View.VISIBLE
                }


// 3. Checkbox visibility - show only in selection mode AND if items are selected
                fileHolder.binding.checkBoxSelect.visibility =
                    if (isSelectionMode && getSelectedCount() > 0) View.VISIBLE else View.GONE




                if (isSelectionMode) {
                    fileHolder.binding.checkBoxSelect.setImageResource(
                        if (file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
                    )
                }
                fileHolder.binding.root.setOnLongClickListener {
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        file.isSelected = true
                        notifyDataSetChanged()
                        listener?.onItemSelectionChanged()
                    }
                    true // important: to consume the long click
                }


                // ✅ Click to toggle selection
                fileHolder.binding.root.setOnClickListener {
                    if (isSelectionMode) {
                        file.isSelected = !file.isSelected
                        if (getSelectedCount() == 0) {
                            isSelectionMode = false
                        }
                        notifyDataSetChanged()
                        listener?.onItemSelectionChanged()
                    } else {
                        if (file.isFolder) {
                            onFolderClick?.invoke(File(file.path))
                        } else {
                            openFile(fileObj, file.name)
                        }
                    }
                }



                // ✅ Menu button logic
                fileHolder.binding.menuButton.setOnClickListener { anchorView ->
                    val context = anchorView.context
                    val density = context.resources.displayMetrics.density
                    val popupWidth = (150 * density).toInt() // Fixed 200dp width

                    val popupView = LayoutInflater.from(context).inflate(R.layout.custom_file_popup, null)
                    val popupWindow = PopupWindow(
                        popupView,
                        popupWidth,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        true
                    )

                    popupWindow.elevation = 10f
                    popupWindow.isOutsideTouchable = true
                    popupWindow.isFocusable = true

                    // Measure height after inflating
                    popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    val popupHeight = popupView.measuredHeight

                    // Get screen dimensions
                    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                    val screenWidth = Resources.getSystem().displayMetrics.widthPixels

                    // Get anchor view location on screen
                    val location = IntArray(2)
                    anchorView.getLocationOnScreen(location)
                    val anchorX = location[0]
                    val anchorY = location[1]
                    val anchorHeight = anchorView.height

                    val bottomSpace = screenHeight - (anchorY + anchorHeight)
                    val topSpace = anchorY

                    val x = anchorX + anchorView.width - popupWidth // Align right edge of popup to anchor
                    val y = if (bottomSpace >= popupHeight) {
                        // Show below anchor
                        anchorY + anchorHeight
                    } else if (topSpace >= popupHeight) {
                        // Show above anchor
                        anchorY - popupHeight
                    } else {
                        // Fit within available space (e.g., center aligned)
                        (screenHeight - popupHeight) / 2
                    }

                    // Show at calculated position
                    popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x.coerceAtLeast(0), y)

                    // Get selected files
                    val selectedFiles = getSelectedFiles()
                    val isMultiSelect = selectedFiles.size > 1

                    fileHolder.binding.menuButton.visibility =
                        if (file.isSelected) View.GONE else View.VISIBLE


                    popupView.findViewById<LinearLayout>(R.id.menushare).visibility =
                        if (file.isFolder) View.GONE else View.VISIBLE

                    // Inside the popup menu setup
                    popupView.findViewById<LinearLayout>(R.id.menupin).visibility =
                        if (file.isPinned) View.GONE else View.VISIBLE

                    popupView.findViewById<LinearLayout>(R.id.menuUnpin).visibility =
                        if (file.isPinned) View.VISIBLE else View.GONE


                    // [Keep your existing popup positioning code here]
                    // Menu button visibility - hide when selected or for folders
                    fileHolder.binding.menuButton.visibility = when {
                        file.isSelected -> View.GONE
                        else -> View.VISIBLE
                    }

                    // Set up click listeners
                    popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
                        isSelectionMode = true
                        file.isSelected = true
                        notifyDataSetChanged()
                        listener?.onItemSelectionChanged()
                        popupWindow.dismiss()
                    }

                    popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
                        if (isSelectionMode) {
                            selectedFiles.forEach { fileItem ->
                                fileActionListener?.onCopy(File(fileItem.file.path))
                            }
                        } else {
                            fileActionListener?.onCopy(fileObj)
                        }
                        popupWindow.dismiss()
                        clearSelection()
                    }

                    popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
                        if (isSelectionMode) {
                            selectedFiles.forEach { fileItem ->
                                fileActionListener?.onCut(File(fileItem.file.path))
                            }
                        } else {
                            fileActionListener?.onCut(fileObj)
                        }
                        popupWindow.dismiss()
                        clearSelection()
                    }

                    // [Keep other menu item click listeners unchanged]
                    popupView.findViewById<LinearLayout>(R.id.menupin).setOnClickListener {
                        fileActionListener?.onPin(fileObj)
                        popupWindow.dismiss()
                        clearSelection()
                    }

                    popupView.findViewById<LinearLayout>(R.id.menuUnpin).setOnClickListener {
                        fileActionListener?.onUnpin(fileObj)
                        popupWindow.dismiss()
                        clearSelection()
                    }
//                    // View File
//                    popupView.findViewById<LinearLayout>(R.id.menuviewfile).setOnClickListener {
//                        if (!file.isFolder) {
//                            openFile(fileObj, file.name)
//                        } else {
//                            Toast.makeText(context, "Cannot view a folder", Toast.LENGTH_SHORT).show()
//                        }
//                        popupWindow.dismiss()
//                    }

// Share File
                    popupView.findViewById<LinearLayout>(R.id.menushare).setOnClickListener {
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                context.packageName + ".fileprovider",
                                fileObj
                            )
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, uri)
                                type = when {
                                    file.name.endsWith(".pdf") -> "application/pdf"
                                    file.name.endsWith(".txt") -> "text/plain"
                                    file.name.endsWith(".png") -> "image/png"
                                    else -> "*/*"
                                }
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share file via"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to share: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        popupWindow.dismiss()
                    }

// Delete File
                    popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
                        // Show confirmation dialog
                        showDeleteConfirmationDialog(fileObj) {
                            // This lambda will execute if user confirms deletion
                            if (fileObj.exists()) {
                                if (fileObj.deleteRecursively()) {
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                    fileActionListener?.onDelete(fileObj)
                                } else {
                                    Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        popupWindow.dismiss()
                    }


                }


            }
        }
    }
//viewfile open
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
    private fun showDeleteConfirmationDialog(file: File, onConfirm: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }
    override fun getItemCount(): Int = fileList.size
}
