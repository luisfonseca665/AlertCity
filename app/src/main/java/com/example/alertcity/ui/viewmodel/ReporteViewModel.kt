package com.example.alertcity.ui.viewmodel

import android.text.Editable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.repository.RoomReporteRepository
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.Call

class ReporteViewModel(
    private val repository: ReporteRepository
) : ViewModel() {
    val listaReportes = repository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun crearReporte(titulo: String, desc: String, cat: String, urgente: Boolean, hora: String, fotoUri: String?, ubicacion: String?) {
        viewModelScope.launch {
            val nuevo = Reporte(
                titulo = titulo,
                descripcion = desc,
                categoria = cat,
                esUrgente = urgente,
                hora = hora,
                fotoUri = fotoUri,
                ubicacion = ubicacion
            )
            repository.saveReport(nuevo)
        }
    }
    fun obtenerReportePorId(id: String): Reporte? {
        return listaReportes.value.find { it.idReporte == id }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                ReporteViewModel(app.reporteRepository)
            }
        }
    }
}