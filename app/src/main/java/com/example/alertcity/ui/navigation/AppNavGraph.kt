package com.example.alertcity.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alertcity.ui.screens.AdminDashboardScreen
import com.example.alertcity.ui.screens.AdminDetalleReporteScreen
import com.example.alertcity.ui.screens.AdminMapaScreen
import com.example.alertcity.ui.screens.CrearReportesScreen
import com.example.alertcity.ui.screens.DetalleReporteScreen
import com.example.alertcity.ui.screens.HistoricoReportesScreen
import com.example.alertcity.ui.screens.ListaReportesScreen
import com.example.alertcity.ui.screens.LoginScreen
import com.example.alertcity.ui.screens.RegistroScreen
import com.example.alertcity.ui.viewmodel.AdminReportesViewModel
import com.example.alertcity.ui.viewmodel.CrearReporteViewModel
import com.example.alertcity.ui.viewmodel.HistoricoReportesViewModel
import com.example.alertcity.ui.viewmodel.ReporteViewModel

@Composable
fun AppNavGraph(viewModel: ReporteViewModel) {
    var sesionIniciada by rememberSaveable { mutableStateOf(false) }
    var modoAdmin by rememberSaveable { mutableStateOf(false) }

    if (modoAdmin) {
        FlujoNavegacionAdmin(
            onSalirModoAdmin = { modoAdmin = false }
        )
    } else {
        FlujoNavegacionCiudadano(
            viewModel = viewModel,
            sesionIniciada = sesionIniciada,
            onSesionIniciadaChange = { sesionIniciada = it },
            onEntrarModoAdmin = { modoAdmin = true }
        )
    }
}

@Composable
private fun FlujoNavegacionCiudadano(
    viewModel: ReporteViewModel,
    sesionIniciada: Boolean,
    onSesionIniciadaChange: (Boolean) -> Unit,
    onEntrarModoAdmin: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mostrarBarraInferior = currentRoute != null &&
        currentRoute != "login" &&
        currentRoute != "registro"

    Scaffold(
        bottomBar = {
            if (mostrarBarraInferior) {
                MenuNavegacionInferior(navController)
            }
        },
        floatingActionButton = {
            if (currentRoute == "inicio") {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate("crear") {
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo reporte") },
                    text = { Text("Reportar") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (sesionIniciada) "inicio" else "login",
            modifier = Modifier.padding(padding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginExitoso = {
                        onSesionIniciadaChange(true)
                        navController.navigate("inicio") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onIrARegistroClick = {
                        navController.navigate("registro") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("registro") {
                RegistroScreen(
                    onRegistroExitoso = {
                        onSesionIniciadaChange(true)
                        navController.navigate("inicio") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onVolverALoginClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("inicio") {
                ListaReportesScreen(
                    viewModel = viewModel,
                    onReporteClick = { id ->
                        navController.navigate("detalle/$id")
                    },
                    onCambiarModoAdmin = onEntrarModoAdmin,
                    onCerrarSesion = {
                        onSesionIniciadaChange(false)
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("crear") {
                val crearReporteViewModel: CrearReporteViewModel = viewModel(
                    factory = CrearReporteViewModel.Factory
                )
                CrearReportesScreen(
                    viewModel = crearReporteViewModel,
                    onBack = { navController.popBackStack() },
                    onReporteGuardado = {
                        navController.navigate("inicio") {
                            popUpTo("inicio") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = "detalle/{reporteId}",
                arguments = listOf(navArgument("reporteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val uiState by viewModel.uiState.collectAsState()
                val reporteId = backStackEntry.arguments?.getString("reporteId")
                val reporteSeleccionado = uiState.reportes.find { it.idReporte == reporteId }

                DetalleReporteScreen(
                    reporte = reporteSeleccionado,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("historico") {
                val historicoViewModel: HistoricoReportesViewModel = viewModel(
                    factory = HistoricoReportesViewModel.Factory
                )
                HistoricoReportesScreen(
                    viewModel = historicoViewModel,
                    onReporteClick = { id ->
                        navController.navigate("detalle/$id")
                    }
                )
            }
        }
    }
}

@Composable
private fun FlujoNavegacionAdmin(
    onSalirModoAdmin: () -> Unit
) {
    val navController = rememberNavController()
    val adminReportesViewModel: AdminReportesViewModel = viewModel(
        factory = AdminReportesViewModel.Factory
    )

    Scaffold(
        bottomBar = { MenuNavegacionInferiorAdmin(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "admin_panel",
            modifier = Modifier.padding(padding)
        ) {
            composable("admin_panel") {
                AdminDashboardScreen(
                    viewModel = adminReportesViewModel,
                    onReporteClick = { id ->
                        navController.navigate("admin_detalle/$id")
                    },
                    onCambiarModoCiudadano = onSalirModoAdmin
                )
            }

            composable("admin_mapa") {
                AdminMapaScreen(
                    viewModel = adminReportesViewModel,
                    onGestionarReporte = { id ->
                        navController.navigate("admin_detalle/$id")
                    },
                    onCambiarModoCiudadano = onSalirModoAdmin
                )
            }

            composable(
                route = "admin_detalle/{reporteId}",
                arguments = listOf(navArgument("reporteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val uiState by adminReportesViewModel.uiState.collectAsState()
                val reporteId = backStackEntry.arguments?.getString("reporteId")
                val reporteSeleccionado = uiState.todosLosReportes.find { it.idReporte == reporteId }

                AdminDetalleReporteScreen(
                    reporte = reporteSeleccionado,
                    onCambiarEstado = { nuevoEstado ->
                        if (reporteSeleccionado != null) {
                            adminReportesViewModel.cambiarEstadoReporte(reporteSeleccionado, nuevoEstado)
                        }
                    },
                    onCambiarUrgencia = { urgente ->
                        if (reporteSeleccionado != null) {
                            adminReportesViewModel.cambiarUrgenciaReporte(reporteSeleccionado, urgente)
                        }
                    },
                    onEliminarReporte = {
                        if (reporteSeleccionado != null) {
                            adminReportesViewModel.eliminarReporte(reporteSeleccionado.idReporte) {
                                navController.popBackStack()
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MenuNavegacionInferior(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        val enDetalle = currentRoute?.startsWith("detalle/") == true

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = currentRoute == "inicio" || (enDetalle && previousRoute != "historico"),
            onClick = {
                navController.navigate("inicio") {
                    popUpTo("inicio") { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = "Histórico") },
            label = { Text("Histórico") },
            selected = currentRoute == "historico" || (enDetalle && previousRoute == "historico"),
            onClick = {
                navController.navigate("historico") {
                    popUpTo("inicio") { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun MenuNavegacionInferiorAdmin(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        val enDetalleAdmin = currentRoute?.startsWith("admin_detalle/") == true

        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Panel") },
            label = { Text("Panel") },
            selected = currentRoute == "admin_panel" || (enDetalleAdmin && previousRoute != "admin_mapa"),
            onClick = {
                navController.navigate("admin_panel") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
            label = { Text("Mapa") },
            selected = currentRoute == "admin_mapa" || (enDetalleAdmin && previousRoute == "admin_mapa"),
            onClick = {
                navController.navigate("admin_mapa") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
    }
}
