package com.bratneytg.worldvpn.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bratneytg.worldvpn.Server
import com.bratneytg.worldvpn.VpnState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HomeScreen(
    state: VpnState,
    server: Server?,
    onToggle: () -> Unit,
    onPickLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) {
        while (state is VpnState.Connected) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val connected = state is VpnState.Connected
    val busy = state is VpnState.Connecting || state is VpnState.Disconnecting
    val canConnect = server?.isConfigured == true

    val buttonColor by animateColorAsState(
        targetValue = when {
            connected -> Green
            busy -> Amber
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "buttonColor",
    )
    val buttonTextColor = if (connected || busy) Color(0xFF052E16) else MaterialTheme.colorScheme.onSurface

    val title: String
    val subtitle: String
    var subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    when (state) {
        is VpnState.Connected -> {
            title = "Conectado"
            subtitle = formatElapsed(now - state.since)
        }
        VpnState.Connecting -> {
            title = "Conectando…"
            subtitle = server?.let { "${it.name}, ${it.country}" } ?: ""
        }
        VpnState.Disconnecting -> {
            title = "Desconectando…"
            subtitle = ""
        }
        is VpnState.Error -> {
            title = "No se pudo conectar"
            subtitle = state.message
            subtitleColor = MaterialTheme.colorScheme.error
        }
        VpnState.Disconnected -> {
            title = "Desconectado"
            subtitle = if (canConnect) {
                "Toca el botón para conectar"
            } else {
                "Falta configurar tu servidor: Ajustes → Importar"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WorldVPN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes")
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .clickable(enabled = !busy && (connected || canConnect), onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    connected -> "Desconectar"
                    busy -> "…"
                    else -> "Conectar"
                },
                color = buttonTextColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = subtitleColor,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.weight(1f))

        LocationCard(server = server, onClick = onPickLocation)
    }
}

@Composable
private fun LocationCard(server: Server?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = server?.flag ?: "🌐", fontSize = 32.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server?.name ?: "Sin ubicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when {
                        server == null -> "Elige una ubicación"
                        server.isConfigured -> server.country
                        else -> "${server.country} · sin servidor configurado"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (server != null && !server.isConfigured) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Cambiar",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun formatElapsed(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s)
}
