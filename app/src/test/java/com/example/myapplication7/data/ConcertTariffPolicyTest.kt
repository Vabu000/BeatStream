package com.example.myapplication7.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConcertTariffPolicyTest {
    @Test
    fun mutableAmountCanBeOverwrittenWhileImmutableQuotePreservesTheBasePrice() {
        val expectedTotal = 210.0
        var mutableAmount = 250.0
        mutableAmount = 40.0

        assertEquals(40.0, mutableAmount, 0.0)
        val quote = PriceQuote(basePrice = 250.0, discount = 40.0)
        assertEquals(expectedTotal, quote.total, 0.0)

        println("[FAIL simulation] expected=$expectedTotal actual=$mutableAmount")
        println("[PASS] immutable value object total=${quote.total}")
    }

    @Test
    fun lookupTablePreservesTariffRulesAcrossRelevantInputs() {
        val cases = listOf(
            Case("Rock", "premium", "Friday", true, true, 350.0),
            Case("Rock", "premium", "Saturday", false, true, 300.0),
            Case("Rock", "premium", "Sunday", true, true, 280.0),
            Case("Rock", "premium", "Sunday", false, false, 250.0),
            Case("Rock", "standard", "Sunday", true, true, 180.0),
            Case("Rock", "standard", "Saturday", false, false, 150.0),
            Case("Pop", "premium", "Saturday", true, true, 320.0),
            Case("Pop", "premium", "Sunday", false, true, 270.0),
            Case("Pop", "other", "Sunday", true, true, 160.0),
            Case("Pop", "other", "Sunday", false, false, 130.0),
            Case("Jazz", "other", "Saturday", true, true, 220.0),
            Case("Jazz", "other", "Friday", false, false, 190.0),
            Case("Jazz", "other", "Sunday", true, true, 170.0),
            Case("Jazz", "other", "Sunday", false, true, 140.0),
            Case("Jazz", "premium", "Saturday", true, true, 220.0),
            Case("Jazz", "standard", "Sunday", true, true, 170.0),
            Case("Classical", "premium", "Saturday", true, true, 80.0)
        )

        cases.forEach { case ->
            val request = ConcertTariffRequest(
                genre = case.genre,
                tier = case.tier,
                dayOfWeek = case.dayOfWeek,
                isHoliday = case.isHoliday,
                hasVip = case.hasVip
            )
            assertEquals(
                "Unexpected price for $case",
                case.expected,
                calculateConcertPriceRefactored(
                    request
                ),
                0.0
            )
            assertEquals(
                "The compatibility overload changed behavior for $case",
                case.expected,
                calculateConcertPriceRefactored(
                    case.genre,
                    case.tier,
                    case.dayOfWeek,
                    case.isHoliday,
                    case.hasVip
                ),
                0.0
            )
        }
    }

    private data class Case(
        val genre: String,
        val tier: String,
        val dayOfWeek: String,
        val isHoliday: Boolean,
        val hasVip: Boolean,
        val expected: Double
    )

    private data class PriceQuote(
        val basePrice: Double,
        val discount: Double
    ) {
        val total: Double
            get() = basePrice - discount
    }
}
