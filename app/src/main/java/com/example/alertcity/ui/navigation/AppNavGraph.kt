package com.example.alertcity.ui.navigation

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alertcity.R
import com.example.alertcity.data.local.UserSession
import com.example.alertcity.ui.components.AlertCityContentType
import com.example.alertcity.ui.components.AlertCityNavigationType
import com.example.alertcity.ui.components.obtenerConfiguracionAdaptativa
import com.example.alertcity.ui.screens.AdminDashboardScreen
import com.example.alertcity.ui.screens.AdminDetalleReporteScreen
import com.example.alertcity.ui.screens.AdminMapaScreen
import com.example.alertcity.ui.screens.CrearReportesScreen
import com.example.alertcity.ui.screens.DetalleReporteScreen
import com.example.alertcity.ui.screens.HistoricoReportesScreen
import com.example.alertcity.ui.screens.ListaReportesScreen
import com.example.alertcity.ui.screens.LoginScreen
import com.example.alertcity.ui.screens.RegisterScreen
import com.example.alertcity.ui.viewmodel.AdminDetalleReporteViewModel
import com.example.alertcity.ui.viewmodel.AdminReportesViewModel
import com.example.alertcity.ui.viewmodel.AuthViewModel
import com.example.alertcity.ui.viewmodel.CrearReporteViewModel
import com.example.alertcity.ui.viewmodel.DetalleReporteViewModel
import com.example.alertcity.ui.viewmodel.HistoricoReportesViewModel
import com.example.alertcity.ui.viewmodel.LoginViewModel
import com.example.alertcity.ui.viewmodel.RegistroViewModel
import com.example.alertcity.ui.viewmodel.ReporteViewModel

@Composable
fun AppNavGraph(
    windowSize: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
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

    val configAdaptativa = obtenerConfiguracionAdaptativa(windowSize)
    val session by authViewModel.sessionFlow.collectAsState()
    val sesionActual = session

    if (sesionActual == null) {
        val authNavController = rememberNavController()
        NavHost(
            navController = authNavController,
            startDestination = "login"
        ) {
            composable("login") {
                val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
                LoginScreen(
                    viewModel = loginViewModel,
                    onIrARegistro = { authNavController.navigate("register") }
                )
            }
            composable("register") {
                val registroViewModel: RegistroViewModel = viewModel(factory = RegistroViewModel.Factory)
                RegisterScreen(
                    viewModel = registroViewModel,
                    onVolverALogin = { authNavController.popBackStack() }
                )
            }
        }
        return
    }

    if (sesionActual.esOperadorOAdmin) {
        FlujoNavegacionAdmin(
            navigationType = configAdaptativa.navigationType,
            contentType = configAdaptativa.contentType,
            onCerrarSesion = { authViewModel.cerrarSesion() }
        )
    } else {
        FlujoNavegacionCiudadano(
            navigationType = configAdaptativa.navigationType,
            contentType = configAdaptativa.contentType,
            viewModel = viewModel,
            session = sesionActual,
            onCerrarSesion = { authViewModel.cerrarSesion() }
        )
    }
}

@Composable
private fun FlujoNavegacionCiudadano(
    navigationType: AlertCityNavigationType,
    contentType: AlertCityContentType,
    viewModel: ReporteViewModel,
    session: UserSession,
    onCerrarSesion: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val irANuevoReporte = {
        navController.navigate("crear") {
            launchSingleTop = true
        }
    }

    val contenidoPrincipal: @Composable () -> Unit = {
        Scaffold(
            bottomBar = {
                if (navigationType == AlertCityNavigationType.BOTTOM_NAVIGATION) {
                    MenuNavegacionInferior(navController)
                }
            },
            floatingActionButton = {
                if (navigationType == AlertCityNavigationType.BOTTOM_NAVIGATION && currentRoute == "inicio") {
                    ExtendedFloatingActionButton(
                        onClick = irANuevoReporte,
                        icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_new_report_desc)) },
                        text = { Text(stringResource(R.string.nav_report)) }
                    )
                }
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (navigationType == AlertCityNavigationType.NAVIGATION_RAIL) {
                    MenuNavegacionLateral(
                        navController = navController,
                        onNuevoReporteClick = irANuevoReporte
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = "inicio",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("inicio") {
                            ListaReportesScreen(
                                viewModel = viewModel,
                                session = session,
                                contentType = contentType,
                                onCerrarSesion = onCerrarSesion,
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
                            val detalleViewModel: DetalleReporteViewModel = viewModel(
                                factory = DetalleReporteViewModel.Factory
                            )
                            val reporteId = backStackEntry.arguments?.getString("reporteId")
                            LaunchedEffect(reporteId) {
                                detalleViewModel.cargarReporte(reporteId)
                            }

                            DetalleReporteScreen(
                                viewModel = detalleViewModel,
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
        }
    }

    if (navigationType == AlertCityNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
                    MenuDrawerPermanenteCiudadano(
                        navController = navController,
                        onNuevoReporteClick = irANuevoReporte
                    )
                }
            }
        ) {
            contenidoPrincipal()
        }
    } else {
        contenidoPrincipal()
    }
}

@Composable
private fun FlujoNavegacionAdmin(
    navigationType: AlertCityNavigationType,
    contentType: AlertCityContentType,
    onCerrarSesion: () -> Unit
) {
    val navController = rememberNavController()
    val adminReportesViewModel: AdminReportesViewModel = viewModel(
        factory = AdminReportesViewModel.Factory
    )

    val contenidoPrincipal: @Composable () -> Unit = {
        Scaffold(
            bottomBar = {
                if (navigationType == AlertCityNavigationType.BOTTOM_NAVIGATION) {
                    MenuNavegacionInferiorAdmin(navController)
                }
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (navigationType == AlertCityNavigationType.NAVIGATION_RAIL) {
                    MenuNavegacionLateralAdmin(navController)
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = "admin_panel",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("admin_panel") {
                            AdminDashboardScreen(
                                viewModel = adminReportesViewModel,
                                contentType = contentType,
                                onReporteClick = { id ->
                                    navController.navigate("admin_detalle/$id")
                                },
                                onCerrarSesion = onCerrarSesion
                            )
                        }

                        composable("admin_mapa") {
                            AdminMapaScreen(
                                viewModel = adminReportesViewModel,
                                onGestionarReporte = { id ->
                                    navController.navigate("admin_detalle/$id")
                                },
                                onCerrarSesion = onCerrarSesion
                            )
                        }

                        composable(
                            route = "admin_detalle/{reporteId}",
                            arguments = listOf(navArgument("reporteId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val adminDetalleViewModel: AdminDetalleReporteViewModel = viewModel(
                                factory = AdminDetalleReporteViewModel.Factory
                            )
                            val reporteId = backStackEntry.arguments?.getString("reporteId")
                            LaunchedEffect(reporteId) {
                                adminDetalleViewModel.cargarReporte(reporteId)
                            }

                            AdminDetalleReporteScreen(
                                viewModel = adminDetalleViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (navigationType == AlertCityNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
                    MenuDrawerPermanenteAdmin(navController = navController)
                }
            }
        ) {
            contenidoPrincipal()
        }
    } else {
        contenidoPrincipal()
    }
}

@Composable
private fun MenuDrawerPermanenteCiudadano(
    navController: NavHostController,
    onNuevoReporteClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    val enDetalle = currentRoute?.startsWith("detalle/") == true

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationCity,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ExtendedFloatingActionButton(
            onClick = onNuevoReporteClick,
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_new_report_desc)) },
            text = { Text(stringResource(R.string.nav_report)) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_home)) },
            selected = currentRoute == "inicio" || (enDetalle && previousRoute != "historico"),
            onClick = {
                navController.navigate("inicio") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.height(4.dp))

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_history)) },
            selected = currentRoute == "historico" || (enDetalle && previousRoute == "historico"),
            onClick = {
                navController.navigate("historico") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.nav_history)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@Composable
private fun MenuDrawerPermanenteAdmin(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    val enDetalleAdmin = currentRoute?.startsWith("admin_detalle/") == true

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_admin_panel)) },
            selected = currentRoute == "admin_panel" || (enDetalleAdmin && previousRoute != "admin_mapa"),
            onClick = {
                navController.navigate("admin_panel") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = stringResource(R.string.nav_admin_panel)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.height(4.dp))

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_admin_map)) },
            selected = currentRoute == "admin_mapa" || (enDetalleAdmin && previousRoute == "admin_mapa"),
            onClick = {
                navController.navigate("admin_mapa") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Map, contentDescription = stringResource(R.string.nav_admin_map)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
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
            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = currentRoute == "inicio" || (enDetalle && previousRoute != "historico"),
            onClick = {
                navController.navigate("inicio") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.nav_history)) },
            label = { Text(stringResource(R.string.nav_history)) },
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

@Composable
fun MenuNavegacionLateral(
    navController: NavHostController,
    onNuevoReporteClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    val enDetalle = currentRoute?.startsWith("detalle/") == true

    NavigationRail(
        header = {
            FloatingActionButton(
                onClick = onNuevoReporteClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_new_report_desc))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    ) {
        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = currentRoute == "inicio" || (enDetalle && previousRoute != "historico"),
            onClick = {
                navController.navigate("inicio") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.nav_history)) },
            label = { Text(stringResource(R.string.nav_history)) },
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

@Composable
fun MenuNavegacionInferiorAdmin(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        val enDetalleAdmin = currentRoute?.startsWith("admin_detalle/") == true

        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = stringResource(R.string.nav_admin_panel)) },
            label = { Text(stringResource(R.string.nav_admin_panel)) },
            selected = currentRoute == "admin_panel" || (enDetalleAdmin && previousRoute != "admin_mapa"),
            onClick = {
                navController.navigate("admin_panel") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Map, contentDescription = stringResource(R.string.nav_admin_map)) },
            label = { Text(stringResource(R.string.nav_admin_map)) },
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

@Composable
fun MenuNavegacionLateralAdmin(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    val enDetalleAdmin = currentRoute?.startsWith("admin_detalle/") == true

    NavigationRail {
        Spacer(modifier = Modifier.height(12.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = stringResource(R.string.nav_admin_panel)) },
            label = { Text(stringResource(R.string.nav_admin_panel)) },
            selected = currentRoute == "admin_panel" || (enDetalleAdmin && previousRoute != "admin_mapa"),
            onClick = {
                navController.navigate("admin_panel") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Map, contentDescription = stringResource(R.string.nav_admin_map)) },
            label = { Text(stringResource(R.string.nav_admin_map)) },
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
