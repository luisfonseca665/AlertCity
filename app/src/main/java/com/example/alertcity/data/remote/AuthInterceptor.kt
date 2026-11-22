package com.example.alertcity.data.remote

import com.example.alertcity.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // No adjuntar Authorization en los endpoints públicos de autenticación
        if (path.contains("/api/v1/auth/")) {
            return chain.proceed(original)
        }

        val token = sessionManager.getAccessToken()
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }

        val authenticated = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticated)
    }
}
