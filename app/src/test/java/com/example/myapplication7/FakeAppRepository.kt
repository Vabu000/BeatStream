package com.example.myapplication7

import com.example.myapplication7.data.Concert
import com.example.myapplication7.data.CreateConcertDto
import com.example.myapplication7.data.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ЛР №13 — Завдання 1: Модульне тестування бізнес-логіки ViewModel.
 * СЦЕНАРІЇ, які дозволяє перевірити:
 *   - shouldReturnError = false → симулює успішну відповідь API (HTTP 200)
 *   - shouldReturnError = true  → симулює помилку мережі (timeout, 404, 500)
 *   - setCachedConcerts(list)   → симулює дані у Room (кеш для офлайн-режиму)
 *   - deleteSuccess = false     → симулює невдале видалення на сервері
 */
class FakeAppRepository {

    // ----------------------------------------------------------------
    // Контрольні змінні — змінюються у кожному тесті через @Before або в Arrange-секції
    // ----------------------------------------------------------------

    /**
     * Імітує відсутність інтернету, тайм-аут або серверну помилку.
     */
    var shouldReturnError: Boolean = false

    /**
     * Перевіряється в тестах: assertEquals("Мережева помилка", result.message)
     */
    var errorMessage: String = "Мережева помилка"

    var concertsToReturn: List<Concert> = emptyList()

    /**
     * Використовується у тестах для перевірки полів щойно доданого об'єкта.
     */
    var createdConcert: Concert? = null

    /**
     * Тест №9 перевіряє, що при невдалому видаленні список не змінюється.
     */
    var deleteSuccess: Boolean = true

    // ----------------------------------------------------------------
    // Кешований Flow — імітує Room Database (локальне сховище)
    // ----------------------------------------------------------------

    /**
     * MutableStateFlow імітує Room DAO, що повертає Flow<List<Concert>>.
     * У ViewModel офлайн-режим реалізований так:
     *   1. Запит до API → помилка мережі (NetworkResult.Error)
     *   2. Читаємо кеш з Room (cachedConcerts.first())
     *   3. Якщо кеш не порожній → Success(isOffline = true), інакше → Error
     *
     * setCachedConcerts(emptyList()) → порожній кеш → UI стан буде Error
     * setCachedConcerts(listOf(concert1)) → є кеш → UI стан буде Success(isOffline=true)
     */
    private val _cachedFlow = MutableStateFlow<List<Concert>>(emptyList())
    val cachedConcerts: Flow<List<Concert>> = _cachedFlow

    /** Встановлює вміст "кешу" (Room DB) для тесту. */
    fun setCachedConcerts(concerts: List<Concert>) {
        _cachedFlow.value = concerts
    }

    // ----------------------------------------------------------------
    // Методи-двійники реального AppRepository
    // ----------------------------------------------------------------

    /**
     * Повертає список усіх концертів.
     * Використовується у тестах 1, 2, 3, 4, 5, 8.
     *
     * NetworkResult.Success → ViewModel виставляє ConcertListUiState.Success або Empty
     * NetworkResult.Error   → ViewModel перевіряє кеш та виставляє Error або Success(offline)
     */
    suspend fun getAllConcerts(): NetworkResult<List<Concert>> {
        return if (shouldReturnError) {
            NetworkResult.Error(errorMessage)
        } else {
            NetworkResult.Success(concertsToReturn)
        }
    }

    /**
     * Повертає один концерт за ID.
     * Використовується у тестах 10 (неіснуючий ID) та 12 (успішні деталі).
     *
     * Якщо id відсутній у concertsToReturn → повертає Error (як справжній 404).
     */
    suspend fun getConcertById(id: Int): NetworkResult<Concert> {
        val concert = concertsToReturn.find { it.id == id }
        return if (shouldReturnError || concert == null) {
            NetworkResult.Error(errorMessage)
        } else {
            NetworkResult.Success(concert)
        }
    }

    /**
     * Створює новий концерт (POST-запит у реальному репозиторії).
     * Використовується у тесті 7 (CRUD: успішне створення).
     *
     * Після виклику:
     *   - createdConcert зберігає щойно створений об'єкт
     *   - Новий Concert додається до concertsToReturn (як на сервері)
     */
    suspend fun createConcert(dto: CreateConcertDto): NetworkResult<Concert> {
        return if (shouldReturnError) {
            NetworkResult.Error(errorMessage)
        } else {
            val concert = Concert(
                id = 99, // фіксований тестовий ID
                artist = dto.artist,
                title = dto.title,
                date = dto.date,
                genre = dto.genre,
                venue = dto.venue
            )
            createdConcert = concert
            concertsToReturn = concertsToReturn + concert // симулюємо збереження
            NetworkResult.Success(concert)
        }
    }

    /**
     * Видаляє концерт за ID (DELETE-запит у реальному репозиторії).
     * Використовується у тесті 9 (невдале видалення → список незмінний).
     *
     * deleteSuccess = true  → список оновлюється (filter видаляє елемент)
     * deleteSuccess = false → повертає Error, список НЕ змінюється
     */
    suspend fun deleteConcert(id: Int): NetworkResult<Unit> {
        return if (!deleteSuccess) {
            NetworkResult.Error(errorMessage)
        } else {
            concertsToReturn = concertsToReturn.filter { it.id != id }
            NetworkResult.Success(Unit)
        }
    }

    /**
     * Зберігає шлях до фото концерту (функціонал ЛР №12).
     * У тестах не використовується — no-op заглушка.
     */
    suspend fun saveImagePath(concertId: Int, imagePath: String?) {
        // no-op для тестів: немає файлової системи в JVM-тестах
    }
}
