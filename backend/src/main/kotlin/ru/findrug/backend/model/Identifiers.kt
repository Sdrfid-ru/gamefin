package ru.findrug.backend.model

import java.util.UUID

@JvmInline
value class GoalId(val value: String) {
    companion object {
        fun new() = GoalId(UUID.randomUUID().toString())
    }
}

@JvmInline value class TaskId(val value: String)

@JvmInline
value class TransactionId(val value: String) {
    companion object {
        fun new() = TransactionId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class IdempotencyKey(val value: String) {
    companion object {
        fun new() = IdempotencyKey(UUID.randomUUID().toString())
    }
}
