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
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.urduocr.scanner.R
import com.urduocr.scanner.ViewPreferenceManager
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

    private var allFiles: List<InternalFileModel> = emptyList()

    private lateinit var currentDir: File
    private var isGridView = false
    private lateinit var viewPreferenceManager: ViewPreferenceManager

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

        viewPreferenceManager = ViewPreferenceManager(requireContext())

        currentDir = arguments?.getString("dirPath")?.let { path ->
            File(path).takeIf { it.exists() && it.isDirectory }
        } ?: requireContext().filesDir

        // Setup back button handling
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPress()
                }
            }
        )

        setupAdapter()
        setupRecyclerView()
        setupSearchUi()
        setupSorting()

        binding.ivMenu.setOnClickListener { showCustomPopupMenu(it) }

        // Load saved preference
        isGridView = viewPreferenceManager.getViewPreference()
        loadAllFiles()
    }

    private fun setupAdapter() {
        adapter = SavedFileAdapter(
            requireContext(),
            emptyList(),
            listener = object : SavedFileAdapter.OnSelectionChangedListener {
                override fun onItemSelectionChanged() {
                    val selectedFiles = adapter.getSelectedFiles()
                        .map { File(it.path) }
                    fileViewModel.setSelectedFiles(selectedFiles)
                }
            }
        ).apply {
            fileActionListener = object : OnFileActionListener {
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
                override fun onRenameFolder(oldFile: File, newName: String) {
                    val newFile = File(oldFile.parent, newName)
                    if (newFile.exists()) {
                        Toast.makeText(requireContext(), "A folder with this name already exists", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (oldFile.renameTo(newFile)) {
                        Toast.makeText(requireContext(), "Folder renamed", Toast.LENGTH_SHORT).show()
                        loadAllFiles()
                    } else {
                        Toast.makeText(requireContext(), "Failed to rename folder", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            onFolderClick = { folder ->
                navigateToFolder(folder)
            }
        }

        binding.recyclerViewAllFiles.adapter = adapter
    }

    private fun navigateToFolder(folder: File) {
        currentDir = folder
        val args = Bundle().apply {
            putString("dirPath", folder.absolutePath)
        }
        findNavController().navigate(R.id.action_savedFragment_self, args)
        loadAllFiles()
    }

    private fun handleBackPress() {
        if (currentDir != requireContext().filesDir) {
            // Navigate to parent folder
            currentDir = currentDir.parentFile
            loadAllFiles()

            // Update the directory in arguments
            val args = Bundle().apply {
                putString("dirPath", currentDir.absolutePath)
            }
            // Replace current fragment with updated arguments
            findNavController().navigate(R.id.action_savedFragment_self, args)
        } else {
            // If we're at root, navigate up
            if (!findNavController().popBackStack()) {
                // If nothing left in back stack, let system handle back
                requireActivity().onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        validateCurrentDir()
        loadAllFiles()
        isGridView = viewPreferenceManager.getViewPreference()
        setupRecyclerView()
        adapter.setGridViewMode(isGridView)
    }

    private fun loadAllFiles() {
        fileViewModel.removedFilePaths.clear()

        allFiles = buildList {
            val items = currentDir.listFiles()
                ?.asSequence()
                ?.distinct()
                ?.filterNot { file ->
                    fileViewModel.removedFilePaths.contains(file.absolutePath) ||
                            !file.exists()
                }
                ?.toList() ?: emptyList()

            // Process folders
            val folders = items.filter { it.isDirectory }
                .sortedBy { it.name.lowercase() }
                .map {
                    InternalFileModel(
                        name = it.name,
                        path = it.absolutePath,
                        isFolder = true,
                        file = it,
                        isPinned = pinnedViewModel.isPinned(it),
                        isSelected = false
                    )
                }

            // Process files
            val supportedExtensions = listOf(".pdf", ".txt", ".png", ".jpg", ".jpeg")
            val docs = items
                .filter { it.isFile }
                .filter { file ->
                    supportedExtensions.any { ext ->
                        file.name.endsWith(ext, ignoreCase = true)
                    }
                }
                .sortedByDescending { it.lastModified() }
                .map {
                    InternalFileModel(
                        name = it.name,
                        path = it.absolutePath,
                        isFolder = false,
                        file = it,
                        isPinned = pinnedViewModel.isPinned(it),
                        isSelected = false
                    )
                }

            addAll(folders)
            addAll(docs)
        }

        updateUIState()
        adapter.updateList(allFiles)
        binding.titleText.text = if (currentDir == requireContext().filesDir) {
            getString(R.string.my_files)
        } else {
            currentDir.name
        }
    }

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

    private fun updateUIState() {
        if (allFiles.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun doCopy(file: File) {
        fileViewModel.clearSelection()
        fileViewModel.selectFile(file)
        fileViewModel.copyFiles()
        Toast.makeText(requireActivity(), "Copied ${file.name}", Toast.LENGTH_SHORT).show()
    }

    private fun doCut(file: File) {
        fileViewModel.clearSelection()
        fileViewModel.selectFile(file)
        fileViewModel.cutFiles()
        Toast.makeText(requireActivity(), "Cut ${file.name}", Toast.LENGTH_SHORT).show()
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

    private fun doShare(file: File) {
        // Implement share functionality
    }

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
            allFiles.filter { it.name.contains(query, ignoreCase = true) }
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
        allFiles = allFiles.sortedBy { it.name.lowercase() }
        adapter.updateList(allFiles)
    }

    private fun sortByNameDesc() {
        allFiles = allFiles.sortedByDescending { it.name.lowercase() }
        adapter.updateList(allFiles)
    }

    private fun sortByDateAsc() {
        allFiles = allFiles.sortedBy { it.file.lastModified() }
        adapter.updateList(allFiles)
    }

    private fun sortByDateDesc() {
        allFiles = allFiles.sortedByDescending { it.file.lastModified() }
        adapter.updateList(allFiles)
    }

    private fun sortBySizeAsc() {
        allFiles = allFiles.sortedBy { it.file.length() }
        adapter.updateList(allFiles)
    }

    private fun sortBySizeDesc() {
        allFiles = allFiles.sortedByDescending { it.file.length() }
        adapter.updateList(allFiles)
    }

    private fun highlightSort(active: ImageView, inactive: ImageView) {
        active.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        inactive.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
    }

    private fun showCustomPopupMenu(anchor: View) {
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        val popupWidth = (180 * density).toInt()

        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_menu_save, null)
        val popupWindow = PopupWindow(
            popupView,
            popupWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupHeight = popupView.measuredHeight

        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        val anchorHeight = anchor.height

        val bottomSpace = screenHeight - (anchorY + anchorHeight)
        val topSpace = anchorY

        val x = anchorX + anchor.width - popupWidth
        val y = if (bottomSpace >= popupHeight) {
            anchorY + anchorHeight
        } else if (topSpace >= popupHeight) {
            anchorY - popupHeight
        } else {
            (screenHeight - popupHeight) / 2
        }

        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x.coerceAtLeast(0), y)

        // Handle menu items
        popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
            adapter.selectAllItems()
            Toast.makeText(requireContext(), "All items selected", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        val selectedFiles = adapter.getSelectedFiles().map { File(it.path) }

        popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.copyFiles()
            Toast.makeText(requireContext(), "Copied ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menuCut).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { fileViewModel.selectFile(it) }
            fileViewModel.cutFiles()
            Toast.makeText(requireContext(), "Cut ${selectedFiles.size} file(s)", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menuPaste).setOnClickListener {
            fileViewModel.pasteFiles(currentDir)
            loadAllFiles()
            Toast.makeText(requireContext(), "Pasted to ${currentDir.name}", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
            if (selectedFiles.isEmpty()) {
                Toast.makeText(requireContext(), "No files selected", Toast.LENGTH_SHORT).show()
                popupWindow.dismiss()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Delete ${selectedFiles.size} files?")
                .setMessage("Are you sure you want to delete these files?")
                .setPositiveButton("Delete") { _, _ ->
                    if (adapter.deleteSelectedFiles()) {
                        selectedFiles.forEach { file ->
                            if (file.exists()) {
                                file.delete()
                                fileViewModel.removedFilePaths.add(file.absolutePath)
                            }
                        }
                        Toast.makeText(requireContext(), "Deleted ${selectedFiles.size} files", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()

            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menUICON).setOnClickListener {
            toggleView(true)
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menuviewfile).setOnClickListener {
            toggleView(false)
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menufolder).setOnClickListener {
            showCreateFolderDialog()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

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

    private fun setupRecyclerView() {
        binding.recyclerViewAllFiles.layoutManager = if (isGridView) {
            GridLayoutManager(requireContext(), 2)
        } else {
            LinearLayoutManager(requireContext())
        }
    }

    private fun toggleView(isGrid: Boolean) {
        isGridView = isGrid
        viewPreferenceManager.saveViewPreference(isGridView)
        setupRecyclerView()
        adapter.setGridViewMode(isGridView)
    }

    private fun resetOtherSortIcons(activeColumn: String) {
        if (activeColumn != "name") {
            binding.toparrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
            binding.downarrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
        }
        if (activeColumn != "date") {
            binding.toparrow2.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
            binding.downarrow2.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
        }
        if (activeColumn != "size") {
            binding.toparrow3.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
            binding.downarrow3.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
        }
    }

    private fun showEmptyState() {
        binding.emptyAnimationView.visibility = View.VISIBLE
        binding.recyclerViewAllFiles.visibility = View.GONE
        binding.emptyAnimationView.playAnimation()
    }

    private fun hideEmptyState() {
        binding.emptyAnimationView.visibility = View.GONE
        binding.recyclerViewAllFiles.visibility = View.VISIBLE
        binding.emptyAnimationView.pauseAnimation()
    }
}