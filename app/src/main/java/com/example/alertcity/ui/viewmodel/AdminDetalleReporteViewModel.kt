package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDetalleReporteUiState(
    val reporteId: String? = null,
    val reporte: Reporte? = null,
    val rolLegible: String = "Operador",
    val categoriaAsignadaUi: String? = null,
    val accesoDenegadoPorRol: Boolean = false,
    val mostrarConfirmacionEliminar: Boolean = false,
    val mostrarPreviewImagen: Boolean = false,
    val mostrarPreviewMapa: Boolean = false,
    val actualizando: Boolean = false
)

class AdminDetalleReporteViewModel(
    private val repository: ReporteRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDetalleReporteUiState())
    val uiState: StateFlow<AdminDetalleReporteUiState> = _uiState.asStateFlow()

    private var todosLosReportes: List<Reporte> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllReports(),
                sessionManager.sessionFlow
            ) { lista, session ->
                lista to session
            }.collect { (lista, session) ->
                todosLosReportes = lista
                actualizarReporteVisible( _uiState.value.reporteId, lista, session)
            }
        }
    }

    fun cargarReporte(reporteId: String?) {
        actualizarReporteVisible(reporteId, todosLosReportes, sessionManager.currentSession)
    }

    private fun actualizarReporteVisible(
        reporteId: String?,
        lista: List<Reporte>,
        session: com.example.alertcity.data.local.UserSession?
    ) {
        val encontrado = reporteId?.let { id -> lista.find { it.idReporte == id } }
        val permitido = if (encontrado != null && session != null) {
            session.puedeVerYGeestionarReporte(encontrado)
        } else {
            encontrado != null
        }

        _uiState.update { actual ->
            actual.copy(
                reporteId = reporteId,
                reporte = if (permitido) encontrado else null,
                rolLegible = session?.rolLegible ?: "Operador",
                categoriaAsignadaUi = session?.categoriaUiAsignada,
                accesoDenegadoPorRol = encontrado != null && !permitido
            )
        }
    }

    fun onMostrarConfirmacionEliminarChange(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarConfirmacionEliminar = mostrar) }
    }

    fun onMostrarPreviewImagenChange(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarPreviewImagen = mostrar) }
    }

    fun onMostrarPreviewMapaChange(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarPreviewMapa = mostrar) }
    }

    fun cambiarEstado(nuevoEstado: EstadoReporte) {
        val reporteActual = _uiState.value.reporte ?: return
        if (reporteActual.estado == nuevoEstado) return
        val session = sessionManager.currentSession
        if (session != null && !session.puedeVerYGeestionarReporte(reporteActual)) return

        viewModelScope.launch {
            _uiState.update { it.copy(actualizando = true) }
            repository.updateReportState(reporteActual.idReporte, nuevoEstado)
            _uiState.update { it.copy(actualizando = false) }
        }
    }

    fun confirmarEliminarReporte(onEliminado: () -> Unit) {
        val reporteActual = _uiState.value.reporte ?: return
        val session = sessionManager.currentSession
        if (session != null && !session.puedeVerYGeestionarReporte(reporteActual)) return

        viewModelScope.launch {
            _uiState.update { it.copy(mostrarConfirmacionEliminar = false) }
            repository.deleteReport(reporteActual.idReporte)
            onEliminado()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                AdminDetalleReporteViewModel(
                    repository = app.reporteRepository,
                    sessionManager = app.sessionManager
                )
            }
        }
    }
}
