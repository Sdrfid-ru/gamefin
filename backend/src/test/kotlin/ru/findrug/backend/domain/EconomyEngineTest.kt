package ru.findrug.backend.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.findrug.backend.model.*

class EconomyEngineTest {
    private val engine = EconomyEngine()

    @Test
    fun `food decision leaves 70 virtual coins`() {
        val result =
            assertIs<EconomyResult.Accepted>(
                engine.spend(Wallet(100), 30, SpendingCategory.NEED, IdempotencyKey.new())
            )
        assertEquals(70, result.wallet.availableCoins)
        assertEquals(-30, result.transaction.deltaAvailable)
    }

    @Test
    fun `cannot buy 80 coin toy after food`() {
        val result = engine.spend(Wallet(70), 80, SpendingCategory.WANT, IdempotencyKey.new())
        assertEquals(EconomyResult.Rejected(EconomyError.INSUFFICIENT_COINS), result)
    }

    @Test
    fun `saving moves coins without changing total`() {
        val result =
            assertIs<EconomyResult.Accepted>(engine.deposit(Wallet(70), 30, IdempotencyKey.new()))
        assertEquals(Wallet(40, 30), result.wallet)
    }
}
