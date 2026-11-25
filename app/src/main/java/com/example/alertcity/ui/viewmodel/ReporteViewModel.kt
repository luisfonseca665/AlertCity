package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.mapper.ReporteMapper
import com.example.alertcity.data.remote.NetworkMonitor
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val reporteSeleccionadoId: String? = null
) {
    val listaVisible: List<Reporte>
        get() = reportesFiltrados ?: reportes

    val reporteSeleccionadoDetalle: Reporte?
        get() = listaVisible.find { it.idReporte == reporteSeleccionadoId }
            ?: listaVisible.firstOrNull()

    val tieneFiltrosActivos: Boolean
        get() = filtroCategoria != null || filtroEstado != null || filtroSoloUrgentes
}

class ReporteViewModel(
    private val repository: ReporteRepository,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ListaReportesUiState(isOnline = networkMonitor?.isOnline?.value ?: true)
    )
    val uiState: StateFlow<ListaReportesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllReports(),
                sessionManager.sessionFlow
            ) { lista, session ->
                val userId = session?.userId
                if (!userId.isNullOrBlank()) {
                    lista.filter { it.idUsuario.equals(userId, ignoreCase = true) }
                } else {
                    emptyList()
                }
            }.collect { misReportes ->
                _uiState.update { actual ->
                    val estadoActualizado = actual.copy(
                        reportes = misReportes,
                        totalReportes = misReportes.size,
                        totalUrgentes = misReportes.count { reporte -> reporte.esUrgente },
                        misActivosCount = misReportes.count { reporte -> reporte.estado != EstadoReporte.RESUELTO },
                        misResueltosCount = misReportes.count { reporte -> reporte.estado == EstadoReporte.RESUELTO }
                    )
                    estadoActualizado.copy(
                        reportesFiltrados = calcularFiltradosPropios(estadoActualizado)
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
                        val actualizado = actual.copy(
                            cargando = false,
                            misActivosCount = dashboard.myActiveReportsCount,
                            misResueltosCount = dashboard.mySolvedReportsCount,
                            mensajeError = null
                        )
                        actualizado.copy(
                            reportesFiltrados = calcularFiltradosPropios(actualizado)
                        )
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
                onSuccess = {
                    _uiState.update { it.copy(sincronizando = false) }
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
        _uiState.update { actual ->
            val nuevo = actual.copy(filtroCategoria = categoria)
            nuevo.copy(reportesFiltrados = calcularFiltradosPropios(nuevo))
        }
    }

    fun onFiltroEstadoChange(estado: String?) {
        _uiState.update { actual ->
            val nuevo = actual.copy(filtroEstado = estado)
            nuevo.copy(reportesFiltrados = calcularFiltradosPropios(nuevo))
        }
    }

    fun onFiltroUrgentesChange(soloUrgentes: Boolean) {
        _uiState.update { actual ->
            val nuevo = actual.copy(filtroSoloUrgentes = soloUrgentes)
            nuevo.copy(reportesFiltrados = calcularFiltradosPropios(nuevo))
        }
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

    fun seleccionarReporteDetalle(idReporte: String) {
        _uiState.update { it.copy(reporteSeleccionadoId = idReporte) }
    }

    private fun calcularFiltradosPropios(estado: ListaReportesUiState): List<Reporte>? {
        if (!estado.tieneFiltrosActivos) return null
        val categoriaEsperada = estado.filtroCategoria?.takeIf { it.isNotBlank() }?.trim()?.uppercase()
        val estadoEsperado = estado.filtroEstado?.takeIf { it.isNotBlank() }?.trim()?.uppercase()

        return estado.reportes.filter { reporte ->
            val cumpleCategoria = categoriaEsperada == null ||
                ReporteMapper.mapCategoryToBackendEnum(reporte.categoria) == categoriaEsperada
            val cumpleEstado = estadoEsperado == null ||
                ReporteMapper.mapDomainStateToBackendEnum(reporte.estado) == estadoEsperado
            val cumpleUrgente = !estado.filtroSoloUrgentes || reporte.esUrgente
            cumpleCategoria && cumpleEstado && cumpleUrgente
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                ReporteViewModel(
                    repository = app.reporteRepository,
                    sessionManager = app.sessionManager,
                    networkMonitor = app.networkMonitor
                )
            }
        }
    }
}
