package com.example.alertcity.data.remote

import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.remote.api.AlertCityApi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private var sessionManager: SessionManager? = null

    fun init(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    // Cliente ligero sin Authenticator para llamadas de refresh de token
    private val authApi: AlertCityApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(DynamicBaseUrlInterceptor())
            .addInterceptor(createLoggingInterceptor())
            .build()

        Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlertCityApi::class.java)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(DynamicBaseUrlInterceptor())

        sessionManager?.let { sm ->
            builder.addInterceptor(AuthInterceptor(sm))
            builder.authenticator(TokenAuthenticator(sm, authApi))
        }

        builder
            .addInterceptor(createLoggingInterceptor())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: AlertCityApi by lazy {
        retrofit.create(AlertCityApi::class.java)
    }

    /**
     * Verifica conectividad LAN con el backend consultando /actuator/health.
     */
    suspend fun checkHealth(host: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").substringBefore("/").substringBefore(":")
        val url = "http://$cleanHost:$port/actuator/health"
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Conexión exitosa ($url)")
                } else {
                    Result.failure(Exception("El servidor respondió HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            val detalle = e.localizedMessage ?: e.javaClass.simpleName
            Result.failure(
                Exception("No se pudo conectar a $url ($detalle).")
            )
        }
    }
}
