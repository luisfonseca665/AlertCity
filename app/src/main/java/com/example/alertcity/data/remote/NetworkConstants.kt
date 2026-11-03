package com.example.alertcity.data.remote

object NetworkConstants {
    /**
     * IP de tu computadora en la red Wi-Fi local.
     * Al usar la IP real, la conexión funciona tanto desde el Emulador de Android
     * como desde un teléfono físico sin sufrir bloqueos del firewall de Hyper-V.
     */
    const val HOST_IP: String = "192.168.1.5"

    val BASE_URL: String
        get() = "http://$HOST_IP:8080/"

    /**
     * Identificador del usuario semilla predeterminado de la base de datos (Laura Sánchez).
     */
    const val DEMO_USER_ID: String = "11111111-1111-4111-a111-111111111111"
}
