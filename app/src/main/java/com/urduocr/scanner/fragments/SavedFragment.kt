package com.urduocr.scanner.fragments


import android.app.AlertDialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private val clipboard = mutableListOf<File>()
    private var isCut = false
    private lateinit var currentDir: File
    private val fileViewModel: SavedFileViewModel by activityViewModels()


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
            override fun onPin(file: File) = doPin(file)
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
        loadAllFiles()
    }
    private fun loadAllFiles() {
        allFiles = buildList {
            val items = currentDir.listFiles()?.toList() ?: emptyList()
            val folders = items.filter { it.isDirectory }.sortedBy { it.name }
            val docs = items.filter { it.isFile && listOf(".pdf",".txt",".png",".jpg",".jpeg")
                .any { ext -> it.name.endsWith(ext, true) } }
                .filter { !fileViewModel.removedFilePaths.contains(it.absolutePath) }
                .sortedByDescending { it.lastModified() }


            if (folders.isNotEmpty()) add(FileListItem.Header("Folders"))
            addAll(
                folders.map {
                    FileListItem.FileItem(
                        InternalFileModel(
                            name = it.name,
                            path = it.absolutePath,
                            isFolder = true,
                            file = File(it.absolutePath)
                        )
                    )
                }
            )
            if (docs.isNotEmpty()) add(FileListItem.Header("Files"))
            addAll(
                docs.map {
                    FileListItem.FileItem(
                        InternalFileModel(
                            name = it.name,
                            path = it.absolutePath,
                            isFolder = false,
                            file = it
                        )
                    )
                }
            )

        }
        adapter.updateList(allFiles)

        // When user taps folder item:
        adapter.onFolderClick = { f ->
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    SavedFragment().apply {
                        arguments = Bundle().apply { putString("dirPath", f.absolutePath) }
                    }
                )
                .addToBackStack(null)
                .commit()
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
    private fun doPin(file: File) { /* ... */ }

    private fun showCreateFolderDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Create Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val newF = File(currentDir, input.text.toString())
                if (!newF.exists() && newF.mkdir()) loadAllFiles()
                else Toast.makeText(requireContext(), "Failed or exists", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel",null).show()
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
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_menu_save, null)
        val popupWidth = (200 * resources.displayMetrics.density).toInt()

        val popupWindow = PopupWindow(
            popupView,
            popupWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 10f
        popupWindow.isOutsideTouchable = true

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val rightMargin = (16 * resources.displayMetrics.density).toInt()
        val xOffset = screenWidth - (location[0] + popupWidth) - rightMargin
        popupWindow.showAsDropDown(anchor, xOffset, 0)

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
            popupWindow.dismiss()
        }

        // ✂ CUT
        popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.cutFiles()
            Toast.makeText(requireContext(), "Cut ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        // 📌 PASTE
        popupView.findViewById<LinearLayout>(R.id.menuPaste).setOnClickListener {
            fileViewModel.pasteFiles(currentDir)
            loadAllFiles()
            Toast.makeText(requireContext(), "Pasted to ${currentDir.name}", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        // 🗑 DELETE
        popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.deleteSelected()
            loadAllFiles()
            Toast.makeText(requireContext(), "Deleted ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        // 📁 Create Folder
        popupView.findViewById<LinearLayout>(R.id.menufolder).setOnClickListener {
            showCreateFolderDialog()
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
    override fun onDestroyView() {
        super.onDestroyView()
        fileViewModel.removedFilePaths.clear()
    }


}