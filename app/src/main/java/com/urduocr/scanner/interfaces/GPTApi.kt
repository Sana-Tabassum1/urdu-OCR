package com.urduocr.scanner.interfaces

import com.urduocr.scanner.models.GPTRequest
import com.urduocr.scanner.models.GPTResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GPTApi {
    @POST("v1/chat/completions")
    fun extractTextFromImage(
        @Header("Authorization") token: String,
        @Body body: GPTRequest
    ): Call<GPTResponse>
}
