package com.herdsense.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herdsense.app.data.Cow
import com.herdsense.app.data.Herd

/** Which cow's sensor light is currently blinking (null = none). */
object FindLight {
    var cowNumber by mutableStateOf<Int?>(null)
}

private val Amber = Color(0xFFFFB300)

@Composable
fun FindButton(cow: Cow, modifier: Modifier = Modifier, compact: Boolean = false) {
    val active = FindLight.cowNumber == cow.number
    if (compact) {
        FilledTonalButton(
            onClick = { FindLight.cowNumber = cow.number },
            modifier = modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (active) Amber else Amber.copy(alpha = 0.18f),
                contentColor = Color(0xFF6D4C00),
            ),
        ) {
            Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Zoek", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Button(
            onClick = { FindLight.cowNumber = cow.number },
            modifier = modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color(0xFF3E2C00)),
        ) {
            Icon(Icons.Filled.LocationOn, null)
            Spacer(Modifier.width(8.dp))
            Text("Zoek koe – lampje laten branden", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun FindCowDialog() {
    val number = FindLight.cowNumber ?: return
    val cow = Herd.get(number) ?: return
    val t = rememberInfiniteTransition(label = "blink")
    val a by t.animateFloat(
        initialValue = 0.1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "alpha",
    )
    AlertDialog(
        onDismissRequest = { FindLight.cowNumber = null },
        title = { Text("Koe ${cow.number} zoeken") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(130.dp)) {
                    val r = size.minDimension / 2
                    drawCircle(Amber.copy(alpha = 0.15f * a), r)
                    drawCircle(Amber.copy(alpha = 0.35f * a), r * 0.7f)
                    drawCircle(Amber.copy(alpha = 0.3f + 0.7f * a), r * 0.38f)
                    drawCircle(Color.White.copy(alpha = 0.8f * a), r * 0.14f)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Het lampje op de oorsensor knippert nu. Loop door de stal en kijk naar het oranje licht.",
                    textAlign = TextAlign.Center, fontSize = 14.sp,
                )
                Spacer(Modifier.height(12.dp))
                KeyValueRow("Oormerk", cow.eartag)
                KeyValueRow("Sensor", cow.sensorId)
                KeyValueRow("Groep", "Groep ${cow.group}")
            }
        },
        confirmButton = {
            Button(
                onClick = { FindLight.cowNumber = null },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            ) { Text("Gevonden – lampje uit") }
        },
    )
}
