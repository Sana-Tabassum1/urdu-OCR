package com.urduocr.scanner.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class SavedFileViewModel: ViewModel() {
    val selectedFiles = mutableSetOf<File>()
    var clipboardFiles: List<File> = emptyList()
    var isCutOperation = false
    val removedFilePaths = mutableSetOf<String>()
    private val _recentFiles = MutableLiveData<List<File>>()
    val recentFiles: LiveData<List<File>> get() = _recentFiles


    fun selectFile(file: File) {
        selectedFiles.add(file)
    }

    fun deselectFile(file: File) {
        selectedFiles.remove(file)
    }

    fun clearSelection() {
        selectedFiles.clear()
    }

    fun copyFiles() {
        clipboardFiles = selectedFiles.toList()
        isCutOperation = false
    }

    fun cutFiles() {
        clipboardFiles = selectedFiles.toList()
        isCutOperation = true
    }

    fun pasteFiles(targetDir: File) {
        clipboardFiles.forEach { file ->
            val newFile = File(targetDir, file.name)
            file.copyTo(newFile, overwrite = true)
            if (isCutOperation) {
                file.delete()
                removedFilePaths.add(file.absolutePath)
            }
        }
        clipboardFiles = emptyList()
        isCutOperation = false
    }


    fun deleteSelected() {
        selectedFiles.forEach { it.delete() }
        clearSelection()
    }
    fun clearDeletedPaths() {
        removedFilePaths.clear()
    }
    fun setSelectedFiles(files: List<File>) {
        selectedFiles.clear()
        selectedFiles.addAll(files)
    }

    fun loadRecentFiles(context: Context) {
        val rootDir = context.filesDir
        val imageDir = File(rootDir, "SavedImages")
        val currentTime = System.currentTimeMillis()

        val allFiles = (rootDir.listFiles()?.toList() ?: emptyList()) +
                (imageDir.listFiles()?.toList() ?: emptyList())

        val recentFiles = allFiles.filter { file ->
            // Filter supported file types
            file.name.endsWith(".txt", true) ||
                    file.name.endsWith(".png", true) ||
                    file.name.endsWith(".jpg", true) ||
                    file.name.endsWith(".jpeg", true) ||
                    file.name.endsWith(".pdf", true)
        }.filter { file ->
            // Filter files modified within last 6 hours
            val fileAgeInMillis = currentTime - file.lastModified()
            fileAgeInMillis <= (6 * 60 * 60 * 1000) // 6 hours
        }.sortedByDescending { it.lastModified() }

        _recentFiles.value = recentFiles
    }
}