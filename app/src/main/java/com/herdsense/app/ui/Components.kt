package com.herdsense.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herdsense.app.data.AlertType

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    trailing?.invoke()
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun <T> SelectField(
    selected: T,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, Color(0xFFD6D6D6), shape)
                .background(Color.White)
                .clickable { open = true }
                .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label(selected), Modifier.weight(1f), fontSize = 14.sp)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(label(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}

@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Column(
                Modifier.weight(1f).clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = if (active) BrandRed else TextMuted,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                )
                Box(Modifier.fillMaxWidth().height(2.dp).background(if (active) BrandRed else DividerColor))
            }
        }
    }
}

@Composable
fun StatusBadge(alert: AlertType?) {
    val (text, color) = if (alert == null) "Goed" to Color(0xFF2E7D32) else alert.label to alert.color
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun Legend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier, perRow: Int = 3) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                        Spacer(Modifier.width(5.dp))
                        Text(label, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

/** Simple cow-head glyph drawn with shapes. */
@Composable
fun CowIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF222222), nostril: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawOval(color, Offset(0f, h * 0.2f), Size(w * 0.32f, h * 0.2f))
        drawOval(color, Offset(w * 0.68f, h * 0.2f), Size(w * 0.32f, h * 0.2f))
        drawOval(color, Offset(w * 0.26f, 0f), Size(w * 0.12f, h * 0.22f))
        drawOval(color, Offset(w * 0.62f, 0f), Size(w * 0.12f, h * 0.22f))
        drawRoundRect(color, Offset(w * 0.24f, h * 0.12f), Size(w * 0.52f, h * 0.6f), CornerRadius(w * 0.2f))
        drawOval(color, Offset(w * 0.2f, h * 0.56f), Size(w * 0.6f, h * 0.42f))
        drawCircle(nostril, w * 0.055f, Offset(w * 0.38f, h * 0.78f))
        drawCircle(nostril, w * 0.055f, Offset(w * 0.62f, h * 0.78f))
    }
}

@Composable
fun CowAvatar(alert: AlertType?, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF1F1F1)),
        contentAlignment = Alignment.Center,
    ) {
        CowIcon(Modifier.size(26.dp), nostril = Color(0xFFF1F1F1))
        if (alert != null) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(3.dp).size(10.dp).clip(CircleShape)
                    .background(Color.White).padding(2.dp).clip(CircleShape).background(alert.color)
            )
        }
    }
}
