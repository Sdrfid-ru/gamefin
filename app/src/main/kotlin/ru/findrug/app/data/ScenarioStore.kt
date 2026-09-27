package ru.findrug.app.data

import android.content.Context
import org.json.JSONObject
import ru.findrug.domain.*

/**
 * Обычная игра и демо хранятся в разных JSON-записях. Имя findrug_scenario_v3 — адрес текущего
 * хранилища, а не версия JSON: его переименование без переноса данных скроет прогресс игрока.
 */
class ScenarioStore(context: Context, storageName: String = "findrug_scenario_v3") {
    private val prefs = context.getSharedPreferences(storageName, Context.MODE_PRIVATE)

    fun read(): ScenarioState = readSlot(prefs.getBoolean("demo_active", false))

    private fun readSlot(demo: Boolean): ScenarioState {
        val raw =
            prefs.getString(if (demo) "demo" else "player", null)
                ?: return ScenarioState(demo = demo)
        return ScenarioCodec.decode(JSONObject(raw)).copy(demo = demo)
    }

    fun save(state: ScenarioState) {
        // Снимок и активный слот записываются одним редактором. Нужен commit(), а не apply():
        // репозиторий должен узнать о сбое до публикации нового баланса в интерфейсе.
        check(
            prefs
                .edit()
                .putString(
                    if (state.demo) "demo" else "player",
                    ScenarioCodec.encode(state).toString(),
                )
                .putBoolean("demo_active", state.demo)
                .commit()
        ) {
            "Не удалось сохранить игру"
        }
    }

    fun enterDemo(): ScenarioState {
        val saved = readSlot(true)
        return (if (saved.started) saved else freshDemo()).also(::save)
    }

    fun exitDemo(): ScenarioState = readSlot(false).also(::save)

    fun resetDemo(): ScenarioState = freshDemo().also(::save)

    fun deleteAll(): ScenarioState {
        check(prefs.edit().clear().commit())
        return ScenarioState()
    }

    private fun freshDemo() =
        ScenarioGame.start(
            ScenarioState(profile = ScenarioProfile("Исследователь", "Рыжик"), demo = true)
        )
}
