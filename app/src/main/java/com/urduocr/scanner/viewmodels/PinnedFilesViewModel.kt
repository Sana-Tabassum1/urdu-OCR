package com.urduocr.scanner.viewmodels

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.urduocr.scanner.models.InternalFileModel
import java.io.File

class PinnedFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _pinnedFiles = MutableLiveData<List<InternalFileModel>>(emptyList())
    val pinnedFiles: LiveData<List<InternalFileModel>> get() = _pinnedFiles

    private val prefs = application.getSharedPreferences("pinned_prefs", Context.MODE_PRIVATE)
    private val PINNED_KEY = "pinned_paths"

    init {
        loadPinnedFilesFromPrefs()
    }

    fun pinFile(file: InternalFileModel) {
        val currentList = _pinnedFiles.value.orEmpty().toMutableList()

        if (!isPinned(file)) {
            val pinnedFile = file.copy(isPinned = true)
            currentList.add(pinnedFile)
            _pinnedFiles.value = currentList
            savePinnedPaths(currentList)
            Toast.makeText(getApplication(), "${file.name} pinned", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplication(), "${file.name} is already pinned", Toast.LENGTH_SHORT).show()
        }
    }

    fun unpinFile(file: InternalFileModel) {
        val updatedList = _pinnedFiles.value.orEmpty().toMutableList()
        updatedList.removeAll { it.path == file.path }
        _pinnedFiles.value = updatedList
        savePinnedPaths(updatedList)
        Toast.makeText(getApplication(), "${file.name} unpinned", Toast.LENGTH_SHORT).show()
    }

    fun isPinned(fileModel: InternalFileModel): Boolean {
        return _pinnedFiles.value?.any { it.path == fileModel.path } == true
    }

    fun isPinned(file: File): Boolean {
        return _pinnedFiles.value?.any { it.path == file.path } == true
    }

    private fun savePinnedPaths(list: List<InternalFileModel>) {
        val pathSet = list.map { it.path }.toSet()
        prefs.edit().putStringSet(PINNED_KEY, pathSet).apply()
    }

    private fun loadPinnedFilesFromPrefs() {
        val pathSet = prefs.getStringSet(PINNED_KEY, emptySet()) ?: emptySet()
        val validFiles = pathSet.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) {
                InternalFileModel(
                    path = file.path,
                    name = file.name,
                    isSelected = false,
                    isPinned = true,
                    file = file,
                    isFolder = file.isDirectory
                )
            } else null
        }
        _pinnedFiles.value = validFiles
    }

    fun clearAllPins() {
        _pinnedFiles.value = emptyList()
        prefs.edit().remove(PINNED_KEY).apply()
    }
}