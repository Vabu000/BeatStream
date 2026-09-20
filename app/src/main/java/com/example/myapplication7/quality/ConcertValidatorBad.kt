package com.example.myapplication7.quality

// =============================================================
// ЛР №1 Завдання 3: Симуляція деградації якості коду
//
// ПОРУШЕННЯ 2: Метод з критично високою когнітивною складністю
// Cognitive Complexity >> 15 (FAIL критерій Quality Gate)
//
// УВАГА: Цей файл НАВМИСНО містить антипатерни для демонстрації
// тригеру Quality Gate. Він буде видалений/рефакторований у Завд.4
// =============================================================

/**
 * АНТИПАТЕРН: Клас-валідатор з монструозним методом.
 * Cognitive Complexity цього методу ≈ 22 (FAIL при порозі ≤ 15).
 *
 * Проблеми:
 * 1. Глибока вкладеність (if всередині if всередині for)
 * 2. Складні булеві умови з &&, ||
 * 3. Повторна логіка перевірки (дублювання)
 * 4. Відсутність Guard Clauses
 */
class ConcertValidatorBad {

    // ==========================================================
    // МЕТОД З КРИТИЧНО ВИСОКОЮ КОГНІТИВНОЮ СКЛАДНІСТЮ (CC ≈ 22)
    // Це НАВМИСНЕ ПОРУШЕННЯ для демонстрації FAIL у SonarCloud
    // ==========================================================
    fun validateAndProcessConcerts(
        concerts: List<Map<String, String?>>,
        maxCapacity: Int,
        strictMode: Boolean
    ): List<String> {
        val results = mutableListOf<String>()                           // +0

        // Перевірка 1: чи є концерти взагалі
        if (concerts.isNotEmpty()) {                                   // +1 (if)
            for (concert in concerts) {                                // +2 (for, nested)
                val artist = concert["artist"]
                val title = concert["title"]
                val date = concert["date"]
                val genre = concert["genre"]
                val venue = concert["venue"]

                // Перевірка обов'язкових полів
                if (artist != null && artist.isNotBlank()) {           // +3 (if, nested + &&)
                    if (title != null && title.isNotBlank()) {         // +4 (if, nested + &&)
                        if (date != null && date.isNotBlank()) {       // +5 (if, nested + &&)
                            // Перевірка формату дати
                            if (date.length == 10 && date[4] == '-' && date[7] == '-') { // +6 + &&
                                if (genre != null) {                   // +7 (if, nested)
                                    val validGenres = listOf("Rock", "Pop", "Jazz", "R&B", "Electronic")
                                    if (genre in validGenres) {        // +8 (if, nested)
                                        if (strictMode) {              // +9 (if, nested)
                                            if (venue != null && venue.isNotBlank()) { // +10 + &&
                                                if (venue.length <= maxCapacity / 10) { // +11
                                                    results.add("VALID: $artist — $title")
                                                } else {
                                                    results.add("WARN: venue too long: $artist")
                                                }
                                            } else {
                                                results.add("FAIL: no venue in strict mode: $artist")
                                            }
                                        } else {                       // +1 (else)
                                            results.add("VALID: $artist — $title")
                                        }
                                    } else if (strictMode) {           // +1 (else if)
                                        results.add("FAIL: invalid genre '$genre' for $artist")
                                    } else {
                                        results.add("WARN: unknown genre for $artist")
                                    }
                                } else {
                                    if (strictMode) {                  // +1 (if, nested)
                                        results.add("FAIL: no genre for $artist")
                                    }
                                }
                            } else {
                                results.add("FAIL: bad date format for $artist: $date")
                            }
                        } else {
                            results.add("FAIL: missing date for $artist")
                        }
                    } else if (strictMode) {                           // +1 (else if)
                        results.add("FAIL: missing title for $artist")
                    }
                } else {
                    results.add("FAIL: missing artist at index ${concerts.indexOf(concert)}")
                }
            }
        } else {
            results.add("EMPTY: no concerts to validate")
        }

        return results
        // Загальна когнітивна складність: приблизно 22 (FAIL при порозі ≤ 15)
    }
}
