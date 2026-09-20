package com.example.myapplication7.data.network

import com.example.myapplication7.data.Concert
import com.example.myapplication7.data.CreateConcertDto
import retrofit2.Response
import retrofit2.http.*

// ЛР №9 Завдання 1: Інтерфейс API-сервісу з 4 ендпоінтами
interface ConcertApiService {

    // GET /concerts — отримати весь список
    @GET("concerts")
    suspend fun getAllConcerts(): Response<List<Concert>>

    // GET /concerts/{id} — отримати елемент за ID
    @GET("concerts/{id}")
    suspend fun getConcertById(@Path("id") id: Int): Response<Concert>

    // POST /concerts — створити новий елемент
    @POST("concerts")
    suspend fun createConcert(@Body concert: CreateConcertDto): Response<Concert>

    // DELETE /concerts/{id} — видалити елемент за ID
    @DELETE("concerts/{id}")
    suspend fun deleteConcert(@Path("id") id: Int): Response<Unit>
}