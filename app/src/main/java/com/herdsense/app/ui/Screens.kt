@file:OptIn(ExperimentalMaterial3Api::class)

package com.herdsense.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herdsense.app.data.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ---------------------------------------------------------------- Dashboard

@Composable
fun DashboardScreen(onOpenCow: (Int) -> Unit, onOpenTab: (Int) -> Unit, onAddCow: () -> Unit) {
    val report = Feed.report()
    val groups = remember(Herd.cows.size) { Metrics.groupStats() }
    val notEaten = Herd.cows.filter { Metrics.intakeStatus(it) == IntakeStatus.NOT_EATEN }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM")) + " · De Hoeve",
                color = TextMuted, fontSize = 14.sp,
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Koeien", "${report.cows}", Icons.Filled.Favorite, Color(0xFF455A64), Modifier.weight(1f)) { onOpenTab(1) }
            StatTile("Voldoende gegeten", "${report.enough}", Icons.Filled.CheckCircle, IntakeStatus.ENOUGH.color, Modifier.weight(1f)) { onOpenTab(2) }
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Niet gegeten", "${report.notEaten}", Icons.Filled.Warning, IntakeStatus.NOT_EATEN.color, Modifier.weight(1f)) { onOpenTab(3) }
            StatTile("Te weinig gegeten", "${report.tooLittle}", Icons.Filled.Info, IntakeStatus.TOO_LITTLE.color, Modifier.weight(1f)) { onOpenTab(2) }
        }

        SectionCard(Modifier.clickable { onOpenTab(2) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Morgen bijvullen", fontSize = 14.sp, color = TextMuted)
                    Text(fmtKg(report.refillKg), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.KeyboardArrowRight, "Naar voer", tint = Color(0xFFBDBDBD))
            }
            Spacer(Modifier.height(12.dp))
            report.lines.forEach { l ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(l.item.name.substringBefore(" ("), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(fmtKg(l.refillKg), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        SectionCard(title = "Niet gegeten (${notEaten.size})") {
            if (notEaten.isEmpty()) Text("Alle koeien hebben gegeten 👍", color = TextMuted)
            notEaten.take(4).forEachIndexed { i, cow ->
                if (i > 0) Divider(color = DividerColor)
                AlertRow(cow, showFind = true) { onOpenCow(cow.number) }
            }
            if (notEaten.size > 4) {
                TextButton(onClick = { onOpenTab(3) }) { Text("Alle ${notEaten.size} bekijken", color = BrandRed) }
            }
        }

        SectionCard(title = "Groepen") {
            groups.forEachIndexed { i, g ->
                if (i > 0) Divider(color = DividerColor)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Groep ${g.group}", fontWeight = FontWeight.SemiBold)
                        Text("${g.cows} koeien · ${g.inHeat} in heat", fontSize = 13.sp, color = TextMuted)
                    }
                    val notEatenCount = Herd.cows.count { it.group == g.group && Metrics.intakeStatus(it) == IntakeStatus.NOT_EATEN }
                    val heatColor = if (notEatenCount > 0) IntakeStatus.NOT_EATEN.color else TextMuted
                    Text(
                        "$notEatenCount niet gegeten",
                        color = heatColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(heatColor.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onAddCow,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(Icons.Filled.Add, null, tint = BrandRed)
            Spacer(Modifier.width(6.dp))
            Text("Koe toevoegen", color = BrandRed)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(label, fontSize = 12.sp, color = TextMuted)
    }
}

@Composable
private fun StatTile(
    label: String, value: String, icon: ImageVector, color: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    Card(
        modifier.fillMaxHeight().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(Modifier.fillMaxHeight().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(label, fontSize = 12.sp, color = TextMuted, lineHeight = 14.sp)
            }
        }
    }
}

// ---------------------------------------------------------------- Kudde

@Composable
fun HerdScreen(onOpenCow: (Int) -> Unit, onAddCow: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableIntStateOf(0) }
    var notEatenOnly by rememberSaveable { mutableStateOf(false) }
    val list = Herd.cows.filter {
        (group == 0 || it.group == group) &&
            (!notEatenOnly || Metrics.intakeStatus(it) != IntakeStatus.ENOUGH) &&
            (it.number.toString().contains(query.trim()) || it.eartag.replace(" ", "").contains(query.trim().replace(" ", ""), true))
    }.let { l -> if (notEatenOnly) l.sortedBy { Metrics.intakeFraction(it) } else l }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Zoek werknummer", maxLines = 1) },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "Wissen") }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = notEatenOnly, onClick = { notEatenOnly = !notEatenOnly },
                            label = { Text("Niet/te weinig gegeten") },
                            leadingIcon = { Icon(Icons.Filled.Warning, null, Modifier.size(16.dp)) },
                        )
                    }
                    items(listOf(0) + Herd.groups.toList()) { g ->
                        FilterChip(
                            selected = group == g, onClick = { group = g },
                            label = { Text(if (g == 0) "Alle groepen" else "Groep $g") },
                        )
                    }
                }
            }
            item {
                Text("${list.size} koeien", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            }
            items(list, key = { it.number }) { cow ->
                val intake = Metrics.intakeStatus(cow)
                Card(
                    Modifier.fillMaxWidth().clickable { onOpenCow(cow.number) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CowAvatar(cow.alert)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${cow.number}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("${cow.eartag} · ${fmtAge(cow.ageMonths)}", fontSize = 12.sp, color = TextMuted)
                            Text(
                                "Groep ${cow.group} · ${intake.label.lowercase()}",
                                fontSize = 12.sp, color = if (intake == IntakeStatus.ENOUGH) TextMuted else intake.color,
                            )
                        }
                        StatusBadge(cow.alert)
                        Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextMuted)
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAddCow,
            containerColor = BrandRed,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Koe toevoegen") },
        )
    }
}

// ---------------------------------------------------------------- Meldingen

@Composable
fun AlertsScreen(onOpenCow: (Int) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(AlertType.NOT_EATEN).forEach { type ->
            val cows = Herd.alerts(type)
            item(key = type.name) {
                SectionCard(title = "${type.label} (${cows.size})") {
                    if (type == AlertType.NOT_EATEN && cows.isNotEmpty()) {
                        Text(
                            "Druk op Zoek: het lampje op de oorsensor gaat branden zodat je de koe snel vindt.",
                            fontSize = 13.sp, color = TextMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (cows.isEmpty()) Text("Geen koeien", color = TextMuted)
                    cows.forEachIndexed { i, cow ->
                        if (i > 0) Divider(color = DividerColor)
                        AlertRow(cow, showFind = true) { onOpenCow(cow.number) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRow(cow: Cow, showFind: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(cow.alert?.color ?: Color.Gray))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${cow.number}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("  Groep ${cow.group}", color = TextMuted, fontSize = 13.sp)
            }
            Text(Metrics.alertDetail(cow), fontSize = 13.sp, color = TextMuted)
        }
        if (showFind) {
            Spacer(Modifier.width(8.dp))
            FindButton(cow, compact = true)
        } else {
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextMuted)
        }
    }
}

// ---------------------------------------------------------------- Meer

@Composable
fun MoreScreen() {
    var push by rememberSaveable { mutableStateOf(true) }
    var notEatenAlerts by rememberSaveable { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Bedrijf") {
            SettingRow(Icons.Filled.Home, "De Hoeve", "NL-01 · ${Herd.cows.size} koeien")
            SettingRow(Icons.Filled.Settings, "Sensoren", "${Herd.cows.size - Herd.sensorsOffline} online · ${Herd.sensorsOffline} offline")
        }
        SectionCard(title = "Meldingen") {
            SwitchRow("Pushmeldingen", push) { push = it }
            SwitchRow("Melding als koe niet gegeten heeft", notEatenAlerts) { notEatenAlerts = it }
        }
        SectionCard(title = "App") {
            SettingRow(Icons.Filled.Person, "Account", "Veehouder")
            SettingRow(Icons.Filled.Info, "Over", "HerdSense 1.1 · demodata")
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = BrandRed)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = BrandRed),
        )
    }
}
