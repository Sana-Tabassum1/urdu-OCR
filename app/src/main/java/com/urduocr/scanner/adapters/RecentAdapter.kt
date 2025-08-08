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
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.ItemSavedFileBinding
import com.urduocr.scanner.interfaces.OnFileActionListener
import com.urduocr.scanner.models.FileListItem
import java.io.File

class RecentAdapter(
    private val context: Context,
    private var fileList: List<FileListItem.FileItem>,
    var listener: OnSelectionChangedListener? = null,
    var fileActionListener: OnFileActionListener? = null
) : RecyclerView.Adapter<RecentAdapter.FileViewHolder>() {

    interface OnSelectionChangedListener {
        fun onItemSelectionChanged()
    }

    private var isSelectionMode = false

    inner class FileViewHolder(val binding: ItemSavedFileBinding) : RecyclerView.ViewHolder(binding.root)

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
            val formattedDate = DateFormat.format("dd/MM/yyyy", file.lastModified()).toString()
            val sizeFormatted = formatFileSize(file.length())
            tvTimeSize.text = "SIZE: $sizeFormatted    DATE: $formattedDate"

            // File icon
            setFileIcon(ivFileIcon, file)

            // Selection mode UI
            val shouldShowCheckbox = isSelectionMode && (item.file.isSelected || getSelectedCount() > 0)
            checkBoxSelect.visibility = if (shouldShowCheckbox) View.VISIBLE else View.GONE
            checkBoxSelect.setImageResource(
                if (item.file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
            )

            materialCardView.isSelected = item.file.isSelected
            menuButton.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

            // Click listeners
            root.setOnClickListener {
                if (isSelectionMode) {
                    item.file.isSelected = !item.file.isSelected
                    if (getSelectedCount() == 0) {
                        isSelectionMode = false
                        notifyDataSetChanged()
                    } else {
                        notifyItemChanged(position)
                    }
                    listener?.onItemSelectionChanged()
                } else {
                    openFile(file)
                }
            }

            root.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    item.file.isSelected = true
                    notifyDataSetChanged()
                    listener?.onItemSelectionChanged()
                }
                true
            }

            // Popup menu
            menuButton.setOnClickListener { view ->
                showFilePopupMenu(view, item, file, position)
            }
        }
    }

    private fun setFileIcon(imageView: ImageView, file: File) {
        imageView.setImageResource(
            when {
                file.name.endsWith(".pdf", true) -> R.drawable.pdf
                file.name.endsWith(".txt", true) -> R.drawable.txt
                file.name.endsWith(".png", true) ||
                        file.name.endsWith(".jpg", true) ||
                        file.name.endsWith(".jpeg", true) -> R.drawable.png
                else -> R.drawable.saved_folder
            }
        )
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        return if (sizeInBytes < 1024 * 1024) {
            String.format("%.1f KB", sizeInBytes / 1024f)
        } else {
            String.format("%.1f MB", sizeInBytes / (1024f * 1024f))
        }
    }

    private fun showFilePopupMenu(
        anchorView: View,
        item: FileListItem.FileItem,
        file: File,
        position: Int
    ) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.custom_file_popup, null)
        val popupWindow = PopupWindow(
            popupView,
            (180 * context.resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
        }

        // Hide folder-specific options since RecentAdapter only deals with files
        popupView.findViewById<LinearLayout>(R.id.renameFolder).visibility = View.GONE
        popupView.findViewById<View>(R.id.viewRename).visibility = View.GONE

        // Position calculation
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupHeight = popupView.measuredHeight

        val x = location[0] + anchorView.width - popupWindow.width
        val y = if (location[1] + anchorView.height + popupHeight <= screenHeight) {
            location[1] + anchorView.height
        } else {
            location[1] - popupHeight
        }

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x.coerceAtLeast(0), y)

        // Menu item click listeners
        popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
            }
            item.file.isSelected = true
            notifyDataSetChanged()
            listener?.onItemSelectionChanged()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
            if (isSelectionMode) {
                getSelectedFiles().forEach { fileItem ->
                    fileActionListener?.onCopy(File(fileItem.file.path))
                }
            } else {
                fileActionListener?.onCopy(file)
            }
            popupWindow.dismiss()
            clearSelection()
        }

        popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
            if (isSelectionMode) {
                getSelectedFiles().forEach { fileItem ->
                    fileActionListener?.onCut(File(fileItem.file.path))
                }
            } else {
                fileActionListener?.onCut(file)
            }
            popupWindow.dismiss()
            clearSelection()
        }

        popupView.findViewById<LinearLayout>(R.id.menupin).setOnClickListener {
            item.file.isPinned = true
            notifyItemChanged(position)
            fileActionListener?.onPin(file)
            popupWindow.dismiss()
            clearSelection()
        }

        popupView.findViewById<LinearLayout>(R.id.menuUnpin).setOnClickListener {
            item.file.isPinned = false
            notifyItemChanged(position)
            fileActionListener?.onUnpin(file)
            popupWindow.dismiss()
            clearSelection()
        }

        popupView.findViewById<LinearLayout>(R.id.menushare).setOnClickListener {
            shareFile(file)
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
            showDeleteConfirmationDialog(file) {
                fileActionListener?.onDelete(file)
            }
            popupWindow.dismiss()
        }
    }

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

    private fun showDeleteConfirmationDialog(file: File, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameFileDialog(file: File) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.rename_file, null)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val etFileName = dialogView.findViewById<EditText>(R.id.etFileName)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancle)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSave)

        etFileName.setText(file.name)
        etFileName.selectAll()

        fun highlightButton(selectedBtn: TextView) {
            listOf(btnCancel, btnSave).forEach { button ->
                val isSelected = button == selectedBtn
                button.setBackgroundResource(
                    if (isSelected) R.drawable.button_selector
                    else R.drawable.unselected_button
                )
                button.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (isSelected) android.R.color.white else android.R.color.black
                    )
                )
            }
        }

        highlightButton(btnSave)

        btnCancel.setOnClickListener {
            highlightButton(it as TextView)
            it.postDelayed({ dialog.dismiss() }, 150)
        }

        btnSave.setOnClickListener {
            highlightButton(it as TextView)
            val newName = etFileName.text.toString().trim()
            when {
                newName.isEmpty() -> Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                newName == file.name -> dialog.dismiss()
                else -> {
                    fileActionListener?.onRenameFile(file, newName)
                    dialog.dismiss()
                }
            }
        }

        btnCancel.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) highlightButton(btnCancel) }
        btnSave.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) highlightButton(btnSave) }

        dialog.show()
    }

    // Utility methods
    fun getSelectedCount(): Int = fileList.count { it.file.isSelected }
    fun getSelectedFiles(): List<FileListItem.FileItem> = fileList.filter { it.file.isSelected }
    fun clearSelection() {
        isSelectionMode = false
        fileList.forEach { it.file.isSelected = false }
        notifyDataSetChanged()
        listener?.onItemSelectionChanged()
    }
    fun updateList(newList: List<FileListItem.FileItem>) {
        fileList = newList
        isSelectionMode = false
        notifyDataSetChanged()
    }
    fun selectAllItems() {
        fileList.forEach { it.file.isSelected = true }
        isSelectionMode = true
        notifyDataSetChanged()
        listener?.onItemSelectionChanged()
    }

    override fun getItemCount(): Int = fileList.size
}