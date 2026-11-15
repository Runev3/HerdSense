package com.herdsense.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.herdsense.app.data.Herd
import com.herdsense.app.data.fmtAge

@Composable
fun AddCowScreen(onDone: (Int?) -> Unit) {
    val ctx = LocalContext.current
    var stapled by rememberSaveable { mutableStateOf(false) }
    var eartag by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var numberEdited by rememberSaveable { mutableStateOf(false) }
    var years by rememberSaveable { mutableStateOf("") }
    var months by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableIntStateOf(1) }

    fun applyTag(v: String) {
        eartag = v.uppercase().trim()
        if (!numberEdited) number = eartag.filter { it.isDigit() }.takeLast(5).trimStart('0')
    }

    fun scan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        GmsBarcodeScanning.getClient(ctx, options).startScan()
            .addOnSuccessListener { b -> b.rawValue?.let { applyTag(it) } }
            .addOnFailureListener { Toast.makeText(ctx, "Scannen mislukt: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
    }

    val num = number.toIntOrNull()
    val ageMonths = (years.toIntOrNull() ?: 0) * 12 + (months.toIntOrNull() ?: 0)
    val tagOk = eartag.count { it.isDigit() } >= 5
    val error = when {
        eartag.isNotBlank() && Herd.eartagExists(eartag) -> "Dit oormerk is al in gebruik"
        num != null && Herd.exists(num) -> "Werknummer $num bestaat al"
        else -> null
    }
    val valid = stapled && tagOk && num != null && num > 0 && ageMonths > 0 && error == null

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Koe toevoegen", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        StepCard(1, "Oormerk nieten", done = stapled) {
            Text(
                "Plaats het oormerk met de oormerktang in het midden van het oor, tussen de twee kraakbeenribben. " +
                    "Het mannelijke deel (met pin) zit aan de achterkant van het oor.",
                fontSize = 14.sp, color = Color(0xFF444444),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { stapled = !stapled }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = stapled, onCheckedChange = { stapled = it },
                    colors = CheckboxDefaults.colors(checkedColor = BrandRed),
                )
                Text("Oormerk is aangebracht", fontWeight = FontWeight.Medium)
            }
        }

        StepCard(2, "Oormerk scannen", done = tagOk) {
            Button(
                onClick = { scan() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Filled.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Scan oormerk (barcode / QR)", fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = eartag,
                onValueChange = { applyTag(it) },
                label = { Text("Oormerknummer") },
                placeholder = { Text("NL 1234 56789") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Of typ het nummer over dat op het oormerk staat.", fontSize = 12.sp, color = TextMuted)
        }

        StepCard(3, "Gegevens", done = num != null && ageMonths > 0) {
            OutlinedTextField(
                value = number,
                onValueChange = { v -> number = v.filter { it.isDigit() }.take(6); numberEdited = true },
                label = { Text("Werknummer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("Leeftijd", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = years, onValueChange = { v -> years = v.filter { it.isDigit() }.take(2) },
                    label = { Text("Jaren") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = months, onValueChange = { v -> months = v.filter { it.isDigit() }.take(2) },
                    label = { Text("Maanden") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            if (ageMonths > 0) Text("= ${fmtAge(ageMonths)}", fontSize = 12.sp, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            Text("Groep", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            SelectField(group, Herd.groups.toList(), { "Groep $it" }, { group = it }, Modifier.fillMaxWidth())
        }

        if (error != null) Text(error, color = BrandRed, fontWeight = FontWeight.SemiBold)

        Button(
            onClick = {
                val cow = Herd.addCow(num!!, eartag, ageMonths, group)
                Toast.makeText(ctx, "Koe ${cow.number} toegevoegd", Toast.LENGTH_SHORT).show()
                onDone(cow.number)
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            shape = RoundedCornerShape(10.dp),
        ) { Text("Koe opslaan", fontSize = 16.sp) }

        TextButton(onClick = { onDone(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Annuleren", color = TextMuted)
        }
    }
}

@Composable
private fun StepCard(step: Int, title: String, done: Boolean, content: @Composable ColumnScope.() -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(if (done) Color(0xFF2E7D32) else BrandRed),
                contentAlignment = Alignment.Center,
            ) {
                if (done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                else Text("$step", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}
