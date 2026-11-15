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
    fun compositionPreservesTheExpectedPriceWithoutInheritance() {
        val composedPrice = ComposedPricePolicy(FlatPriceRule()).price(
            basePrice = 100,
            region = "north"
        )

        assertEquals(80, composedPrice)
        println("[PASS] composition preserves the expected price with no custom superclass")
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

