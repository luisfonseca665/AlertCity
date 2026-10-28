package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import com.example.alertcity.ui.components.LATITUD_CIUDAD_DEFECTO
import com.example.alertcity.ui.components.LONGITUD_CIUDAD_DEFECTO
import com.example.alertcity.ui.components.parsearCoordenadasUbicacion
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LATITUD_POR_DEFECTO = LATITUD_CIUDAD_DEFECTO
private const val LONGITUD_POR_DEFECTO = LONGITUD_CIUDAD_DEFECTO

data class MarcadorReporte(
    val reporte: Reporte,
    val latitud: Double,
    val longitud: Double,
    val tieneUbicacionExacta: Boolean
)

data class HistoricoReportesUiState(
    val reportes: List<Reporte> = emptyList(),
    val marcadores: List<MarcadorReporte> = emptyList(),
    val reporteSeleccionado: Reporte? = null,
    val latitudCentro: Double = LATITUD_POR_DEFECTO,
    val longitudCentro: Double = LONGITUD_POR_DEFECTO,
    val zoomMapa: Double = 15.0,
    val enfocarTrigger: Int = 0
)

class HistoricoReportesViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoricoReportesUiState())
    val uiState: StateFlow<HistoricoReportesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllReports().collect { lista ->
                val marcadoresCalculados = construirMarcadores(lista)
                _uiState.update { actual ->
                    val primerMarcador = marcadoresCalculados.firstOrNull()
                    val mantenerCentro = actual.enfocarTrigger > 0 && actual.marcadores.isNotEmpty()
                    val seleccionadoActualizado = actual.reporteSeleccionado?.let { sel ->
                        lista.find { it.idReporte == sel.idReporte }
                    }
                    actual.copy(
                        reportes = lista,
                        marcadores = marcadoresCalculados,
                        reporteSeleccionado = seleccionadoActualizado,
                        latitudCentro = if (mantenerCentro) actual.latitudCentro else (primerMarcador?.latitud ?: LATITUD_POR_DEFECTO),
                        longitudCentro = if (mantenerCentro) actual.longitudCentro else (primerMarcador?.longitud ?: LONGITUD_POR_DEFECTO),
                        enfocarTrigger = if (!mantenerCentro && primerMarcador != null) actual.enfocarTrigger + 1 else actual.enfocarTrigger
                    )
                }
            }
        }
    }

    fun seleccionarReporteEnMapa(idReporte: String) {
        val marcador = _uiState.value.marcadores.find { it.reporte.idReporte == idReporte } ?: return
        _uiState.update {
            it.copy(
                reporteSeleccionado = marcador.reporte,
                latitudCentro = marcador.latitud,
                longitudCentro = marcador.longitud,
                zoomMapa = 17.0,
                enfocarTrigger = it.enfocarTrigger + 1
            )
        }
    }

    fun limpiarSeleccionMapa() {
        _uiState.update { it.copy(reporteSeleccionado = null) }
    }

    fun centrarVistaGeneral() {
        val primerMarcador = _uiState.value.marcadores.firstOrNull()
        _uiState.update {
            it.copy(
                reporteSeleccionado = null,
                latitudCentro = primerMarcador?.latitud ?: LATITUD_POR_DEFECTO,
                longitudCentro = primerMarcador?.longitud ?: LONGITUD_POR_DEFECTO,
                zoomMapa = 15.0,
                enfocarTrigger = it.enfocarTrigger + 1
            )
        }
    }

    fun obtenerReportePorId(id: String): Reporte? {
        return _uiState.value.reportes.find { it.idReporte == id }
    }

    private fun construirMarcadores(reportes: List<Reporte>): List<MarcadorReporte> {
        val conteoPorCoordenada = mutableMapOf<String, Int>()

        return reportes.map { reporte ->
            val parseada = parsearCoordenadasUbicacion(reporte.ubicacion)
            val latBase = parseada?.first ?: LATITUD_POR_DEFECTO
            val lonBase = parseada?.second ?: LONGITUD_POR_DEFECTO
            val clave = "${"%.4f".format(latBase)},${"%.4f".format(lonBase)}"

            val indiceRepetido = conteoPorCoordenada.getOrDefault(clave, 0)
            conteoPorCoordenada[clave] = indiceRepetido + 1

            // Si varios reportes comparten la misma coordenada, se separan ligeramente en circulo para ver todos los pines
            val desplazamiento = if (indiceRepetido == 0) {
                0.0 to 0.0
            } else {
                val angulo = indiceRepetido * (Math.PI / 3.0)
                val radio = 0.0006 * ((indiceRepetido + 5) / 6)
                (radio * cos(angulo)) to (radio * sin(angulo))
            }

            MarcadorReporte(
                reporte = reporte,
                latitud = latBase + desplazamiento.first,
                longitud = lonBase + desplazamiento.second,
                tieneUbicacionExacta = parseada != null
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                HistoricoReportesViewModel(app.reporteRepository)
            }
        }
    }
}
