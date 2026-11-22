package com.ajayraj2126.s1control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayraj2126.s1control.model.ScooterManager
import com.ajayraj2126.s1control.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Read-only Android head-unit diagnostics. This is NOT a CAN/BMS decoder.
 * Every request is initiated by the user and has a fixed read-only operation.
 */
@Composable
fun AdvancedDiagnosticsScreen(manager: ScooterManager, onBack: () -> Unit) {
    val connected by manager.isConnected.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(BgDark)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column {
                Text("Advanced diagnostics", fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Read-only head-unit health · AJ Edition", fontSize = 12.sp,
                    color = TextSecondary)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SYSTEM SNAPSHOT", fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = AccentMint)
                Text(
                    "Checks the infotainment Android system only. These are NOT traction-battery " +
                        "or vehicle CAN measurements.",
                    fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
                )
                Button(
                    onClick = {
                        if (loading || !connected) return@Button
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                val info = manager.fetchScooterInfo()
                                val ram = manager.fetchRamInfo()
                                val uptime = manager.runRootShell("cat /proc/uptime")
                                val load = manager.runRootShell("cat /proc/loadavg")
                                data = listOf(
                                    "Scooter display model" to info.model.ifBlank { "Unknown" },
                                    "MoveOS build" to info.buildId.ifBlank { "Unknown" },
                                    "Android head-unit version" to info.androidVersion.ifBlank { "Unknown" },
                                    "Head-unit uptime" to DiagnosticsFormat.uptime(uptime),
                                    "Head-unit memory" to if (ram != null)
                                        "${ram.freeMb} MB available / ${ram.totalMb} MB total"
                                        else "Unavailable",
                                    "Android system load" to DiagnosticsFormat.load(load)
                                )
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = e.message ?: "Diagnostic read failed"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = connected && !loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(18.dp),
                            color = BgDark, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (loading) "Reading..." else "Refresh head-unit diagnostics")
                }
                if (!connected) Text("Connect a scooter first. No diagnostic read is possible offline.",
                    color = StatusWarning, fontSize = 12.sp)
                error?.let { Text(it, color = StatusDanger, fontSize = 12.sp) }
                if (data.isEmpty()) {
                    Text("No live readings collected in this session.",
                        color = TextMuted, fontSize = 12.sp)
                } else {
                    data.forEach { (label, value) ->
                        HorizontalDivider(color = DividerColor)
                        Text(label, color = TextSecondary, fontSize = 11.sp)
                        Text(value, color = TextPrimary, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = StatusWarning)
                    Spacer(Modifier.width(8.dp))
                    Text("Vehicle CAN & BMS diagnostics", color = TextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Battery SoC/SoH, cell voltages, charging watts, motor/VCU/MCU " +
                        "health, live fault codes and true range require a verified " +
                        "generation-specific CAN decoder. Those values are not available " +
                        "from the published S1Mods v1.5 Kotlin source.",
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp
                )
                Text(
                    "Status: Not integrated — no invented readings, no CAN writes or fault clearing.",
                    color = StatusWarning, fontSize = 12.sp
                )
            }
        }
    }
}

/** Parsers can be unit-tested without Android or a scooter connection. */
internal object DiagnosticsFormat {
    fun uptime(output: String): String {
        val seconds = output.trim().split(Regex("\\s+"))
            .firstOrNull()?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }?.toLong()
            ?: return "Unavailable"
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        return if (days > 0) "${days}d ${hours}h ${minutes}m"
        else "${hours}h ${minutes}m"
    }

    fun load(output: String): String {
        val first = output.trim().split(Regex("\\s+"))
        if (first.size < 3 || first.take(3).any { it.toDoubleOrNull() == null })
            return "Unavailable"
        return "${first[0]} / ${first[1]} / ${first[2]} (1/5/15 min)"
    }
}
