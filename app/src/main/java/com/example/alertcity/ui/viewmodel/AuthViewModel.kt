package com.example.alertcity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.local.UserSession
import com.example.alertcity.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(
    private val authRepository: AuthRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val sessionFlow: StateFlow<UserSession?> = sessionManager.sessionFlow

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
