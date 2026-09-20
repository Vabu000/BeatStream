package com.example.myapplication7.data

import com.example.myapplication7.data.local.AppDatabase
import com.example.myapplication7.data.local.ConcertEntity
import com.example.myapplication7.data.network.ConcertApiService
import com.example.myapplication7.data.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

// ЛР №9 Завдання 3 + 5: Repository з мережевим шаром та кешуванням
// ЛР №12: Додано видалення файлу фото при видаленні концерту
class AppRepository(
    private val api: ConcertApiService = RetrofitClient.apiService,
    private val db: AppDatabase? = null  // null у Preview-режимі
) {
    // Кешований список концертів з Room (Flow)
    val cachedConcerts: Flow<List<Concert>>? = db?.concertDao()
        ?.getAllConcerts()
        ?.map { entities -> entities.map { it.toDomain() } }

    // GET /concerts
    suspend fun getAllConcerts(): NetworkResult<List<Concert>> {
        val result = safeApiCall { api.getAllConcerts() }
        if (result is NetworkResult.Success) {
            // Зберігаємо успішну відповідь у кеш, зберігаючи локальні imagePath
            val existing = db?.concertDao()?.getAllConcerts()
            db?.concertDao()?.upsertAll(result.data.map { concert ->
                concert.toEntity()
            })
        }
        return result
    }

    // GET /concerts/{id}
    suspend fun getConcertById(id: Int): NetworkResult<Concert> {
        val networkResult = safeApiCall { api.getConcertById(id) }
        // Підтягуємо локальний imagePath з кешу
        if (networkResult is NetworkResult.Success) {
            val cached = db?.concertDao()?.getConcertById(id)
            return NetworkResult.Success(
                networkResult.data.copy(imagePath = cached?.imagePath)
            )
        }
        return networkResult
    }

    // POST /concerts
    suspend fun createConcert(dto: CreateConcertDto): NetworkResult<Concert> {
        val result = safeApiCall { api.createConcert(dto) }
        if (result is NetworkResult.Success) {
            db?.concertDao()?.upsertAll(listOf(result.data.toEntity()))
        }
        return result
    }

    // DELETE /concerts/{id}
    // ЛР №12 Завдання 3: При видаленні — також видаляємо файл фото з filesDir
    suspend fun deleteConcert(id: Int): NetworkResult<Unit> {
        // Отримуємо шлях до фото перед видаленням
        val imagePath = db?.concertDao()?.getConcertById(id)?.imagePath
        val result = safeApiCall { api.deleteConcert(id) }
        if (result is NetworkResult.Success) {
            db?.concertDao()?.deleteConcertById(id)
            // Видаляємо файл зображення з файлової системи
            imagePath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
        return result
    }

    // ЛР №12 Завдання 3: Збереження шляху до фото для концерту
    suspend fun saveImagePath(concertId: Int, imagePath: String?) {
        db?.concertDao()?.updateImagePath(concertId, imagePath)
    }
}

// Маппери
fun ConcertEntity.toDomain() = Concert(
    id = id, artist = artist, title = title,
    date = date, genre = genre, venue = venue,
    description = description, imagePath = imagePath
)

fun Concert.toEntity() = ConcertEntity(
    id = id, artist = artist, title = title,
    date = date, genre = genre, venue = venue,
    description = description, imagePath = imagePath
)