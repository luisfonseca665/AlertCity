package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.local.UserSession
import com.example.alertcity.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val usernameOrEmail: String = "",
    val password: String = "",
    val username: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val cargando: Boolean = false,
    val mensajeError: String? = null
) {
    val loginValido: Boolean
        get() = usernameOrEmail.isNotBlank() && password.isNotBlank() && !cargando

    val registroValido: Boolean
        get() = username.isNotBlank() &&
            email.isNotBlank() &&
            password.length >= 6 &&
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            !cargando
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val sessionFlow: StateFlow<UserSession?> = sessionManager.sessionFlow

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onUsernameOrEmailChange(value: String) {
        _uiState.update { it.copy(usernameOrEmail = value, mensajeError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, mensajeError = null) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, mensajeError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, mensajeError = null) }
    }

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, mensajeError = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, mensajeError = null) }
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        if (!estado.loginValido) return

        _uiState.update { it.copy(cargando = true, mensajeError = null) }
        viewModelScope.launch {
            val result = authRepository.login(estado.usernameOrEmail, estado.password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeError = error.localizedMessage ?: "Error al iniciar sesión"
                        )
                    }
                }
            )
        }
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
                lastName = estado.lastName
            )
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState()
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

    fun cerrarSesion() {
        authRepository.logout()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                AuthViewModel(
                    authRepository = app.authRepository,
                    sessionManager = app.sessionManager
                )
            }
        }
    }
}
