package com.soul.ocr

object DocumentDetector {
    fun getClient(): DocumentScanner = DocumentScanner.getClient(
        DocumentScannerOptions.Builder()
            .setScannerMode(DocumentScannerOptions.SCANNER_MODE_BASE)
            .build()
    )
}
