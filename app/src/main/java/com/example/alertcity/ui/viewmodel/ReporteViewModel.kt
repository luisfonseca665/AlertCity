package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alertcity.data.repository.FakeReporteRepository
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReporteViewModel(
    private val repository: ReporteRepository = FakeReporteRepository()
) : ViewModel() {
    val listaReportes = repository.obtenerReportes()
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
            repository.guardarReporte(nuevo)
        }
    }
    fun obtenerReportePorId(id: String): Reporte? {
        return listaReportes.value.find { it.id == id }
    }
}