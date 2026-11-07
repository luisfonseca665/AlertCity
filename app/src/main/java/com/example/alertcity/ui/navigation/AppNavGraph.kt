package com.example.alertcity.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.alertcity.ui.viewmodel.CrearReporteViewModel
import com.example.alertcity.ui.viewmodel.HistoricoReportesViewModel
import com.example.alertcity.ui.viewmodel.ReporteViewModel

@Composable
fun AppNavGraph(viewModel: ReporteViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = { MenuNavegacionInferior(navController) },
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
            startDestination = "inicio",
            modifier = Modifier.padding(padding)
        ) {
            composable("inicio") {
                ListaReportesScreen(
                    viewModel = viewModel,
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
                    onBack = { navController.popBackStack() },
                    onReporteGuardado = {
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
                    popUpTo(navController.graph.startDestinationId)
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
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
    }
}
