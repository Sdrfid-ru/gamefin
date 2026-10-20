package ru.findrug.app.data

import org.json.JSONArray
import org.json.JSONObject
import ru.findrug.domain.UnexpectedExpense
import ru.findrug.domain.UnexpectedExpenseStatus
import ru.findrug.domain.UnexpectedExpenseWithdrawal

internal object UnexpectedExpenseCodec {
    fun encode(event: UnexpectedExpense): JSONObject =
        JSONObject()
            .put("openedPeriod", event.openedPeriod)
            .put("status", event.status.name)
            .put("paidPeriod", event.paidPeriod)
            .put("balanceBeforePayment", event.balanceBeforePayment)
            .put(
                "withdrawals",
                JSONArray().apply {
                    event.withdrawals.forEach { entry ->
                        put(
                            JSONObject()
                                .put("amount", entry.amount)
                                .put("period", entry.period)
                                .put("revision", entry.revision)
                                .put("goalId", entry.goalId)
                                .put("goalTitle", entry.goalTitle)
                                .put("goalPrice", entry.goalPrice)
                                .put("coinsBefore", entry.coinsBefore)
                                .put("savingsBefore", entry.savingsBefore)
                        )
                    }
                },
            )

    fun decode(json: JSONObject): UnexpectedExpense =
        UnexpectedExpense(
            openedPeriod = json.getInt("openedPeriod"),
            status = UnexpectedExpenseStatus.valueOf(json.getString("status")),
            paidPeriod = if (json.isNull("paidPeriod")) null else json.getInt("paidPeriod"),
            balanceBeforePayment =
                if (json.isNull("balanceBeforePayment")) null
                else json.getInt("balanceBeforePayment"),
            withdrawals =
                json.getJSONArray("withdrawals").let { entries ->
                    List(entries.length()) { index ->
                        val entry = entries.getJSONObject(index)
                        UnexpectedExpenseWithdrawal(
                            entry.getInt("amount"),
                            entry.getInt("period"),
                            entry.getInt("revision"),
                            entry.getString("goalId"),
                            entry.getString("goalTitle"),
                            entry.getInt("goalPrice"),
                            entry.getInt("coinsBefore"),
                            entry.getInt("savingsBefore"),
                        )
                    }
                },
        )
}
