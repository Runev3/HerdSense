package com.herdsense.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herdsense.app.data.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

enum class Metric(val label: String) {
    ACTIVITY("Activiteit"),
    TEMPERATURE("Temperatuur"),
    BEHAVIOUR("Gedrag"),
    RUMINATION("Herkauwen"),
    EATING("Vreten"),
}

private val periods = listOf(1, 5, 14, 30)
private fun periodLabel(d: Int) = if (d == 1) "Laatste 24 uur" else "Laatste $d dagen"

@Composable
fun CowDetailScreen(number: Int, onNavigate: (Int) -> Unit, onRemoved: () -> Unit) {
    val cow = Herd.get(number) ?: return
    var metric by rememberSaveable { mutableStateOf(Metric.ACTIVITY) }
    var period by rememberSaveable { mutableIntStateOf(5) }
    var hourly by rememberSaveable { mutableIntStateOf(0) }
    var confirmRemove by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CowIcon(Modifier.size(36.dp), nostril = ScreenBg)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${cow.number}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Groep ${cow.group} · ${cow.eartag}", color = TextMuted, fontSize = 14.sp)
            }
            NavSquare(Icons.Filled.KeyboardArrowLeft, "Vorige koe") { onNavigate(Herd.neighbour(number, -1)) }
            Spacer(Modifier.width(8.dp))
            NavSquare(Icons.Filled.KeyboardArrowRight, "Volgende koe") { onNavigate(Herd.neighbour(number, 1)) }
        }

        cow.alert?.let { alert ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(alert.color.copy(alpha = 0.1f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, null, tint = alert.color)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(alert.label, color = alert.color, fontWeight = FontWeight.Bold)
                    Text(Metrics.alertDetail(cow), fontSize = 13.sp, color = Color(0xFF444444))
                }
            }
        }

        FindButton(cow)

        // Intake today
        val frac = Metrics.intakeFraction(cow)
        val status = Metrics.intakeStatus(cow)
        SectionCard(title = "Voeropname vandaag") {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${Metrics.intakeKg(cow).roundToInt()} kg", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = status.color)
                Text("  van ${Feed.kgPerCow.roundToInt()} kg rantsoen", color = TextMuted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = frac.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = status.color,
                trackColor = Color(0xFFEAEAEA),
            )
            Spacer(Modifier.height(6.dp))
            Text("${status.label} · ${(frac * 100).roundToInt()}%", fontSize = 13.sp, color = status.color, fontWeight = FontWeight.SemiBold)
        }

        SelectField(metric, Metric.values().toList(), { it.label }, { metric = it }, Modifier.fillMaxWidth())

        SectionCard {
            if (metric == Metric.BEHAVIOUR) {
                SegmentedTabs(listOf("Per dag", "Per uur"), hourly, { hourly = it })
                Spacer(Modifier.height(12.dp))
            }
            if (!(metric == Metric.BEHAVIOUR && hourly == 1)) {
                SelectField(period, periods, ::periodLabel, { period = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            when (metric) {
                Metric.ACTIVITY -> ActivityChart(cow, period)
                Metric.TEMPERATURE -> TemperatureChart(cow, period)
                Metric.BEHAVIOUR -> BehaviourChart(cow, period, hourly == 1)
                Metric.RUMINATION -> SingleBehaviourChart(cow, period, 0)
                Metric.EATING -> SingleBehaviourChart(cow, period, 2)
            }
        }

        SectionCard(title = "Gegevens") {
            DetailRow("Status") { StatusBadge(cow.alert) }
            DetailRow("In heat") { Text(if (cow.inHeat) "Ja" else "Nee", color = if (cow.inHeat) Color(0xFFD81B60) else Color.Unspecified, fontWeight = if (cow.inHeat) FontWeight.Bold else FontWeight.Normal) }
            DetailRow("Oormerk") { Text(cow.eartag) }
            DetailRow("Leeftijd") { Text(fmtAge(cow.ageMonths)) }
            DetailRow("Groep") { Text("Groep ${cow.group}") }
            DetailRow("Lactatie") { Text(if (cow.lactation == 0) "Pink" else "${cow.lactation}") }
            DetailRow("Dagen in lactatie") { Text("${cow.daysInMilk}") }
            DetailRow("Oorsensor") { Text(cow.sensorId) }
        }

        if (cow.addedByUser) {
            TextButton(onClick = { confirmRemove = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Koe verwijderen", color = BrandRed)
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Koe ${cow.number} verwijderen?") },
            text = { Text("De koe en het oormerk ${cow.eartag} worden uit de kudde gehaald.") },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; Herd.removeCow(cow.number); onRemoved() }) {
                    Text("Verwijderen", color = BrandRed)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Annuleren", color = TextMuted) } },
        )
    }
}

@Composable
private fun NavSquare(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
            .border(1.dp, Color(0xFFD6D6D6), RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Color(0xFF333333)) }
}

@Composable
private fun DetailRow(label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, modifier = Modifier.weight(1f))
        value()
    }
}

@Composable
private fun ActivityChart(cow: Cow, days: Int) {
    val data = remember(cow, days) { Metrics.activity(cow, days * 24) }
    val max = (data.current + data.reference).max()
    val yMax = ceil(max / 20f) * 20f
    LineChart(
        series = listOf(
            LineSeries(data.reference, Color(0xFF9EC5F5), 1.5.dp),
            LineSeries(data.current, Color(0xFF1E5BC6), 1.8.dp),
        ),
        yMin = 0f, yMax = yMax, yStep = yMax / 5f,
        xLabels = timeAxisLabels(days * 24),
        band = data.heatBand,
    )
    Spacer(Modifier.height(8.dp))
    val legend = mutableListOf("Activiteit" to Color(0xFF1E5BC6), "Normaal" to Color(0xFF9EC5F5))
    if (data.heatBand != null) legend += "In heat" to Color(0xFF34A853)
    Legend(legend)
}

@Composable
private fun TemperatureChart(cow: Cow, days: Int) {
    val (raw, ref) = remember(cow, days) { Metrics.temperature(cow, days * 24) }
    val lo = floor(raw.min()).coerceAtMost(36f)
    LineChart(
        series = listOf(
            LineSeries(raw, Color(0xFF222222), 1.2.dp),
            LineSeries(ref, Color(0xFFD0202E), 1.8.dp),
        ),
        yMin = lo, yMax = 41f, yStep = ((41f - lo) / 5f).coerceAtLeast(1f),
        xLabels = timeAxisLabels(days * 24),
        yFormat = { "%.0f°".format(it) },
    )
    Spacer(Modifier.height(8.dp))
    Legend(listOf("Oortemp." to Color(0xFF222222), "Gemiddelde 24 u" to Color(0xFFD0202E)))
    Spacer(Modifier.height(6.dp))
    Text(
        "Laatste: %.1f °C · korte dips zijn drinkmomenten".format(raw.last()),
        fontSize = 12.sp, color = TextMuted,
    )
}

@Composable
private fun BehaviourChart(cow: Cow, days: Int, hourly: Boolean) {
    val bars = remember(cow, days, hourly) {
        if (hourly) Metrics.hourlyBehaviour(cow) else Metrics.dailyBehaviour(cow, days)
    }
    val labels = remember(days, hourly) { if (hourly) hourLabels() else dayLabels(days) }
    val total = if (hourly) 60f else 1440f
    var selected by remember(cow, days, hourly) { mutableStateOf<Int?>(null) }
    val pct = bars.map { b -> FloatArray(b.size) { b[it] / total * 100f } }

    StackedBarChart(
        bars = pct,
        colors = Behaviour.values().map { it.color },
        xLabels = labels,
        yMax = 100f, yStep = 20f,
        selected = selected,
        onSelect = { selected = if (selected == it) null else it },
        yFormat = { "${it.toInt()}%" },
    )
    Spacer(Modifier.height(8.dp))
    val idx = selected ?: bars.lastIndex
    Text(
        (if (hourly) "Uur ${labels[idx]}:00" else labels[idx]) + if (selected == null) "  (tik op een balk)" else "",
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(6.dp))
    Behaviour.values().reversed().forEach { b ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(b.color))
            Spacer(Modifier.width(8.dp))
            Text(b.label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            val m = bars[idx][b.ordinal]
            Text(if (hourly) "${m.toInt()} min" else fmtMinutes(m), fontSize = 13.sp, color = TextMuted)
        }
    }
}

@Composable
private fun SingleBehaviourChart(cow: Cow, days: Int, index: Int) {
    val d = days.coerceAtLeast(2)
    val values = remember(cow, d, index) { Metrics.dailyBehaviour(cow, d).map { floatArrayOf(it[index]) } }
    val b = Behaviour.values()[index]
    var selected by remember(cow, d, index) { mutableStateOf<Int?>(null) }
    val labels = dayLabels(d)
    StackedBarChart(
        bars = values,
        colors = listOf(b.color),
        xLabels = labels,
        yMax = 600f, yStep = 120f,
        selected = selected,
        onSelect = { selected = if (selected == it) null else it },
        yFormat = { "${(it / 60).toInt()}u" },
    )
    Spacer(Modifier.height(8.dp))
    val i = selected ?: values.lastIndex
    val avg = values.map { it[0] }.average().toFloat()
    Row {
        Text("${labels[i]}: ${fmtMinutes(values[i][0])}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("Gem. ${fmtMinutes(avg)}", color = TextMuted, fontSize = 13.sp)
    }
}
