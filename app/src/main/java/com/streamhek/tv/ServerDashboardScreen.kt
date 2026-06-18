package com.streamhek.tv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.streamhek.tv.engine.EngineController

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ServerDashboardScreen() {
    val context = LocalContext.current
    val localIp = remember { getLocalIpAddress() }
    val mgmtPort = Constants.MGMT_PORT
    val mgmtUrl = "http://$localIp:$mgmtPort/dashboard"

    val activeProfileId by EngineController.activeProfileId.collectAsState()
    val activeProfileStatus by EngineController.activeProfile.collectAsState()
    val profiles by EngineController.profiles.collectAsState()

    val runningProfileName = remember(activeProfileId, profiles) {
        profiles.find { it.id == activeProfileId }?.name ?: "None"
    }

    val isRunning = activeProfileStatus?.running == true
    val channelsCount = activeProfileStatus?.channelsCount ?: 0
    val hlsPort = activeProfileStatus?.hlsAddr?.substringAfter(":")?.toIntOrNull() ?: 4600
    val proxyPort = activeProfileStatus?.proxyAddr?.substringAfter(":")?.toIntOrNull() ?: 4800

    val qrBitmap = remember(mgmtUrl) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(mgmtUrl, BarcodeFormat.QR_CODE, 512, 512)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) { android.util.Log.w("StreamHek", "QR generation failed", e); null }
    }

    val configuration = LocalConfiguration.current
    val screenW = configuration.screenWidthDp.dp
    val screenH = configuration.screenHeightDp.dp
    val aspectRatio = configuration.screenWidthDp.toFloat() / maxOf(1, configuration.screenHeightDp)
    val isLandscape = aspectRatio > 1.1f
    val isUltrawide = aspectRatio > 1.9f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C09)),
        contentAlignment = Alignment.Center
    ) {
        val scrollState = rememberScrollState()
        // TV overscan safety: 5% margin on all sides
        val overscanMargin = if (screenW > 600.dp) (screenW.coerceAtMost(screenH)) * 0.05f else 0.dp
        val safeW = screenW - overscanMargin * 2
        val safeH = screenH - overscanMargin * 2
        val qrSize = when {
            isUltrawide -> (safeH * 0.6f).coerceAtMost(280.dp)
            isLandscape -> (safeH * 0.5f).coerceAtMost(220.dp)
            else -> (safeW * 0.4f).coerceAtMost(180.dp)
        }
        val horzPad = (overscanMargin + 16.dp).coerceAtLeast(screenW * 0.03f)
        val vertPad = (overscanMargin + 12.dp).coerceAtLeast(screenH * 0.02f)

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = horzPad, vertical = vertPad),
                horizontalArrangement = Arrangement.spacedBy((48.dp).coerceAtMost(safeW * 0.06f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(minOf(16.dp, safeH * 0.025f))
                ) {
                    HeaderSection()
                    StatusPanel(
                        localIp = localIp, runningProfileName = runningProfileName,
                        isRunning = isRunning, channelsCount = channelsCount,
                        hlsPort = hlsPort, proxyPort = proxyPort
                    )
                    RestartButton { restartApp(context) }
                    PrivacyPolicyCard()
                }
                if (isUltrawide) {
                    Spacer(Modifier.width(minOf(32.dp, safeW * 0.03f)))
                }
                QrCodePanel(qrBitmap = qrBitmap, mgmtUrl = mgmtUrl, size = qrSize)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = horzPad, vertical = vertPad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(minOf(20.dp, safeH * 0.03f))
            ) {
                HeaderSection()
                QrCodePanel(qrBitmap = qrBitmap, mgmtUrl = mgmtUrl, size = qrSize)
                StatusPanel(
                    localIp = localIp, runningProfileName = runningProfileName,
                    isRunning = isRunning, channelsCount = channelsCount,
                    hlsPort = hlsPort, proxyPort = proxyPort
                )
                RestartButton { restartApp(context) }
                PrivacyPolicyCard()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PrivacyPolicyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color(0xFF0C120E), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF16251A), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "PRIVACY POLICY",
            color = Color(0xFF4A6A54),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "StreamHek processes all IPTV data locally on your device. No personal data, viewing habits, or credentials are collected, stored externally, or shared with any third party. The app communicates only with the IPTV portal you configure. No analytics, no tracking, no ads.",
            color = Color(0xFF8BA38D),
            fontSize = 11.sp
        )
    }
}

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RestartButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16251A))
            .border(1.dp, Color(0xFF2D8A4E), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("↺", color = Color(0xFF2D8A4E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("RESTART SERVER APP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderSection() {
    Column {
        Text(
            text = "StreamHek Server",
            color = Color(0xFFFFFFFF),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "STB emulation engine & streaming server running in background",
            color = Color(0xFF8BA38D),
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusPanel(
    localIp: String,
    runningProfileName: String,
    isRunning: Boolean,
    channelsCount: Int,
    hlsPort: Int,
    proxyPort: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C120E), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF16251A), RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SYSTEM STATUS",
            color = Color(0xFF4A6A54),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Status Indicators
        StatusIndicatorRow(
            label = "Management Dashboard UI",
            status = "Online",
            indicatorColor = Color(0xFF2D8A4E)
        )

        StatusIndicatorRow(
            label = "IPTV Engine",
            status = if (isRunning) "Active (Streaming)" else "Stopped",
            indicatorColor = if (isRunning) Color(0xFF2D8A4E) else Color(0xFF6B806D)
        )

        StatusIndicatorRow(
            label = "Active Profile",
            status = if (isRunning) runningProfileName else "None",
            indicatorColor = if (isRunning) Color(0xFF2D8A4E) else Color(0xFF6B806D)
        )

        if (isRunning) {
            StatusIndicatorRow(
                label = "Channels Indexed",
                status = "$channelsCount channels",
                indicatorColor = Color(0xFF2D8A4E)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF16251A))
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Service Endpoints
        EndpointRow(label = "Dashboard Link", value = "http://$localIp:4400")
        EndpointRow(label = "Proxy API Link", value = "http://$localIp:$proxyPort")
        EndpointRow(label = "HLS Stream Link", value = "http://$localIp:$hlsPort")
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusIndicatorRow(label: String, status: String, indicatorColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF8BA38D),
            fontSize = 14.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(indicatorColor, CircleShape)
            )
            Text(
                text = status,
                color = Color(0xFFE2ECE3),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EndpointRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF6B806D),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color(0xFF2D8A4E),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QrCodePanel(qrBitmap: Bitmap?, mgmtUrl: String, size: androidx.compose.ui.unit.Dp = 240.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(size * 0.05f),
            contentAlignment = Alignment.Center
        ) {
            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Dashboard QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(minOf(16.dp, size * 0.06f)))
        Text(
            text = mgmtUrl,
            color = Color(0xFF2D8A4E),
            fontSize = if (18f < size.value * 0.075f) 18.sp else (size.value * 0.075f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Scan to open settings dashboard",
            color = Color(0xFF6B806D),
            fontSize = if (12f < size.value * 0.05f) 12.sp else (size.value * 0.05f).sp,
            textAlign = TextAlign.Center
        )
    }
}
