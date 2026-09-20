package com.example.myapplication7.quality

// =============================================================
// ЛР №1 Завдання 4: Архітектурний рефакторинг
//
// РЕФАКТОРИНГ: ConcertValidatorBad + ConcertValidatorDuplicated
// → ConcertValidator (чистий, без дублювання)
//
// Застосовані техніки:
// 1. Guard Clauses (раннє повернення) — замість глибоких if-else
// 2. Extract Method — виділення validateSingleConcert(), checkVenue()
// 3. DRY — єдина точка визначення VALID_GENRES, констант
// 4. Single Responsibility — кожен метод робить одну річ
//
// Результат:
// - Cognitive Complexity головного методу: ~5 (було ~22) ✅
// - Дублювання коду: 0% (видалено ConcertValidatorDuplicated) ✅
// - Technical Debt: суттєво зменшено ✅
// =============================================================

private val VALID_GENRES = listOf("Rock", "Pop", "Jazz", "R&B", "Electronic")
private const val MIN_ARTIST_LENGTH = 2
private const val MIN_TITLE_LENGTH = 3
private const val DATE_FORMAT_LENGTH = 10

/**
 * Валідатор концертів — після рефакторингу.
 *
 * Cognitive Complexity ≤ 15 (відповідає Quality Gate).
 * Код розділено на дрібні методи з єдиною відповідальністю.
 */
class ConcertValidator {

    /**
     * Перевіряє список концертів і повертає список результатів.
     *
     * Когнітивна складність: ~5 (Guard Clauses + делегування у приватні методи).
     *
     * ДО (CC ≈ 22): 8-рівнева вкладеність if-else всередині for-циклу.
     * ПІСЛЯ (CC ≈ 5): плоска структура — один рівень for, делегування у validateSingleConcert().
     */
    fun validateAndProcessConcerts(
        concerts: List<Map<String, String?>>,
        maxCapacity: Int,
        strictMode: Boolean
    ): List<String> {
        // Guard Clause #1: порожній список — одразу повертаємо результат
        if (concerts.isEmpty()) return listOf("EMPTY: no concerts to validate")

        // Плоска структура: for + делегування (CC +1 за for, далі лише in validateSingleConcert)
        return concerts.map { concert ->
            validateSingleConcert(concert, maxCapacity, strictMode)
        }
    }

    /**
     * Validate one concert map entry and return a status string.
     *
     * Extract Method: виділено з монструозного методу.
     * Guard Clauses: кожна перевірка — ранній return при провалі.
     * Cognitive Complexity: ~7
     */
    private fun validateSingleConcert(
        concert: Map<String, String?>,
        maxCapacity: Int,
        strictMode: Boolean
    ): String {
        val artist = concert["artist"]
        val title = concert["title"]
        val date = concert["date"]
        val genre = concert["genre"]
        val venue = concert["venue"]

        // Guard Clause: обов'язкові поля
        if (!isValidArtist(artist)) return "FAIL: missing artist"
        if (!isValidTitle(title) && strictMode) return "FAIL: missing title for $artist"
        if (!isValidDate(date)) return "FAIL: bad date for $artist: $date"

        // Guard Clause: жанр
        if (!isValidGenre(genre)) {
            return if (strictMode) "FAIL: invalid genre '$genre' for $artist"
            else "WARN: unknown genre for $artist"
        }

        // Guard Clause: strictMode — вимагає venue
        if (strictMode) return checkVenue(venue, artist!!, maxCapacity)

        return "VALID: $artist — $title"
    }

    /**
     * Extract Method: перевірка venue в strictMode.
     * Когнітивна складність: 2 (два if).
     */
    private fun checkVenue(venue: String?, artist: String, maxCapacity: Int): String {
        if (venue.isNullOrBlank()) return "FAIL: no venue in strict mode: $artist"
        if (venue.length > maxCapacity / 10) return "WARN: venue too long: $artist"
        return "VALID: $artist"
    }

    // -------------------------------------------------------
    // Валідатори полів — єдина точка визначення правил
    // (DRY: раніше ці перевірки дублювались у ConcertValidatorDuplicated)
    // -------------------------------------------------------

    fun isValidArtist(artist: String?): Boolean {
        if (artist.isNullOrBlank()) return false
        return artist.length >= MIN_ARTIST_LENGTH
    }

    fun isValidTitle(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        return title.length >= MIN_TITLE_LENGTH
    }

    fun isValidDate(date: String?): Boolean {
        if (date.isNullOrBlank()) return false
        if (date.length != DATE_FORMAT_LENGTH) return false
        return date[4] == '-' && date[7] == '-'
    }

    fun isValidGenre(genre: String?): Boolean {
        return genre != null && genre in VALID_GENRES
    }
}

// =============================================================
// Порівняльний аналіз: ДО та ПІСЛЯ рефакторингу
//
// ДО (ConcertValidatorBad.validateAndProcessConcerts):
//   Рівні вкладеності: 8
//   Кількість if:      11
//   Кількість else:    6
//   Cognitive Complexity: ~22   ← FAIL (поріг ≤ 15)
//   Рядків коду:       ~70
//
// ПІСЛЯ (ConcertValidator.validateSingleConcert + checkVenue):
//   Рівні вкладеності: 2
//   Кількість if:      8 (Guard Clauses — по одному рівню)
//   Кількість else:    1
//   Cognitive Complexity: ~5    ← PASS ✅
//   Рядків коду:       ~25 (основна логіка) + допоміжні методи
//
// Техніки рефакторингу:
//   [1] Extract Method    — виділено validateSingleConcert(), checkVenue()
//   [2] Guard Clauses     — раннє return замість вкладених if-else
//   [3] DRY               — видалено ConcertValidatorDuplicated (дублювання ↓)
//   [4] Single Responsibility — кожен метод ≤ 15 рядків корисного коду
// =============================================================
