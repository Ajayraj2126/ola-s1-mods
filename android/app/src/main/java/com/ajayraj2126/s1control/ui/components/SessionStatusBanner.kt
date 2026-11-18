package com.ajayraj2126.s1control.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajayraj2126.s1control.model.SessionState
import com.ajayraj2126.s1control.ui.theme.*

/** A session update never offers to rerun installation or other scooter changes. */
@Composable
fun SessionStatusBanner(
    state: SessionState,
    onChooseScooter: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is SessionState.Connected) return
    val waiting = state is SessionState.Connecting || state is SessionState.Reconnecting ||
        state is SessionState.WaitingForNetwork
    val title = when (state) {
        is SessionState.Connecting -> "Connecting to scooter"
        is SessionState.Reconnecting -> "Checking your scooter connection"
        is SessionState.WaitingForNetwork -> "Waiting for Wi-Fi"
        is SessionState.Failed -> "Connection needs attention"
        else -> "Scooter disconnected"
    }
    val detail = when (state) {
        is SessionState.Connecting -> "Verifying the diagnostic session."
        is SessionState.Reconnecting -> "No installation or scooter changes are replayed."
        is SessionState.WaitingForNetwork -> "Reconnect starts automatically when Wi-Fi returns."
        is SessionState.Failed -> state.message
        else -> "Connect to use scooter tools. You can still review this workspace."
    }
    Row(
        modifier = modifier.fillMaxWidth().background(SurfaceVariantDark)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (waiting) {
            CircularProgressIndicator(Modifier.size(18.dp), color = AccentMint, strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.CloudOff, contentDescription = null,
                tint = if (state is SessionState.Failed) StatusWarning else TextMuted,
                modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(detail, fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
        }
        if (!waiting) {
            TextButton(onClick = onChooseScooter) { Text("Connect", color = AccentMint, fontSize = 12.sp) }
        }
    }
}
