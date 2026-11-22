package com.example.alertcity.data.remote

import android.content.Context
import android.content.SharedPreferences

object NetworkConstants {
    // IP de la computadora en la red Wi-Fi local (por defecto)
    const val DEFAULT_HOST_IP: String = "192.168.1.5"
    const val DEFAULT_PORT: Int = 8080

    private const val PREFS_NAME = "alertcity_network_prefs"
    private const val KEY_HOST_IP = "host_ip"
    private const val KEY_PORT = "host_port"

    private var prefs: SharedPreferences? = null

    @Volatile
    var hostIp: String = DEFAULT_HOST_IP
        private set

    @Volatile
    var port: Int = DEFAULT_PORT
        private set

    val BASE_URL: String
        get() = "http://$hostIp:$port/"

    // id del usuario por defecto (fallback offline)
    const val DEMO_USER_ID: String = "11111111-1111-4111-a111-111111111111"

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        hostIp = sp.getString(KEY_HOST_IP, DEFAULT_HOST_IP)?.trim().takeUnless { it.isNullOrBlank() } ?: DEFAULT_HOST_IP
        port = sp.getInt(KEY_PORT, DEFAULT_PORT).takeIf { it in 1..65535 } ?: DEFAULT_PORT
    }

    fun updateServer(newHostIp: String, newPort: Int = DEFAULT_PORT) {
        val cleanHost = newHostIp
            .trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
            .ifBlank { DEFAULT_HOST_IP }
        val cleanPort = newPort.takeIf { it in 1..65535 } ?: DEFAULT_PORT

        hostIp = cleanHost
        port = cleanPort

        prefs?.edit()
            ?.putString(KEY_HOST_IP, cleanHost)
            ?.putInt(KEY_PORT, cleanPort)
            ?.apply()
    }

    fun resolveMediaUrl(imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || imageUrl.startsWith("file://") || imageUrl.startsWith("content://")) {
            return imageUrl
        }
        return BASE_URL.trimEnd('/') + "/" + imageUrl.trimStart('/')
    }
}
