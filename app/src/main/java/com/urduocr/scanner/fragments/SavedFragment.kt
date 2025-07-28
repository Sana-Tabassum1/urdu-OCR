package com.urduocr.scanner.fragments


import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.urduocr.scanner.R
import com.urduocr.scanner.adapters.SavedFileAdapter
import com.urduocr.scanner.databinding.FragmentSavedBinding
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.models.InternalFileModel
import java.io.File

class SavedFragment : Fragment(), SavedFileAdapter.FileAdapterListener {

    private lateinit var binding: FragmentSavedBinding
    private lateinit var adapter: SavedFileAdapter
    private var allFiles: List<FileListItem.FileItem> = emptyList()
    private var currentQuery: String = ""
    private var isNameAsc = true
    private var isDateAsc = true
    private var isSizeAsc = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Load all files
        allFiles = loadAllFiles()

        // 2) Set adapter
        adapter = SavedFileAdapter(requireContext(), allFiles, this)
        binding.recyclerViewAllFiles.adapter = adapter
        //sorting setup
        setupSorting()
        /* -------- Search UI setup -------- */
        setupSearchUi()

        binding.ivMenu.setOnClickListener {
            showCustomPopupMenu(it)
        }

    }


    private fun loadAllFiles(): List<FileListItem.FileItem> {
        val dir = requireContext().filesDir
        val files = dir.listFiles() ?: return emptyList()

        return files.filter {
            it.name.endsWith(".pdf", true) ||
                    it.name.endsWith(".txt", true) ||
                    it.name.endsWith(".png", true) ||
                    it.name.endsWith(".jpg", true) ||
                    it.name.endsWith(".jpeg", true)
        }.map {
            FileListItem.FileItem(
                InternalFileModel(it.name, it.absolutePath)
            )
        }
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
        allFiles = allFiles.sortedBy { it.file.name.lowercase() }
        adapter.updateList(allFiles)
    }

    private fun sortByNameDesc() {
        allFiles = allFiles.sortedByDescending { it.file.name.lowercase() }
        adapter.updateList(allFiles)
    }

    private fun sortByDateAsc() {
        allFiles = allFiles.sortedBy { File(it.file.path).lastModified() }
        adapter.updateList(allFiles)
    }

    private fun sortByDateDesc() {
        allFiles = allFiles.sortedByDescending { File(it.file.path).lastModified() }
        adapter.updateList(allFiles)
    }

    private fun sortBySizeAsc() {
        allFiles = allFiles.sortedBy { File(it.file.path).length() }
        adapter.updateList(allFiles)
    }

    private fun sortBySizeDesc() {
        allFiles = allFiles.sortedByDescending { File(it.file.path).length() }
        adapter.updateList(allFiles)
    }

    private fun highlightSort(active: ImageView, inactive: ImageView) {
        active.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        inactive.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
    }

    //popup menu
    private fun showCustomPopupMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_menu_save, null)
        val popupWindow = PopupWindow(
            popupView,
            (200 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.isOutsideTouchable = true

        popupWindow.elevation = 10f
        popupWindow.isOutsideTouchable = true


        val anchor = binding.ivMenu
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val popupWidth = (200 * density).toInt()

        val anchorWidth = anchor.width
        val anchorHeight = anchor.height

        val xOffset = location[0] - (popupWidth - anchorWidth)
        val yOffset = location[1] + anchorHeight

        popupWindow.showAsDropDown(anchor, xOffset, 0)



        // --- Handle clicks ---
        popupView.findViewById<LinearLayout>(R.id.menuSelect).setOnClickListener {
            Toast.makeText(requireContext(), "Select clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menufolder).setOnClickListener {
            Toast.makeText(requireContext(), "New Folder clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.menuCopy).setOnClickListener {
            Toast.makeText(requireContext(), "Copy clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        // Repeat for menuCut, menuPaste, menudelete, etc.
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
    override fun onItemSelectionChanged() {
        val count = adapter.getSelectedCount()
        // later: show/hide your selection bar
    }
}