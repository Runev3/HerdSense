package com.herdsense.app.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class FeedItem(
    val id: String,
    val name: String,
    val kgPerCow: Float,
    val pricePerTon: Float,
    val stockKg: Float,
)

data class FeedLine(
    val item: FeedItem,
    val needDayKg: Float,
    val refillKg: Float,
    val daysLeft: Float,
    val buyKg: Float,
    val buyCost: Float,
    val costDay: Float,
)

data class FeedReport(
    val cows: Int,
    val enough: Int,
    val tooLittle: Int,
    val notEaten: Int,
    val requiredKg: Float,
    val providedKg: Float,
    val eatenKg: Float,
    val leftoverKg: Float,
    val refillKg: Float,
    val costDay: Float,
    val buyCost: Float,
    val planDays: Int,
    val lines: List<FeedLine>,
)

object Feed {
    /** Extra on top of the ration to cover spillage. */
    const val SPILL = 0.03f

    private var prefs: SharedPreferences? = null

    val items = mutableStateListOf(
        FeedItem("ruw", "Ruwvoer (gras- & maïskuil)", 40f, 55f, 52_000f),
        FeedItem("kracht", "Krachtvoer", 8f, 360f, 6_500f),
        FeedItem("mineraal", "Mineralen", 0.15f, 900f, 120f),
    )

    var planDays by mutableIntStateOf(30)

    val kgPerCow: Float get() = items.sumOf { it.kgPerCow.toDouble() }.toFloat()

    fun update(item: FeedItem) {
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) items[i] = item
        save()
    }

    fun updatePlanDays(days: Int) {
        planDays = days
        save()
    }

    fun report(): FeedReport {
        val cows = Herd.cows.toList()
        val req = kgPerCow
        val statuses = cows.map { Metrics.intakeStatus(it) }
        val eaten = cows.sumOf { (Metrics.intakeFraction(it).coerceAtMost(1.1f) * req).toDouble() }.toFloat()
        val requiredKg = cows.size * req
        val provided = requiredKg * (1 + SPILL)
        val leftover = (provided - eaten).coerceAtLeast(0f)
        val lines = items.map { item ->
            val share = if (req > 0) item.kgPerCow / req else 0f
            val needDay = cows.size * item.kgPerCow
            // Tomorrow: the full ration; if cows ate more than that today, today's intake + margin.
            // Either way the refill is above what was eaten today.
            val refill = maxOf(needDay, eaten * share * (1 + SPILL))
            val buy = (needDay * planDays - item.stockKg).coerceAtLeast(0f)
            FeedLine(
                item = item,
                needDayKg = needDay,
                refillKg = refill,
                daysLeft = if (needDay > 0f) item.stockKg / needDay else Float.POSITIVE_INFINITY,
                buyKg = buy,
                buyCost = buy / 1000f * item.pricePerTon,
                costDay = needDay / 1000f * item.pricePerTon,
            )
        }
        return FeedReport(
            cows = cows.size,
            enough = statuses.count { it == IntakeStatus.ENOUGH },
            tooLittle = statuses.count { it == IntakeStatus.TOO_LITTLE },
            notEaten = statuses.count { it == IntakeStatus.NOT_EATEN },
            requiredKg = requiredKg,
            providedKg = provided,
            eatenKg = eaten,
            leftoverKg = leftover,
            refillKg = lines.sumOf { it.refillKg.toDouble() }.toFloat(),
            costDay = lines.sumOf { it.costDay.toDouble() }.toFloat(),
            buyCost = lines.sumOf { it.buyCost.toDouble() }.toFloat(),
            planDays = planDays,
            lines = lines,
        )
    }

    fun load(p: SharedPreferences) {
        prefs = p
        planDays = p.getInt("plan_days", 30)
        runCatching {
            val arr = JSONArray(p.getString("feed_items", null) ?: return)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val idx = items.indexOfFirst { it.id == o.getString("id") }
                if (idx >= 0) items[idx] = items[idx].copy(
                    kgPerCow = o.getDouble("kg").toFloat(),
                    pricePerTon = o.getDouble("price").toFloat(),
                    stockKg = o.getDouble("stock").toFloat(),
                )
            }
        }
    }

    private fun save() {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("id", it.id).put("kg", it.kgPerCow.toDouble()).put("price", it.pricePerTon.toDouble()).put("stock", it.stockKg.toDouble()))
        }
        prefs?.edit()?.putString("feed_items", arr.toString())?.putInt("plan_days", planDays)?.apply()
    }
}

private val NL = Locale("nl", "NL")

fun fmtKg(kg: Float): String =
    if (kg >= 10_000f) String.format(NL, "%,.1f ton", kg / 1000f)
    else String.format(NL, "%,.0f kg", kg)

fun fmtEuro(e: Float): String =
    if (e < 100f) String.format(NL, "€ %,.2f", e) else String.format(NL, "€ %,.0f", e)

fun fmtNum(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else String.format(NL, "%.2f", v).trimEnd('0').trimEnd(',')

fun String.toNum(): Float? {
    val s = trim()
    return (if (s.contains(',')) s.replace(".", "").replace(',', '.') else s).toFloatOrNull()
}
