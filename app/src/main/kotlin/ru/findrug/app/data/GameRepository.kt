package ru.findrug.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.findrug.domain.ScenarioState

/**
 * Граница хранения для ViewModel. Успешный возврат означает завершённую запись; ошибка должна дойти
 * до вызывающего кода, чтобы он не показал несохранённый прогресс.
 */
internal interface GameRepository {
    suspend fun read(): ScenarioState

    suspend fun save(state: ScenarioState)

    suspend fun enterDemo(): ScenarioState

    suspend fun exitDemo(): ScenarioState

    suspend fun resetDemo(): ScenarioState

    suspend fun deleteAll(): ScenarioState
}

/** Синхронные операции SharedPreferences выполняются вне главного потока. */
internal class LocalGameRepository(
    private val store: ScenarioStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : GameRepository {
    override suspend fun read() = withContext(io) { store.read() }

    override suspend fun save(state: ScenarioState) = withContext(io) { store.save(state) }

    override suspend fun enterDemo() = withContext(io) { store.enterDemo() }

    override suspend fun exitDemo() = withContext(io) { store.exitDemo() }

    override suspend fun resetDemo() = withContext(io) { store.resetDemo() }

    override suspend fun deleteAll() = withContext(io) { store.deleteAll() }
}
