package com.soul.ocr.ViewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BatchImageviewModel: ViewModel() {
    // Step 1: LiveData define
    private val _imagelist = MutableLiveData<MutableList<Bitmap>>()
    val imageList:LiveData<MutableList<Bitmap>> =_imagelist

     init {
         // Step 2: Empty list initialize
         _imagelist.value=mutableListOf()
     }

    // Step 3: Image add function
    fun addImage(bitmap: Bitmap) {
        val updatedList = _imagelist.value ?: mutableListOf()
        updatedList.add(bitmap)
        _imagelist.value = updatedList
    }


    // Optional: Clear list
    fun clearAll(){
        _imagelist.value=mutableListOf()
    }
    fun removeImageAt(index: Int) {
        _imagelist.value?.let {
            if (index in it.indices) {
                it.removeAt(index)
                _imagelist.value = it
            }
        }
    }
    fun updateImageAt(index: Int, newBitmap: Bitmap) {
        _imagelist.value?.let {
            if (index in it.indices) {
                it[index] = newBitmap
                _imagelist.value = it.toMutableList() // trigger observer
            }
        }
    }


}