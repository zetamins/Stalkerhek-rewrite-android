package com.stalkerhek.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stalkerhek.tv.engine.EngineController
import com.stalkerhek.tv.tv.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val profiles by EngineController.profiles.collectAsState()

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080C09))) {
                NavHost(
                    navController = navController,
                    startDestination = if (profiles.isEmpty()) "settings" else "channels"
                ) {
                    composable("channels")    { ChannelGridScreen(navController) }
                    composable("search")      { SearchScreen(navController) }
                    composable("epg")         { EpgScreen(navController) }
                    composable("vod")         { VodScreen(navController) }
                    composable("favourites")  { FavouritesScreen(navController) }
                    composable("settings")    { SettingsScreen(navController) }
                    composable("qr")          { QrCodeScreen(navController) }
                }
            }
        }
    }
}
