package com.stalkerhek.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.stalkerhek.tv.engine.EngineController
import com.stalkerhek.tv.engine.EngineState
import com.stalkerhek.tv.tv.*

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val engineState by EngineController.engineState.collectAsState()

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080C09))) {
                when (engineState) {

                    // ── Loading screen while Rust engine initialises ──────────
                    is EngineState.Uninitialized,
                    is EngineState.Initializing -> {
                        EngineLoadingScreen()
                    }

                    // ── Error ─────────────────────────────────────────────────
                    is EngineState.Error -> {
                        val msg = (engineState as EngineState.Error).message
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚠", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("Engine failed to start", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(msg, color = Color(0xFF8BA38D), fontSize = 13.sp)
                            }
                        }
                    }

                    // ── Ready ─────────────────────────────────────────────────
                    is EngineState.Ready -> {
                        ServerDashboardScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EngineLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Animated logo
            Box(
                modifier = Modifier.size(80.dp).background(Color(0xFF2D8A4E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color.White, fontSize = 32.sp)
            }
            Text("Stalkerhek", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Starting engine...", color = Color(0xFF8BA38D), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            // Simple loading dots
            LoadingDots()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingDots() {
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            tick = (tick + 1) % 4
        }
    }
    Text(
        "●".repeat(tick + 1).padEnd(3, '○').replace("", " ").trim(),
        color = Color(0xFF2D8A4E),
        fontSize = 20.sp,
        letterSpacing = 4.sp
    )
}
