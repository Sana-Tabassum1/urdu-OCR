package com.urduocr.scanner.models

sealed class FileListItem {
    data class Header(val title: String) : FileListItem()
    data class FileItem(val file: InternalFileModel) : FileListItem()
//    data class Fileitem(val file: recentInternalFileModel) : FileListItem()

}