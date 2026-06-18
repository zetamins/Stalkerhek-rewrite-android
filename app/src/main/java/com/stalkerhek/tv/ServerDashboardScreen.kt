package com.stalkerhek.tv

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
import com.stalkerhek.tv.engine.EngineController
import java.net.NetworkInterface
import java.util.Locale


private fun getLocalIpAddress(): String {
    try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { iface ->
            if (iface.isLoopback || !iface.isUp) return@forEach
            iface.inetAddresses.toList().forEach { addr ->
                val host = addr.hostAddress ?: return@forEach
                if (!host.contains(":") && !host.startsWith("127.") && !host.startsWith("169.254.")) return host
            }
        }
    } catch (_: Exception) {}
    return "127.0.0.1"
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ServerDashboardScreen() {
    val context = LocalContext.current
    val localIp = remember { getLocalIpAddress() }
    val mgmtPort = 4400
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
        } catch (_: Exception) { null }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C09)),
        contentAlignment = Alignment.Center
    ) {
        val scrollState = rememberScrollState()
        val screenW = configuration.screenWidthDp.dp
        val screenH = configuration.screenHeightDp.dp
        val qrSize = if (isLandscape) minOf(screenH * 0.55f, 240.dp) else minOf(screenW * 0.45f, 200.dp)
        val horzPad = maxOf(24.dp, screenW * 0.04f)

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horzPad, maxOf(16.dp, screenH * 0.03f)),
                horizontalArrangement = Arrangement.spacedBy(minOf(48.dp, screenW * 0.06f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeaderSection()
                    StatusPanel(
                        localIp = localIp, runningProfileName = runningProfileName,
                        isRunning = isRunning, channelsCount = channelsCount,
                        hlsPort = hlsPort, proxyPort = proxyPort
                    )
                    RestartButton { restartApp(context) }
                }
                QrCodePanel(qrBitmap = qrBitmap, mgmtUrl = mgmtUrl, size = qrSize)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horzPad, maxOf(16.dp, screenH * 0.03f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HeaderSection()
                QrCodePanel(qrBitmap = qrBitmap, mgmtUrl = mgmtUrl, size = qrSize)
                StatusPanel(
                    localIp = localIp, runningProfileName = runningProfileName,
                    isRunning = isRunning, channelsCount = channelsCount,
                    hlsPort = hlsPort, proxyPort = proxyPort
                )
                RestartButton { restartApp(context) }
            }
        }
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
            text = "Stalkerhek Server",
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
            fontSize = minOf(18.sp, (size.value * 0.075f).sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Scan to open settings dashboard",
            color = Color(0xFF6B806D),
            fontSize = minOf(12.sp, (size.value * 0.05f).sp),
            textAlign = TextAlign.Center
        )
    }
}
