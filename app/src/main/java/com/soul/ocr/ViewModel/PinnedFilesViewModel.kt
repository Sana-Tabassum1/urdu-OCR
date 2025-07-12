package com.soul.ocr.ViewModel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.soul.ocr.ModelClass.InternalFileModel
import com.soul.ocr.PinnedStorage

class PinnedFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _pinnedFiles = MutableLiveData<MutableList<InternalFileModel>>()
    val pinnedFiles: LiveData<MutableList<InternalFileModel>> = _pinnedFiles

    init {
        // Load from disk
        _pinnedFiles.value = PinnedStorage.loadPinnedFiles(application.applicationContext)
    }

    fun pinFile(file: InternalFileModel) {
        val list = _pinnedFiles.value ?: mutableListOf()
        if (list.none { it.path == file.path }) {   // ✅ skip duplicates
            list.add(file)
            _pinnedFiles.value = list
            PinnedStorage.savePinnedFiles(getApplication(), list)
        }
    }


    fun unpinFile(file: InternalFileModel) {
        val list = _pinnedFiles.value ?: return
        list.removeIf { it.path == file.path }
        _pinnedFiles.value = list
        PinnedStorage.savePinnedFiles(getApplication(), list)
    }
}



