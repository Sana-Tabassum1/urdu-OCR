package com.urduocr.scanner.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
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

    private var allFiles: List<InternalFileModel> = emptyList()
    private var currentQuery: String = ""
    private var isNameAsc = true
    private var isDateAsc = true
    private var isSizeAsc = true
    private lateinit var currentDir: File

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

        // Observer for pinned files
        pinnedViewModel.pinnedFiles.observe(viewLifecycleOwner) { pinnedFiles ->
            allFiles = pinnedFiles
            applyCurrentSort()

            if (pinnedFiles.isEmpty()) {
                showEmptyAnimation()
            } else {
                hideEmptyAnimation()
            }
        }

        currentDir = arguments?.getString("dirPath")?.let(::File)
            ?: requireContext().filesDir

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

            override fun onRenameFolder(oldFile: File, newName: String) {
                // Implement if needed
            }
        }

        binding.ivMenu.setOnClickListener {
            showCustomPopupMenu(it)
        }

        binding.btndaimond.setOnClickListener {
            findNavController().navigate(R.id.action_nav_pinned_to_modelScreenFragment)
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

    private fun showCustomPopupMenu(anchor: View) {
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        val popupWidth = (150 * density).toInt()

        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_menu_pin, null)
        val popupWindow = PopupWindow(
            popupView,
            popupWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 10f
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

        val selectedFiles = adapter.getSelectedFiles().map { File(it.path) }

        popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
            adapter.selectAllItems()
            Toast.makeText(requireContext(), "All items selected", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

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
            val destinationDir = requireContext().filesDir
            fileViewModel.pasteFiles(destinationDir)
            Toast.makeText(requireContext(), "Pasted to app storage", Toast.LENGTH_SHORT).show()
            adapter.clearSelection()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menudelete).setOnClickListener {
            fileViewModel.clearSelection()
            selectedFiles.forEach { file ->
                fileViewModel.selectFile(file)
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
                loadAllFiles()
            } else {
                Toast.makeText(requireContext(), "No files selected to unpin", Toast.LENGTH_SHORT).show()
            }
            adapter.clearSelection()
            popupWindow.dismiss()
        }
    }

    private fun loadAllFiles() {
        pinnedViewModel.pinnedFiles.value?.let { pinnedFiles ->
            allFiles = pinnedFiles
            adapter.updateList(allFiles)
        }
    }

    private fun applyCurrentSort() {
        when {
            !isNameAsc -> sortByNameDesc()
            !isDateAsc -> sortByDateDesc()
            !isSizeAsc -> sortBySizeDesc()
            else -> adapter.updateList(allFiles)
        }
    }

    override fun onItemSelectionChanged() {
        val selectedFiles = adapter.getSelectedFiles().map { File(it.path) }
        Toast.makeText(requireContext(), "${selectedFiles.size} selected", Toast.LENGTH_SHORT).show()
    }

    private fun showEmptyAnimation() {
        binding.emptyAnimationView.visibility = View.VISIBLE
        binding.pinrecyclerview.visibility = View.GONE
        binding.emptyAnimationView.playAnimation()
    }

    private fun hideEmptyAnimation() {
        binding.emptyAnimationView.visibility = View.GONE
        binding.pinrecyclerview.visibility = View.VISIBLE
        binding.emptyAnimationView.pauseAnimation()
    }
}