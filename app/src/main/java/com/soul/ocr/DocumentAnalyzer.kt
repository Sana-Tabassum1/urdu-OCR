package com.soul.ocr

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
//import com.google.mlkit.vision.documentscanner.GmsDocumentScannerResult

class DocumentAnalyzer(
    private val overlayView: OverlayView,
    private val onDocumentDetected: () -> Unit
) : ImageAnalysis.Analyzer {

    private val documentScanner: GmsDocumentScanner = GmsDocumentScanning.getClient(
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            .build()
    )

    private var lastTriggerTime = 0L
    override fun analyze(image: ImageProxy) {
        TODO("Not yet implemented")
    }

    //  @OptIn(ExperimentalGetImage::class)
//    override fun analyze(imageProxy: ImageProxy) {
//        val mediaImage = imageProxy.image ?: run {
//            imageProxy.close()
//            return
//        }
//
//        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//        documentScanner.processImage(inputImage)
//            .addOnSuccessListener { result: GmsDocumentScannerResult ->
//                // NOTE: MLKit Document Scanner doesn't give corners in processImage result
//                // So this section is for demonstration if you have logic to extract corners.
//
//                Log.d("DocumentAnalyzer", "Scan success: ${result.pages.size} page(s)")
//
//                // Trigger capture
//                val now = System.currentTimeMillis()
//                if (now - lastTriggerTime > 2000) {
//                    lastTriggerTime = now
//                    onDocumentDetected()
//                }
//
//                // For visual overlay (optional)
//                overlayView.clear() // you can draw dummy rectangle if needed
//
//            }
//            .addOnFailureListener {
//                Log.e("DocumentAnalyzer", "Document scan failed: ${it.message}")
//                overlayView.clear()
//            }
//            .addOnCompleteListener {
//                imageProxy.close()
//            }
//    }
}
