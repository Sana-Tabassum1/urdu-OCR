package com.soul.ocr.fragments


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.soul.ocr.Adaptors.RecentAdapter
import com.soul.ocr.Adaptors.SavedFileAdapter
import com.soul.ocr.ModelClass.FileListItem
import com.soul.ocr.ModelClass.InternalFileModel
import com.soul.ocr.ModelClass.RecentItem
import com.soul.ocr.R
import com.soul.ocr.databinding.FragmentHome2Binding
import java.io.File


class home2Fragment : Fragment()  {
    private  lateinit var binding: FragmentHome2Binding
    private lateinit var adapter: RecentAdapter
    private lateinit var displayList: List<FileListItem.FileItem>
    private lateinit var allFiles: List<FileListItem.FileItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentHome2Binding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        allFiles   = loadRecentFiles()
        adapter    = RecentAdapter(requireContext(), allFiles)
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecyclerView.adapter      = adapter

        setupSearchUi()
        binding.cameraBox.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_scannerFragment)

        }
        binding.textToImageBox.setOnClickListener {
            findNavController().navigate(R.id.action_home2Fragment_to_editFragment)

        }

        binding.scanningBox.setOnClickListener {
            findNavController().navigate(R.id.action_home2Fragment_to_batchScanningFragment)
        }
       binding.recentlayout.setOnClickListener {
           findNavController().navigate(R.id.action_home2Fragment_to_savedFragment)
       }
        binding.btndaimond.setOnClickListener {
            findNavController().navigate(R.id.action_home2Fragment_to_modelScreenFragment)
        }


    }
    private fun loadRecentFiles(): List<FileListItem.FileItem> {
        val rootDir = requireContext().filesDir
        val imageDir = File(rootDir, "SavedImages")

        val recentFiles = mutableListOf<FileListItem.FileItem>()

        val allFiles = (rootDir.listFiles()?.toList() ?: emptyList()) +
                (imageDir.listFiles()?.toList() ?: emptyList())

        for (file in allFiles) {
            if (!file.name.endsWith(".txt") && !file.name.endsWith(".png") && !file.name.endsWith(".pdf")) continue

            val diff = System.currentTimeMillis() - file.lastModified()
            val hours = diff / (1000 * 60 * 60)

            if (hours < 12) {
                val model = InternalFileModel(name = file.name, path = file.absolutePath)
                recentFiles.add(FileListItem.FileItem(model))
            }
        }

        return recentFiles
    }

    private fun setupSearchUi() {
        binding.ivSearch.setOnClickListener {
            binding.etSearch.requestFocus()
            showKeyboard()
        }

        binding.ivClear.setOnClickListener { clearSearch() }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                binding.ivClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterFiles(query)
            }
        })
    }

    private fun filterFiles(query: String) {
        val filtered = if (query.isBlank()) {
            allFiles
        } else {
            allFiles.filter { it.file.name.contains(query, ignoreCase = true) }
        }
        adapter.updateList(filtered)
    }

    private fun clearSearch() {
        binding.etSearch.text?.clear()
        binding.etSearch.clearFocus()
        hideKeyboard()
        adapter.updateList(allFiles)
        binding.ivClear.visibility = View.GONE
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }



}