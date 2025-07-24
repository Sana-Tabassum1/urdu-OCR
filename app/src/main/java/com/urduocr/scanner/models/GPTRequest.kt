package com.urduocr.scanner.models

data class GPTRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int
)

data class Message(
    val role: String,
    val content: List<Content>
)

data class Content(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String,
    val detail: String = "high"
)

