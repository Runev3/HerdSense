package com.herdsense.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herdsense.app.data.*

private val Track = Color(0xFFEDEDED)

@Composable
fun FeedScreen(onOpenNotEaten: () -> Unit) {
    val report = Feed.report()
    var editing by remember { mutableStateOf<FeedItem?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Refill tomorrow — the main number
        SectionCard {
            Text("Morgen bijvullen", fontSize = 14.sp, color = TextMuted)
            Text(fmtKg(report.refillKg), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            report.lines.forEach { l ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(shortName(l.item), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(fmtKg(l.refillKg), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 2. Eaten today
        SectionCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Vandaag gevreten", fontSize = 14.sp, color = TextMuted)
                    Text(fmtKg(report.eatenKg), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text("van ${fmtKg(report.providedKg)}", fontSize = 13.sp, color = TextMuted)
            }
            Spacer(Modifier.height(10.dp))
            Bar(if (report.providedKg > 0) report.eatenKg / report.providedKg else 0f, Behaviour.EATING.color)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountChip(report.enough, "Voldoende", IntakeStatus.ENOUGH.color, Modifier.weight(1f))
                CountChip(report.tooLittle, "Te weinig", IntakeStatus.TOO_LITTLE.color, Modifier.weight(1f))
                CountChip(report.notEaten, "Niet gegeten", IntakeStatus.NOT_EATEN.color, Modifier.weight(1f), onOpenNotEaten)
            }
        }

        // 3. Costs
        SectionCard {
            Text("Voerkosten", fontSize = 14.sp, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.height(IntrinsicSize.Min)) {
                CostCell("Per dag", fmtEuro(report.costDay), Modifier.weight(1f))
                VDivider()
                CostCell("Per maand", fmtEuro(report.costDay * 30f), Modifier.weight(1f))
                VDivider()
                CostCell("Per koe", fmtEuro(if (report.cows > 0) report.costDay / report.cows else 0f), Modifier.weight(1f))
            }
        }

        // 4. Stock & purchase
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Voorraad & inkoop", fontSize = 14.sp, color = TextMuted, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(7, 14, 30, 60).forEach { d ->
                    val sel = Feed.planDays == d
                    Text(
                        "$d d",
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (sel) Color.White else Color(0xFF444444),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) BrandRed else Track)
                            .clickable { Feed.updatePlanDays(d) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            report.lines.forEach { l -> StockRow(l, report.planDays) { editing = l.item } }
            Divider(color = DividerColor, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inkopen voor ${report.planDays} dagen", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(fmtEuro(report.buyCost), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BrandRed)
            }
        }
    }

    editing?.let { item ->
        FeedEditDialog(item, onDismiss = { editing = null }, onSave = { Feed.update(it); editing = null })
    }
}

private fun shortName(item: FeedItem) = item.name.substringBefore(" (")

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
private fun StockRow(l: FeedLine, planDays: Int, onClick: () -> Unit) {
    val days = if (l.daysLeft.isInfinite()) planDays.toFloat() else l.daysLeft
    val color = when {
        days < 7f -> BrandRed
        days < planDays -> Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row {
                Text(shortName(l.item), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(
                    if (l.daysLeft.isInfinite()) "–" else "nog ${l.daysLeft.toInt()} d",
                    fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Bar(days / planDays, color)
            Spacer(Modifier.height(6.dp))
            Text(
                if (l.buyKg <= 0f) "Voorraad ${fmtKg(l.item.stockKg)} · genoeg"
                else "Voorraad ${fmtKg(l.item.stockKg)} · koop ${fmtKg(l.buyKg)} (${fmtEuro(l.buyCost)})",
                fontSize = 12.sp, color = TextMuted,
            )
        }
        Icon(Icons.Filled.KeyboardArrowRight, "Bewerken", tint = Color(0xFFBDBDBD), modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun CountChip(count: Int, label: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$count", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = color)
    }
}

@Composable
private fun CostCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, fontSize = 12.sp, color = TextMuted)
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(DividerColor))
}

@Composable
fun KeyValueRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = TextMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun FeedEditDialog(item: FeedItem, onDismiss: () -> Unit, onSave: (FeedItem) -> Unit) {
    var kg by remember { mutableStateOf(fmtNum(item.kgPerCow)) }
    var price by remember { mutableStateOf(fmtNum(item.pricePerTon)) }
    var stock by remember { mutableStateOf(fmtNum(item.stockKg)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Kg per koe per dag", kg) { kg = it }
                NumField("Prijs per ton (€)", price) { price = it }
                NumField("Voorraad (kg)", stock) { stock = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    item.copy(
                        kgPerCow = kg.toNum() ?: item.kgPerCow,
                        pricePerTon = price.toNum() ?: item.pricePerTon,
                        stockKg = stock.toNum() ?: item.stockKg,
                    )
                )
            }) { Text("Opslaan", color = BrandRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren", color = TextMuted) } },
    )
}

@Composable
fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
