package com.soul.ocr.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.soul.ocr.R
import com.soul.ocr.databinding.FragmentPreview2Binding
import com.yalantis.ucrop.UCrop
import java.io.File

class previewFragment : Fragment() {
   private lateinit var binding: FragmentPreview2Binding
    private lateinit var imageUri: Uri
    private var croppedImageUri: Uri? = null

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    croppedImageUri = resultUri
                   // binding.imagePreview.setImageURI(croppedImageUri)
                } else {
                    Toast.makeText(requireActivity(), "Crop failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireActivity(), "Crop canceled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentPreview2Binding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireActivity(), R.color.green)
        val uriString = arguments?.getString("imageUri")
        Log.d("PreviewFragment", "Received URI from Bundle: $uriString")
        if (uriString == null) {
            Toast.makeText(requireActivity(), "No image URI passed", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        imageUri = uriString.toUri()

        startCrop(imageUri)

//        binding.btnDone.setOnClickListener {
//            if (croppedImageUri != null) {
//                val bundle = Bundle()
//                bundle.putString("croppedImageUri", croppedImageUri.toString())
//                findNavController().navigate(R.id.texttoImageFragment, bundle)
//            } else {
//                Toast.makeText(requireActivity(), "Please crop the image first", Toast.LENGTH_SHORT).show()
//            }
//        }
    }
    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(
            File(
                requireActivity().cacheDir,
                "cropped_${System.currentTimeMillis()}.jpg"
            )
        )

        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setToolbarTitle("Crop Image")
        }

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f) // Optional: square crop
        Log.d("PreviewFragment", "Starting crop with URI: $sourceUri")
        cropLauncher.launch(uCrop.getIntent(requireActivity()))
    }
}