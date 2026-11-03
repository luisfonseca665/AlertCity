package com.example.alertcity.data.remote.model

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("myActiveReportsCount") val myActiveReportsCount: Int,
    @SerializedName("mySolvedReportsCount") val mySolvedReportsCount: Int,
    @SerializedName("myReports") val myReports: List<ReportResponse>,
    @SerializedName("allReports") val allReports: List<ReportResponse>
)
