package com.soul.ocr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectDetectionAnalyzer(
    val currentMode: String, // "Document", "ID Card", etc.
    val overlayView: OverlayView
) : ImageAnalysis.Analyzer {

    // ✅ Correct ObjectDetectorOptions
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification() // optional
        .build()

    private val detector = ObjectDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { detectedObjects ->
                val rects = detectedObjects.map { it.boundingBox }

                // Filter based on mode
                val filteredRects = when (currentMode) {
                    "Document" -> rects.filter { it.width() > 500 && it.height() > 500 }
                    "ID Card" -> rects.filter { it.width() in 200..500 && it.height() in 100..400 }
                    "Business Card" -> rects.filter { it.width() in 250..600 && it.height() in 150..300 }
                    else -> rects
                }

                overlayView.setRects(filteredRects)
            }
            .addOnFailureListener {
                overlayView.setRects(emptyList())
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
