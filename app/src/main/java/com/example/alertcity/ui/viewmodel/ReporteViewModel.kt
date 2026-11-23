package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.remote.NetworkMonitor
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListaReportesUiState(
    val reportes: List<Reporte> = emptyList(),
    val reportesFiltrados: List<Reporte>? = null,
    val totalReportes: Int = 0,
    val totalUrgentes: Int = 0,
    val misActivosCount: Int? = null,
    val misResueltosCount: Int? = null,
    val filtroCategoria: String? = null,
    val filtroEstado: String? = null,
    val filtroSoloUrgentes: Boolean = false,
    val cargando: Boolean = false,
    val sincronizando: Boolean = false,
    val isOnline: Boolean = true,
    val pendingSyncCount: Int = 0,
    val mensajeError: String? = null,
    val actualizandoEstado: Boolean = false,
    val mensajeEstadoReporte: String? = null
) {
    val listaVisible: List<Reporte>
        get() = reportesFiltrados ?: reportes

    val tieneFiltrosActivos: Boolean
        get() = filtroCategoria != null || filtroEstado != null || filtroSoloUrgentes
}

class ReporteViewModel(
    private val repository: ReporteRepository,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ListaReportesUiState(isOnline = networkMonitor?.isOnline?.value ?: true)
    )
    val uiState: StateFlow<ListaReportesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllReports().collect { lista ->
                _uiState.update {
                    it.copy(
                        reportes = lista,
                        totalReportes = lista.size,
                        totalUrgentes = lista.count { reporte -> reporte.esUrgente }
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.observePendingSyncCount().collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }

        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                var wasOnline = monitor.isOnline.value
                monitor.isOnline.collect { online ->
                    _uiState.update { it.copy(isOnline = online) }
                    if (!wasOnline && online && _uiState.value.pendingSyncCount > 0) {
                        sincronizarPendientes()
                    }
                    wasOnline = online
                }
            }
        }
    }

    fun refrescar() {
        if (_uiState.value.cargando) return
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeError = null) }
            val resultado = repository.refreshDashboard()
            resultado.fold(
                onSuccess = { dashboard ->
                    _uiState.update { actual ->
                        actual.copy(
                            cargando = false,
                            misActivosCount = dashboard.myActiveReportsCount,
                            misResueltosCount = dashboard.mySolvedReportsCount,
                            mensajeError = null
                        )
                    }
                    if (_uiState.value.tieneFiltrosActivos) {
                        aplicarFiltrosRemotos()
                    }
                },
                onFailure = { error ->
                    _uiState.update { actual ->
                        actual.copy(
                            cargando = false,
                            mensajeError = error.localizedMessage
                        )
                    }
                }
            )
        }
    }

    fun sincronizarPendientes() {
        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true, mensajeError = null) }
            val syncResult = repository.syncPendingReports()
            syncResult.fold(
                onSuccess = { synced ->
                    _uiState.update {
                        it.copy(
                            sincronizando = false,
                            mensajeEstadoReporte = if (synced > 0) {
                                "$synced reporte(s) sincronizado(s) con el servidor"
                            } else {
                                it.mensajeEstadoReporte
                            }
                        )
                    }
                    refrescar()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            sincronizando = false,
                            mensajeError = error.localizedMessage ?: "No se pudieron sincronizar los reportes pendientes"
                        )
                    }
                }
            )
        }
    }

    fun onFiltroCategoriaChange(categoria: String?) {
        _uiState.update { it.copy(filtroCategoria = categoria) }
        aplicarFiltrosRemotos()
    }

    fun onFiltroEstadoChange(estado: String?) {
        _uiState.update { it.copy(filtroEstado = estado) }
        aplicarFiltrosRemotos()
    }

    fun onFiltroUrgentesChange(soloUrgentes: Boolean) {
        _uiState.update { it.copy(filtroSoloUrgentes = soloUrgentes) }
        aplicarFiltrosRemotos()
    }

    fun limpiarFiltros() {
        _uiState.update {
            it.copy(
                filtroCategoria = null,
                filtroEstado = null,
                filtroSoloUrgentes = false,
                reportesFiltrados = null,
                mensajeError = null
            )
        }
    }

    private fun aplicarFiltrosRemotos() {
        val estado = _uiState.value
        if (!estado.tieneFiltrosActivos) {
            _uiState.update { it.copy(reportesFiltrados = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeError = null) }
            val result = repository.getFilteredReports(
                category = estado.filtroCategoria,
                state = estado.filtroEstado,
                urgent = if (estado.filtroSoloUrgentes) true else null
            )
            result.fold(
                onSuccess = { lista ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            reportesFiltrados = lista
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeError = error.localizedMessage
                        )
                    }
                }
            )
        }
    }

    fun actualizarEstadoReporte(reportId: String, nuevoEstado: EstadoReporte) {
        viewModelScope.launch {
            _uiState.update { it.copy(actualizandoEstado = true, mensajeEstadoReporte = null) }
            val resultado = repository.updateReportState(reportId, nuevoEstado)
            resultado.fold(
                onSuccess = { actualizado ->
                    _uiState.update { actual ->
                        actual.copy(
                            actualizandoEstado = false,
                            reportesFiltrados = actual.reportesFiltrados?.map {
                                if (it.idReporte == actualizado.idReporte) actualizado else it
                            },
                            mensajeEstadoReporte = "Estado actualizado a ${actualizado.estado.etiqueta}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { actual ->
                        actual.copy(
                            actualizandoEstado = false,
                            mensajeEstadoReporte = error.localizedMessage ?: "No se pudo actualizar el estado"
                        )
                    }
                }
            )
        }
    }

    fun limpiarMensajeEstadoReporte() {
        _uiState.update { it.copy(mensajeEstadoReporte = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                ReporteViewModel(
                    repository = app.reporteRepository,
                    networkMonitor = app.networkMonitor
                )
            }
        }
    }
}
