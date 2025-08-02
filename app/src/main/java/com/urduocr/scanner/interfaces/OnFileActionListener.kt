package com.urduocr.scanner.interfaces

import java.io.File

interface OnFileActionListener {
    fun onCopy(file: File)
    fun onCut(file: File)
    fun onPaste()
    fun onDelete(file: File)
    fun onShare(file: File)
    fun onPin(file: File)
    fun onUnpin(files: File)
    fun onRenameFolder(oldFile: File, newName: String)
}
