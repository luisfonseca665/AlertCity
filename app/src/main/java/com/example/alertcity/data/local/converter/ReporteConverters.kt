package com.example.alertcity.data.local.converter

import androidx.room.TypeConverter
import com.example.alertcity.domain.model.EstadoReporte

class ReporteConverters {
    @TypeConverter
    fun fromEstadoReporte(estado: EstadoReporte): String{
        return estado.name
    }

    @TypeConverter
    fun toEstadoReporte(valor: String): EstadoReporte{
        return runCatching { EstadoReporte.valueOf(valor) }.getOrDefault(EstadoReporte.ENVIADO)
    }
}