package com.example.myapplication7.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Lab8ArchitectureDemoTest {
    @Test
    fun interfaceBasedCoordinatorPreservesCheckoutBehavior() {
        val coordinator = CheckoutCoordinator(
            catalog = FakeCatalog(),
            inventory = FakeInventory(),
            payments = FakePayments(),
            receipts = FakeReceipts()
        )
        val request = CheckoutRequest(itemId = "concert-1", userId = "user-1")

        assertTrue(coordinator.checkout(request))
        println("[PASS] interface-based coordinator preserves checkout behavior")
    }

    @Test
    fun compositionRemovesTheSyntheticInheritanceChainWithoutChangingThePrice() {
        val inheritedPrice = VipPolicy().price(basePrice = 100, region = "north")
        val composedPrice = ComposedPricePolicy(FlatPriceRule()).price(
            basePrice = 100,
            region = "north"
        )

        assertEquals(80, inheritedPrice)
        assertEquals(inheritedPrice, composedPrice)
        println("[BASELINE] synthetic inheritance depth=3; overridden methods=3")
        println("[PASS] composition matches inherited behavior; composed class has no overrides")
    }
}

private data class CheckoutRequest(val itemId: String, val userId: String)

private interface CatalogPort {
    fun priceFor(itemId: String): Int?
}

private interface InventoryPort {
    fun reserve(itemId: String): Boolean
}

private interface PaymentPort {
    fun charge(userId: String, amount: Int): Boolean
}

private interface ReceiptPort {
    fun send(userId: String, amount: Int)
}

private class FakeCatalog : CatalogPort {
    override fun priceFor(itemId: String): Int? = if (itemId == "concert-1") 100 else null
}

private class FakeInventory : InventoryPort {
    override fun reserve(itemId: String): Boolean = itemId == "concert-1"
}

private class FakePayments : PaymentPort {
    override fun charge(userId: String, amount: Int): Boolean = amount > 0
}

private class FakeReceipts : ReceiptPort {
    override fun send(userId: String, amount: Int) = Unit
}

private class CheckoutCoordinator(
    private val catalog: CatalogPort,
    private val inventory: InventoryPort,
    private val payments: PaymentPort,
    private val receipts: ReceiptPort
) {
    fun checkout(request: CheckoutRequest): Boolean {
        val amount = catalog.priceFor(request.itemId) ?: return false
        if (!inventory.reserve(request.itemId)) return false
        if (!payments.charge(request.userId, amount)) return false
        receipts.send(request.userId, amount)
        return true
    }
}

private open class RootPolicy {
    open fun price(basePrice: Int, region: String): Int = basePrice
}

private open class RegionalPolicy : RootPolicy() {
    override fun price(basePrice: Int, region: String): Int =
        super.price(basePrice, region) - if (region == "north") 5 else 0
}

private open class WeekendPolicy : RegionalPolicy() {
    override fun price(basePrice: Int, region: String): Int =
        super.price(basePrice, region) - 5
}

private class VipPolicy : WeekendPolicy() {
    override fun price(basePrice: Int, region: String): Int =
        super.price(basePrice, region) - 10
}

private fun interface PriceRule {
    fun price(basePrice: Int, region: String): Int
}

private class FlatPriceRule : PriceRule {
    override fun price(basePrice: Int, region: String): Int =
        basePrice - if (region == "north") 20 else 15
}

private class ComposedPricePolicy(private val rule: PriceRule) {
    fun price(basePrice: Int, region: String): Int = rule.price(basePrice, region)
}
