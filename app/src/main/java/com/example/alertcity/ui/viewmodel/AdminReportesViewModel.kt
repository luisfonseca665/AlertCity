package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import com.example.alertcity.domain.util.LATITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.LONGITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.MarcadorReporte
import com.example.alertcity.domain.util.construirMarcadoresReportes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminReportesUiState(
    val todosLosReportes: List<Reporte> = emptyList(),
    val reportesFiltrados: List<Reporte> = emptyList(),
    val totalReportes: Int = 0,
    val totalEnviados: Int = 0,
    val totalEnRevision: Int = 0,
    val totalEnProceso: Int = 0,
    val totalResueltos: Int = 0,
    val totalUrgentesActivos: Int = 0,
    val busqueda: String = "",
    val filtroEstado: EstadoReporte? = null,
    val soloUrgentes: Boolean = false,
    val mapaFiltroEstado: EstadoReporte? = null,
    val mapaSoloUrgentes: Boolean = false,
    val marcadoresMapa: List<MarcadorReporte> = emptyList(),
    val reporteSeleccionadoMapa: Reporte? = null,
    val latitudCentro: Double = LATITUD_CIUDAD_DEFECTO,
    val longitudCentro: Double = LONGITUD_CIUDAD_DEFECTO,
    val zoomMapa: Double = 15.0,
    val enfocarTrigger: Int = 0,
    val cargando: Boolean = false
)

class AdminReportesViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReportesUiState())
    val uiState: StateFlow<AdminReportesUiState> = _uiState.asStateFlow()

    init {
        refrescar()
        viewModelScope.launch {
            repository.getAllReports().collect { lista ->
                _uiState.update { actual ->
                    val estadoBase = actual.copy(todosLosReportes = lista)
                    val recalculado = recalcularEstado(estadoBase)
                    val primerMarcador = recalculado.marcadoresMapa.firstOrNull()
                    val mantenerCamara = actual.enfocarTrigger > 0 && actual.marcadoresMapa.isNotEmpty()

                    recalculado.copy(
                        latitudCentro = if (mantenerCamara) actual.latitudCentro else (primerMarcador?.latitud ?: LATITUD_CIUDAD_DEFECTO),
                        longitudCentro = if (mantenerCamara) actual.longitudCentro else (primerMarcador?.longitud ?: LONGITUD_CIUDAD_DEFECTO),
                        enfocarTrigger = if (!mantenerCamara && primerMarcador != null) actual.enfocarTrigger + 1 else actual.enfocarTrigger
                    )
                }
            }
        }
    }

    fun refrescar() {
        if (_uiState.value.cargando) return
        viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            _uiState.update { it.copy(cargando = true) }
            repository.refreshReports()
            val transcurrido = System.currentTimeMillis() - inicio
            if (transcurrido < 350L) {
                delay(350L - transcurrido)
            }
            _uiState.update { it.copy(cargando = false) }
        }
    }

    fun onBusquedaChange(nuevaBusqueda: String) {
        _uiState.update { recalcularEstado(it.copy(busqueda = nuevaBusqueda)) }
    }

    fun onFiltroEstadoChange(estado: EstadoReporte?) {
        _uiState.update { actual ->
            val nuevoFiltro = if (actual.filtroEstado == estado) null else estado
            recalcularEstado(actual.copy(filtroEstado = nuevoFiltro))
        }
    }

    fun onSoloUrgentesChange(activo: Boolean) {
        _uiState.update { recalcularEstado(it.copy(soloUrgentes = activo)) }
    }

    fun limpiarFiltrosDashboard() {
        _uiState.update {
            recalcularEstado(
                it.copy(
                    busqueda = "",
                    filtroEstado = null,
                    soloUrgentes = false
                )
            )
        }
    }

    fun onMapaFiltroEstadoChange(estado: EstadoReporte?) {
        _uiState.update { actual ->
            val nuevoFiltro = if (actual.mapaFiltroEstado == estado) null else estado
            recalcularEstado(actual.copy(mapaFiltroEstado = nuevoFiltro))
        }
    }

    fun onMapaSoloUrgentesChange(activo: Boolean) {
        _uiState.update { recalcularEstado(it.copy(mapaSoloUrgentes = activo)) }
    }

    fun seleccionarReporteEnMapa(idReporte: String) {
        val marcador = _uiState.value.marcadoresMapa.find { it.reporte.idReporte == idReporte } ?: return
        _uiState.update {
            it.copy(
                reporteSeleccionadoMapa = marcador.reporte,
                latitudCentro = marcador.latitud,
                longitudCentro = marcador.longitud,
                zoomMapa = 17.0,
                enfocarTrigger = it.enfocarTrigger + 1
            )
        }
    }

    fun limpiarSeleccionMapa() {
        _uiState.update { it.copy(reporteSeleccionadoMapa = null) }
    }

    fun centrarVistaGeneralMapa() {
        val primerMarcador = _uiState.value.marcadoresMapa.firstOrNull()
        _uiState.update {
            it.copy(
                reporteSeleccionadoMapa = null,
                latitudCentro = primerMarcador?.latitud ?: LATITUD_CIUDAD_DEFECTO,
                longitudCentro = primerMarcador?.longitud ?: LONGITUD_CIUDAD_DEFECTO,
                zoomMapa = 15.0,
                enfocarTrigger = it.enfocarTrigger + 1
            )
        }
    }

    fun cambiarEstadoReporte(reporte: Reporte, nuevoEstado: EstadoReporte) {
        if (reporte.estado == nuevoEstado) return
        viewModelScope.launch {
            repository.updateReport(reporte.copy(estado = nuevoEstado))
        }
    }

    fun avanzarEstadoReporte(reporte: Reporte) {
        val siguiente = when (reporte.estado) {
            EstadoReporte.ENVIADO -> EstadoReporte.REVISION
            EstadoReporte.REVISION -> EstadoReporte.EN_PROCESO
            EstadoReporte.EN_PROCESO -> EstadoReporte.RESUELTO
            EstadoReporte.RESUELTO -> EstadoReporte.RESUELTO
        }
        cambiarEstadoReporte(reporte, siguiente)
    }

    fun cambiarUrgenciaReporte(reporte: Reporte, esUrgente: Boolean) {
        if (reporte.esUrgente == esUrgente) return
        viewModelScope.launch {
            repository.updateReport(reporte.copy(esUrgente = esUrgente))
        }
    }

    fun eliminarReporte(idReporte: String, onEliminado: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteReport(idReporte)
            onEliminado()
        }
    }

    private fun recalcularEstado(estado: AdminReportesUiState): AdminReportesUiState {
        val lista = estado.todosLosReportes
        val consulta = estado.busqueda.trim().lowercase()

        val filtradosLista = lista.filter { reporte ->
            val cumpleEstado = estado.filtroEstado == null || reporte.estado == estado.filtroEstado
            val cumpleUrgencia = !estado.soloUrgentes || reporte.esUrgente
            val cumpleBusqueda = consulta.isEmpty() ||
                reporte.titulo.lowercase().contains(consulta) ||
                reporte.categoria.lowercase().contains(consulta) ||
                reporte.descripcion.lowercase().contains(consulta)
            cumpleEstado && cumpleUrgencia && cumpleBusqueda
        }

        val filtradosMapa = lista.filter { reporte ->
            val cumpleEstado = estado.mapaFiltroEstado == null || reporte.estado == estado.mapaFiltroEstado
            val cumpleUrgencia = !estado.mapaSoloUrgentes || reporte.esUrgente
            cumpleEstado && cumpleUrgencia
        }

        val marcadores = construirMarcadoresReportes(filtradosMapa)
        val seleccionadoMapaActualizado = estado.reporteSeleccionadoMapa?.let { sel ->
            filtradosMapa.find { it.idReporte == sel.idReporte }
        }

        return estado.copy(
            reportesFiltrados = filtradosLista,
            totalReportes = lista.size,
            totalEnviados = lista.count { it.estado == EstadoReporte.ENVIADO },
            totalEnRevision = lista.count { it.estado == EstadoReporte.REVISION },
            totalEnProceso = lista.count { it.estado == EstadoReporte.EN_PROCESO },
            totalResueltos = lista.count { it.estado == EstadoReporte.RESUELTO },
            totalUrgentesActivos = lista.count { it.esUrgente && it.estado != EstadoReporte.RESUELTO },
            marcadoresMapa = marcadores,
            reporteSeleccionadoMapa = seleccionadoMapaActualizado
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                AdminReportesViewModel(app.reporteRepository)
            }
        }
    }
}
