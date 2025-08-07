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
import com.urduocr.scanner.databinding.ItemSavedFileGridBinding
import com.urduocr.scanner.interfaces.OnFileActionListener
import com.urduocr.scanner.models.InternalFileModel
import java.io.File

class SavedFileAdapter(
    private val context: Context,
    private var fileList: List<InternalFileModel>,
    var listener: OnSelectionChangedListener? = null,
    var fileActionListener: OnFileActionListener? = null,
    var onFolderClick: ((File) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FILE_LIST = 0
        private const val VIEW_TYPE_FILE_GRID = 1
    }

    var isGridView = false

    interface OnSelectionChangedListener {
        fun onItemSelectionChanged()
    }

    private var isSelectionMode = false

    inner class FileViewHolder(val binding: ItemSavedFileBinding) : RecyclerView.ViewHolder(binding.root)
    inner class FileGridViewHolder(val binding: ItemSavedFileGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) VIEW_TYPE_FILE_GRID else VIEW_TYPE_FILE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FILE_GRID -> FileGridViewHolder(
                ItemSavedFileGridBinding.inflate(
                    LayoutInflater.from(context), parent, false
                )
            )
            else -> FileViewHolder(
                ItemSavedFileBinding.inflate(
                    LayoutInflater.from(context), parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val file = fileList[position]
        val fileObj = File(file.path)
        val formattedDate = DateFormat.format("dd/MM/yyyy", fileObj.lastModified()).toString()
        val sizeFormatted = formatFileSize(fileObj.length())

        when (holder) {
            is FileGridViewHolder -> {
                with(holder.binding) {
                    setFileIcon(ivFileIcon, file)
                    tvFileName.text = file.name
                    tvTimeSize.text = "$sizeFormatted, $formattedDate"
                    handleSelectionUI(this, file, position)
                }
            }
            is FileViewHolder -> {
                with(holder.binding) {
                    setFileIcon(ivFileIcon, file)
                    tvFileName.text = file.name
                    tvTimeSize.text = "SIZE: $sizeFormatted    DATE: $formattedDate"
                    handleSelectionUI(this, file, position)
                }
            }
        }
    }

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

    private fun formatFileSize(sizeInBytes: Long): String {
        return if (sizeInBytes < 1024 * 1024) {
            String.format("%.1f KB", sizeInBytes / 1024f)
        } else {
            String.format("%.1f MB", sizeInBytes / (1024f * 1024f))
        }
    }

    private fun handleSelectionUI(binding: ItemSavedFileBinding, file: InternalFileModel, position: Int) {
        with(binding) {
            val shouldShowCheckbox = isSelectionMode && (file.isSelected || getSelectedCount() > 0)

            checkBoxSelect.visibility = if (shouldShowCheckbox) View.VISIBLE else View.GONE
            checkBoxSelect.setImageResource(
                if (file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
            )

            materialCardView.isSelected = file.isSelected
            menuButton.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

            root.setOnClickListener {
                if (isSelectionMode) {
                    file.isSelected = !file.isSelected
                    if (getSelectedCount() == 0) {
                        isSelectionMode = false
                        notifyDataSetChanged()
                    } else {
                        notifyItemChanged(position)
                    }
                    listener?.onItemSelectionChanged()
                } else {
                    if (file.isFolder) onFolderClick?.invoke(File(file.path))
                    else openFile(File(file.path), file.name)
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
            val shouldShowCheckbox = isSelectionMode && (file.isSelected || getSelectedCount() > 0)

            checkBoxSelect.visibility = if (shouldShowCheckbox) View.VISIBLE else View.GONE
            checkBoxSelect.setImageResource(
                if (file.isSelected) R.drawable.ratio_checked else R.drawable.ratio_unchecked
            )

            cardView.isSelected = file.isSelected
            menuButton.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

            root.setOnClickListener {
                if (isSelectionMode) {
                    file.isSelected = !file.isSelected
                    if (getSelectedCount() == 0) {
                        isSelectionMode = false
                        notifyDataSetChanged()
                    } else {
                        notifyItemChanged(position)
                    }
                    listener?.onItemSelectionChanged()
                } else {
                    if (file.isFolder) onFolderClick?.invoke(File(file.path))
                    else openFile(File(file.path), file.name)
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

    private fun showFilePopupMenu(
        anchorView: View,
        file: InternalFileModel,
        fileObj: File,
        position: Int
    ) {
        val context = anchorView.context
        val density = context.resources.displayMetrics.density
        val popupWidth = (180 * density).toInt()

        val popupView = LayoutInflater.from(context).inflate(R.layout.custom_file_popup, null)
        val popupWindow = PopupWindow(
            popupView,
            popupWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        popupView.findViewById<LinearLayout>(R.id.menushare).visibility =
            if (file.isFolder) View.GONE else View.VISIBLE
        popupView.findViewById<View>(R.id.viewShare).visibility =
            if (file.isFolder) View.GONE else View.VISIBLE

        popupView.findViewById<LinearLayout>(R.id.menupin).visibility =
            if (file.isPinned) View.GONE else View.VISIBLE
        popupView.findViewById<View>(R.id.viewpin).visibility =
            if (file.isPinned) View.GONE else View.VISIBLE

        popupView.findViewById<LinearLayout>(R.id.menuUnpin).visibility =
            if (file.isPinned) View.VISIBLE else View.GONE
        popupView.findViewById<View>(R.id.viewunpin).visibility =
            if (file.isPinned) View.VISIBLE else View.GONE

        popupView.findViewById<LinearLayout>(R.id.renameFolder).visibility =
            if (file.isFolder) View.VISIBLE else View.GONE
        popupView.findViewById<View>(R.id.viewRename).visibility =
            if (file.isFolder) View.VISIBLE else View.GONE

        popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
            }
            file.isSelected = true
            notifyDataSetChanged()
            listener?.onItemSelectionChanged()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
            if (isSelectionMode) {
                getSelectedFiles().forEach { fileItem ->
                    fileActionListener?.onCopy(File(fileItem.path))
                }
            } else {
                fileActionListener?.onCopy(fileObj)
            }
            popupWindow.dismiss()
            clearSelection()
        }

        popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
            if (isSelectionMode) {
                getSelectedFiles().forEach { fileItem ->
                    fileActionListener?.onCut(File(fileItem.path))
                }
            } else {
                fileActionListener?.onCut(fileObj)
            }
            popupWindow.dismiss()
            clearSelection()
        }

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

        popupView.findViewById<LinearLayout>(R.id.renameFolder).setOnClickListener {
            if (file.isFolder) {
                showRenameFolderDialog(fileObj)
            } else {
                Toast.makeText(context, "Can only rename folders", Toast.LENGTH_SHORT).show()
            }
            popupWindow.dismiss()
        }

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

        popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
            showDeleteConfirmationDialog(fileObj) {
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

        val popupHeight = popupView.measuredHeight
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val x = location[0] + anchorView.width - popupWidth
        val y = if (Resources.getSystem().displayMetrics.heightPixels - (location[1] + anchorView.height) >= popupHeight) {
            location[1] + anchorView.height
        } else {
            location[1] - popupHeight
        }

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x.coerceAtLeast(0), y)
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

    fun getSelectedCount(): Int = fileList.count { it.isSelected }

    fun clearSelection() {
        isSelectionMode = false
        fileList.forEach { it.isSelected = false }
        notifyDataSetChanged()
        listener?.onItemSelectionChanged()
    }

    fun getSelectedFiles(): List<InternalFileModel> = fileList.filter { it.isSelected }

    fun updateList(newList: List<InternalFileModel>) {
        newList.forEach { it.isSelected = false }
        fileList = newList
        isSelectionMode = false
        notifyDataSetChanged()
    }

    private fun showDeleteConfirmationDialog(file: File, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
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
            .show()
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

        etFileName.setText(folder.name)
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
                newName == folder.name -> dialog.dismiss()
                else -> {
                    fileActionListener?.onRenameFolder(folder, newName)
                    dialog.dismiss()
                }
            }
        }

        btnCancel.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) highlightButton(btnCancel) }
        btnSave.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) highlightButton(btnSave) }

        dialog.show()
    }

    fun setGridViewMode(isGrid: Boolean) {
        this.isGridView = isGrid
        notifyDataSetChanged()
    }

    fun selectAllItems() {
        fileList.forEach { it.isSelected = true }
        isSelectionMode = true
        notifyDataSetChanged()
        listener?.onItemSelectionChanged()
    }

    fun deleteSelectedFiles(): Boolean {
        val selectedFiles = getSelectedFiles()
        if (selectedFiles.isEmpty()) return false

        val newList = fileList.toMutableList().apply {
            removeAll { it.isSelected }
        }

        fileList = newList
        isSelectionMode = false
        notifyDataSetChanged()
        return true
    }

    override fun getItemCount(): Int = fileList.size
}