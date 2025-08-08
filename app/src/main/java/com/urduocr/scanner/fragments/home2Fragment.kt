package com.urduocr.scanner.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.urduocr.scanner.R
import com.urduocr.scanner.adapters.HomeSliderAdapter
import com.urduocr.scanner.adapters.RecentAdapter
import com.urduocr.scanner.databinding.FragmentHome2Binding
import com.urduocr.scanner.interfaces.OnFileActionListener
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.models.InternalFileModel
import com.urduocr.scanner.models.SliderItem
import com.urduocr.scanner.viewmodels.BatchScanningViewModel
import com.urduocr.scanner.viewmodels.PinnedFilesViewModel
import com.urduocr.scanner.viewmodels.SavedFileViewModel
import java.io.File

class home2Fragment : Fragment() {

    private lateinit var binding: FragmentHome2Binding
    private lateinit var adapter: RecentAdapter
    private lateinit var viewModel: BatchScanningViewModel
    private lateinit var savedFileViewModel: SavedFileViewModel
    private lateinit var pinnedViewModel: PinnedFilesViewModel
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var sliderAdapter: HomeSliderAdapter
    private lateinit var sliderHandler: Handler
    private lateinit var sliderRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[BatchScanningViewModel::class.java]
        savedFileViewModel = ViewModelProvider(requireActivity())[SavedFileViewModel::class.java]
        pinnedViewModel = ViewModelProvider(requireActivity())[PinnedFilesViewModel::class.java]

        setupDocumentScanner()
    }

    private fun setupDocumentScanner() {
        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleScanResult(result.resultCode, result.data)
        }
    }

    private fun handleScanResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
            scanResult?.pages?.let { pages ->
                val bitmaps = pages.mapNotNull { page ->
                    requireContext().contentResolver.openInputStream(page.imageUri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                if (bitmaps.isNotEmpty()) {
                    viewModel.setImages(bitmaps)
                    findNavController().navigate(R.id.batchExtractFragment)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHome2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadRecentFiles()
        setupObservers()
        setupAutoSlider()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = RecentAdapter(requireContext(), emptyList(), object : RecentAdapter.OnSelectionChangedListener {
            override fun onItemSelectionChanged() {
                val selectedFiles = adapter.getSelectedFiles().map { File(it.file.path) }
                savedFileViewModel.setSelectedFiles(selectedFiles)
            }
        }).apply {
            fileActionListener = createFileActionListener()
        }

        binding.homeRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@home2Fragment.adapter
        }
    }

    private fun loadRecentFiles() {
        savedFileViewModel.loadRecentFiles(requireContext())
    }

    private fun createFileActionListener(): OnFileActionListener = object : OnFileActionListener {
        override fun onCopy(file: File) = handleCopy(file)
        override fun onCut(file: File) = handleCut(file)
        override fun onDelete(file: File) = handleDelete(file)
        override fun onShare(file: File) = handleShare(file)
        override fun onPin(file: File) = handlePin(file)
        override fun onUnpin(file: File) = handleUnpin(file)
        override fun onRenameFile(file: File, newName: String) = handleRename(file, newName)
        override fun onRenameFolder(folder: File, newName: String) {} // Not used in recent
        override fun onPaste() {} // Not used in recent
    }

    private fun handleCopy(file: File) {
        savedFileViewModel.clearSelection()
        savedFileViewModel.selectFile(file)
        savedFileViewModel.copyFiles()
        Toast.makeText(requireContext(), "Copied ${file.name}", Toast.LENGTH_SHORT).show()
    }

    private fun handleCut(file: File) {
        savedFileViewModel.clearSelection()
        savedFileViewModel.selectFile(file)
        savedFileViewModel.cutFiles()
        Toast.makeText(requireContext(), "Cut ${file.name}", Toast.LENGTH_SHORT).show()
    }

    private fun handleDelete(file: File) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                savedFileViewModel.clearSelection()
                savedFileViewModel.selectFile(file)
                savedFileViewModel.deleteSelected()
                Toast.makeText(requireContext(), "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleShare(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = when {
                file.name.endsWith(".pdf") -> "application/pdf"
                file.name.endsWith(".txt") -> "text/plain"
                file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                file.name.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
                file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                else -> "*/*"
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun handlePin(file: File) {
        val internalModel = InternalFileModel(
            path = file.path,
            name = file.name,
            isSelected = false,
            isPinned = true,
            file = file,
            isFolder = false
        )

        if (!pinnedViewModel.isPinned(internalModel)) {
            pinnedViewModel.pinFile(internalModel)  // Pass InternalFileModel directly
            Toast.makeText(requireContext(), "File pinned", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Already pinned", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUnpin(file: File) {
        val internalModel = InternalFileModel(
            path = file.path,
            name = file.name,
            isSelected = false,
            isPinned = false,
            file = file,
            isFolder = false
        )
        pinnedViewModel.unpinFile(internalModel)  // Pass InternalFileModel directly
        Toast.makeText(requireContext(), "File unpinned", Toast.LENGTH_SHORT).show()
    }

    private fun handleRename(file: File, newName: String) {
        val newFile = File(file.parent, newName)
        if (file.renameTo(newFile)) {
            Toast.makeText(requireContext(), "Renamed to $newName", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Failed to rename", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        savedFileViewModel.recentFiles.observe(viewLifecycleOwner) { files ->
            adapter.updateList(files.map { file ->
                FileListItem.FileItem(
                    InternalFileModel(
                        name = file.name,
                        path = file.path,
                        file = file,
                        isSelected = false,
                        isPinned = pinnedViewModel.isPinned(file),
                        isFolder = false,
                        lastModified = file.lastModified()
                    )
                )
            })
        }
    }

    private fun setupClickListeners() {
        binding.cameraBox.setOnClickListener { launchDocumentScanner() }
        binding.textToImageBox.setOnClickListener { findNavController().navigate(R.id.editFragment) }
        binding.scanningBox.setOnClickListener { launchDocumentScanner() }
        binding.recentlayout.setOnClickListener { findNavController().navigate(R.id.nav_library) }
        binding.btndaimond.setOnClickListener { showCreditsBottomSheet() }
    }

    private fun launchDocumentScanner() {
        val scannerOptions = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        GmsDocumentScanning.getClient(scannerOptions)
            .getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Scanner error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupAutoSlider() {
        val sliderItems = listOf(
            SliderItem(R.drawable.urduu, "Most accurate Urdu OCR","Whether its handwriting or a book,\n" +
                    "Urdu OCR recogize text with 90% accuracy"),
            SliderItem(R.drawable.file, "Image to Urdu image","Type Urdu and generate image of Urdu text.\n" +
                    "Choose from five different Urdu fonts."),
            SliderItem(R.drawable.photo, "Organize your files","Type Urdu and generate image of Urdu text.\n" +
                    "Choose from five different Urdu fonts.")
        )
        sliderAdapter = HomeSliderAdapter(sliderItems)
        binding.homeSlider.adapter = sliderAdapter
        binding.sliderDots.setViewPager2(binding.homeSlider)

        sliderHandler = Handler(Looper.getMainLooper())
        sliderRunnable = Runnable {
            val nextItem = (binding.homeSlider.currentItem + 1) % sliderItems.size
            binding.homeSlider.setCurrentItem(nextItem, true)
            sliderHandler.postDelayed(sliderRunnable, 3000)
        }
        sliderHandler.postDelayed(sliderRunnable, 3000)

        binding.homeSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        })
    }

    private fun showCreditsBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.item_credit_package, null)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        view.findViewById<View>(R.id.btncontinue).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sliderHandler.removeCallbacks(sliderRunnable)
    }
}