package com.soul.ocr.ModelClass

data class InternalFileModel(
    val name: String,  // File name
    val path: String ,  // Absolute file path
    var isSelected: Boolean = false,
    var isPinned: Boolean = false
)
