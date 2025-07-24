package com.urduocr.scanner.models

data class GPTResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageContent
)

data class MessageContent(
    val content: String
)

