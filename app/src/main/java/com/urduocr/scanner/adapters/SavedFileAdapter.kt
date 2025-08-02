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
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.ItemFileHeaderBinding
import com.urduocr.scanner.databinding.ItemSavedFileBinding
import com.urduocr.scanner.databinding.ItemSavedFileGridBinding
import com.urduocr.scanner.interfaces.OnFileActionListener
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.models.InternalFileModel
import java.io.File

class SavedFileAdapter(
    private val context: Context,
    private var fileList: List<FileListItem>,
    var listener: OnSelectionChangedListener? = null,
    var fileActionListener: OnFileActionListener? = null,
    var onFolderClick: ((File) -> Unit)? = null,


) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_FILE_LIST = 1
        private const val VIEW_TYPE_FILE_GRID = 2
    }

    // Add this variable to your adapter
    var isGridView = false

    interface OnSelectionChangedListener {
        fun onItemSelectionChanged()
    }

    private var isSelectionMode = false

    inner class FileViewHolder(val binding: ItemSavedFileBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HeaderViewHolder(val binding: ItemFileHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Add this ViewHolder class
    inner class FileGridViewHolder(val binding: ItemSavedFileGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (fileList[position]) {
            is FileListItem.Header -> VIEW_TYPE_HEADER
            is FileListItem.FileItem -> if (isGridView) VIEW_TYPE_FILE_GRID else VIEW_TYPE_FILE_LIST
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    ItemFileHeaderBinding.inflate(
                        LayoutInflater.from(context), parent, false
                    )
                )
            }

            VIEW_TYPE_FILE_LIST -> {
                FileViewHolder(
                    ItemSavedFileBinding.inflate(
                        LayoutInflater.from(context), parent, false
                    )
                )
            }

            VIEW_TYPE_FILE_GRID -> {
                FileGridViewHolder(
                    ItemSavedFileGridBinding.inflate(
                        LayoutInflater.from(context), parent, false
                    )
                )
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = fileList[position]) {
            is FileListItem.Header -> {
                (holder as HeaderViewHolder).binding.headerText.text = item.title
            }

            is FileListItem.FileItem -> {
                val file = item.file
                val fileObj = File(file.path)
                val formattedDate = DateFormat.format("dd/MM/yyyy", fileObj.lastModified()).toString()
                val sizeFormatted = formatFileSize(fileObj.length())

                when {
                    isGridView && holder is FileGridViewHolder -> {
                        // Grid View Binding
                        with(holder.binding) {
                            setFileIcon(ivFileIcon, file)
                            tvFileName.text = file.name
                            tvTimeSize.text = "$sizeFormatted, $formattedDate" // Always compact for grid
                            handleSelectionUI(this, file, position)
                        }
                    }
                    holder is FileViewHolder -> {
                        // List View Binding
                        with(holder.binding) {
                            setFileIcon(ivFileIcon, file)
                            tvFileName.text = file.name
                            tvTimeSize.text = "SIZE: $sizeFormatted    DATE: $formattedDate" // Always detailed for list
                            handleSelectionUI(this, file, position)
                        }
                    }
                }
            }
        }
    }

    // Helper function to set file icons
    private fun setFileIcon(imageView: ImageView, file: InternalFileModel) {
        imageView.setImageResource(
            when {
                file.isFolder -> R.drawable.saved_folder
                file.name.endsWith(".png", true) -> R.drawable.png
                file.name.endsWith(".pdf", true) -> R.drawable.pdf
                file.name.endsWith(".txt", true) -> R.drawable.txt
                else -> R.drawable.saved_folder
            }
        )
    }

    // Helper function to format file size
    private fun formatFileSize(sizeInBytes: Long): String {
        return if (sizeInBytes < 1024 * 1024) {
            String.format("%.1f KB", sizeInBytes / 1024f)
        } else {
            String.format("%.1f MB", sizeInBytes / (1024f * 1024f))
        }
    }

    // Helper function to handle selection UI
    private fun handleSelectionUI(binding: ItemSavedFileBinding, file: InternalFileModel, position: Int) {
        with(binding) {
            // Selection handling
            checkBoxSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            if (isSelectionMode) {
                checkBoxSelect.setImageResource(
                    if (file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
                )
            }

            // Menu button visibility
            menuButton.visibility = when {
                file.isSelected || isSelectionMode -> View.GONE
                else -> View.VISIBLE
            }

            // Click listeners
            root.setOnClickListener {
                if (isSelectionMode) {
                    file.isSelected = !file.isSelected
                    if (getSelectedCount() == 0) isSelectionMode = false
                    notifyDataSetChanged()
                    listener?.onItemSelectionChanged()
                } else {
                    if (file.isFolder) {
                        onFolderClick?.invoke(File(file.path))
                    } else {
                        openFile(File(file.path), file.name)
                    }
                }
            }

            root.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    file.isSelected = true
                    notifyDataSetChanged()
                    listener?.onItemSelectionChanged()
                }
                true
            }

            menuButton.setOnClickListener { anchorView ->
                showFilePopupMenu(anchorView, file, File(file.path), position)
            }
        }
    }

    private fun handleSelectionUI(binding: ItemSavedFileGridBinding, file: InternalFileModel, position: Int) {
        with(binding) {
            // Selection handling
            checkBoxSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            if (isSelectionMode) {
                checkBoxSelect.setImageResource(
                    if (file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
                )
            }

            // Menu button visibility
            menuButton.visibility = when {
                file.isSelected || isSelectionMode -> View.GONE
                else -> View.VISIBLE
            }

            // Click listeners
            root.setOnClickListener {
                if (isSelectionMode) {
                    file.isSelected = !file.isSelected
                    if (getSelectedCount() == 0) isSelectionMode = false
                    notifyDataSetChanged()
                    listener?.onItemSelectionChanged()
                } else {
                    if (file.isFolder) {
                        onFolderClick?.invoke(File(file.path))
                    } else {
                        openFile(File(file.path), file.name)
                    }
                }
            }

            root.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    file.isSelected = true
                    notifyDataSetChanged()
                    listener?.onItemSelectionChanged()
                }
                true
            }

            menuButton.setOnClickListener { anchorView ->
                showFilePopupMenu(anchorView, file, File(file.path), position)
            }
        }
    }

    // Extract popup menu logic to a separate function
    private fun showFilePopupMenu(
        anchorView: View,
        file: InternalFileModel,
        fileObj: File,
        position: Int
    ) {
        val context = anchorView.context
        val density = context.resources.displayMetrics.density
        val popupWidth = (150 * density).toInt()

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

        // Measure and position popup
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupHeight = popupView.measuredHeight

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        val anchorHeight = anchorView.height

        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val bottomSpace = screenHeight - (anchorY + anchorHeight)
        val topSpace = anchorY

        val x = anchorX + anchorView.width - popupWidth
        val y = if (bottomSpace >= popupHeight) {
            anchorY + anchorHeight
        } else if (topSpace >= popupHeight) {
            anchorY - popupHeight
        } else {
            (screenHeight - popupHeight) / 2
        }

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x.coerceAtLeast(0), y)


        // Get selected files
        val selectedFiles = getSelectedFiles()
        val isMultiSelect = selectedFiles.size > 1
        // Menu item visibility
        popupView.findViewById<LinearLayout>(R.id.menushare).visibility =
            if (file.isFolder) View.GONE else View.VISIBLE

        popupView.findViewById<LinearLayout>(R.id.menupin).visibility =
            if (file.isPinned) View.GONE else View.VISIBLE

        popupView.findViewById<LinearLayout>(R.id.menuUnpin).visibility =
            if (file.isPinned) View.VISIBLE else View.GONE

        popupView.findViewById<LinearLayout>(R.id.renameFolder).visibility =
            if (file.isFolder) View.VISIBLE else View.GONE

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
                getSelectedFiles().forEach { fileItem ->
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
            file.isPinned = true
            notifyItemChanged(position)
            fileActionListener?.onPin(fileObj)
            popupWindow.dismiss()
            clearSelection()
        }

        popupView.findViewById<LinearLayout>(R.id.menuUnpin).setOnClickListener {
            file.isPinned = false
            notifyItemChanged(position)
            fileActionListener?.onUnpin(fileObj)
            popupWindow.dismiss()
            clearSelection()
        }
        // Inside the popup menu setup in onBindViewHolder
        popupView.findViewById<LinearLayout>(R.id.renameFolder).setOnClickListener {
            if (file.isFolder) {
                showRenameFolderDialog(fileObj)
            } else {
                Toast.makeText(context, "Can only rename folders", Toast.LENGTH_SHORT).show()
            }
            popupWindow.dismiss()
        }


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

    private fun showRenameFolderDialog(folder: File) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.rename_file, null)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val etFileName = dialogView.findViewById<EditText>(R.id.etFileName)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancle)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSave)

        // Set current folder name
        etFileName.setText(folder.name)
        etFileName.selectAll()

        // Highlight function for dialog buttons
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

        // Initially highlight the Save button
        highlightButton(btnSave)

        btnCancel.setOnClickListener {
            // Highlight the button first
            highlightButton(it as TextView)

            // Add a slight delay to show the highlighted state before dismissing
            it.postDelayed({
                dialog.dismiss()
            }, 150) // 150ms delay to show the pressed state
        }

        btnSave.setOnClickListener {
            highlightButton(it as TextView)
            val newName = etFileName.text.toString().trim()

            when {
                newName.isEmpty() -> {
                    Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                newName == folder.name -> {
                    dialog.dismiss()
                    return@setOnClickListener
                }
                else -> {
                    // Call the rename listener
                    fileActionListener?.onRenameFolder(folder, newName)
                    dialog.dismiss()
                }
            }
        }

        // Handle button focus changes
        btnCancel.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) highlightButton(btnCancel)
        }

        btnSave.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) highlightButton(btnSave)
        }

        dialog.show()
    }

    fun setGridViewMode(isGrid: Boolean) {
        if (isGridView != isGrid) {  // Only update if changed
            isGridView = isGrid
            notifyDataSetChanged()  // Force complete refresh
        }
    }
    override fun getItemCount(): Int = fileList.size
}
