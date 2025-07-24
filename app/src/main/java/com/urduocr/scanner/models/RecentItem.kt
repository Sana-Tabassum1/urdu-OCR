package com.urduocr.scanner.models

data class RecentItem(
    val iconResId: Int,      // Drawable resource ID for the icon
    val title: String,       // Title of the document
    val subtitle: String     // Subtitle or description
)