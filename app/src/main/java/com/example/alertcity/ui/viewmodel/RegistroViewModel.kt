package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroUiState(
    val firstName: String = "",
    val lastName: String = "",
    val direccion: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val contrasenaVisible: Boolean = false,
    val confirmarVisible: Boolean = false,
    val cargando: Boolean = false,
    val mensajeError: String? = null
) {
    val contrasenasNoCoinciden: Boolean
        get() = confirmPassword.isNotEmpty() && password != confirmPassword

    val registroValido: Boolean
        get() = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            username.isNotBlank() &&
            email.isNotBlank() &&
            password.length >= 6 &&
            (confirmPassword.isEmpty() || password == confirmPassword) &&
            !cargando
}

class RegistroViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, mensajeError = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, mensajeError = null) }
    }

    fun onDireccionChange(value: String) {
        _uiState.update { it.copy(direccion = value, mensajeError = null) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, mensajeError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, mensajeError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, mensajeError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, mensajeError = null) }
    }

    fun onToggleContrasenaVisible() {
        _uiState.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }
    }

    fun onToggleConfirmarVisible() {
        _uiState.update { it.copy(confirmarVisible = !it.confirmarVisible) }
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }

    fun registrarUsuario() {
        val estado = _uiState.value
        if (!estado.registroValido) return

        _uiState.update { it.copy(cargando = true, mensajeError = null) }
        viewModelScope.launch {
            val result = authRepository.register(
                username = estado.username,
                email = estado.email,
                password = estado.password,
                firstName = estado.firstName,
                lastName = estado.lastName,
                direccion = estado.direccion
            )
            result.fold(
                onSuccess = {
                    _uiState.value = RegistroUiState()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeError = error.localizedMessage ?: "Error al registrar usuario"
                        )
                    }
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                RegistroViewModel(authRepository = app.authRepository)
            }
        }
    }
}
