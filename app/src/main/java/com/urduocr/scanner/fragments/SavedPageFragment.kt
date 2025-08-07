package com.urduocr.scanner.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.urduocr.scanner.adapters.SavedFileAdapter
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.viewmodels.PinnedFilesViewModel
import com.urduocr.scanner.databinding.FragmentSavedPageBinding
import java.io.File

class SavedPageFragment : Fragment(){

    private lateinit var binding: FragmentSavedPageBinding
    private lateinit var adapter: SavedFileAdapter
    private var fileType: String? = null
    var selectionListener: SelectionChangeListener? = null
    private lateinit var allFiles: List<FileListItem>
    private val pinnedViewModel: PinnedFilesViewModel by activityViewModels()



    companion object {
        fun newInstance(type: String): SavedPageFragment {
            val fragment = SavedPageFragment()
            val args = Bundle()
            args.putString("type", type)
            fragment.arguments = args
            return fragment
        }
    }

    interface SelectionChangeListener {
        fun onSelectionChanged(isSelecting: Boolean, selectedCount: Int)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is SelectionChangeListener) {
            selectionListener = parentFragment as SelectionChangeListener
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileType = arguments?.getString("type")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavedPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       // allFiles = loadFilesByType()
        //adapter  = SavedFileAdapter(requireContext(), allFiles, this)

        binding.recyclerViewFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFiles.adapter      = adapter


    }

//    private fun loadFilesByType(): List<FileListItem> {
//        val rootDir = requireContext().filesDir
//        val allFiles = rootDir.listFiles()?.toList() ?: emptyList()
//
//        // ❗ Get list of pinned file paths from ViewModel
//        val pinnedPaths = pinnedViewModel.pinnedFiles.value?.map { it.path } ?: emptyList()
//
//        val filtered = allFiles.filter {
//            when (fileType) {
//                "PDF" -> it.name.endsWith(".pdf")
//                "TXT" -> it.name.endsWith(".txt")
//                else -> it.name.endsWith(".pdf", true) ||
//                        it.name.endsWith(".txt", true) ||
//                        it.name.endsWith(".png", true) || it.name.endsWith(".jpg", true) || it.name.endsWith(".jpeg", true)
//
//            }
//        }.map {
//            val isPinned = pinnedPaths.contains(it.absolutePath)
//            val model = InternalFileModel(
//                name = it.name,
//                path = it.absolutePath
//            ).apply {
//                this.isPinned = isPinned
//            }
//            FileListItem.FileItem(model)
//        }
//
//
//        return filtered
//    }


//    override fun onItemSelectionChanged() {
//        val selectedCount = adapter.getSelectedCount()
//        if (selectedCount > 0) {
//            selectionListener?.onSelectionChanged(true, selectedCount)
//        } else {
//            selectionListener?.onSelectionChanged(false, 0)
//        }
//    }

//    fun deleteSelectedFiles() {
//        val selectedFiles = adapter.getSelectedFiles()
//
//        if (selectedFiles.isEmpty()) return
//
//        AlertDialog.Builder(requireContext())
//            .setTitle("Delete Files")
//            .setMessage("Are you sure you want to delete this file?")
//            .setPositiveButton("Delete") { _, _ ->
//                selectedFiles.forEach {
//                    val file = File(it.file.path)
//                    if (file.exists()) file.delete()
//                }
//                refreshList()
//                //onItemSelectionChanged()
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }

//    fun shareSelectedFiles() {
//        val selectedFiles = adapter.getSelectedFiles()
//        if (selectedFiles.isEmpty()) return
//
//        val uris = selectedFiles.map {
//            FileProvider.getUriForFile(
//                requireContext(),
//                requireContext().packageName + ".fileprovider",
//                File(it.file.path)
//            )
//        }
//
//        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
//            type = "*/*"
//            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//
//        startActivity(Intent.createChooser(shareIntent, "Share files via"))
//    }

//    fun togglePinSelectedFiles(): Boolean {
//        val selectedItems = adapter.getSelectedFiles()
//
//        if (selectedItems.isEmpty()) {
//            Toast.makeText(requireContext(), "Select files to pin/unpin", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        val shouldUnpin = areAllSelectedFilesPinned()
//
//        selectedItems.forEach {
//            val file = it.file
//            if (shouldUnpin) {
//                pinnedViewModel.unpinFile(file)
//            } else {
//                pinnedViewModel.pinFile(file)
//            }
//        }
//
//        clearSelection()
//        Toast.makeText(requireContext(), "Pin state updated", Toast.LENGTH_SHORT).show()
//
//        return !shouldUnpin // return new state: true means now pinned
//    }


    fun clearSelection() {
        adapter.clearSelection()
       // onItemSelectionChanged()
    }
//    private fun refreshList() {
//        //val updatedFiles = loadFilesByType()
//        //adapter.updateList(updatedFiles)
//    }
    /* ------------ PUBLIC FUNCTION CALLED FROM PARENT ------------ */
//    fun filterFiles(query: String) {
//        val filtered = if (query.isBlank()) {
//            allFiles
//        } else {
//            allFiles.filterIsInstance<FileListItem.FileItem>()
//                .filter { it.file.name.contains(query, ignoreCase = true) }
//        }
//        adapter.updateList(filtered)
//    }


    fun areAllSelectedFilesPinned(): Boolean {
        val selectedFiles = adapter.getSelectedFiles()
        if (selectedFiles.isEmpty()) return false
        return selectedFiles.all { file ->
            pinnedViewModel.pinnedFiles.value?.any { it.path == file.file.path } == true
        }
    }

}