package com.example.alertcity.ui.components

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Tipos de navegación adaptativa según el codelab oficial de Android Compose:
 * - Compact (< 600dp): Barra de navegación inferior (BOTTOM_NAVIGATION)
 * - Medium (600dp - 839dp): Riel de navegación lateral (NAVIGATION_RAIL)
 * - Expanded (>= 840dp): Panel lateral permanente (PERMANENT_NAVIGATION_DRAWER)
 */
enum class AlertCityNavigationType {
    BOTTOM_NAVIGATION,
    NAVIGATION_RAIL,
    PERMANENT_NAVIGATION_DRAWER
}

/**
 * Tipos de distribución de contenido según el codelab oficial de Android Compose:
 * - LIST_ONLY: Muestra una sola vista principal (Compact y Medium)
 * - LIST_AND_DETAIL: Muestra lista y detalle simultáneamente lado a lado en 2 paneles (Expanded)
 */
enum class AlertCityContentType {
    LIST_ONLY,
    LIST_AND_DETAIL
}

data class ConfiguracionAdaptativa(
    val navigationType: AlertCityNavigationType,
    val contentType: AlertCityContentType
)

fun obtenerConfiguracionAdaptativa(windowSize: WindowWidthSizeClass): ConfiguracionAdaptativa {
    return when (windowSize) {
        WindowWidthSizeClass.Compact -> ConfiguracionAdaptativa(
            navigationType = AlertCityNavigationType.BOTTOM_NAVIGATION,
            contentType = AlertCityContentType.LIST_ONLY
        )
        WindowWidthSizeClass.Medium -> ConfiguracionAdaptativa(
            navigationType = AlertCityNavigationType.NAVIGATION_RAIL,
            contentType = AlertCityContentType.LIST_ONLY
        )
        WindowWidthSizeClass.Expanded -> ConfiguracionAdaptativa(
            navigationType = AlertCityNavigationType.PERMANENT_NAVIGATION_DRAWER,
            contentType = AlertCityContentType.LIST_AND_DETAIL
        )
        else -> ConfiguracionAdaptativa(
            navigationType = AlertCityNavigationType.BOTTOM_NAVIGATION,
            contentType = AlertCityContentType.LIST_ONLY
        )
    }
}

@Composable
fun esPantallaGrande(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 600
}
