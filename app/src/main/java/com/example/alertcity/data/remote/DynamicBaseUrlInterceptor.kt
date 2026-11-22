package com.example.alertcity.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Reescribe dinámicamente el host y puerto en cada petición saliente usando
 * los valores actuales de NetworkConstants, permitiendo cambiar la IP de la
 * red local en tiempo real sin recompilar ni reiniciar la app.
 */
class DynamicBaseUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .scheme("http")
            .host(NetworkConstants.hostIp)
            .port(NetworkConstants.port)
            .build()

        val updatedRequest = original.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(updatedRequest)
    }
}
