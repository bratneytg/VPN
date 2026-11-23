package com.bratneytg.worldvpn

import org.json.JSONObject

/**
 * Una ubicación (servidor WireGuard). Una ubicación está "configurada" cuando tiene
 * endpoint (IP o dominio) y clave pública del servidor.
 */
data class Server(
    val id: String,
    val name: String,
    val country: String,
    val flag: String,
    val region: String,
    val endpoint: String,
    val port: Int,
    val publicKey: String,
    val clientAddress: String,
    val dns: String,
) {
    val isConfigured: Boolean
        get() = endpoint.isNotBlank() && publicKey.isNotBlank()

    companion object {
        const val DEFAULT_PORT = 51820
        const val DEFAULT_CLIENT_ADDRESS = "10.66.66.2/32"
        const val DEFAULT_DNS = "1.1.1.1, 1.0.0.1"

        fun fromJson(o: JSONObject): Server? {
            val id = o.optString("id").trim()
            if (id.isEmpty()) return null
            return Server(
                id = id,
                name = o.optString("name").ifBlank { id },
                country = o.optString("country"),
                flag = o.optString("flag").ifBlank { "🌐" },
                region = o.optString("region").ifBlank { "Otras" },
                endpoint = o.optString("endpoint").trim(),
                port = o.optInt("port", DEFAULT_PORT),
                publicKey = o.optString("publicKey").trim(),
                clientAddress = o.optString("clientAddress").ifBlank { DEFAULT_CLIENT_ADDRESS },
                dns = o.optString("dns").ifBlank { DEFAULT_DNS },
            )
        }
    }
}
