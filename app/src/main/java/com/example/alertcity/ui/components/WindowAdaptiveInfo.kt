package com.example.alertcity.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Determina si el dispositivo tiene un factor de forma de pantalla grande
 * (tablets, plegables o teléfonos en orientación horizontal >= 600dp,
 * equivalente a Medium / Expanded WindowSizeClass de Material 3).
 */
@Composable
fun esPantallaGrande(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 600
}
