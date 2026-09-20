package com.example.myapplication7.data
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ЛР №9 Завдання 2: Модель даних із підтримкою серіалізації/десеріалізації JSON
@Serializable
data class Concert(
    val id: Int = 0,
    val artist: String,
    val title: String,
    val date: String,
    val genre: String,
    @SerialName("venue") val venue: String? = null,
    @SerialName("description") val description: String? = null,
    // ЛР №12 Завдання 3: Локальний шлях до фото (не зберігається на сервері)
    val imagePath: String? = null
)

// DTO для POST-запиту (без id — сервер генерує сам)
@Serializable
data class CreateConcertDto(
    val artist: String,
    val title: String,
    val date: String,
    val genre: String,
    val venue: String? = null,
    val description: String? = null
)
