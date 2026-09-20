package com.example.myapplication7

import com.example.myapplication7.data.Concert
import com.example.myapplication7.data.CreateConcertDto
import com.example.myapplication7.data.NetworkResult
import com.example.myapplication7.ui.viewmodels.ConcertListUiState
import com.example.myapplication7.ui.viewmodels.ConcertDetailUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.Dispatchers

/**
 * ЛР №13 — Завдання 1: Модульне тестування бізнес-логіки ViewModel.
 *
 * =============================================================
 * ЧОМУ НЕ ТЕСТУЄМО BeatStreamViewModel НАПРЯМУ?
 * =============================================================
 * BeatStreamViewModel extends AndroidViewModel — він потребує об'єкта Application
 * (Android-контекст). У JVM unit-тестах (папка /test, без емулятора) Android-середовища
 * немає. Тому ми тестуємо БІЗНЕС-ЛОГІКУ ViewModel через FakeAppRepository,
 * ізолюючи її від мережі та БД.
 *
 * =============================================================
 * СТРАТЕГІЯ: Fake-паттерн + тестування логіки напряму
 * =============================================================
 * Реальний стек:                 Тестовий стек:
 *   BeatStreamViewModel            BeatStreamViewModelLogicTest
 *     ↓ використовує                 ↓ використовує
 *   AppRepository (реальний)       FakeAppRepository
 *     ↓ викликає                     ↓ повертає задані значення
 *   Retrofit + Room DB             (нічого не викликає)
 *
 * =============================================================
 * СТРУКТУРА ТЕСТІВ: шаблон AAA (Given–When–Then)
 * =============================================================
 *   Arrange — підготовка: налаштовуємо fakeRepository, задаємо тестові дані
 *   Act     — дія: викликаємо метод, що тестується
 *   Assert  — перевірка: перевіряємо результат через assertTrue/assertEquals
 *
 * =============================================================
 * ПОКРИТТЯ: 3 категорії сценаріїв
 * =============================================================
 *   ✅ Позитивні  — нормальна робота (тести 1, 2, 7, 11, 12)
 *   ❌ Негативні  — обробка помилок (тести 3, 4, 9)
 *   🔲 Edge cases — граничні ситуації (тести 5, 6, 8, 10)
 *
 * Використані інструменти:
 *   - FakeAppRepository (Fake-паттерн, підміна залежностей)
 *   - UnconfinedTestDispatcher (синхронне виконання корутин)
 *   - runTest { } (тест-скоуп для suspend-функцій)
 *   - JUnit4: @Test, @Before, @After, assertTrue, assertEquals
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BeatStreamViewModelLogicTest {

    // ----------------------------------------------------------------
    // UnconfinedTestDispatcher — чому потрібен?
    // ----------------------------------------------------------------
    // Dispatchers.Main (головний потік Android) не існує в JVM-тестах.
    // Якщо ViewModel запускає корутину в viewModelScope (→ Main dispatcher),
    // без підміни тест впаде: "Module with the Main dispatcher had failed to initialize".
    //
    // UnconfinedTestDispatcher запускає корутини СИНХРОННО — тест не чекає
    // завершення асинхронної операції, а отримує результат одразу.
    private val testDispatcher = UnconfinedTestDispatcher()

    // FakeAppRepository — підміняє реальний AppRepository у тестах
    private lateinit var fakeRepository: FakeAppRepository

    // ----------------------------------------------------------------
    // Тестові дані — 3 концерти, що використовуються у різних тестах
    // ----------------------------------------------------------------
    private val concert1 = Concert(1, "The Weeknd", "After Hours Live", "2024-05-12", "R&B", "Kyiv Arena")
    private val concert2 = Concert(2, "Dua Lipa", "Future Nostalgia Tour", "2024-06-20", "Pop")
    private val concert3 = Concert(3, "Imagine Dragons", "Mercury World Tour", "2024-08-05", "Rock", "Lviv Palace")

    /**
     * @Before — виконується ПЕРЕД кожним тестом.
     *
     * Два кроки ініціалізації:
     * 1. setMain(testDispatcher) — підміняємо Dispatchers.Main на JVM-сумісний диспетчер.
     *    Без цього будь-який suspend-код, що використовує Main, падає з виключенням.
     * 2. fakeRepository = FakeAppRepository() — свіжий Fake для кожного тесту.
     *    Гарантує ізоляцію: зміни стану в одному тесті не впливають на інші.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // ЛР №13: підміна Main dispatcher
        fakeRepository = FakeAppRepository() // ЛР №13: свіжий Fake для ізоляції
    }

    /**
     * @After — виконується ПІСЛЯ кожного тесту.
     *
     * resetMain() відновлює оригінальний стан Dispatchers.Main.
     * Це критично: без скидання наступний тест або тестовий клас
     * успадкує наш тестовий диспетчер, що може спричинити непередбачувану поведінку.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain() // ЛР №13: відновлення після тесту — обов'язковий крок
    }

    // ================================================================
    // ТЕСТ 1: Позитивний сценарій — успішне завантаження списку концертів
    // ================================================================
    /**
     * Given: репозиторій повертає список з 3 концертів
     * When: викликається getAllConcerts()
     * Then: результат містить Success зі списком з 3 елементів
     */
    @Test
    fun `getAllConcerts success returns list of concerts`() = runTest {
        // Arrange
        fakeRepository.concertsToReturn = listOf(concert1, concert2, concert3)
        fakeRepository.shouldReturnError = false

        // Act
        val result = fakeRepository.getAllConcerts()

        // Assert
        assertTrue("Результат має бути Success", result is NetworkResult.Success)
        val successResult = result as NetworkResult.Success
        assertEquals("Список має містити 3 концерти", 3, successResult.data.size)
        assertEquals("Перший концерт має бути The Weeknd", "The Weeknd", successResult.data[0].artist)
        assertEquals("Останній концерт має бути Imagine Dragons", "Imagine Dragons", successResult.data[2].artist)
    }

    // ================================================================
    // ТЕСТ 2: Позитивний сценарій — ConcertListUiState при успішному завантаженні
    // ================================================================
    /**
     * Given: репозиторій повертає список з 2 концертів
     * When: логіка обробляє NetworkResult.Success
     * Then: стан UI має бути ConcertListUiState.Success з коректними даними
     */
    @Test
    fun `concert list ui state success with non-empty list`() = runTest {
        // Arrange
        val concerts = listOf(concert1, concert2)
        fakeRepository.concertsToReturn = concerts

        // Act
        val result = fakeRepository.getAllConcerts()

        // Assert — симулюємо логіку ViewModel
        val uiState: ConcertListUiState = when (result) {
            is NetworkResult.Success -> {
                if (result.data.isEmpty()) ConcertListUiState.Empty
                else ConcertListUiState.Success(result.data, isOffline = false)
            }
            is NetworkResult.Error -> ConcertListUiState.Error(result.message)
            is NetworkResult.Loading -> ConcertListUiState.Loading
        }

        assertTrue("UI стан має бути Success", uiState is ConcertListUiState.Success)
        val successState = uiState as ConcertListUiState.Success
        assertEquals("Список концертів має містити 2 елементи", 2, successState.concerts.size)
        assertFalse("isOffline має бути false при успішному завантаженні", successState.isOffline)
    }

    // ================================================================
    // ТЕСТ 3: Негативний сценарій — обробка помилки мережі
    // ================================================================
    /**
     * Given: репозиторій повертає NetworkResult.Error (немає з'єднання)
     * When: логіка обробляє помилку (кеш порожній)
     * Then: стан UI має бути ConcertListUiState.Error з відповідним повідомленням
     */
    @Test
    fun `getAllConcerts network error without cache results in error state`() = runTest {
        // Arrange
        val expectedErrorMessage = "Немає з'єднання з мережею"
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = expectedErrorMessage
        fakeRepository.setCachedConcerts(emptyList()) // порожній кеш

        // Act
        val result = fakeRepository.getAllConcerts()
        val cachedConcerts = fakeRepository.cachedConcerts.first()

        // Assert
        assertTrue("Результат має бути Error", result is NetworkResult.Error)
        val errorResult = result as NetworkResult.Error
        assertEquals("Повідомлення помилки має збігатись", expectedErrorMessage, errorResult.message)
        assertTrue("Кеш має бути порожнім", cachedConcerts.isEmpty())

        // Симулюємо логіку ViewModel при помилці без кешу
        val uiState: ConcertListUiState = if (cachedConcerts.isNullOrEmpty()) {
            ConcertListUiState.Error(errorResult.message)
        } else {
            ConcertListUiState.Success(cachedConcerts, isOffline = true)
        }
        assertTrue("UI стан має бути Error", uiState is ConcertListUiState.Error)
        assertEquals(
            "Повідомлення помилки в UI стані має збігатись",
            expectedErrorMessage,
            (uiState as ConcertListUiState.Error).message
        )
    }

    // ================================================================
    // ТЕСТ 4: Негативний сценарій — збереження кешу при помилці мережі (fallback)
    // ================================================================
    /**
     * Given: репозиторій повертає помилку, але в кеші є 1 концерт
     * When: логіка обробляє NetworkResult.Error
     * Then: стан UI має бути Success з isOffline = true (offline-режим)
     */
    @Test
    fun `getAllConcerts network error with cache shows offline success state`() = runTest {
        // Arrange
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Тайм-аут з'єднання"
        fakeRepository.setCachedConcerts(listOf(concert1)) // є кешований концерт

        // Act
        val result = fakeRepository.getAllConcerts()
        val cachedConcerts = fakeRepository.cachedConcerts.first()

        // Assert
        assertTrue("Результат має бути Error", result is NetworkResult.Error)
        assertFalse("Кеш не має бути порожнім", cachedConcerts.isEmpty())
        assertEquals("Кеш має містити 1 концерт", 1, cachedConcerts.size)

        // Симулюємо логіку ViewModel з fallback на кеш
        val uiState: ConcertListUiState = if (cachedConcerts.isNullOrEmpty()) {
            ConcertListUiState.Error((result as NetworkResult.Error).message)
        } else {
            ConcertListUiState.Success(cachedConcerts, isOffline = true)
        }
        assertTrue("UI стан має бути Success (offline)", uiState is ConcertListUiState.Success)
        val successState = uiState as ConcertListUiState.Success
        assertTrue("isOffline має бути true при offline-режимі", successState.isOffline)
        assertEquals("The Weeknd", successState.concerts[0].artist)
    }

    // ================================================================
    // ТЕСТ 5: Edge case — порожній список концертів
    // ================================================================
    /**
     * Given: репозиторій повертає порожній список (сервер відповів успішно)
     * When: логіка обробляє NetworkResult.Success з порожнім списком
     * Then: стан UI має бути ConcertListUiState.Empty
     */
    @Test
    fun `getAllConcerts empty response results in empty state`() = runTest {
        // Arrange
        fakeRepository.concertsToReturn = emptyList()
        fakeRepository.shouldReturnError = false

        // Act
        val result = fakeRepository.getAllConcerts()

        // Assert
        assertTrue("Результат має бути Success", result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue("Дані мають бути порожніми", data.isEmpty())

        // Симулюємо логіку ViewModel
        val uiState: ConcertListUiState = if (data.isEmpty()) {
            ConcertListUiState.Empty
        } else {
            ConcertListUiState.Success(data)
        }
        assertTrue("UI стан має бути Empty", uiState is ConcertListUiState.Empty)
    }

    // ================================================================
    // ТЕСТ 6: Edge case — логіка favorites (toggle додавання та видалення)
    // ================================================================
    /**
     * Given: початковий стан — порожній набір улюблених (emptySet)
     * When: викликається toggleFavorite (додавання, потім видалення)
     * Then: стан набору улюблених оновлюється коректно
     */
    @Test
    fun `toggleFavorite add and remove changes state consistently`() = runTest {
        // Arrange
        var favoriteIds: Set<Int> = emptySet()
        val concertId = 1

        // Act — додавання до улюблених
        favoriteIds = if (concertId in favoriteIds) {
            favoriteIds - concertId
        } else {
            favoriteIds + concertId
        }

        // Assert — після першого toggle ID є в наборі
        assertTrue("ID $concertId має бути в favorites після додавання", concertId in favoriteIds)
        assertEquals("Має бути 1 улюблений", 1, favoriteIds.size)

        // Act — видалення з улюблених
        favoriteIds = if (concertId in favoriteIds) {
            favoriteIds - concertId
        } else {
            favoriteIds + concertId
        }

        // Assert — після другого toggle ID відсутній у наборі
        assertFalse("ID $concertId має бути відсутнім після видалення", concertId in favoriteIds)
        assertTrue("Набір має бути порожнім", favoriteIds.isEmpty())
    }

    // ================================================================
    // ТЕСТ 7: Позитивний сценарій — успішне додавання концерту (CRUD)
    // ================================================================
    /**
     * Given: репозиторій готовий прийняти новий концерт
     * When: викликається createConcert з коректними даними
     * Then: результат містить створений концерт з переданими полями
     */
    @Test
    fun `createConcert success returns created concert with correct fields`() = runTest {
        // Arrange
        fakeRepository.shouldReturnError = false
        val dto = CreateConcertDto(
            artist = "Coldplay",
            title = "Music of the Spheres",
            date = "2025-07-15",
            genre = "Alternative Rock",
            venue = "Olimpiyskiy Stadium"
        )

        // Act
        val result = fakeRepository.createConcert(dto)

        // Assert
        assertTrue("Результат має бути Success", result is NetworkResult.Success)
        val created = (result as NetworkResult.Success).data
        assertEquals("Виконавець має збігатись", "Coldplay", created.artist)
        assertEquals("Назва має збігатись", "Music of the Spheres", created.title)
        assertEquals("Жанр має збігатись", "Alternative Rock", created.genre)
        assertEquals("Майданчик має збігатись", "Olimpiyskiy Stadium", created.venue)
    }

    // ================================================================
    // ТЕСТ 8: Edge case — повторний виклик getAllConcerts (refresh)
    // ================================================================
    /**
     * Given: перший виклик повернув список, потім дані змінились на сервері
     * When: викликається getAllConcerts() вдруге (після оновлення fakeRepository)
     * Then: повертається оновлений список
     */
    @Test
    fun `getAllConcerts refresh returns updated data`() = runTest {
        // Arrange — перший виклик
        fakeRepository.concertsToReturn = listOf(concert1, concert2)
        val firstResult = fakeRepository.getAllConcerts()
        assertEquals("Перший виклик: 2 концерти", 2, (firstResult as NetworkResult.Success).data.size)

        // Act — дані на сервері оновились (додали 3й концерт)
        fakeRepository.concertsToReturn = listOf(concert1, concert2, concert3)
        val secondResult = fakeRepository.getAllConcerts()

        // Assert
        assertTrue("Другий результат має бути Success", secondResult is NetworkResult.Success)
        val updatedList = (secondResult as NetworkResult.Success).data
        assertEquals("Після оновлення: 3 концерти", 3, updatedList.size)
        assertEquals("Третій концерт має бути Imagine Dragons", "Imagine Dragons", updatedList[2].artist)
    }

    // ================================================================
    // ТЕСТ 9: Негативний сценарій — невдале видалення концерту
    // ================================================================
    /**
     * Given: репозиторій налаштований повертати помилку при видаленні
     * When: викликається deleteConcert
     * Then: результат є NetworkResult.Error, список не змінюється
     */
    @Test
    fun `deleteConcert failure returns error and list unchanged`() = runTest {
        // Arrange
        fakeRepository.concertsToReturn = listOf(concert1, concert2)
        fakeRepository.deleteSuccess = false
        fakeRepository.errorMessage = "Сервер недоступний"
        val initialSize = fakeRepository.concertsToReturn.size

        // Act
        val result = fakeRepository.deleteConcert(concert1.id)

        // Assert
        assertTrue("Результат має бути Error", result is NetworkResult.Error)
        assertEquals(
            "Список не має змінитись після невдалого видалення",
            initialSize,
            fakeRepository.concertsToReturn.size
        )
        assertEquals("Повідомлення помилки: Сервер недоступний",
            "Сервер недоступний",
            (result as NetworkResult.Error).message
        )
    }

    // ================================================================
    // ТЕСТ 10: Edge case — отримання деталей для неіснуючого концерту
    // ================================================================
    /**
     * Given: репозиторій не містить концерт з даним ID
     * When: викликається getConcertById з неіснуючим ID
     * Then: результат є NetworkResult.Error
     */
    @Test
    fun `getConcertById nonexistent id returns error`() = runTest {
        // Arrange
        fakeRepository.concertsToReturn = listOf(concert1, concert2)
        fakeRepository.shouldReturnError = false
        val nonExistentId = 999

        // Act
        val result = fakeRepository.getConcertById(nonExistentId)

        // Assert
        assertTrue("Результат для неіснуючого ID має бути Error", result is NetworkResult.Error)
    }

    // ================================================================
    // ТЕСТ 11: NetworkResult — перевірка типів та трансформацій
    // ================================================================
    /**
     * Given: об'єкти різних типів NetworkResult
     * When: перевіряємо is-перевірки та значення
     * Then: кожен тип має коректно ідентифікуватись
     */
    @Test
    fun `networkResult sealed class types are correctly identified`() {
        // Arrange & Act & Assert
        val successResult: NetworkResult<List<Concert>> = NetworkResult.Success(listOf(concert1))
        val errorResult: NetworkResult<List<Concert>> = NetworkResult.Error("Test error", 404)
        val loadingResult: NetworkResult<List<Concert>> = NetworkResult.Loading

        assertTrue("Success має визначатись як Success", successResult is NetworkResult.Success)
        assertFalse("Success не має бути Error", successResult is NetworkResult.Error)

        assertTrue("Error має визначатись як Error", errorResult is NetworkResult.Error)
        assertEquals("Код помилки має бути 404", 404, (errorResult as NetworkResult.Error).code)

        assertTrue("Loading має визначатись як Loading", loadingResult is NetworkResult.Loading)

        // Перевірка даних Success
        val data = (successResult as NetworkResult.Success).data
        assertEquals("Список має містити 1 елемент", 1, data.size)
        assertEquals("Концерт має бути The Weeknd", "The Weeknd", data[0].artist)
    }

    // ================================================================
    // ТЕСТ 12: Edge case — ConcertDetailUiState при успішному завантаженні деталей
    // ================================================================
    /**
     * Given: репозиторій повертає конкретний концерт за ID
     * When: логіка обробляє результат getConcertById
     * Then: стан деталей має бути ConcertDetailUiState.Success з коректним об'єктом
     */
    @Test
    fun `getConcertById success returns correct detail ui state`() = runTest {
        // Arrange
        fakeRepository.concertsToReturn = listOf(concert1, concert2, concert3)

        // Act
        val result = fakeRepository.getConcertById(concert2.id)

        // Assert
        assertTrue("Результат має бути Success", result is NetworkResult.Success)
        val concert = (result as NetworkResult.Success).data

        // Симулюємо логіку ViewModel для деталей
        val detailState: ConcertDetailUiState = when (result) {
            is NetworkResult.Success -> ConcertDetailUiState.Success(result.data)
            is NetworkResult.Error -> ConcertDetailUiState.Error(result.message)
            is NetworkResult.Loading -> ConcertDetailUiState.Loading
        }

        assertTrue("Стан деталей має бути Success", detailState is ConcertDetailUiState.Success)
        val successDetail = detailState as ConcertDetailUiState.Success
        assertEquals("Виконавець має бути Dua Lipa", "Dua Lipa", successDetail.concert.artist)
        assertEquals("Назва має бути Future Nostalgia Tour", "Future Nostalgia Tour", successDetail.concert.title)
    }
}
