package com.soul.ocr.ViewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.soul.ocr.ExtractionStage

class BatchScanningViewModel : ViewModel() {

    private val _bitmapImages = MutableLiveData<MutableList<Bitmap>>(mutableListOf())
    val extractionStage = MutableLiveData<ExtractionStage>()

    val bitmapImages: LiveData<MutableList<Bitmap>> = _bitmapImages
    val extractedText = MutableLiveData<String?>()


    fun addBitmap(bitmap: Bitmap) {
        _bitmapImages.value?.add(bitmap)
        _bitmapImages.value = _bitmapImages.value // Trigger observer
    }

    fun removeImageAt(position: Int) {
        _bitmapImages.value?.removeAt(position)
        _bitmapImages.value = _bitmapImages.value
    }

    fun updateImage(position: Int, bitmap: Bitmap) {
        _bitmapImages.value?.set(position, bitmap)
        _bitmapImages.value = _bitmapImages.value
    }

    fun clearAll() {
        _bitmapImages.value?.clear()
        _bitmapImages.value = _bitmapImages.value
    }

    fun getImageCount(): Int = _bitmapImages.value?.size ?: 0

    fun setImages(images: List<Bitmap>) {
        _bitmapImages.value = images.toMutableList()
    }

}
