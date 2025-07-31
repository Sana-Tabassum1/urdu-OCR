package com.urduocr.scanner.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.urduocr.scanner.adapters.SavedFileAdapter
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.FragmentPinBinding
import com.urduocr.scanner.interfaces.OnFileActionListener
import com.urduocr.scanner.models.InternalFileModel
import com.urduocr.scanner.viewmodels.PinnedFilesViewModel
import com.urduocr.scanner.viewmodels.SavedFileViewModel
import java.io.File

class PinFragment : Fragment(), SavedFileAdapter.OnSelectionChangedListener {

    private lateinit var binding: FragmentPinBinding
    private lateinit var adapter: SavedFileAdapter

    private val pinnedViewModel: PinnedFilesViewModel by activityViewModels()
    private val fileViewModel: SavedFileViewModel by activityViewModels()

    private var allFiles: List<FileListItem> = emptyList()
    private var currentQuery: String = ""
    private var pinnedItems: List<FileListItem.FileItem> = emptyList()
    private var isNameAsc = true
    private var isDateAsc = true
    private var isSizeAsc = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SavedFileAdapter(
            requireContext(),
            emptyList(),
            listener = this
        )
        binding.pinrecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.pinrecyclerview.adapter = adapter

        setupSearchUi()
        setupSorting()
        pinnedViewModel.pinnedFiles.observe(viewLifecycleOwner) { pinnedFiles ->
            val pinnedItems = pinnedFiles.map { fileModel ->
                FileListItem.FileItem(fileModel)
            }
            adapter.updateList(pinnedItems)
        }

        adapter.listener = object : SavedFileAdapter.OnSelectionChangedListener {
            override fun onItemSelectionChanged() {
                val selectedFiles = adapter.getSelectedFiles()
                    .filterIsInstance<FileListItem.FileItem>()
                    .map { File(it.file.path) }

                //pinnedViewModel.setSelectedFiles(selectedFiles)

                Toast.makeText(requireContext(), "${selectedFiles.size} selected", Toast.LENGTH_SHORT).show()
            }
        }





//        // Back from selection
//        binding.ivBackSelection.setOnClickListener {
//            adapter.clearSelection()
//        }
       binding.ivMenu.setOnClickListener {
           showCustomPopupMenu(it)
       }

        binding.btndaimond.setOnClickListener {
            findNavController().navigate(R.id.action_nav_pinned_to_modelScreenFragment)
        }
    }

    // 🔍 SEARCH – untouched
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

    //popup menu
    private fun showCustomPopupMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_menu_pin, null)
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

        // Get current selected files
        val selectedFiles = adapter.getSelectedFiles()
            .filterIsInstance<FileListItem.FileItem>()
            .map { File(it.file.path) }

        // Select Mode
        popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
            Toast.makeText(requireContext(), "Select Mode Enabled", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        // COPY
        popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.copyFiles()
            Toast.makeText(requireContext(), "Copied ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        // CUT
        popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.cutFiles()
            Toast.makeText(requireContext(), "Cut ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        // PASTE
        // Replace the paste operation with:
        popupView.findViewById<LinearLayout>(R.id.menuPaste).setOnClickListener {
            val destinationDir = requireContext().filesDir // Default app directory
            fileViewModel.pasteFiles(destinationDir)
            Toast.makeText(requireContext(), "Pasted to app storage", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        // DELETE
        popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { file ->
                fileViewModel.selectFile(file)
                // Also unpin when deleting
                pinnedViewModel.unpinFile(
                    InternalFileModel(
                        path = file.path,
                        name = file.name,
                        file = file,
                        isFolder = file.isDirectory,
                        isPinned = true
                    )
                )
            }
            fileViewModel.deleteSelected()
            loadAllFiles()
            Toast.makeText(requireContext(), "Deleted ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        // UNPIN (changed from PIN in SavedFragment)
        popupView.findViewById<LinearLayout>(R.id.menuUnpin).setOnClickListener {
            if (selectedFiles.isNotEmpty()) {
                selectedFiles.forEach { file ->
                    pinnedViewModel.unpinFile(
                        InternalFileModel(
                            path = file.path,
                            name = file.name,
                            file = file,
                            isFolder = file.isDirectory,
                            isPinned = true
                        )
                    )
                    Toast.makeText(requireContext(), "Unpinned ${file.name}", Toast.LENGTH_SHORT).show()
                }
                loadAllFiles() // Refresh the list after unpinning
            } else {
                Toast.makeText(requireContext(), "No files selected to unpin", Toast.LENGTH_SHORT).show()
            }
            adapter.clearSelection()
            popupWindow.dismiss()
        }
    }
    private fun loadAllFiles() {
        pinnedViewModel.pinnedFiles.value?.let { pinnedFiles ->
            val pinnedItems = pinnedFiles.map { fileModel ->
                FileListItem.FileItem(fileModel)
            }
            adapter.updateList(pinnedItems)
        }
    }
    override fun onItemSelectionChanged() {
        TODO("Not yet implemented")
    }

}
