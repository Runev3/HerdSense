package com.herdsense.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class AlertType(val label: String, val color: Color) {
    NOT_EATEN("Niet gegeten", Color(0xFFB71C1C)),
    HEAT("Tochtig", Color(0xFFD81B60)),
    HEALTH("Gezondheid", Color(0xFFD0202E)),
    LOW_EATING("Weinig gevreten", Color(0xFFEF6C00)),
}

/** Stack order (bottom → top) used by every behaviour chart. */
enum class Behaviour(val label: String, val color: Color) {
    RUMINATION("Herkauwen", Color(0xFF1E5BC6)),
    INACTIVE("Inactief", Color(0xFF5BC0F0)),
    EATING("Vreten", Color(0xFF3FAE49)),
    ACTIVE("Actief", Color(0xFFF6D02F)),
    HIGH_ACTIVE("Zeer actief", Color(0xFFE53935)),
}

enum class IntakeStatus(val label: String, val color: Color) {
    ENOUGH("Voldoende gegeten", Color(0xFF2E7D32)),
    TOO_LITTLE("Te weinig gegeten", Color(0xFFEF6C00)),
    NOT_EATEN("Niet gegeten", Color(0xFFB71C1C)),
}

data class Cow(
    val number: Int,
    val group: Int,
    val lactation: Int,
    val daysInMilk: Int,
    val sensorId: String,
    val alert: AlertType?,
    val alertSinceHours: Int,
    val seed: Long,
    val eartag: String,
    val ageMonths: Int,
    val addedByUser: Boolean = false,
    val inHeat: Boolean = false,
)

data class GroupStat(val group: Int, val cows: Int, val avgRumination: Float, val alerts: Int, val inHeat: Int)

fun fmtAge(months: Int): String {
    val y = months / 12; val m = months % 12
    return when {
        y == 0 -> "$m mnd"
        m == 0 -> "$y jaar"
        else -> "$y jaar $m mnd"
    }
}

object Herd {
    private var prefs: SharedPreferences? = null

    val cows = mutableStateListOf<Cow>().apply { addAll(generate()) }

    private fun generate(): List<Cow> {
        val r = Random(2026)
        val numbers = mutableSetOf<Int>()
        while (numbers.size < 64) {
            numbers += when (r.nextInt(3)) {
                0 -> r.nextInt(10, 999)
                1 -> r.nextInt(1000, 9999)
                else -> r.nextInt(10000, 29999)
            }
        }
        return numbers.sorted().mapIndexed { i, n ->
            // Exactly 3 cows have not eaten today (indices 6, 28, 50).
            val alert = if (i % 22 == 6) AlertType.NOT_EATEN else null
            val lact = 1 + r.nextInt(6)
            Cow(
                number = n,
                group = 1 + r.nextInt(5),
                lactation = lact,
                daysInMilk = r.nextInt(5, 380),
                sensorId = "HS-%06X".format(r.nextInt(0xFFFFFF)),
                alert = alert,
                alertSinceHours = r.nextInt(1, 14),
                seed = n * 31L + 7L,
                eartag = "NL %04d %05d".format(r.nextInt(1000, 9999), n),
                ageMonths = 22 + lact * 13 + r.nextInt(0, 10),
                inHeat = alert == null && i % 9 == 4,
            )
        }
    }

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences("herdsense", Context.MODE_PRIVATE)
        prefs = p
        runCatching {
            val arr = JSONArray(p.getString("added_cows", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                insertSorted(
                    Cow(
                        number = o.getInt("number"),
                        group = o.getInt("group"),
                        lactation = o.optInt("lactation", 0),
                        daysInMilk = 0,
                        sensorId = o.getString("sensor"),
                        alert = null,
                        alertSinceHours = 0,
                        seed = o.getInt("number") * 31L + 7L,
                        eartag = o.getString("eartag"),
                        ageMonths = o.getInt("age"),
                        addedByUser = true,
                    )
                )
            }
        }
        Feed.load(p)
    }

    private fun insertSorted(cow: Cow) {
        val idx = cows.indexOfFirst { it.number > cow.number }.let { if (it < 0) cows.size else it }
        cows.add(idx, cow)
    }

    fun exists(number: Int) = cows.any { it.number == number }
    fun eartagExists(tag: String) = cows.any { it.eartag.replace(" ", "").equals(tag.replace(" ", ""), true) }

    fun addCow(number: Int, eartag: String, ageMonths: Int, group: Int): Cow {
        val cow = Cow(
            number = number,
            group = group,
            lactation = if (ageMonths < 24) 0 else max(1, (ageMonths - 22) / 13),
            daysInMilk = 0,
            sensorId = "HS-%06X".format(Random.nextInt(0xFFFFFF)),
            alert = null,
            alertSinceHours = 0,
            seed = number * 31L + 7L,
            eartag = eartag,
            ageMonths = ageMonths,
            addedByUser = true,
        )
        insertSorted(cow)
        save()
        return cow
    }

    fun removeCow(number: Int) {
        cows.removeAll { it.number == number && it.addedByUser }
        save()
    }

    private fun save() {
        val arr = JSONArray()
        cows.filter { it.addedByUser }.forEach { c ->
            arr.put(
                JSONObject()
                    .put("number", c.number).put("group", c.group).put("lactation", c.lactation)
                    .put("sensor", c.sensorId).put("eartag", c.eartag).put("age", c.ageMonths)
            )
        }
        prefs?.edit()?.putString("added_cows", arr.toString())?.apply()
    }

    val groups = 1..5
    const val sensorsOffline = 1

    fun get(number: Int): Cow? = cows.firstOrNull { it.number == number }

    fun neighbour(number: Int, delta: Int): Int {
        val i = cows.indexOfFirst { it.number == number }
        return cows[(i + delta + cows.size) % cows.size].number
    }

    fun alerts(type: AlertType) = cows.filter { it.alert == type }
}

private fun Random.noise() = (nextFloat() + nextFloat() + nextFloat() - 1.5f) * 2f

private fun epochHour() = System.currentTimeMillis() / 3_600_000L
private fun epochDay() = LocalDate.now().toEpochDay()

fun movingAverage(values: List<Float>, window: Int): List<Float> {
    if (window <= 1) return values
    var sum = 0f
    return values.mapIndexed { i, v ->
        sum += v
        if (i >= window) sum -= values[i - window]
        sum / min(i + 1, window)
    }
}

data class ActivitySeries(val current: List<Float>, val reference: List<Float>, val heatBand: ClosedFloatingPointRange<Float>?)

object Metrics {

    /** Share of today's ration this cow has eaten (1.0 = full ration). */
    fun intakeFraction(cow: Cow): Float {
        val r = Random(cow.seed * 4_243L + epochDay())
        return when (cow.alert) {
            AlertType.NOT_EATEN -> r.nextFloat() * 0.15f
            AlertType.LOW_EATING -> 0.45f + r.nextFloat() * 0.3f
            AlertType.HEALTH -> 0.6f + r.nextFloat() * 0.25f
            else -> 0.86f + r.nextFloat() * 0.22f
        }
    }

    fun intakeStatus(cow: Cow): IntakeStatus {
        val f = intakeFraction(cow)
        return when {
            f >= 0.9f -> IntakeStatus.ENOUGH
            f >= 0.25f -> IntakeStatus.TOO_LITTLE
            else -> IntakeStatus.NOT_EATEN
        }
    }

    fun intakeKg(cow: Cow): Float = intakeFraction(cow) * Feed.kgPerCow

    fun dayBehaviour(cow: Cow, fromEnd: Int): FloatArray {
        val r = Random(cow.seed * 1_000_003L + (epochDay() - fromEnd) * 7_919L)
        var rum = 500f + r.noise() * 25f
        var eat = 285f + r.noise() * 20f
        var act = 170f + r.noise() * 18f
        var high = 40f + r.noise() * 8f
        when (cow.alert) {
            AlertType.HEALTH -> if (fromEnd < 3) {
                val f = (3 - fromEnd) / 3f
                rum -= 150f * f; eat -= 90f * f; act -= 40f * f
            }
            AlertType.LOW_EATING -> if (fromEnd < 2) eat -= 100f * (2 - fromEnd) / 2f
            AlertType.NOT_EATEN -> if (fromEnd == 0) { eat = 15f + r.nextFloat() * 20f; rum -= 220f }
            AlertType.HEAT -> if (fromEnd == 0) {
                high += 120f; act += 70f; rum -= 90f; eat -= 40f
            }
            null -> {}
        }
        if (cow.inHeat && fromEnd == 0) { high += 120f; act += 70f; rum -= 90f; eat -= 40f }
        high = high.coerceAtLeast(5f)
        val inactive = 1440f - rum - eat - act - high
        return floatArrayOf(rum, inactive, eat, act, high)
    }

    fun dailyBehaviour(cow: Cow, days: Int): List<FloatArray> =
        (days - 1 downTo 0).map { dayBehaviour(cow, it) }

    fun hourlyBehaviour(cow: Cow): List<FloatArray> {
        val nowHour = LocalDateTime.now().hour
        val eh = epochHour()
        return (23 downTo 0).map { fromEnd ->
            val hour = ((nowHour - fromEnd) % 24 + 24) % 24
            val r = Random(cow.seed * 7_777L + (eh - fromEnd))
            val feeding = hour in 5..8 || hour in 16..19
            val night = hour < 5 || hour >= 22
            var rum = (if (night) 30f else if (feeding) 10f else 22f) + r.noise() * 3f
            var eat = (if (feeding) 25f else if (night) 3f else 10f) + r.noise() * 2f
            var act = (if (feeding) 12f else if (night) 3f else 9f) + r.noise() * 2f
            var high = (if (feeding) 4f else 1.5f) + r.noise() * 0.6f
            if (cow.alert == AlertType.HEAT && fromEnd < 12) { high += 10f; act += 8f; rum -= 10f }
            if (cow.alert == AlertType.HEALTH) { rum *= 0.7f; eat *= 0.6f }
            if (cow.alert == AlertType.NOT_EATEN && fromEnd < 14) { eat *= 0.08f; rum *= 0.5f }
            rum = rum.coerceAtLeast(0.5f); eat = eat.coerceAtLeast(0.5f)
            act = act.coerceAtLeast(0.5f); high = high.coerceAtLeast(0.3f)
            val inactive = (60f - rum - eat - act - high).coerceAtLeast(2f)
            val arr = floatArrayOf(rum, inactive, eat, act, high)
            val s = arr.sum()
            FloatArray(arr.size) { arr[it] * 60f / s }
        }
    }

    /** Hourly ear temperature (°C) plus a 24 h rolling reference. */
    fun temperature(cow: Cow, hours: Int): Pair<List<Float>, List<Float>> {
        val nowHour = LocalDateTime.now().hour
        val eh = epochHour()
        val raw = (hours - 1 downTo 0).map { fromEnd ->
            val r = Random(cow.seed * 31L + (eh - fromEnd) * 101L)
            val hod = ((nowHour - fromEnd) % 24 + 24) % 24
            var t = 38.4f + 0.35f * sin((hod - 6) / 24.0 * 2 * PI).toFloat() + r.noise() * 0.12f
            if (r.nextFloat() < 0.06f) t -= 1.5f + r.nextFloat() * 4.5f // drinking dips
            if (cow.alert == AlertType.HEALTH && fromEnd < 48) t += 1.1f * (48 - fromEnd) / 48f
            t
        }
        return raw to movingAverage(raw, 24)
    }

    private fun activityBase(hod: Int): Float {
        val h = hod.toFloat()
        return 18f + 30f * exp(-((h - 7f) * (h - 7f)) / 4f) +
            28f * exp(-((h - 17.5f) * (h - 17.5f)) / 4f) + if (hod in 9..15) 8f else 0f
    }

    fun activity(cow: Cow, hours: Int): ActivitySeries {
        val nowHour = LocalDateTime.now().hour
        val eh = epochHour()
        val raw = ArrayList<Float>(hours)
        val ref = ArrayList<Float>(hours)
        for (fromEnd in hours - 1 downTo 0) {
            val r = Random(cow.seed * 17L + (eh - fromEnd) * 13L)
            val hod = ((nowHour - fromEnd) % 24 + 24) % 24
            val base = activityBase(hod)
            var v = base * (1f + r.noise() * 0.15f)
            if (cow.inHeat && fromEnd < 20) {
                v *= 1f + 1.6f * sin(PI * (20 - fromEnd) / 20.0).toFloat()
            }
            raw += v.coerceAtLeast(2f)
            ref += base
        }
        val band = if (cow.inHeat && hours > 1) {
            val start = max(0, hours - 1 - 20).toFloat() / (hours - 1)
            start..1f
        } else null
        return ActivitySeries(movingAverage(raw, 3), movingAverage(ref, 3), band)
    }

    fun herdBehaviourToday(): FloatArray {
        val acc = FloatArray(Behaviour.values().size)
        val list = Herd.cows.toList()
        list.forEach { c -> dayBehaviour(c, 0).forEachIndexed { i, v -> acc[i] += v } }
        return FloatArray(acc.size) { acc[it] / list.size.coerceAtLeast(1) }
    }

    fun groupStats(): List<GroupStat> = Herd.groups.map { g ->
        val members = Herd.cows.filter { it.group == g }
        GroupStat(
            group = g,
            cows = members.size,
            avgRumination = if (members.isEmpty()) 0f else members.map { dayBehaviour(it, 0)[0] }.average().toFloat(),
            alerts = members.count { it.alert != null },
            inHeat = members.count { it.inHeat },
        )
    }

    fun alertDetail(cow: Cow): String = when (cow.alert) {
        AlertType.NOT_EATEN -> "Vandaag ${intakeKg(cow).roundToInt()} kg van ${Feed.kgPerCow.roundToInt()} kg gevreten"
        AlertType.HEAT -> "Tochtig sinds ${cow.alertSinceHours} u · insemineren over ${max(1, 10 - cow.alertSinceHours)}–${max(4, 18 - cow.alertSinceHours)} u"
        AlertType.HEALTH -> {
            val now = dayBehaviour(cow, 0)[0]
            val base = (4..7).map { dayBehaviour(cow, it)[0] }.average().toFloat()
            "Herkauwen −${((1f - now / base) * 100).roundToInt()}% t.o.v. vorige week"
        }
        AlertType.LOW_EATING -> "Vreettijd ${fmtMinutes(dayBehaviour(cow, 0)[2])} vandaag, lager dan normaal"
        null -> "Geen aandacht nodig"
    }
}

fun fmtMinutes(m: Float): String {
    val t = m.roundToInt().coerceAtLeast(0)
    return "${t / 60}u ${"%02d".format(t % 60)}m"
}

private val dayFmt = DateTimeFormatter.ofPattern("dd-MM")
private val hourFmt = DateTimeFormatter.ofPattern("HH:mm")

/** Evenly spaced time labels for an hourly series that ends now. */
fun timeAxisLabels(hours: Int, count: Int = 5): List<String> {
    val now = LocalDateTime.now().withMinute(0)
    return (0 until count).map { i ->
        val back = ((hours - 1) * (1f - i / (count - 1f))).roundToInt().toLong()
        val t = now.minusHours(back)
        if (hours <= 24) t.format(hourFmt) else t.format(dayFmt)
    }
}

fun dayLabels(days: Int): List<String> {
    val today = LocalDate.now()
    return (days - 1 downTo 0).map { today.minusDays(it.toLong()).format(dayFmt) }
}

fun hourLabels(): List<String> {
    val now = LocalDateTime.now().withMinute(0)
    return (23 downTo 0).map { now.minusHours(it.toLong()).format(DateTimeFormatter.ofPattern("HH")) }
}
