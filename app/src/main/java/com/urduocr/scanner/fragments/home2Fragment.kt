package com.urduocr.scanner.fragments

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.viewpager2.widget.ViewPager2
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.urduocr.scanner.adapters.RecentAdapter
import com.urduocr.scanner.models.FileListItem
import com.urduocr.scanner.models.InternalFileModel
import com.urduocr.scanner.R
import com.urduocr.scanner.adapters.HomeSliderAdapter
import com.urduocr.scanner.viewmodels.BatchScanningViewModel
import com.urduocr.scanner.databinding.FragmentHome2Binding
import com.urduocr.scanner.models.SliderItem
import com.urduocr.scanner.models.recentInternalFileModel
import java.io.File
import java.util.Calendar

class home2Fragment : Fragment() {

    private lateinit var binding: FragmentHome2Binding
    private lateinit var adapter: RecentAdapter
    private lateinit var allFiles: List<FileListItem.FileItem>
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var viewModel: BatchScanningViewModel
    private lateinit var sliderAdapter: HomeSliderAdapter
    private lateinit var sliderHandler: Handler
    private lateinit var sliderRunnable: Runnable

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
        adapter = RecentAdapter(requireContext(), allFiles, object : RecentAdapter.FileAdapterListener {
            override fun onItemSelectionChanged() {



            }
        })
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecyclerView.adapter = adapter




        binding.cameraBox.setOnClickListener {
            launchDocumentScanner()
        }

        binding.textToImageBox.setOnClickListener {
            findNavController().navigate(R.id.editFragment)
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
        val dotsIndicator = binding.sliderDots
        dotsIndicator.setViewPager2(binding.homeSlider)

        // Optional: Smooth left/right transition
        binding.homeSlider.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // Auto-slide setup
        sliderHandler = Handler(Looper.getMainLooper())
        sliderRunnable = Runnable {
            val nextItem = (binding.homeSlider.currentItem + 1) % sliderItems.size
            binding.homeSlider.setCurrentItem(nextItem, true)
            sliderHandler.postDelayed(sliderRunnable, 3000) // 3 seconds
        }

        // Start auto sliding
        sliderHandler.postDelayed(sliderRunnable, 3000)

        // Reset timer on manual swipe
        binding.homeSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        })
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
        val currentTime = System.currentTimeMillis()

        // Create a list to hold our results
        val recentFiles = mutableListOf<FileListItem.FileItem>()

        // Get all files from both directories
        val allFiles = (rootDir.listFiles()?.toList() ?: emptyList()) +
                (imageDir.listFiles()?.toList() ?: emptyList())

        for (file in allFiles) {
            // Filter only supported file types
            if (!file.name.endsWith(".txt", true) &&
                !file.name.endsWith(".png", true) &&
                !file.name.endsWith(".jpg", true) &&
                !file.name.endsWith(".jpeg", true) &&
                !file.name.endsWith(".pdf", true)) {
                continue
            }

            // Check if file was modified within last 6 hours
            val fileAgeInMillis = currentTime - file.lastModified()
            val sixHoursInMillis = 6 * 60 * 60 * 1000 // 6 hours in milliseconds

            if (fileAgeInMillis <= sixHoursInMillis) {
                recentFiles.add(
                    FileListItem.FileItem(
                        InternalFileModel(
                            name = file.name,
                            path = file.absolutePath,
                            file = file,
                            isFolder = false,
                            lastModified = file.lastModified()
                        )
                    )
                )
            }
        }

        // Sort by newest first
        return recentFiles.sortedByDescending { it.file.lastModified }
    }




    private fun filterFiles(query: String) {
        val filtered = if (query.isBlank()) {
            allFiles
        } else {
            allFiles.filter { it.file.name.contains(query, ignoreCase = true) }
        }
        adapter.updateList(filtered)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

}
