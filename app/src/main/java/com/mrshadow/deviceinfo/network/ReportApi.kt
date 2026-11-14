package com.mrshadow.deviceinfo.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class DeviceReportRequest(val report: String)
data class DeviceReportResponse(val success: Boolean)

interface ReportApi {
    @POST("v1/device-reports")
    suspend fun submit(@Body request: DeviceReportRequest): Response<DeviceReportResponse>
}
