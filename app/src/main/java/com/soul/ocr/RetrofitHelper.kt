package com.soul.ocr

import com.soul.ocr.interfce.GPTApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {

    private const val BASE_URL = "https://api.openai.com/"

    private val client = OkHttpClient.Builder().build()

    val instance: GPTApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GPTApi::class.java)
    }
}