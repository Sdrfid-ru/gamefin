package ru.findrug.app.data

import org.json.JSONArray
import org.json.JSONObject
import ru.findrug.domain.*

/**
 * Единственное описание JSON-снимка; version — версия формата, а не APK. Новые необязательные поля
 * читаются с совместимыми значениями по умолчанию. Для несовместимого изменения потребуется явное
 * преобразование формата и проверка сохранений, а не сброс профиля.
 */
internal object ScenarioCodec {
    private fun budget(a: BudgetAmounts) =
        JSONObject().put("need", a.need).put("want", a.want).put("save", a.save)

    private fun parseBudget(j: JSONObject) =
        BudgetAmounts(j.getInt("need"), j.getInt("want"), j.getInt("save"))

    fun encode(s: ScenarioState): JSONObject =
        JSONObject().apply {
            put("version", 2)
            put("planLocked", s.planLocked)
            put("ownedItems", JSONArray(s.ownedItems.toList()))
            put("collectedGoals", JSONObject(s.collectedGoals))
            put("paidRepeats", s.paidRepeats)
            put("taskEarnings", s.taskEarnings)
            put("recoveryEarnings", s.recoveryEarnings)
            s.profile?.let {
                put(
                    "profile",
                    JSONObject()
                        .put("player", it.player)
                        .put("pet", it.pet)
                        .put("hat", it.hat)
                        .put("accessory", it.accessory)
                        .put("color", it.color)
                        .put("clothes", it.clothes),
                )
            }
            put("started", s.started)
            put("coins", s.coins)
            put("savings", s.savings)
            put("goal", s.goalId)
            put("period", s.period)
            s.plan?.let { put("plan", budget(it)) }
            put("actual", budget(s.actual))
            put("satiety", s.satiety)
            put("mood", s.mood)
            put("care", s.care)
            put("energy", s.energy)
            put("tasks", JSONArray(s.completedTasks.toList()))
            put("periodTasks", s.periodTasks)
            put("claims", JSONArray(s.rewardClaims.toList()))
            put("purchases", JSONArray(s.purchases))
            put(
                "history",
                JSONArray().apply {
                    s.history.forEach {
                        put(
                            JSONObject()
                                .put("source", it.source)
                                .put("account", it.account)
                                .put("amount", it.amount)
                                .put("period", it.period)
                        )
                    }
                },
            )
            put(
                "reports",
                JSONArray().apply {
                    s.reports.forEach {
                        put(
                            JSONObject()
                                .put("period", it.period)
                                .put("plan", budget(it.plan))
                                .put("actual", budget(it.actual))
                                .put("successful", it.successful)
                                .put("satiety", it.satiety)
                                .put("care", it.care)
                        )
                    }
                },
            )
            put("closed", s.periodClosed)
            put("sound", s.sound)
            put("petSounds", s.petSounds)
            put("animations", s.animations)
        }

    fun decode(j: JSONObject): ScenarioState {
        require(j.getInt("version") == 2) { "Неподдерживаемый формат сохранения" }
        fun strings(key: String): List<String> =
            j.optJSONArray(key)?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
        val collected = j.optJSONObject("collectedGoals")
        return ScenarioState(
            planLocked = j.getBoolean("planLocked"),
            ownedItems = strings("ownedItems").toSet(),
            collectedGoals =
                collected?.keys()?.asSequence()?.associateWith { collected.getInt(it) }
                    ?: emptyMap(),
            paidRepeats = j.getInt("paidRepeats"),
            // В снимках без отдельного счётчика восстанавливаем заработок текущего периода
            // из истории. Префиксы источников здесь являются частью совместимости формата.
            taskEarnings =
                j.optInt(
                    "taskEarnings",
                    j.getJSONArray("history").let { entries ->
                        (0 until entries.length()).sumOf { i ->
                            val entry = entries.getJSONObject(i)
                            val source = entry.getString("source")
                            if (
                                entry.getInt("period") == j.getInt("period") &&
                                    (source.startsWith("Задание:") ||
                                        source.startsWith("Тренировка:"))
                            )
                                entry.getInt("amount").coerceAtLeast(0)
                            else 0
                        }
                    },
                ),
            recoveryEarnings = j.optInt("recoveryEarnings", 0),
            profile =
                j.optJSONObject("profile")?.let {
                    ScenarioProfile(
                        it.getString("player"),
                        it.getString("pet"),
                        it.optInt("hat"),
                        it.optInt("accessory"),
                        it.optInt("color"),
                        it.optInt("clothes"),
                    )
                },
            started = j.optBoolean("started"),
            coins = j.getInt("coins"),
            savings = j.getInt("savings"),
            goalId = j.optString("goal").takeIf { it.isNotBlank() && it != "null" },
            period = j.getInt("period"),
            plan = j.optJSONObject("plan")?.let(::parseBudget),
            actual = parseBudget(j.getJSONObject("actual")),
            satiety = j.getInt("satiety"),
            mood = j.getInt("mood"),
            care = j.getInt("care"),
            energy = j.getInt("energy"),
            completedTasks = strings("tasks").map(String::toInt).toSet(),
            periodTasks = j.optInt("periodTasks"),
            rewardClaims = strings("claims").toSet(),
            purchases = strings("purchases"),
            history =
                j.getJSONArray("history").let { a ->
                    List(a.length()) {
                        a.getJSONObject(it).let {
                            CoinEntry(
                                it.getString("source"),
                                it.getInt("amount"),
                                it.getInt("period"),
                                it.optString("account", "balance"),
                            )
                        }
                    }
                },
            reports =
                j.getJSONArray("reports").let { a ->
                    List(a.length()) {
                        a.getJSONObject(it).let {
                            PeriodReport(
                                it.getInt("period"),
                                parseBudget(it.getJSONObject("plan")),
                                parseBudget(it.getJSONObject("actual")),
                                it.getBoolean("successful"),
                                if (it.isNull("satiety")) null else it.getInt("satiety"),
                                if (it.isNull("care")) null else it.getInt("care"),
                            )
                        }
                    }
                },
            periodClosed = j.optBoolean("closed"),
            sound = j.optBoolean("sound", true),
            petSounds = j.getBoolean("petSounds"),
            animations = j.optBoolean("animations", true),
        )
    }
}
