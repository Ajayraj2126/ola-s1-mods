package com.asjohn720.s1mods.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.AppTab
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.theme.*

/**
 * S1 Control by AJ: a read-only dashboard.
 * The original screens still own all ADB, root, diagnostic and installation actions.
 */
@Composable
fun AjHomeScreen(manager: ScooterManager, onNavigate: (AppTab) -> Unit) {
    val connected by manager.isConnected.collectAsState()
    val summary by manager.vehicleSummary.collectAsState()
    val ip by manager.scooterIp.collectAsState()
    val running by manager.isSetupRunning.collectAsState()
    val success by manager.setupSuccess.collectAsState()
    val step by manager.currentStep.collectAsState()
    val logs by manager.logs.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AJ EDITION  •  CONTROL CENTER", fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = AccentMint)
                Spacer(Modifier.height(4.dp))
                Text("Your scooter. Your way.", fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Everything you need in one place.", fontSize = 12.sp,
                    color = TextSecondary)
            }
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ElectricScooter, contentDescription = null,
                    tint = AccentMint, modifier = Modifier.size(28.dp))
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(
                    Color(0xFF153C44), SurfaceDark, Color(0xFF101E39))))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape)
                    .background(if (connected) StatusOk else TextMuted))
                Spacer(Modifier.width(8.dp))
                Text(if (connected) "SCOOTER CONNECTED" else "OFFLINE WORKSPACE",
                    color = if (connected) StatusOk else TextSecondary,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Wifi, contentDescription = null,
                    tint = if (connected) AccentMint else TextMuted)
            }
            Text(summary?.model?.takeUnless { it.isBlank() } ?: "Ola infotainment",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (connected) ip.ifBlank { "Connected over local Wi-Fi" }
                 else "Connect your scooter from the Setup tab",
                fontSize = 12.sp, color = TextSecondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            HorizontalDivider(color = CardBorder)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AjMetric("MOVEOS", summary?.moveOs ?: "Not checked", Modifier.weight(1f))
                AjMetric("COMPATIBILITY", when {
                    summary == null -> "Not checked"
                    summary?.supported == true -> "Supported"
                    else -> "Review"
                }, Modifier.weight(1f))
            }
        }

        if (running || success) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(15.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (running) "INSTALLATION RUNNING" else "SETUP COMPLETED",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (running) StatusWarning else StatusOk)
                Text(if (running) step?.title ?: "Working on the scooter..."
                     else "Review your installed apps and scooter settings.",
                    fontSize = 12.sp, color = TextSecondary)
                if (running) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(),
                        color = AccentMint, trackColor = CardBorder)
                }
            }
        }

        Text("QUICK ACCESS", color = AccentMint, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            AjShortcut("Setup", "Install mods", Icons.Default.Bolt,
                Modifier.weight(1f)) { onNavigate(AppTab.SETUP) }
            AjShortcut("Tools", "Diagnostics", Icons.Default.Tune,
                Modifier.weight(1f)) { onNavigate(AppTab.TOOLBOX) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            AjShortcut("Apps", "App manager", Icons.Default.Apps,
                Modifier.weight(1f)) { onNavigate(AppTab.CUSTOM_APK) }
            AjShortcut("Shell", "Advanced", Icons.Default.Terminal,
                Modifier.weight(1f)) { onNavigate(AppTab.TERMINAL) }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(15.dp))
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("RECENT ACTIVITY", fontSize = 11.sp,
                fontWeight = FontWeight.Bold, color = AccentMint)
            if (logs.isEmpty()) {
                Text("No recent events. Your session logs appear here.",
                    fontSize = 12.sp, color = TextSecondary)
            } else {
                logs.takeLast(3).asReversed().forEach { event ->
                    Text("• " + event, fontSize = 12.sp, color = TextSecondary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Text(
            "Custom UI by AJ (@Ajayraj2126) · Original S1Mods by Anand (@Asjohn720). " +
                "MIT licensed, not affiliated with Ola Electric. Use only while parked.",
            fontSize = 11.sp, lineHeight = 16.sp, color = TextMuted
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun AjMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontSize = 10.sp, color = TextMuted, letterSpacing = 1.sp)
        Text(value, fontSize = 13.sp, color = TextPrimary,
            fontWeight = FontWeight.SemiBold, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AjShortcut(
    title: String,
    caption: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.height(126.dp).clip(RoundedCornerShape(17.dp))
            .background(SurfaceDark).border(1.dp, CardBorder, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
            .background(AccentMintSubtle), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = AccentMint,
                modifier = Modifier.size(23.dp))
        }
        Column {
            Text(title, color = TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold)
            Text(caption, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
