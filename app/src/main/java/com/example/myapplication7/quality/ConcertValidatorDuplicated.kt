package com.example.myapplication7.quality

// =============================================================
// ЛР №1 Завдання 3: ПОРУШЕННЯ 1 — Дублювання коду
//
// Цей файл є НАВМИСНИМ дублікатом логіки з ConcertValidatorBad.kt
// та частини ValidationUtils.
//
// Мета: Підняти Duplicated Lines % вище порогу 3%
// щоб спрацював Quality Gate FAIL для критерію "Overall Code".
//
// УВАГА: Цей файл НАВМИСНО скопійований — він буде
// видалений при рефакторингу (Завд.4 → PASS).
// =============================================================

private val VALID_GENRES_COPY = listOf("Rock", "Pop", "Jazz", "R&B", "Electronic")
private const val MIN_ARTIST_LENGTH = 2
private const val MIN_TITLE_LENGTH = 3
private const val DATE_FORMAT_LENGTH = 10

/**
 * ДУБЛІКАТ: Клас-валідатор з дубльованою логікою.
 * Cognitive Complexity цього методу теж висока (≈ 22).
 *
 * Проблеми:
 * 1. Логіка validateAndProcessConcerts повністю повторює ConcertValidatorBad
 * 2. Константи VALID_GENRES, форматування дати — дублюються
 * 3. Відсутній єдиний метод-обгортка (порушення DRY)
 */
class ConcertValidatorDuplicated {

    // ==========================================================
    // ДУБЛІКАТ методу validateAndProcessConcerts з ConcertValidatorBad
    // Дублювання коду навмисне — для демонстрації FAIL Quality Gate
    // ==========================================================
    fun validateAndProcessConcerts(
        concerts: List<Map<String, String?>>,
        maxCapacity: Int,
        strictMode: Boolean
    ): List<String> {
        val results = mutableListOf<String>()

        if (concerts.isNotEmpty()) {
            for (concert in concerts) {
                val artist = concert["artist"]
                val title = concert["title"]
                val date = concert["date"]
                val genre = concert["genre"]
                val venue = concert["venue"]

                if (artist != null && artist.isNotBlank()) {
                    if (title != null && title.isNotBlank()) {
                        if (date != null && date.isNotBlank()) {
                            if (date.length == DATE_FORMAT_LENGTH && date[4] == '-' && date[7] == '-') {
                                if (genre != null) {
                                    if (genre in VALID_GENRES_COPY) {
                                        if (strictMode) {
                                            if (venue != null && venue.isNotBlank()) {
                                                if (venue.length <= maxCapacity / 10) {
                                                    results.add("VALID: $artist — $title")
                                                } else {
                                                    results.add("WARN: venue too long: $artist")
                                                }
                                            } else {
                                                results.add("FAIL: no venue in strict mode: $artist")
                                            }
                                        } else {
                                            results.add("VALID: $artist — $title")
                                        }
                                    } else if (strictMode) {
                                        results.add("FAIL: invalid genre '$genre' for $artist")
                                    } else {
                                        results.add("WARN: unknown genre for $artist")
                                    }
                                } else {
                                    if (strictMode) {
                                        results.add("FAIL: no genre for $artist")
                                    }
                                }
                            } else {
                                results.add("FAIL: bad date format for $artist: $date")
                            }
                        } else {
                            results.add("FAIL: missing date for $artist")
                        }
                    } else if (strictMode) {
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
    }

    // ==========================================================
    // ДУБЛІКАТ простих валідацій — теж є в ConcertValidatorBad
    // ==========================================================
    fun isValidArtist(artist: String?): Boolean {
        if (artist == null) return false
        if (artist.isBlank()) return false
        if (artist.length < MIN_ARTIST_LENGTH) return false
        return true
    }

    fun isValidTitle(title: String?): Boolean {
        if (title == null) return false
        if (title.isBlank()) return false
        if (title.length < MIN_TITLE_LENGTH) return false
        return true
    }

    fun isValidDate(date: String?): Boolean {
        if (date == null) return false
        if (date.isBlank()) return false
        if (date.length != DATE_FORMAT_LENGTH) return false
        if (date[4] != '-') return false
        if (date[7] != '-') return false
        return true
    }

    fun isValidGenre(genre: String?): Boolean {
        if (genre == null) return false
        return genre in VALID_GENRES_COPY
    }
}
