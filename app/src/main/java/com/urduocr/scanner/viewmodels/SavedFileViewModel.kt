package com.urduocr.scanner.viewmodels

import androidx.lifecycle.ViewModel
import java.io.File

class SavedFileViewModel: ViewModel() {
    val selectedFiles = mutableSetOf<File>()
    var clipboardFiles: List<File> = emptyList()
    var isCutOperation = false
    val removedFilePaths = mutableSetOf<String>()


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
    fun setSelectedFiles(files: List<File>) {
        selectedFiles.clear()
        selectedFiles.addAll(files)
    }

}