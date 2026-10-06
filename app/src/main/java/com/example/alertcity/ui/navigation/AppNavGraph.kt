package com.example.alertcity.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alertcity.ui.screens.CrearReportesScreen
import com.example.alertcity.ui.screens.DetalleReporteScreen
import com.example.alertcity.ui.screens.ListaReportesScreen
import com.example.alertcity.ui.viewmodel.ReporteViewModel

@Composable
fun AppNavGraph(viewModel: ReporteViewModel) {
    val navController = rememberNavController()

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
                    onReporteClick = { id ->
                        navController.navigate("detalle/$id")
                    }
                )
            }

            composable("crear") {
                CrearReportesScreen(
                    onGuardar = { t, d, c, u, h, foto, ubi ->
                        viewModel.crearReporte(t, d, c, u, h, foto, ubi)
                        navController.navigate("inicio") { popUpTo(0) }
                    }
                )
            }

            composable(
                route = "detalle/{reporteId}",
                arguments = listOf(navArgument("reporteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val reporteId = backStackEntry.arguments?.getString("reporteId")
                val reporteSeleccionado = reporteId?.let { viewModel.obtenerReportePorId(it) }

                DetalleReporteScreen(
                    reporte = reporteSeleccionado,
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
            selected = currentRoute == "inicio" || currentRoute?.startsWith("detalle") == true,
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
    }
}