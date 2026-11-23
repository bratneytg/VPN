package com.bratneytg.worldvpn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    devicePublicKey: String,
    importMessage: String?,
    onImport: (String) -> Unit,
    onClearImported: () -> Unit,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Clave del dispositivo ---
            SectionCard(
                title = "Clave pública de este móvil",
                description = "Cada servidor necesita conocer esta clave para aceptar tu conexión. " +
                    "Pásala al script server/setup-server.sh al crear un servidor.",
            ) {
                SelectionContainer {
                    Text(
                        text = devicePublicKey,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { clipboard.setText(AnnotatedString(devicePublicKey)) }) {
                    Text("Copiar clave")
                }
            }

            // --- Importar servidores ---
            SectionCard(
                title = "Servidores",
                description = "Pega aquí el JSON que imprime setup-server.sh (o una lista completa) " +
                    "para activar una ubicación sin recompilar la app.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        importText = clipboard.getText()?.text.orEmpty()
                        showImport = true
                    }) {
                        Text("Importar")
                    }
                    OutlinedButton(onClick = onClearImported) {
                        Text("Borrar importados")
                    }
                }
                if (importMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = importMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // --- Privacidad ---
            SectionCard(
                title = "Cómo funciona",
                description = "El tráfico de tu móvil viaja cifrado con WireGuard hasta TU servidor en la " +
                    "ubicación elegida y sale a internet desde allí. La app no tiene servidores propios, " +
                    "no usa analíticas y no guarda registros. Tu clave privada nunca sale del móvil.",
            ) {}
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Importar servidores") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = { Text("{ \"id\": \"de-fra\", \"endpoint\": \"…\", \"publicKey\": \"…\" }") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onImport(importText)
                    showImport = false
                }) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImport = false
                    onDismissMessage()
                }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
