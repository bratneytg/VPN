package com.bratneytg.worldvpn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ServerRepository(application)

    private val _servers = MutableStateFlow(repo.loadServers())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val _selectedId = MutableStateFlow(repo.selectedId(_servers.value))
    val selectedId: StateFlow<String> = _selectedId.asStateFlow()

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    val state: StateFlow<VpnState> = VpnController.state

    /** Clave pública de este móvil: hay que añadirla como "peer" en cada servidor. */
    val devicePublicKey: String = repo.publicKey()

    fun selectedServer(): Server? =
        _servers.value.firstOrNull { it.id == _selectedId.value }

    fun select(server: Server) {
        repo.saveSelected(server.id)
        _selectedId.value = server.id
        // Si ya hay una conexión activa, cambiar de ubicación = reconectar a la nueva
        val current = VpnController.state.value
        if ((current is VpnState.Connected || current is VpnState.Connecting) && server.isConfigured) {
            connect()
        }
    }

    fun connect() {
        val server = selectedServer() ?: return
        if (!server.isConfigured) return
        val privateKey = repo.privateKey()
        viewModelScope.launch {
            VpnController.connect(getApplication(), server, privateKey)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            VpnController.disconnect(getApplication())
        }
    }

    fun importServers(text: String) {
        _importMessage.value = try {
            val count = repo.importServers(text)
            _servers.value = repo.loadServers()
            _selectedId.value = repo.selectedId(_servers.value)
            "Se importaron $count ubicación(es)."
        } catch (e: Exception) {
            "No se pudo importar: ${e.message ?: "error desconocido"}"
        }
    }

    fun clearImported() {
        repo.clearImported()
        _servers.value = repo.loadServers()
        _selectedId.value = repo.selectedId(_servers.value)
        _importMessage.value = "Se han borrado los servidores importados."
    }

    fun dismissImportMessage() {
        _importMessage.value = null
    }
}
