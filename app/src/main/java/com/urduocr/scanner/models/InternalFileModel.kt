package com.urduocr.scanner.models

import java.io.File

data class InternalFileModel(
    val name: String,  // File name
    val path: String,  // Absolute file path
    var isSelected: Boolean = false,
    var isPinned: Boolean = false,
    val file: File,
    val isFolder: Boolean = false,
    val lastModified: Long = file.lastModified() // Add this line
)
