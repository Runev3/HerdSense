@file:OptIn(ExperimentalMaterial3Api::class)

package com.herdsense.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herdsense.app.data.Herd

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Dashboard", Icons.Filled.Home),
    Tab("Kudde", Icons.Filled.List),
    Tab("Voer", Icons.Filled.ShoppingCart),
    Tab("Meldingen", Icons.Filled.Notifications),
    Tab("Meer", Icons.Filled.Settings),
)

private const val TAB_HERD = 1
private const val TAB_FEED = 2
private const val TAB_ALERTS = 3

@Composable
fun HerdSenseApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var cowNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var addingCow by rememberSaveable { mutableStateOf(false) }
    val alertCount = Herd.cows.count { it.alert != null }

    BackHandler(enabled = cowNumber != null || addingCow || tab != 0) {
        when {
            addingCow -> addingCow = false
            cowNumber != null -> cowNumber = null
            else -> tab = 0
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            HerdTopBar(
                showBack = cowNumber != null || addingCow,
                onBack = { if (addingCow) addingCow = false else cowNumber = null },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i; cowNumber = null; addingCow = false },
                        icon = {
                            if (i == TAB_ALERTS) {
                                BadgedBox(badge = { Badge(containerColor = BrandRed) { Text("$alertCount") } }) {
                                    Icon(t.icon, contentDescription = t.label)
                                }
                            } else {
                                Icon(t.icon, contentDescription = t.label)
                            }
                        },
                        label = { Text(t.label, fontSize = 11.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandRed,
                            selectedTextColor = BrandRed,
                            indicatorColor = Color(0xFFFDE3E5),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val n = cowNumber
            when {
                addingCow -> AddCowScreen(onDone = { added ->
                    addingCow = false
                    if (added != null) cowNumber = added
                })
                n != null -> CowDetailScreen(number = n, onNavigate = { cowNumber = it }, onRemoved = { cowNumber = null })
                else -> when (tab) {
                    0 -> DashboardScreen(
                        onOpenCow = { cowNumber = it },
                        onOpenTab = { tab = it },
                        onAddCow = { addingCow = true },
                    )
                    TAB_HERD -> HerdScreen(onOpenCow = { cowNumber = it }, onAddCow = { addingCow = true })
                    TAB_FEED -> FeedScreen(onOpenNotEaten = { tab = TAB_ALERTS })
                    TAB_ALERTS -> AlertsScreen(onOpenCow = { cowNumber = it })
                    else -> MoreScreen()
                }
            }
        }
    }

    FindCowDialog()
}

@Composable
private fun HerdTopBar(showBack: Boolean, onBack: () -> Unit) {
    Surface(color = BrandRed, shadowElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Terug", tint = Color.White) }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CowIcon(Modifier.size(18.dp), color = BrandRed)
                Spacer(Modifier.width(6.dp))
                Text("HerdSense", color = BrandRed, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.weight(1f))
            FarmSelector()
            IconButton(onClick = {}) { Icon(Icons.Filled.AccountCircle, "Account", tint = Color.White) }
        }
    }
}

@Composable
private fun FarmSelector() {
    val farms = listOf("NL-01" to "De Hoeve", "NL-02" to "Zuidweide", "BE-07" to "Hof ter Linde")
    var farm by rememberSaveable { mutableIntStateOf(0) }
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).clickable { open = true }.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(farms[farm].first, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            farms.forEachIndexed { i, (code, name) ->
                DropdownMenuItem(
                    text = { Text("$code · $name") },
                    onClick = { farm = i; open = false },
                    trailingIcon = { if (i == farm) Icon(Icons.Filled.Check, null, tint = BrandRed) },
                )
            }
        }
    }
}
