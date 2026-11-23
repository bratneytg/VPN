package com.bratneytg.worldvpn

import android.content.Context
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import org.json.JSONArray
import org.json.JSONObject

/**
 * - Lista base de ubicaciones: assets/servers.json
 * - Ubicaciones/servidores importados por el usuario (pegando JSON): se guardan en SharedPreferences
 *   y se superponen sobre la lista base por "id".
 * - Par de claves WireGuard del dispositivo (se genera la primera vez).
 */
class ServerRepository(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- Servidores ----------

    fun loadServers(): List<Server> {
        val merged = LinkedHashMap<String, JSONObject>()
        for (o in parseObjects(readAsset())) {
            merged[o.optString("id")] = o
        }
        for (o in parseObjects(prefs.getString(KEY_IMPORTED, "[]") ?: "[]")) {
            merged[o.optString("id")] = overlay(merged[o.optString("id")], o)
        }
        return merged.values.mapNotNull { Server.fromJson(it) }
    }

    /**
     * Importa servidores desde JSON pegado por el usuario. Acepta:
     *  - {"servers":[ {...}, {...} ]}
     *  - [ {...}, {...} ]
     *  - {...}   (un solo servidor, es lo que imprime server/setup-server.sh)
     * Devuelve cuántos servidores se importaron. Lanza excepción con un mensaje legible si algo falla.
     */
    fun importServers(text: String): Int {
        val parsed: List<JSONObject> = try {
            parseObjects(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("El texto no es un JSON válido.")
        }
        val incoming = parsed.filter { it.optString("id").isNotBlank() }

        require(incoming.isNotEmpty()) { "No he encontrado ningún servidor con el campo \"id\"." }

        for (o in incoming) {
            for (field in listOf("publicKey", "clientPrivateKey")) {
                val key = o.optString(field).trim()
                if (key.isNotEmpty()) {
                    try {
                        Key.fromBase64(key)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("El campo \"$field\" de \"${o.optString("id")}\" no es una clave válida.")
                    }
                }
            }
        }

        val stored = LinkedHashMap<String, JSONObject>()
        for (o in parseObjects(prefs.getString(KEY_IMPORTED, "[]") ?: "[]")) {
            stored[o.optString("id")] = o
        }
        for (o in incoming) {
            stored[o.optString("id")] = overlay(stored[o.optString("id")], o)
        }

        val array = JSONArray()
        for (o in stored.values) array.put(o)
        prefs.edit().putString(KEY_IMPORTED, array.toString()).apply()
        return incoming.size
    }

    fun clearImported() {
        prefs.edit().remove(KEY_IMPORTED).apply()
    }

    // ---------- Selección ----------

    fun selectedId(servers: List<Server>): String {
        val saved = prefs.getString(KEY_SELECTED, null)
        if (saved != null && servers.any { it.id == saved }) return saved
        return (servers.firstOrNull { it.isConfigured } ?: servers.firstOrNull())?.id ?: ""
    }

    fun saveSelected(id: String) {
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    // ---------- Claves del dispositivo ----------

    fun privateKey(): String {
        ensureKeys()
        return prefs.getString(KEY_PRIVATE, "") ?: ""
    }

    fun publicKey(): String {
        ensureKeys()
        return prefs.getString(KEY_PUBLIC, "") ?: ""
    }

    @Synchronized
    private fun ensureKeys() {
        if (prefs.getString(KEY_PRIVATE, null).isNullOrBlank()) {
            val pair = KeyPair()
            prefs.edit()
                .putString(KEY_PRIVATE, pair.privateKey.toBase64())
                .putString(KEY_PUBLIC, pair.publicKey.toBase64())
                .apply()
        }
    }

    // ---------- Utilidades ----------

    private fun readAsset(): String =
        app.assets.open("servers.json").bufferedReader().use { it.readText() }

    private fun parseObjects(text: String): List<JSONObject> {
        val t = text.trim()
        val array: JSONArray = if (t.startsWith("[")) {
            JSONArray(t)
        } else {
            val obj = JSONObject(t)
            if (obj.has("servers")) obj.getJSONArray("servers") else JSONArray().put(obj)
        }
        val out = ArrayList<JSONObject>(array.length())
        for (i in 0 until array.length()) {
            array.optJSONObject(i)?.let { out.add(it) }
        }
        return out
    }

    /** Copia `base` y le superpone los campos de `extra`. */
    private fun overlay(base: JSONObject?, extra: JSONObject): JSONObject {
        val result = if (base != null) JSONObject(base.toString()) else JSONObject()
        val keys = extra.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            result.put(k, extra.get(k))
        }
        return result
    }

    private companion object {
        const val PREFS = "worldvpn"
        const val KEY_IMPORTED = "imported_servers"
        const val KEY_SELECTED = "selected_server"
        const val KEY_PRIVATE = "wg_private_key"
        const val KEY_PUBLIC = "wg_public_key"
    }
}
