package com.urduocr.scanner.models

import java.io.File

data class recentInternalFileModel(
    val name: String,  // File name
    val path: String,  // Absolute file path
    var isSelected: Boolean = false,
    var isPinned: Boolean = false,
    val isFolder: Boolean = false
)