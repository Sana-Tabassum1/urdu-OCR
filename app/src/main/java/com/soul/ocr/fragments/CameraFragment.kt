package com.soul.ocr.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.soul.ocr.R
import com.soul.ocr.databinding.FragmentCameraBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera()
        else Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImageUri = result.data?.data
            selectedImageUri?.let { startCrop(it) }
        }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val croppedUri = UCrop.getOutput(result.data!!)
            if (croppedUri != null) {
                val bundle = Bundle().apply {
                    putString("croppedImageUri", croppedUri.toString())
                }
                findNavController().navigate(R.id.texttoImageFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Crop failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Crop canceled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }

        binding.btnCapture.setOnClickListener {
            capturePhoto()
        }

//        binding.btnSwitchCamera.setOnClickListener {
//            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
//                CameraSelector.LENS_FACING_FRONT
//            else
//                CameraSelector.LENS_FACING_BACK
//            startCamera()
//        }
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
            // requireActivity().onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun capturePhoto() {
        val photoFile = File(requireContext().cacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                startCrop(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(requireContext(), "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_${UUID.randomUUID()}.jpg"))
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setToolbarTitle("Crop Image")
        }

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)

        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
