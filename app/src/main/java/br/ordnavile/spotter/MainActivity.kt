package br.ordnavile.spotter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.ordnavile.spotter.ui.screens.home.MonilocScreen
import br.ordnavile.spotter.ui.theme.SpotterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpotterTheme {
                MonilocScreen()
            }
        }
    }
}