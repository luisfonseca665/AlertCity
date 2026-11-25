package com.example.alertcity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alertcity.ui.navigation.AppNavGraph
import com.example.alertcity.ui.theme.AlertCityTheme
import com.example.alertcity.ui.viewmodel.AuthViewModel
import com.example.alertcity.ui.viewmodel.ReporteViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlertCityTheme {
                val windowSize = calculateWindowSizeClass(this)
                val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
                val viewModel: ReporteViewModel = viewModel(factory = ReporteViewModel.Factory)
                AppNavGraph(
                    windowSize = windowSize.widthSizeClass,
                    authViewModel = authViewModel,
                    viewModel = viewModel
                )
            }
        }
    }
}
