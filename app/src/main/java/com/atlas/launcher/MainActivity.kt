package com.atlas.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ink = Color(0xFF372719)
private val paper = Color(0xFFE9D4A5)
private val ocean = Color(0xFFB2C4AE)
private data class Island(val name: String, val subtitle: String, val x: Float, val y: Float)
private val islands = listOf(
    Island("TAVERN", "Social", .25f, .27f),
    Island("SHIPYARD", "Travel", .73f, .28f),
    Island("VAULT", "Files", .28f, .72f),
    Island("OBSERVATORY", "Utilities", .73f, .72f)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("atlas", MODE_PRIVATE)
        setContent {
            var selected by remember { mutableStateOf<Island?>(null) }
            var logOpen by remember { mutableStateOf(false) }
            var soundEnabled by remember { mutableStateOf(prefs.getBoolean("music", false)) }
            val apps = remember {
                packageManager.queryIntentActivities(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
                ).filter { it.activityInfo.packageName != packageName }
                    .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            }
            BackHandler(selected != null || logOpen) {
                if (logOpen) logOpen = false else selected = null
            }
            Column(Modifier.fillMaxSize().background(paper).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("ATLAS", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                        fontSize = 32.sp, color = ink)
                    Text(if (soundEnabled) "SOUND: ON" else "SOUND: OFF",
                        Modifier.clickable {
                            soundEnabled = !soundEnabled
                            prefs.edit().putBoolean("music", soundEnabled).apply()
                        }, color = ink, fontSize = 12.sp)
                }
                Text("THE CARTOGRAPHER'S HOME", fontFamily = FontFamily.Serif,
                    fontSize = 12.sp, color = ink)
                Spacer(Modifier.height(12.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(ocean)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        islands.forEach { island ->
                            val cx = island.x * w
                            val cy = island.y * h
                            val p = Path().apply {
                                moveTo(cx - 75, cy - 30)
                                cubicTo(cx - 80, cy - 95, cx + 80, cy - 95, cx + 85, cy - 25)
                                cubicTo(cx + 115, cy + 65, cx - 60, cy + 90, cx - 75, cy - 30)
                                close()
                            }
                            drawPath(p, paper)
                            drawPath(p, ink, style = Stroke(3f))
                            drawCircle(ink, 5f, Offset(cx, cy))
                        }
                    }
                    if (selected == null) {
                        islands.forEach { island ->
                            Text(island.name, Modifier
                                .offset(x = maxWidth * island.x - 55.dp,
                                    y = maxHeight * island.y - 12.dp)
                                .width(110.dp)
                                .clickable { selected = island },
                                color = ink, fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                textAlign = TextAlign.Center)
                        }
                    } else {
                        Column(Modifier.fillMaxSize().padding(12.dp).background(paper).padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("BACK TO MAP", Modifier.clickable { selected = null }, color = ink)
                                Text(selected!!.name, color = ink, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Buildings / installed applications", color = ink)
                            LazyColumn {
                                items(apps) { app ->
                                    Row(Modifier.fillMaxWidth().clickable {
                                        val intent = packageManager.getLaunchIntentForPackage(
                                            app.activityInfo.packageName)
                                        if (intent != null) startActivity(intent)
                                    }.padding(vertical = 11.dp)) {
                                        Text("⌂   " + app.loadLabel(packageManager).toString(),
                                            color = ink, fontFamily = FontFamily.Serif, fontSize = 17.sp)
                                    }
                                    HorizontalDivider(color = ink.copy(alpha = .2f))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CAPTAIN'S LOG", Modifier.clickable { logOpen = !logOpen }, color = ink)
                    Text("COMPASS", Modifier.clickable { selected = null }, color = ink)
                }
                if (logOpen) {
                    Text(java.text.SimpleDateFormat("EEE, dd MMM yyyy",
                        java.util.Locale.getDefault()).format(java.util.Date()), color = ink)
                    Text("Android widget hosting is planned for a later version.",
                        color = ink, fontSize = 12.sp)
                }
                if (soundEnabled) {
                    Text("Audio setting saved; soundtrack is not bundled in this GitHub build.",
                        color = ink, fontSize = 11.sp)
                }
            }
        }
    }
}
