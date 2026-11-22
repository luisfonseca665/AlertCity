package com.example.alertcity.ui.navigation

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alertcity.ui.screens.CrearReportesScreen
import com.example.alertcity.ui.screens.DetalleReporteScreen
import com.example.alertcity.ui.screens.HistoricoReportesScreen
import com.example.alertcity.ui.screens.ListaReportesScreen
import com.example.alertcity.ui.screens.LoginScreen
import com.example.alertcity.ui.screens.RegisterScreen
import com.example.alertcity.ui.viewmodel.AuthViewModel
import com.example.alertcity.ui.viewmodel.CrearReporteViewModel
import com.example.alertcity.ui.viewmodel.HistoricoReportesViewModel
import com.example.alertcity.ui.viewmodel.ReporteViewModel

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel,
    viewModel: ReporteViewModel
) {
    val context = LocalContext.current
    val localNetworkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 36) {
            val permission = "android.permission.ACCESS_LOCAL_NETWORK"
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                localNetworkLauncher.launch(permission)
            }
        }
    }

    val session by authViewModel.sessionFlow.collectAsState()

    if (session == null) {
        val authNavController = rememberNavController()
        NavHost(
            navController = authNavController,
            startDestination = "login"
        ) {
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onIrARegistro = { authNavController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onVolverALogin = { authNavController.popBackStack() }
                )
            }
        }
        return
    }

    val navController = rememberNavController()
    val puedeCambiarEstado = session?.puedeCambiarEstadoReportes == true

    Scaffold(
        bottomBar = { MenuNavegacionInferior(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "inicio",
            modifier = Modifier.padding(padding)
        ) {
            composable("inicio") {
                ListaReportesScreen(
                    viewModel = viewModel,
                    session = session,
                    onCerrarSesion = { authViewModel.cerrarSesion() },
                    onReporteClick = { id ->
                        navController.navigate("detalle/$id")
                    }
                )
            }

            composable("crear") {
                val crearReporteViewModel: CrearReporteViewModel = viewModel(
                    factory = CrearReporteViewModel.Factory
                )
                CrearReportesScreen(
                    viewModel = crearReporteViewModel,
                    onReporteGuardado = {
                        viewModel.refrescar()
                        navController.navigate("inicio") { popUpTo(0) }
                    }
                )
            }

            composable(
                route = "detalle/{reporteId}",
                arguments = listOf(navArgument("reporteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val uiState by viewModel.uiState.collectAsState()
                val reporteId = backStackEntry.arguments?.getString("reporteId")
                val reporteSeleccionado = uiState.listaVisible.find { it.idReporte == reporteId }
                    ?: uiState.reportes.find { it.idReporte == reporteId }

                DetalleReporteScreen(
                    reporte = reporteSeleccionado,
                    puedeCambiarEstado = puedeCambiarEstado,
                    actualizandoEstado = uiState.actualizandoEstado,
                    sincronizando = uiState.sincronizando,
                    mensajeAccion = uiState.mensajeEstadoReporte,
                    onLimpiarMensaje = { viewModel.limpiarMensajeEstadoReporte() },
                    onCambiarEstado = { nuevoEstado ->
                        if (reporteId != null) {
                            viewModel.actualizarEstadoReporte(reporteId, nuevoEstado)
                        }
                    },
                    onReintentarSincronizacion = { viewModel.sincronizarPendientes() },
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
                        navController.navigate("detalle_historico/$id")
                    }
                )
            }

            composable(
                route = "detalle_historico/{reporteId}",
                arguments = listOf(navArgument("reporteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val historicoViewModel: HistoricoReportesViewModel = viewModel(
                    factory = HistoricoReportesViewModel.Factory
                )
                val histState by historicoViewModel.uiState.collectAsState()
                val repState by viewModel.uiState.collectAsState()
                val reporteId = backStackEntry.arguments?.getString("reporteId")
                val reporteSeleccionado = repState.reportes.find { it.idReporte == reporteId }
                    ?: histState.reportes.find { it.idReporte == reporteId }

                DetalleReporteScreen(
                    reporte = reporteSeleccionado,
                    puedeCambiarEstado = puedeCambiarEstado,
                    actualizandoEstado = repState.actualizandoEstado,
                    sincronizando = repState.sincronizando,
                    mensajeAccion = repState.mensajeEstadoReporte,
                    onLimpiarMensaje = { viewModel.limpiarMensajeEstadoReporte() },
                    onCambiarEstado = { nuevoEstado ->
                        if (reporteId != null) {
                            viewModel.actualizarEstadoReporte(reporteId, nuevoEstado)
                        }
                    },
                    onReintentarSincronizacion = { viewModel.sincronizarPendientes() },
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

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = currentRoute == "inicio" || currentRoute?.startsWith("detalle/") == true,
            onClick = {
                navController.navigate("inicio") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Add, contentDescription = "Reportar") },
            label = { Text("Reportar") },
            selected = currentRoute == "crear",
            onClick = {
                navController.navigate("crear") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = "Histórico") },
            label = { Text("Histórico") },
            selected = currentRoute == "historico" || currentRoute?.startsWith("detalle_historico") == true,
            onClick = {
                navController.navigate("historico") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
    }
}
