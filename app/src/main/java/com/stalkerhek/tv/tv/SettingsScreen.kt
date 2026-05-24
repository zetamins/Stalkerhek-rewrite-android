package com.stalkerhek.tv.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.stalkerhek.tv.engine.EngineController
import com.stalkerhek.tv.engine.ProfileConfig
import com.stalkerhek.tv.engine.ProfileStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val profiles by EngineController.profiles.collectAsState()
    val activeProfile by EngineController.activeProfile.collectAsState()
    val activeProfileId by EngineController.activeProfileId.collectAsState()
    var startingId by remember { mutableStateOf<Int?>(null) }

    // When a profile becomes active after we triggered start, navigate to channels
    LaunchedEffect(activeProfileId) {
        if (activeProfileId > 0 && startingId != null && activeProfileId == startingId) {
            startingId = null
            navController.navigate("channels") {
                popUpTo("settings") { inclusive = false }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF080C09)).padding(24.dp)
    ) {
        Text(
            "Settings",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (profiles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No profiles configured", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Open the management UI in a browser on the same network,\nor scan the QR code below.",
                        color = Color(0xFF8BA38D),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate("qr") },
                        colors = ButtonDefaults.colors(containerColor = Color(0xFF0C120E), focusedContainerColor = Color(0xFF1A2C1F))
                    ) {
                        Text("📡  Show Connection Info", color = Color(0xFF2D8A4E))
                    }
                }
            }
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(profiles) { profile ->
                val isStarting = startingId == profile.id
                ProfileCard(
                    profile = profile,
                    status = if (activeProfile?.let { true } == true && activeProfileId == profile.id) activeProfile else null,
                    isStarting = isStarting,
                    onStart = {
                        startingId = profile.id
                        scope.launch {
                            val result = EngineController.startProfile(profile)
                            if (result.isFailure) {
                                startingId = null
                            }
                        }
                    },
                    onStop = {
                        scope.launch { EngineController.stopProfile(profile.id) }
                    }
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("qr") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF0C120E),
                        focusedContainerColor = Color(0xFF1A2C1F)
                    )
                ) {
                    Text("📡  Connection Info & QR Code", color = Color(0xFF2D8A4E), fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileCard(
    profile: ProfileConfig,
    status: ProfileStatus?,
    isStarting: Boolean = false,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val isRunning = status?.running == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isFocused) Color(0xFF1A2C1F) else Color(0xFF111A14))
            .padding(16.dp)
            .onFocusChanged { isFocused = it.isFocused },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(profile.name.ifEmpty { "Profile ${profile.id}" }, color = Color.White, fontSize = 18.sp)
            Text(
                when {
                    isStarting -> "Starting..."
                    isRunning -> "Running — ${status?.channelsCount ?: 0} channels"
                    else -> "Stopped"
                },
                color = when {
                    isStarting -> Color(0xFFD4A94A)
                    isRunning -> Color(0xFF2D8A4E)
                    else -> Color.Gray
                },
                fontSize = 14.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                isStarting -> Button(onClick = {}, colors = ButtonDefaults.colors(containerColor = Color(0xFF1A2C1F))) {
                    Text("Starting...", color = Color(0xFFD4A94A))
                }
                isRunning -> Button(onClick = onStop) {
                    Text("Stop", color = Color.White)
                }
                else -> Button(onClick = onStart) {
                    Text("Start", color = Color.White)
                }
            }
        }
    }
}
