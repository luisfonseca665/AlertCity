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

data class LoginUiState(
    val usernameOrEmail: String = "",
    val password: String = "",
    val contrasenaVisible: Boolean = false,
    val cargando: Boolean = false,
    val mensajeError: String? = null
) {
    val loginValido: Boolean
        get() = usernameOrEmail.isNotBlank() && password.isNotBlank() && !cargando
}

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameOrEmailChange(value: String) {
        _uiState.update { it.copy(usernameOrEmail = value, mensajeError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, mensajeError = null) }
    }

    fun onToggleContrasenaVisible() {
        _uiState.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }
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
                    _uiState.value = LoginUiState()
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlertCityApp)
                LoginViewModel(authRepository = app.authRepository)
            }
        }
    }
}
