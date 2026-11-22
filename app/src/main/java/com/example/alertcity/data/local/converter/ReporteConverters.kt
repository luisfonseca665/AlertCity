package com.example.alertcity.data.local.converter

import androidx.room.TypeConverter
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.SyncStatus

class ReporteConverters {
    @TypeConverter
    fun fromEstadoReporte(estado: EstadoReporte): String {
        return estado.name
    }

    @TypeConverter
    fun toEstadoReporte(valor: String): EstadoReporte {
        return runCatching { EstadoReporte.valueOf(valor) }.getOrDefault(EstadoReporte.ENVIADO)
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(valor: String): SyncStatus {
        return runCatching { SyncStatus.valueOf(valor) }.getOrDefault(SyncStatus.SYNCED)
    }
}
