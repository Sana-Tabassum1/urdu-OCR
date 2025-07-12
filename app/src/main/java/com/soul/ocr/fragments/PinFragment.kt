package com.soul.ocr.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.soul.ocr.Adaptors.SavedFileAdapter
import com.soul.ocr.ModelClass.FileListItem
import com.soul.ocr.R
import com.soul.ocr.ViewModel.PinnedFilesViewModel
import com.soul.ocr.databinding.FragmentPinBinding
import java.io.File
import java.util.ArrayList

class PinFragment : Fragment(), SavedFileAdapter.FileAdapterListener {

    private lateinit var binding: FragmentPinBinding
    private lateinit var adapter: SavedFileAdapter

    private val pinnedViewModel: PinnedFilesViewModel by activityViewModels()

    /* ---------- master list + query ---------- */
    private var allFiles: List<FileListItem> = emptyList()
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* -------- RecyclerView -------- */
        adapter = SavedFileAdapter(requireContext(), emptyList(), this)
        binding.pinrecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.pinrecyclerview.adapter = adapter

        /* -------- Search UI setup -------- */
        setupSearchUi()

        /* -------- Observe pinned list -------- */
        pinnedViewModel.pinnedFiles.observe(viewLifecycleOwner) { pinnedList ->
            allFiles = pinnedList.map { FileListItem.FileItem(it) }
            filterFiles(currentQuery)        // honour current query
        }

        /* -------- Selection actions -------- */
        binding.ivBackSelection.setOnClickListener {
            adapter.clearSelections()
            toggleSelectionBar(false)
        }
        binding.ivDelete.setOnClickListener { deleteSelectedFiles() }
        binding.ivShare.setOnClickListener { shareSelectedFiles() }
        binding.ivunPin.setOnClickListener { unpinSelectedFiles() }

        /* -------- Diamond nav -------- */
        binding.btndaimond.setOnClickListener {
            findNavController().navigate(R.id.action_nav_pinned_to_modelScreenFragment)
        }
    }

    /* ============================================================
       SEARCH HANDLING
       ============================================================ */
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

    /* ============================================================
       SELECTION + SHARE / DELETE / UNPIN
       ============================================================ */
    private fun toggleSelectionBar(visible: Boolean) {
        binding.selectionLayout.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onItemSelectionChanged() {
        val count = adapter.getSelectedFiles().size
        if (count > 0) {
            toggleSelectionBar(true)
            binding.tvSelectedCount.text = "$count× selected"
        } else {
            toggleSelectionBar(false)
        }
    }

    private fun deleteSelectedFiles() {
        val selectedFiles = adapter.getSelectedFiles()
        if (selectedFiles.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Files")
            .setMessage("Are you sure you want to delete these files?")
            .setPositiveButton("Delete") { _, _ ->
                selectedFiles.forEach {
                    File(it.file.path).delete()
                    pinnedViewModel.unpinFile(it.file)
                }
                adapter.clearSelections()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareSelectedFiles() {
        val selectedFiles = adapter.getSelectedFiles()
        if (selectedFiles.isEmpty()) return

        val uris = selectedFiles.map {
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                File(it.file.path)
            )
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share files via"))
    }

    private fun unpinSelectedFiles() {
        adapter.getSelectedFiles().forEach { pinnedViewModel.unpinFile(it.file) }
        adapter.clearSelections()
    }

    /* optional: preview click handler */
    fun onFileClick(file: FileListItem.FileItem) { /* open preview etc. */ }
}
