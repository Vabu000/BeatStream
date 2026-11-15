package com.example.myapplication7.data

// =============================================================
// ЛР №6 — Завдання 2: PASS-стан
//
// COMMIT: refactor(audit): eliminate conditional branches using lookup table, PASS
//
// Data-Driven рефакторинг: усі if/else/when розгалуження
// замінено статичною Map<TariffKey, Double>.
// Lizard CCN = 2 для calculateConcertPriceRefactored() ✅
//
// Техніка: Lookup Table (Data-Driven Programming)
// До: CC = 18 (FAIL — Lizard WARNING)
// Після: CC = 2  (PASS — нижче порогу 10)
// =============================================================

/** Складений ключ тарифу: genre|tier|dayType|holiday|vip */
private data class TariffKey(
    val genre: String,
    val tier: String,
    val dayType: String,   // "weekend" | "weekday"
    val holiday: Boolean,
    val vip: Boolean
)

data class ConcertTariffRequest(
    val genre: String,
    val tier: String,
    val dayOfWeek: String,
    val isHoliday: Boolean,
    val hasVip: Boolean
)

/**
 * Статична таблиця тарифів — єдиний контрольний рядок для всіх цін.
 * Нові тарифи додаються як рядки таблиці, а не нові if-гілки.
 * Нерелевантні для кожного тарифу виміри нормалізуються перед пошуком.
 */
private val TARIFF_TABLE: Map<TariffKey, Double> = mapOf(
    TariffKey("Rock",  "premium",  "weekend", true,  false) to 350.0,
    TariffKey("Rock",  "premium",  "weekend", false, false) to 300.0,
    TariffKey("Rock",  "premium",  "weekday", false, true)  to 280.0,
    TariffKey("Rock",  "premium",  "weekday", false, false) to 250.0,
    TariffKey("Rock",  "standard", "sunday",  false, false) to 180.0,
    TariffKey("Rock",  "standard", "other",   false, false) to 150.0,
    TariffKey("Rock",  "other",    "any",     false, false) to 100.0,
    TariffKey("Pop",   "premium",  "any",     true,  false) to 320.0,
    TariffKey("Pop",   "premium",  "any",     false, false) to 270.0,
    TariffKey("Pop",   "other",    "any",     false, true)  to 160.0,
    TariffKey("Pop",   "other",    "any",     false, false) to 130.0,
    TariffKey("Jazz",  "other",    "weekend", false, true)  to 220.0,
    TariffKey("Jazz",  "other",    "weekend", false, false) to 190.0,
    TariffKey("Jazz",  "other",    "weekday", true,  false) to 170.0,
    TariffKey("Jazz",  "other",    "weekday", false, false) to 140.0,
)

private val DAY_TYPE_OVERRIDES: Map<Triple<String, String, String>, String> = mapOf(
    Triple("Rock", "premium", "Friday") to "weekend",
    Triple("Rock", "premium", "Saturday") to "weekend",
    Triple("Rock", "standard", "Sunday") to "sunday",
    Triple("Jazz", "other", "Friday") to "weekend",
    Triple("Jazz", "other", "Saturday") to "weekend"
)

private val DEFAULT_DAY_TYPES: Map<Pair<String, String>, String> = mapOf(
    ("Rock" to "premium") to "weekday",
    ("Rock" to "standard") to "other",
    ("Jazz" to "other") to "weekday"
)

private val TIER_OVERRIDES: Map<Pair<String, String>, String> = mapOf(
    ("Rock" to "premium") to "premium",
    ("Rock" to "standard") to "standard",
    ("Pop" to "premium") to "premium"
)

private val HOLIDAY_AWARE_TARIFFS = setOf(
    Triple("Rock", "premium", "weekend"),
    Triple("Pop", "premium", "any"),
    Triple("Jazz", "other", "weekday")
)

private val VIP_AWARE_TARIFFS = setOf(
    Triple("Rock", "premium", "weekday"),
    Triple("Pop", "other", "any"),
    Triple("Jazz", "other", "weekend")
)

private fun normalizedDayType(genre: String, tier: String, dayOfWeek: String): String =
    DAY_TYPE_OVERRIDES[Triple(genre, tier, dayOfWeek)]
        ?: DEFAULT_DAY_TYPES[genre to tier]
        ?: "any"

private fun String.toNormalizedTier(genre: String): String =
    TIER_OVERRIDES[genre to this] ?: "other"

private fun normalizedHoliday(
    genre: String,
    tier: String,
    dayType: String,
    isHoliday: Boolean
): Boolean =
    isHoliday && Triple(genre, tier, dayType) in HOLIDAY_AWARE_TARIFFS

private fun normalizedVip(
    genre: String,
    tier: String,
    dayType: String,
    hasVip: Boolean
): Boolean =
    hasVip && Triple(genre, tier, dayType) in VIP_AWARE_TARIFFS

private fun toTariffKey(request: ConcertTariffRequest): TariffKey {
    val normalizedTier = request.tier.toNormalizedTier(request.genre)
    val dayType = normalizedDayType(request.genre, normalizedTier, request.dayOfWeek)
    return TariffKey(
        request.genre,
        normalizedTier,
        dayType,
        normalizedHoliday(request.genre, normalizedTier, dayType, request.isHoliday),
        normalizedVip(request.genre, normalizedTier, dayType, request.hasVip)
    )
}

/**
 * Розрахунок ціни квитка через Lookup Table.
 *
 * Lizard CC = 2: лише підготовка ключа й пошук у незмінній таблиці даних.
 * Розширення тарифів: додати рядок до TARIFF_TABLE, не модифікуючи логіку.
 */
fun calculateConcertPriceRefactored(request: ConcertTariffRequest): Double =
    TARIFF_TABLE[toTariffKey(request)] ?: 80.0

fun calculateConcertPriceRefactored(
    genre: String,
    tier: String,
    dayOfWeek: String,
    isHoliday: Boolean,
    hasVip: Boolean
): Double = calculateConcertPriceRefactored(
    ConcertTariffRequest(genre, tier, dayOfWeek, isHoliday, hasVip)
)
