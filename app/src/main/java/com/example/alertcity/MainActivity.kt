package com.example.alertcity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alertcity.ui.navigation.AppNavGraph
import com.example.alertcity.ui.theme.AlertCityTheme
import com.example.alertcity.ui.viewmodel.ReporteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlertCityTheme {
                val viewModel: ReporteViewModel = viewModel()
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}