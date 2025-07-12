package com.soul.ocr.ModelClass
import com.soul.ocr.ModelClass.InternalFileModel
sealed class FileListItem {
    data class Header(val title: String) : FileListItem()
    data class FileItem(val file: InternalFileModel) : FileListItem()
}