package com.bratneytg.worldvpn

import android.content.Context
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

sealed interface VpnState {
    object Disconnected : VpnState
    object Connecting : VpnState
    data class Connected(val since: Long, val serverId: String) : VpnState
    object Disconnecting : VpnState
    data class Error(val message: String) : VpnState
}

/**
 * Singleton que vive lo mismo que el proceso: así la VPN sigue activa aunque se destruya la Activity.
 * Usa el backend userspace de WireGuard (GoBackend), que funciona sin root a través de VpnService.
 */
object VpnController {

    private const val TUNNEL_NAME = "worldvpn" // máx. 15 caracteres, [a-zA-Z0-9_=+.-]

    private val _state = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    private val mutex = Mutex()
    private var backend: GoBackend? = null

    private val tunnel = object : Tunnel {
        override fun getName(): String = TUNNEL_NAME

        override fun onStateChange(newState: Tunnel.State) {
            // El sistema puede tumbar la VPN (otra app de VPN, revocación de permiso...).
            if (newState == Tunnel.State.DOWN && _state.value is VpnState.Connected) {
                _state.value = VpnState.Disconnected
            }
        }
    }

    @Synchronized
    private fun ensureBackend(context: Context): GoBackend =
        backend ?: GoBackend(context.applicationContext).also { backend = it }

    suspend fun connect(context: Context, server: Server, privateKey: String) {
        mutex.withLock {
            _state.value = VpnState.Connecting
            try {
                val config = buildConfig(server, privateKey)
                withContext(Dispatchers.IO) {
                    ensureBackend(context).setState(tunnel, Tunnel.State.UP, config)
                }
                _state.value = VpnState.Connected(System.currentTimeMillis(), server.id)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _state.value = VpnState.Error(describe(t))
            }
        }
    }

    suspend fun disconnect(context: Context) {
        mutex.withLock {
            _state.value = VpnState.Disconnecting
            try {
                withContext(Dispatchers.IO) {
                    ensureBackend(context).setState(tunnel, Tunnel.State.DOWN, null)
                }
                _state.value = VpnState.Disconnected
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _state.value = VpnState.Error(describe(t))
            }
        }
    }

    private fun buildConfig(server: Server, privateKey: String): Config {
        val host = server.endpoint.trim()
        // IPv6 literal sin corchetes -> añadirlos
        val endpoint = if (host.contains(':') && !host.startsWith("[")) {
            "[$host]:${server.port}"
        } else {
            "$host:${server.port}"
        }
        val text = buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = ${server.clientAddress}")
            appendLine("DNS = ${server.dns}")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${server.publicKey}")
            appendLine("Endpoint = $endpoint")
            appendLine("AllowedIPs = 0.0.0.0/0, ::/0")
            appendLine("PersistentKeepalive = 25")
        }
        return Config.parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
    }

    private fun describe(t: Throwable): String = when {
        t is BackendException && t.reason == BackendException.Reason.VPN_NOT_AUTHORIZED ->
            "Android no ha concedido el permiso de VPN."
        t is BadConfigException ->
            "La configuración del servidor no es válida. Revisa el endpoint y la clave pública."
        t is UnsatisfiedLinkError ->
            "Este dispositivo no es compatible con el túnel WireGuard."
        else -> t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
    }
}
