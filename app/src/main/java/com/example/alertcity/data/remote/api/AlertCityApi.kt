package com.example.alertcity.data.remote.api

import com.example.alertcity.data.remote.model.CreateReportRequest
import com.example.alertcity.data.remote.model.DashboardResponse
import com.example.alertcity.data.remote.model.ReportResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AlertCityApi {

    /**
     * Carga el estado inicial del Dashboard en un solo llamado (BFF).
     */
    @GET("api/v1/dashboard")
    suspend fun getDashboard(
        @Query("userId") userId: String? = null
    ): Response<DashboardResponse>

    /**
     * Consulta reportes generales o de un usuario específico.
     */
    @GET("api/v1/reports")
    suspend fun getReports(
        @Query("userId") userId: String? = null
    ): Response<List<ReportResponse>>

    /**
     * Registra un nuevo reporte ciudadano en el Backend y BD PostgreSQL/PostGIS.
     */
    @POST("api/v1/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): Response<ReportResponse>
}
