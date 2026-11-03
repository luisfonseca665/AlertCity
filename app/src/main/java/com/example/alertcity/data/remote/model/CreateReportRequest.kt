package com.example.alertcity.data.remote.model

import com.google.gson.annotations.SerializedName

data class CreateReportRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("urgent") val urgent: Boolean,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("addressReference") val addressReference: String?,
    @SerializedName("state") val state: String = "ENVIADO"
)
