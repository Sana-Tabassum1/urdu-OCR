package com.soul.ocr.fragments

import android.animation.*
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.soul.ocr.R
import com.soul.ocr.ViewModel.BatchScanningViewModel
import com.soul.ocr.databinding.FragmentBatchScanningBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.media.MediaPlayer


class BatchScanningFragment : Fragment() {

    private var _binding: FragmentBatchScanningBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BatchScanningViewModel
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var flashEnabled = false
    private var camera: Camera? = null
    private var mediaPlayer: MediaPlayer? = null

    // 🔊 System shutter sound
    private val shutterSound = MediaActionSound().apply {
        load(MediaActionSound.SHUTTER_CLICK)
    }

    // 📂 Gallery picker
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    uriToBitmap(clip.getItemAt(i).uri)?.let(viewModel::addBitmap)
                }
            } ?: data?.data?.let { uri ->
                uriToBitmap(uri)?.let(viewModel::addBitmap)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCameraFlow()
            else Toast.makeText(requireContext(),"Camera permission denied",Toast.LENGTH_SHORT).show()
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBatchScanningBinding.inflate(inflater, container, false)
        binding.ivStackedImages.setOnClickListener {
            val images = viewModel.bitmapImages.value
            if (images.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please capture or select at least one image", Toast.LENGTH_SHORT).show()
            } else {
                findNavController().navigate(R.id.action_batchScanningFragment_to_cropPreviewFragment)
            }
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity())[BatchScanningViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupListeners()
        checkAndLaunchCamera()
        observeImages()
        updateFlashIcon()
    }

    private fun setupListeners() = with(binding) {
        btnCapture.setOnClickListener { takePhoto() }

        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            galleryLauncher.launch(intent)
        }

        ivFlash.setOnClickListener {
            flashEnabled = !flashEnabled
            camera?.cameraControl?.enableTorch(flashEnabled)
            updateFlashIcon()        // ← refresh icon every tap
        }


        ivBack.setOnClickListener {
            viewModel.clearAll() //  bitmap list clear
          findNavController().navigate(R.id.action_batchScanningFragment_to_nav_home)
        }
    }


    private fun observeImages() {
        viewModel.bitmapImages.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                binding.ivStackedImages.setImageBitmap(list.last())
                binding.tvImageCount.text = list.size.toString()

                // 👇 Make visible
                binding.ivStackedImages.visibility = View.VISIBLE
                binding.tvImageCount.visibility = View.VISIBLE
            } else {
                // 👇 Hide if empty
                binding.ivStackedImages.visibility = View.GONE
                binding.tvImageCount.visibility = View.GONE
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            camera = cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            updateFlashIcon()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // 🚫 Disable the capture button to prevent multiple clicks
        binding.btnCapture.isEnabled = false

        val file = File(requireContext().cacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    val uri = android.net.Uri.fromFile(file)
                    val bitmap = uriToBitmap(uri)
                    bitmap?.let {
                        playCustomShutterSound()
                        animatePhotoToGallery(it, binding.root as ViewGroup, binding.ivStackedImages)
                        viewModel.addBitmap(it)
                    }

                    // ✅ Re-enable capture button
                    binding.btnCapture.isEnabled = true
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()

                    // ✅ Re-enable even on error
                    binding.btnCapture.isEnabled = true
                }
            }
        )
    }


    private fun uriToBitmap(uri: android.net.Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 🎞️ Animate captured image to gallery
    fun animatePhotoToGallery(bitmap: Bitmap, root: ViewGroup, galleryIcon: ImageView) {
        val overlay = ImageView(root.context).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.CENTER_CROP
            val overlay = ImageView(root.context).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

        }
        root.addView(overlay)

        overlay.post {
            val startX = overlay.x
            val startY = overlay.y

            val galleryPos = IntArray(2)
            val rootPos = IntArray(2)
            galleryIcon.getLocationOnScreen(galleryPos)
            root.getLocationOnScreen(rootPos)

            // Target position: ivStackedImages center
            val endX = galleryPos[0] - rootPos[0] + (galleryIcon.width - overlay.width) / 2f
            val endY = galleryPos[1] - rootPos[1] + (galleryIcon.height - overlay.height) / 2f

            val scaleX = galleryIcon.width.toFloat() / overlay.width
            val scaleY = galleryIcon.height.toFloat() / overlay.height

            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(overlay, View.TRANSLATION_X, 0f, endX - startX),
                    ObjectAnimator.ofFloat(overlay, View.TRANSLATION_Y, 0f, endY - startY),
                    ObjectAnimator.ofFloat(overlay, View.SCALE_X, 1f, scaleX),
                    ObjectAnimator.ofFloat(overlay, View.SCALE_Y, 1f, scaleY)
                )
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        root.removeView(overlay)
                        galleryIcon.setImageBitmap(
                            Bitmap.createScaledBitmap(bitmap, galleryIcon.width, galleryIcon.height, true)
                        )
                    }
                })
                start()
            }
        }
    }

    private fun playCustomShutterSound() {
        mediaPlayer?.release() // in case already used
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.capturevoice)
        mediaPlayer?.start()
    }

    private fun updateFlashIcon() {
        val iconRes = if (flashEnabled) R.drawable.flashoff else R.drawable.flashicon
        binding.ivFlash.setImageResource(iconRes)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndLaunchCamera() {
        val perm = android.Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(requireContext(), perm)
            == PackageManager.PERMISSION_GRANTED) {
            startCameraFlow()
        } else {
            cameraPermission.launch(perm)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startCameraFlow() {
        setupCamera()      // tumhari existing function
    }


    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
        cameraExecutor.shutdown()
    }

}
