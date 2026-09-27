package ru.findrug.backend.domain

import ru.findrug.backend.model.*

sealed interface EconomyResult {
    data class Accepted(val wallet: Wallet, val transaction: LedgerEntry) : EconomyResult

    data class Rejected(val reason: EconomyError) : EconomyResult
}

enum class EconomyError {
    INVALID_AMOUNT,
    INSUFFICIENT_COINS,
    DUPLICATE_COMMAND,
}

data class LedgerEntry(
    val id: TransactionId,
    val idempotencyKey: IdempotencyKey,
    val type: TransactionType,
    val category: SpendingCategory,
    val deltaAvailable: Int,
    val deltaReserved: Int,
)

/** Pure policy: persistence must make [idempotencyKey] unique per user. */
class EconomyEngine {
    fun spend(
        wallet: Wallet,
        amount: Int,
        category: SpendingCategory,
        key: IdempotencyKey,
    ): EconomyResult {
        if (amount <= 0) return EconomyResult.Rejected(EconomyError.INVALID_AMOUNT)
        if (wallet.availableCoins < amount)
            return EconomyResult.Rejected(EconomyError.INSUFFICIENT_COINS)
        return EconomyResult.Accepted(
            wallet.copy(availableCoins = wallet.availableCoins - amount),
            LedgerEntry(TransactionId.new(), key, TransactionType.PURCHASE, category, -amount, 0),
        )
    }

    fun deposit(wallet: Wallet, amount: Int, key: IdempotencyKey): EconomyResult {
        if (amount <= 0) return EconomyResult.Rejected(EconomyError.INVALID_AMOUNT)
        if (wallet.availableCoins < amount)
            return EconomyResult.Rejected(EconomyError.INSUFFICIENT_COINS)
        return EconomyResult.Accepted(
            Wallet(wallet.availableCoins - amount, wallet.reservedCoins + amount),
            LedgerEntry(
                TransactionId.new(),
                key,
                TransactionType.SAVINGS_DEPOSIT,
                SpendingCategory.SAVE,
                -amount,
                amount,
            ),
        )
    }
}
