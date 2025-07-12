package com.soul.ocr.interfce

import com.soul.ocr.ModelClass.GPTRequest
import com.soul.ocr.ModelClass.GPTResponse
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
