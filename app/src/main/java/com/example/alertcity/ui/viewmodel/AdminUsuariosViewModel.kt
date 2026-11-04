package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.domain.model.EstadoReporte
import com.example.alertcity.domain.model.Reporte
import com.example.alertcity.domain.model.Usuario
import com.example.alertcity.domain.repository.ReporteRepository
import com.example.alertcity.domain.repository.UsuarioRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsuarioEstadisticaUi(
    val usuario: Usuario,
    val totalReportes: Int,
    val reportesActivos: Int
)

data class AdminUsuariosUiState(
    val todosLosUsuarios: List<UsuarioEstadisticaUi> = emptyList(),
    val usuariosFiltrados: List<UsuarioEstadisticaUi> = emptyList(),
    val busqueda: String = "",
    val mostrarDialogoFormulario: Boolean = false,
    val idUsuarioEditando: String? = null,
    val nombreForm: String = "",
    val apellidoForm: String = "",
    val usuarioForm: String = "",
    val direccionForm: String = "",
    val contrasenaForm: String = "",
    val usuarioPorEliminar: Usuario? = null
) {
    val formularioValido: Boolean
        get() = nombreForm.isNotBlank() &&
            apellidoForm.isNotBlank() &&
            usuarioForm.isNotBlank() &&
            direccionForm.isNotBlank()

    val totalConReportesActivos: Int
        get() = todosLosUsuarios.count { it.reportesActivos > 0 }
}

class AdminUsuariosViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val reporteRepository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsuariosUiState())
    val uiState: StateFlow<AdminUsuariosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            usuarioRepository.ensureSeedUsers()
        }
        viewModelScope.launch {
            combine(
                usuarioRepository.getAllUsuarios(),
                reporteRepository.getAllReports()
            ) { usuarios, reportes ->
                construirListaEstadisticas(usuarios, reportes)
            }.collect { estadisticas ->
                _uiState.update { actual ->
                    val conLista = actual.copy(todosLosUsuarios = estadisticas)
                    conLista.copy(usuariosFiltrados = filtrarUsuarios(estadisticas, conLista.busqueda))
                }
            }
        }
    }

    fun onBusquedaChange(texto: String) {
        _uiState.update { actual ->
            actual.copy(
                busqueda = texto,
                usuariosFiltrados = filtrarUsuarios(actual.todosLosUsuarios, texto)
            )
        }
    }

    fun abrirDialogoNuevoUsuario() {
        _uiState.update {
            it.copy(
                mostrarDialogoFormulario = true,
                idUsuarioEditando = null,
                nombreForm = "",
                apellidoForm = "",
                usuarioForm = "",
                direccionForm = "",
                contrasenaForm = ""
            )
        }
    }

    fun abrirDialogoEditarUsuario(usuario: Usuario) {
        _uiState.update {
            it.copy(
                mostrarDialogoFormulario = true,
                idUsuarioEditando = usuario.idUsuario,
                nombreForm = usuario.nombre,
                apellidoForm = usuario.apellido,
                usuarioForm = usuario.usuario,
                direccionForm = usuario.direccion,
                contrasenaForm = usuario.contrasena
            )
        }
    }

    fun cerrarDialogoFormulario() {
        _uiState.update { it.copy(mostrarDialogoFormulario = false) }
    }

    fun onNombreFormChange(valor: String) {
        _uiState.update { it.copy(nombreForm = valor) }
    }

    fun onApellidoFormChange(valor: String) {
        _uiState.update { it.copy(apellidoForm = valor) }
    }

    fun onUsuarioFormChange(valor: String) {
        _uiState.update { it.copy(usuarioForm = valor) }
    }

    fun onDireccionFormChange(valor: String) {
        _uiState.update { it.copy(direccionForm = valor) }
    }

    fun onContrasenaFormChange(valor: String) {
        _uiState.update { it.copy(contrasenaForm = valor) }
    }

    fun guardarUsuario() {
        val actual = _uiState.value
        if (!actual.formularioValido) return

        val usuario = Usuario(
            idUsuario = actual.idUsuarioEditando ?: UUID.randomUUID().toString(),
            usuario = actual.usuarioForm.trim(),
            contrasena = actual.contrasenaForm.ifBlank { "••••••••" },
            nombre = actual.nombreForm.trim(),
            apellido = actual.apellidoForm.trim(),
            direccion = actual.direccionForm.trim()
        )

        viewModelScope.launch {
            usuarioRepository.saveUsuario(usuario)
            _uiState.update { it.copy(mostrarDialogoFormulario = false) }
        }
    }

    fun solicitarEliminarUsuario(usuario: Usuario) {
        _uiState.update { it.copy(usuarioPorEliminar = usuario) }
    }

    fun cancelarEliminarUsuario() {
        _uiState.update { it.copy(usuarioPorEliminar = null) }
    }

    fun confirmarEliminarUsuario() {
        val usuario = _uiState.value.usuarioPorEliminar ?: return
        viewModelScope.launch {
            usuarioRepository.deleteUsuario(usuario.idUsuario)
            _uiState.update { it.copy(usuarioPorEliminar = null) }
        }
    }

    private fun construirListaEstadisticas(
        usuarios: List<Usuario>,
        reportes: List<Reporte>
    ): List<UsuarioEstadisticaUi> {
        val reportesPorUsuario = reportes.groupBy { it.idUsuario }
        return usuarios.map { usuario ->
            val delUsuario = reportesPorUsuario[usuario.idUsuario].orEmpty()
            UsuarioEstadisticaUi(
                usuario = usuario,
                totalReportes = delUsuario.size,
                reportesActivos = delUsuario.count { it.estado != EstadoReporte.RESUELTO }
            )
        }
    }

    private fun filtrarUsuarios(
        lista: List<UsuarioEstadisticaUi>,
        busqueda: String
    ): List<UsuarioEstadisticaUi> {
        val q = busqueda.trim().lowercase()
        if (q.isEmpty()) return lista
        return lista.filter { item ->
            val u = item.usuario
            "${u.nombre} ${u.apellido}".lowercase().contains(q) ||
                u.usuario.lowercase().contains(q) ||
                u.direccion.lowercase().contains(q)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                AdminUsuariosViewModel(
                    usuarioRepository = app.usuarioRepository,
                    reporteRepository = app.reporteRepository
                )
            }
        }
    }
}
