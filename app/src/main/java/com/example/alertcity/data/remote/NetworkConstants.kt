package com.example.alertcity.data.remote

import android.content.Context
import android.content.SharedPreferences

object NetworkConstants {
    // Dominio en la nube de Microsoft Azure App Service (por defecto)
    const val DEFAULT_HOST_IP: String = "alertcity-drf9a3hbfzd2ddae.westus3-01.azurewebsites.net"
    const val DEFAULT_PORT: Int = 443

    private const val PREFS_NAME = "alertcity_network_prefs_v2"
    private const val KEY_HOST_IP = "host_ip"
    private const val KEY_PORT = "host_port"

    private var prefs: SharedPreferences? = null

    @Volatile
    var hostIp: String = DEFAULT_HOST_IP
        private set

    @Volatile
    var port: Int = DEFAULT_PORT
        private set

    fun isCloudHost(host: String = hostIp, targetPort: Int = port): Boolean {
        return host.endsWith(".azurewebsites.net", ignoreCase = true) || targetPort == 443
    }

    val BASE_URL: String
        get() = if (isCloudHost()) {
            "https://$hostIp/"
        } else {
            "http://$hostIp:$port/"
        }

    // id del usuario por defecto (fallback offline)
    const val DEMO_USER_ID: String = "11111111-1111-4111-a111-111111111111"

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        hostIp = sp.getString(KEY_HOST_IP, DEFAULT_HOST_IP)?.trim().takeUnless { it.isNullOrBlank() } ?: DEFAULT_HOST_IP
        port = sp.getInt(KEY_PORT, DEFAULT_PORT).takeIf { it in 1..65535 } ?: DEFAULT_PORT
    }

    fun resolveMediaUrl(imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        val clean = imageUrl.trim()
        if (clean.startsWith("file://") || clean.startsWith("content://")) {
            return clean
        }
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            // Si el backend detrás de un proxy generó http://localhost o http://127.0.0.1, reemplazar por BASE_URL actual
            val localRegex = Regex("^https?://(localhost|127\\.0\\.0\\.1|10\\.0\\.2\\.2)(:\\d+)?/", RegexOption.IGNORE_CASE)
            if (localRegex.containsMatchIn(clean)) {
                return clean.replaceFirst(localRegex, BASE_URL)
            }
            // Si es el host de Azure o el host actual en la nube pero vino con http://, elevar a https://
            if (clean.startsWith("http://") && (clean.contains(".azurewebsites.net", ignoreCase = true) || isCloudHost())) {
                return "https://" + clean.removePrefix("http://")
            }
            return clean
        }
        return BASE_URL.trimEnd('/') + "/" + clean.trimStart('/')
    }
}
