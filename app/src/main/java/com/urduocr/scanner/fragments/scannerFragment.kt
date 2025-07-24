package com.urduocr.scanner.fragments

import android.app.Activity
import android.content.IntentSender
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.documentscanner.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.urduocr.scanner.databinding.FragmentScannerBinding

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var documentScannerClient: GmsDocumentScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            val resultCode = activityResult.resultCode
            val data = activityResult.data
            val result = GmsDocumentScanningResult.fromActivityResultIntent(data)

            if (resultCode == Activity.RESULT_OK && result != null) {
                val pages = result.pages
                if (!pages.isNullOrEmpty()) {
                    val firstPage = pages[0]
                    val uri = firstPage.imageUri
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    //binding.ivScannedImage.setImageBitmap(bitmap)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("Scanner", "User cancelled scan")
            } else {
                Log.e("Scanner", "Scan failed")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        documentScannerClient = GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setResultFormats(RESULT_FORMAT_PDF)
                .build()
        )

//        binding.btnScanDocument.setOnClickListener {
//            launchDocumentScanner()
//        }
    }

    private fun launchDocumentScanner() {
        documentScannerClient.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                val request = IntentSenderRequest.Builder(intentSender).build()
                scannerLauncher.launch(request)
            }
            .addOnFailureListener { e ->
                Log.e("Scanner", "Error launching scanner: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
