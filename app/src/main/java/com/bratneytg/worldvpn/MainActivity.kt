package com.bratneytg.worldvpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bratneytg.worldvpn.ui.HomeScreen
import com.bratneytg.worldvpn.ui.LocationsScreen
import com.bratneytg.worldvpn.ui.SettingsScreen
import com.bratneytg.worldvpn.ui.WorldVpnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorldVpnTheme {
                WorldVpnApp()
            }
        }
    }
}

private enum class Screen { Home, Locations, Settings }

@Composable
fun WorldVpnApp(vm: VpnViewModel = viewModel()) {
    val context = LocalContext.current

    val servers by vm.servers.collectAsState()
    val selectedId by vm.selectedId.collectAsState()
    val state by vm.state.collectAsState()
    val importMessage by vm.importMessage.collectAsState()

    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    // Android pide confirmación la primera vez que una app quiere crear una VPN.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.connect()
    }

    val onToggle: () -> Unit = {
        when (state) {
            is VpnState.Connected -> vm.disconnect()
            is VpnState.Connecting, is VpnState.Disconnecting -> Unit
            else -> {
                val intent = VpnService.prepare(context)
                if (intent != null) permissionLauncher.launch(intent) else vm.connect()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (screen) {
            Screen.Home -> HomeScreen(
                state = state,
                server = servers.firstOrNull { it.id == selectedId },
                onToggle = onToggle,
                onPickLocation = { screen = Screen.Locations },
                onOpenSettings = { screen = Screen.Settings },
            )

            Screen.Locations -> LocationsScreen(
                servers = servers,
                selectedId = selectedId,
                onSelect = { server ->
                    vm.select(server)
                    screen = Screen.Home
                },
                onBack = { screen = Screen.Home },
            )

            Screen.Settings -> SettingsScreen(
                devicePublicKey = vm.devicePublicKey,
                importMessage = importMessage,
                onImport = vm::importServers,
                onClearImported = vm::clearImported,
                onDismissMessage = vm::dismissImportMessage,
                onOpenVpnSettings = {
                    context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                },
                onBack = { screen = Screen.Home },
            )
        }
    }
}
