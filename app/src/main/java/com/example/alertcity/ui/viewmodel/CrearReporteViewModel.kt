package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.repository.ReporteRepository
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LATITUD_INICIAL = 20.1387
private const val LONGITUD_INICIAL = -101.1895

private val CATEGORIAS_REPORTE = listOf(
    "Baches y Vialidad",
    "Alumbrado Público",
    "Fugas de Agua",
    "Basura y Limpieza",
    "Seguridad y Vandalismo",
    "Parques y Áreas Verdes",
    "Ruido Excesivo",
    "Animales en Abandono",
    "Otro"
)

data class CrearReporteUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categorias: List<String> = CATEGORIAS_REPORTE,
    val categoria: String = CATEGORIAS_REPORTE.first(),
    val categoriaExpandida: Boolean = false,
    val urgente: Boolean = false,
    val hora: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val minuto: Int = Calendar.getInstance().get(Calendar.MINUTE),
    val mostrarTimePicker: Boolean = false,
    val fotoUri: String? = null,
    val tempUri: String? = null,
    val mostrarOpcionesFoto: Boolean = false,
    val latitudSeleccionada: Double? = null,
    val longitudSeleccionada: Double? = null,
    val latitudCamara: Double = LATITUD_INICIAL,
    val longitudCamara: Double = LONGITUD_INICIAL,
    val animarMapaTrigger: Int = 0,
    val guardando: Boolean = false
) {
    val horaFormateada: String
        get() = String.format(Locale.getDefault(), "%02d:%02d", hora, minuto)

    val ubicacion: String?
        get() = if (latitudSeleccionada != null && longitudSeleccionada != null) {
            val ns = if (latitudSeleccionada >= 0.0) "N" else "S"
            val ew = if (longitudSeleccionada >= 0.0) "E" else "W"
            String.format(
                Locale.US,
                "%.4f° %s, %.4f° %s",
                abs(latitudSeleccionada),
                ns,
                abs(longitudSeleccionada),
                ew
            )
        } else {
            null
        }

    val formularioValido: Boolean
        get() = titulo.isNotBlank() && descripcion.isNotBlank() && !guardando
}

class CrearReporteViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrearReporteUiState())
    val uiState: StateFlow<CrearReporteUiState> = _uiState.asStateFlow()

    fun onTituloChange(nuevoTitulo: String) {
        _uiState.update { it.copy(titulo = nuevoTitulo) }
    }

    fun onDescripcionChange(nuevaDescripcion: String) {
        _uiState.update { it.copy(descripcion = nuevaDescripcion) }
    }

    fun onCategoriaChange(nuevaCategoria: String) {
        _uiState.update {
            it.copy(
                categoria = nuevaCategoria,
                categoriaExpandida = false
            )
        }
    }

    fun onCategoriaExpandidaChange(expandida: Boolean) {
        _uiState.update { it.copy(categoriaExpandida = expandida) }
    }

    fun onUrgenteChange(esUrgente: Boolean) {
        _uiState.update { it.copy(urgente = esUrgente) }
    }

    fun onMostrarTimePickerChange(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarTimePicker = mostrar) }
    }

    fun onConfirmarHora(hora: Int, minuto: Int) {
        _uiState.update {
            it.copy(
                hora = hora,
                minuto = minuto,
                mostrarTimePicker = false
            )
        }
    }

    fun onMostrarOpcionesFotoChange(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarOpcionesFoto = mostrar) }
    }

    fun onTempUriGenerada(uri: String) {
        _uiState.update { it.copy(tempUri = uri) }
    }

    fun onFotoCapturada(uri: String) {
        _uiState.update { it.copy(fotoUri = uri) }
    }

    fun onUbicacionGpsObtenida(latitud: Double, longitud: Double) {
        _uiState.update {
            it.copy(
                latitudSeleccionada = latitud,
                longitudSeleccionada = longitud,
                latitudCamara = latitud,
                longitudCamara = longitud,
                animarMapaTrigger = it.animarMapaTrigger + 1
            )
        }
    }

    fun onMapaClick(latitud: Double, longitud: Double) {
        _uiState.update {
            it.copy(
                latitudSeleccionada = latitud,
                longitudSeleccionada = longitud
            )
        }
    }

    fun guardarReporte(onReporteGuardado: () -> Unit) {
        val estadoActual = _uiState.value
        if (!estadoActual.formularioValido) return

        _uiState.update { it.copy(guardando = true) }

        viewModelScope.launch {
            val nuevo = Reporte(
                titulo = estadoActual.titulo,
                descripcion = estadoActual.descripcion,
                categoria = estadoActual.categoria,
                esUrgente = estadoActual.urgente,
                hora = estadoActual.horaFormateada,
                fotoUri = estadoActual.fotoUri,
                ubicacion = estadoActual.ubicacion
            )
            repository.saveReport(nuevo)
            _uiState.value = CrearReporteUiState()
            onReporteGuardado()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                CrearReporteViewModel(app.reporteRepository)
            }
        }
    }
}
