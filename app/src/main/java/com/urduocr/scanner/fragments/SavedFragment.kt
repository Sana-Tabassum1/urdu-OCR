package com.urduocr.scanner.fragments


import android.app.AlertDialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.urduocr.scanner.R
import com.urduocr.scanner.adapters.SavedFileAdapter
import com.urduocr.scanner.databinding.FragmentSavedBinding
import com.urduocr.scanner.interfaces.OnFileActionListener
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.models.InternalFileModel
import com.urduocr.scanner.viewmodels.PinnedFilesViewModel
import com.urduocr.scanner.viewmodels.SavedFileViewModel
import java.io.File

class SavedFragment : Fragment() {

    private lateinit var binding: FragmentSavedBinding
    private lateinit var adapter: SavedFileAdapter
    private var currentQuery: String = ""
    private var isNameAsc = true
    private var isDateAsc = true
    private var isSizeAsc = true

    private var allFiles: List<FileListItem> = emptyList()

    private lateinit var currentDir: File
    private val fileViewModel: SavedFileViewModel by activityViewModels()
    private val pinnedViewModel: PinnedFilesViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        currentDir = arguments?.getString("dirPath")?.let(::File)
            ?: requireContext().filesDir

        adapter = SavedFileAdapter(requireContext(), emptyList())
        adapter.fileActionListener = object : OnFileActionListener {
            override fun onCopy(file: File) = doCopy(file)
            override fun onCut(file: File) = doCut(file)
            override fun onPaste() = doPaste()
            override fun onDelete(file: File) = doDelete(file)
            override fun onShare(file: File) = doShare(file)
            override fun onPin(file: File) {
                val internalModel = InternalFileModel(
                    path = file.path,
                    name = file.name,
                    isSelected = false,
                    isPinned = true,
                    file = file,
                    isFolder = file.isDirectory
                )

                if (!pinnedViewModel.isPinned(internalModel)) {
                    pinnedViewModel.pinFile(internalModel)
                    Toast.makeText(requireContext(), "File pinned", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Already pinned", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onUnpin(file: File) {
                val internalModel = InternalFileModel(
                    path = file.path,
                    name = file.name,
                    isSelected = false,
                    isPinned = false,
                    file = file,
                    isFolder = file.isDirectory
                )

                pinnedViewModel.unpinFile(internalModel)
                Toast.makeText(requireContext(), "File unpinned", Toast.LENGTH_SHORT).show()
            }







        }
        adapter.listener = object : SavedFileAdapter.OnSelectionChangedListener {
            override fun onItemSelectionChanged() {
                val selectedFiles = adapter.getSelectedFiles()
                    .filterIsInstance<FileListItem.FileItem>()
                    .map { File(it.file.path) }

                fileViewModel.setSelectedFiles(selectedFiles)

                Toast.makeText(requireContext(), "${selectedFiles.size} selected", Toast.LENGTH_SHORT).show()
            }
        }


        binding.recyclerViewAllFiles.adapter = adapter
        setupSearchUi()
        setupSorting()
        binding.ivMenu.setOnClickListener { showCustomPopupMenu(it) }
    }

    override fun onResume() {
        super.onResume()
        validateCurrentDir()
        loadAllFiles()
    }

    private fun loadAllFiles() {
        // Clear any existing removed file paths
        fileViewModel.removedFilePaths.clear()

        allFiles = buildList {
            val items = currentDir.listFiles()?.toList()?.distinct() ?: emptyList() // Add distinct()

            // Folders first
            val folders = items
                .filter { it.isDirectory }
                .filter { it.exists() } // Add existence check
                .sortedBy { it.name }

            if (folders.isNotEmpty()) add(FileListItem.Header("Folders"))
            addAll(folders.map {
                FileListItem.FileItem(
                    InternalFileModel(
                        name = it.name,
                        path = it.absolutePath,
                        isFolder = true,
                        file = it,
                        isPinned = pinnedViewModel.isPinned(it)
                    ) // This closing parenthesis was missing
                )
            })

            // Then files
            val docs = items
                .filter { it.isFile }
                .filter { listOf(".pdf", ".txt", ".png", ".jpg", ".jpeg")
                    .any { ext -> it.name.endsWith(ext, true) } }
                .filter { it.exists() } // Add existence check
                .sortedByDescending { it.lastModified() }

            if (docs.isNotEmpty()) add(FileListItem.Header("Files"))
            addAll(docs.map {
                FileListItem.FileItem(
                    InternalFileModel(
                        name = it.name,
                        path = it.absolutePath,
                        isFolder = false,
                        file = it,
                        isPinned = pinnedViewModel.isPinned(it)
                    )
                )
            })
        }
        adapter.updateList(allFiles)
    }
    // Add this function to validate the current directory
    private fun validateCurrentDir() {
        if (!currentDir.exists() || !currentDir.isDirectory) {
            currentDir = requireContext().filesDir
            Toast.makeText(
                requireContext(),
                "Invalid directory, using default",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun doCopy(file: File) {
        fileViewModel.clearSelection()
        fileViewModel.selectFile(file)
        fileViewModel.copyFiles()
        Toast.makeText(requireActivity(), "Copied ${file.name}",Toast.LENGTH_SHORT).show()
    }
    private fun doCut(file: File) {
        fileViewModel.clearSelection()
        fileViewModel.selectFile(file)
        fileViewModel.cutFiles()
        Toast.makeText(requireActivity(), "Cut ${file.name}",Toast.LENGTH_SHORT).show()
    }
    private fun doPaste() {
        fileViewModel.pasteFiles(currentDir)
        loadAllFiles()
        Toast.makeText(requireContext(), "Pasted to ${currentDir.name}", Toast.LENGTH_SHORT).show()
    }

    private fun doDelete(file: File) {
        fileViewModel.clearSelection()
        fileViewModel.selectFile(file)
        fileViewModel.deleteSelected()
        loadAllFiles()
        Toast.makeText(requireContext(), "Deleted selected files", Toast.LENGTH_SHORT).show()
    }
    private fun doShare(file: File) { /* ... */ }





    private fun showCreateFolderDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Folder name"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Create Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val folderName = input.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    val newFolder = File(currentDir, folderName)
                    if (newFolder.exists()) {
                        Toast.makeText(
                            requireContext(),
                            "Folder already exists",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (newFolder.mkdir()) {
                        loadAllFiles()
                        Toast.makeText(
                            requireContext(),
                            "Folder created",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to create folder",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSearchUi() {
        binding.ivSearch.setOnClickListener {
            binding.etSearch.requestFocus()
            showKeyboard()
        }

        binding.ivClear.setOnClickListener { clearSearch() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFiles(s?.toString() ?: "")
            }
        })
    }

    private fun filterFiles(query: String) {
        currentQuery = query
        binding.ivClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

        val filtered = if (query.isBlank()) {
            allFiles
        } else {
            allFiles.filterIsInstance<FileListItem.FileItem>()
                .filter { it.file.name.contains(query, ignoreCase = true) }
        }
        adapter.updateList(filtered)
    }

    private fun clearSearch() {
        binding.etSearch.text?.clear()
        binding.etSearch.clearFocus()
        hideKeyboard()
        filterFiles("")
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }


    //sorting
    private fun setupSorting() {
        binding.name.setOnClickListener {
            if (isNameAsc) {
                sortByNameAsc()
                highlightSort(binding.toparrow, binding.downarrow)
            } else {
                sortByNameDesc()
                highlightSort(binding.downarrow, binding.toparrow)
            }
            isNameAsc = !isNameAsc
            resetOtherSortIcons("name")
        }

        binding.date.setOnClickListener {
            if (isDateAsc) {
                sortByDateAsc()
                highlightSort(binding.toparrow2, binding.downarrow2)
            } else {
                sortByDateDesc()
                highlightSort(binding.downarrow2, binding.toparrow2)
            }
            isDateAsc = !isDateAsc
            resetOtherSortIcons("date")
        }

        binding.size.setOnClickListener {
            if (isSizeAsc) {
                sortBySizeAsc()
                highlightSort(binding.toparrow3, binding.downarrow3)
            } else {
                sortBySizeDesc()
                highlightSort(binding.downarrow3, binding.toparrow3)
            }
            isSizeAsc = !isSizeAsc
            resetOtherSortIcons("size")
        }
    }

    private fun sortByNameAsc() {
        val sortedFiles = allFiles.filterIsInstance<FileListItem.FileItem>()
            .sortedBy { it.file.name.lowercase() }

        val headers = allFiles.filterIsInstance<FileListItem.Header>()

        allFiles = headers + sortedFiles
        adapter.updateList(allFiles)
    }

    private fun sortByNameDesc() {
        val sortedFiles = allFiles.filterIsInstance<FileListItem.FileItem>()
            .sortedByDescending { it.file.name.lowercase() }

        val headers = allFiles.filterIsInstance<FileListItem.Header>()
        allFiles = headers + sortedFiles
        adapter.updateList(allFiles)
    }


    private fun sortByDateAsc() {
        val sortedFiles = allFiles.filterIsInstance<FileListItem.FileItem>()
            .sortedBy { File(it.file.path).lastModified() }

        val headers = allFiles.filterIsInstance<FileListItem.Header>()
        allFiles = headers + sortedFiles
        adapter.updateList(allFiles)
    }


    private fun sortByDateDesc() {
        val sortedFiles = allFiles.filterIsInstance<FileListItem.FileItem>()
            .sortedByDescending { File(it.file.path).lastModified() }

        val headers = allFiles.filterIsInstance<FileListItem.Header>()
        allFiles = headers + sortedFiles
        adapter.updateList(allFiles)
    }

    private fun sortBySizeAsc() {
        val sortedFiles = allFiles.filterIsInstance<FileListItem.FileItem>()
            .sortedBy { File(it.file.path).length() }

        val headers = allFiles.filterIsInstance<FileListItem.Header>()
        allFiles = headers + sortedFiles
        adapter.updateList(allFiles)
    }

    private fun sortBySizeDesc() {
        val sortedFiles = allFiles.filterIsInstance<FileListItem.FileItem>()
            .sortedByDescending { File(it.file.path).length() }

        val headers = allFiles.filterIsInstance<FileListItem.Header>()
        allFiles = headers + sortedFiles
        adapter.updateList(allFiles)
    }

    private fun highlightSort(active: ImageView, inactive: ImageView) {
        active.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        inactive.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
    }

    //popup menu
    private fun showCustomPopupMenu(anchor: View) {
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        val popupWidth = (150 * density).toInt() // Fixed 200dp width

        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_menu_save, null)
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
        anchor.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        val anchorHeight = anchor.height

        val bottomSpace = screenHeight - (anchorY + anchorHeight)
        val topSpace = anchorY

        val x = anchorX + anchor.width - popupWidth // Align right edge of popup to anchor
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
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x.coerceAtLeast(0), y)

        // Select Mode (you can enhance this later)
        popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
            Toast.makeText(requireContext(), "Select Mode Enabled", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        // 🔄 Get current selected files
        val selectedFiles = adapter.getSelectedFiles()
            .filterIsInstance<FileListItem.FileItem>()
            .map { File(it.file.path) }

        // 📋 COPY
        popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.copyFiles()
            Toast.makeText(requireContext(), "Copied ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        // ✂ CUT
        popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.cutFiles()
            Toast.makeText(requireContext(), "Cut ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        // 📌 PASTE
        popupView.findViewById<LinearLayout>(R.id.menuPaste).setOnClickListener {
            fileViewModel.pasteFiles(currentDir)
            loadAllFiles()
            Toast.makeText(requireContext(), "Pasted to ${currentDir.name}", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        // 🗑 DELETE
        popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.deleteSelected()
            loadAllFiles()
            Toast.makeText(requireContext(), "Deleted ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        // 📁 Create Folder
        popupView.findViewById<LinearLayout>(R.id.menufolder).setOnClickListener {
            showCreateFolderDialog()
            adapter.clearSelection()
            popupWindow.dismiss()
        }
        // 📌 PIN
        popupView.findViewById<LinearLayout>(R.id.menupin).setOnClickListener {
            if (selectedFiles.isNotEmpty()) {
                selectedFiles.forEach { file ->
                    val model = InternalFileModel(
                        path = file.path,
                        name = file.name,
                        isSelected = false,
                        isPinned = false,
                        file = file,
                        isFolder = file.isDirectory
                    )

                    if (!pinnedViewModel.isPinned(model)) {
                        pinnedViewModel.pinFile(model)
                        Toast.makeText(requireContext(), "Pinned ${file.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        pinnedViewModel.unpinFile(model)
                        Toast.makeText(requireContext(), "Unpinned ${file.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "No files selected to pin/unpin", Toast.LENGTH_SHORT).show()
            }
            adapter.clearSelection()
            popupWindow.dismiss()
        }




    }





    private fun resetOtherSortIcons(activeColumn: String) {
        if (activeColumn != "name") {
            binding.toparrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
            binding.downarrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
        }
        if (activeColumn != "date") {
            binding.toparrow2.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
            binding.downarrow2.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.gray
                )
            )
        }
        if (activeColumn != "size") {
            binding.toparrow3.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
            binding.downarrow3.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.gray
                )
            )
        }
    }









}