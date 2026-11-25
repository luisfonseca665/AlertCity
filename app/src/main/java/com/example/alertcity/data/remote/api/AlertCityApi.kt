package com.example.alertcity.data.remote.api

import com.example.alertcity.data.remote.model.AuthResponse
import com.example.alertcity.data.remote.model.CreateReportRequest
import com.example.alertcity.data.remote.model.DashboardResponse
import com.example.alertcity.data.remote.model.LoginRequest
import com.example.alertcity.data.remote.model.PageResponse
import com.example.alertcity.data.remote.model.RefreshTokenRequest
import com.example.alertcity.data.remote.model.RegisterRequest
import com.example.alertcity.data.remote.model.ReportResponse
import com.example.alertcity.data.remote.model.UpdateReportStateRequest
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AlertCityApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<AuthResponse>

    // Llamada síncrona usada exclusivamente por OkHttp TokenAuthenticator
    @POST("api/v1/auth/refresh")
    fun refreshSync(@Body request: RefreshTokenRequest): Call<AuthResponse>

    // Carga el estado inicial del Dashboard en un solo llamado (el backend deduce userId del JWT)
    @GET("api/v1/dashboard")
    suspend fun getDashboard(
        @Query("userId") userId: String? = null
    ): Response<DashboardResponse>

    // Consulta reportes con filtros dinámicos opcionales
    @GET("api/v1/reports")
    suspend fun getReports(
        @Query("userId") userId: String? = null,
        @Query("category") category: String? = null,
        @Query("state") state: String? = null,
        @Query("urgent") urgent: Boolean? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null
    ): Response<List<ReportResponse>>

    // Consulta reportes paginados
    @GET("api/v1/reports/page")
    suspend fun getReportsPage(
        @Query("userId") userId: String? = null,
        @Query("category") category: String? = null,
        @Query("state") state: String? = null,
        @Query("urgent") urgent: Boolean? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "dateTime,desc"
    ): Response<PageResponse<ReportResponse>>

    // Búsqueda geoespacial PostGIS por radio en kilómetros
    @GET("api/v1/reports/nearby")
    suspend fun getNearbyReports(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double = 3.0
    ): Response<List<ReportResponse>>

    @GET("api/v1/reports/{id}")
    suspend fun getReportById(
        @Path("id") reportId: String
    ): Response<ReportResponse>

    // Registra un nuevo reporte ciudadano en el Backend y BD PostgreSQL/PostGIS
    @POST("api/v1/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): Response<ReportResponse>

    // Actualiza el estado de un reporte (solo ADMIN o rol operativo correspondiente)
    @PATCH("api/v1/reports/{id}/state")
    suspend fun updateReportState(
        @Path("id") reportId: String,
        @Body request: UpdateReportStateRequest
    ): Response<ReportResponse>

    // Sube evidencia fotográfica real (multipart/form-data, campo "file")
    @Multipart
    @POST("api/v1/reports/{id}/media")
    suspend fun uploadReportMedia(
        @Path("id") reportId: String,
        @Part file: MultipartBody.Part
    ): Response<ReportResponse>

    // Elimina un reporte en el Backend y BD PostgreSQL/PostGIS
    @DELETE("api/v1/reports/{id}")
    suspend fun deleteReport(
        @Path("id") reportId: String
    ): Response<Unit>
}
