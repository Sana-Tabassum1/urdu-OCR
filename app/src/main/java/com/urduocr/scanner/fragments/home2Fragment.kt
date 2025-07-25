package com.urduocr.scanner.fragments

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.urduocr.scanner.adapters.RecentAdapter
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.models.InternalFileModel
import com.urduocr.scanner.R
import com.urduocr.scanner.viewmodels.BatchScanningViewModel
import com.urduocr.scanner.databinding.FragmentHome2Binding
import java.io.File

class home2Fragment : Fragment() {

    private lateinit var binding: FragmentHome2Binding
    private lateinit var adapter: RecentAdapter
    private lateinit var allFiles: List<FileListItem.FileItem>
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var viewModel: BatchScanningViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BatchScanningViewModel::class.java]

        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val resultCode = result.resultCode
            val data = result.data
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)

            if (resultCode == Activity.RESULT_OK && scanResult != null) {
                val pages = scanResult.pages

                if (!pages.isNullOrEmpty()) {
                    val bitmaps = pages.mapNotNull { page ->
                        val inputStream = requireContext().contentResolver.openInputStream(page.imageUri)
                        BitmapFactory.decodeStream(inputStream)
                    }

                    if (isAdded){
                        viewModel.setImages(bitmaps)
                        findNavController().navigate(R.id.batchExtractFragment)
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("Scanner", "User cancelled scan")
            } else {
                Log.e("Scanner", "Scan failed or unknown error")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHome2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allFiles = loadRecentFiles()
        adapter = RecentAdapter(requireContext(), allFiles)
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecyclerView.adapter = adapter

        setupSearchUi()

        binding.cameraBox.setOnClickListener {
            launchDocumentScanner()
        }

        binding.textToImageBox.setOnClickListener {
            findNavController().navigate(R.id.kFragment)
        }

        binding.scanningBox.setOnClickListener {
            launchDocumentScanner()
        }

        binding.recentlayout.setOnClickListener {
            findNavController().navigate(R.id.nav_library)
        }

        binding.btndaimond.setOnClickListener {
            findNavController().navigate(R.id.modelScreenFragment)
        }
    }

    private fun launchDocumentScanner() {
        val scannerOptions = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val documentScanner = GmsDocumentScanning.getClient(scannerOptions)

        documentScanner.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                val request = IntentSenderRequest.Builder(intentSender).build()
                scannerLauncher.launch(request)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error launching scanner: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Scanner", "Failed to launch: ${e.message}")
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

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
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
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }
}
