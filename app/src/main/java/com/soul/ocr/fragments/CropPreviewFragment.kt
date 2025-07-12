package com.soul.ocr.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import com.soul.ocr.R
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.soul.ocr.Adaptors.CropPreviewImageAdapter
import com.soul.ocr.ViewModel.BatchScanningViewModel
import com.soul.ocr.databinding.FragmentCropPreviewBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*

class CropPreviewFragment : Fragment() {

    private var _binding: FragmentCropPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BatchScanningViewModel
    private lateinit var adapter: CropPreviewImageAdapter

    private var selectedCropPosition: Int = -1

    // uCrop Result Handler
    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                val croppedBitmap = getBitmapFromUri(uri)
                if (croppedBitmap != null && selectedCropPosition != -1) {
                    viewModel.updateImage(selectedCropPosition, croppedBitmap)
                    adapter.notifyItemChanged(selectedCropPosition)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCropPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity())[BatchScanningViewModel::class.java]

        setupRecyclerView()
        observeImages()

        binding.continueButton.setOnClickListener {
            val imageList = viewModel.bitmapImages.value
            if (imageList.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please select or capture at least one image", Toast.LENGTH_SHORT).show()
            } else {
                findNavController().navigate(R.id.action_cropPreviewFragment_to_batchExtractFragment)
            }
        }


        binding.cameraCardview.setOnClickListener {
            findNavController().navigate(R.id.action_cropPreviewFragment_to_batchScanningFragment)
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigate(R.id.action_cropPreviewFragment_to_batchScanningFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = CropPreviewImageAdapter(
            imageList = mutableListOf(),
            onCropClick = { position, bitmap ->
                selectedCropPosition = position
                startCrop(bitmap)
            },
            onDeleteClick = { position ->
                viewModel.removeImageAt(position)
                adapter.updateList(viewModel.bitmapImages.value ?: emptyList())
            }
        )

        binding.CPRecyclerview.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.CPRecyclerview.adapter = adapter
    }

    private fun observeImages() {
        viewModel.bitmapImages.observe(viewLifecycleOwner) { list ->
            adapter.updateList(list)
        }
    }

    private fun startCrop(bitmap: Bitmap) {
        val sourceUri = getImageUri(bitmap)
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "${UUID.randomUUID()}.jpg"))

        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setToolbarTitle("Crop")
        }

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(0f, 0f)

        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val path = MediaStore.Images.Media.insertImage(requireContext().contentResolver, bitmap, "temp", null)
        return Uri.parse(path)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
