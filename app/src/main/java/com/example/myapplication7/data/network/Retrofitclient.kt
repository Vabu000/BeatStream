package com.example.myapplication7.data.network

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

// ЛР №9 Завдання 1: Налаштування мережевого шару (Retrofit + OkHttp)
object RetrofitClient {

    // Замініть на URL вашого MockAPI або локального сервера
    private const val BASE_URL = "https://6a00e87f36fb6ad04de09317.mockapi.io/api/v1/"

    private val json = Json {
        ignoreUnknownKeys = true   // ігноруємо невідомі поля JSON
        coerceInputValues = true   // обробка null для non-nullable полів
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: ConcertApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(ConcertApiService::class.java)
    }
}
