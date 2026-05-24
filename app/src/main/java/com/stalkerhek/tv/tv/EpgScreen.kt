package com.stalkerhek.tv.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.stalkerhek.tv.engine.Channel
import com.stalkerhek.tv.engine.EngineController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class EpgEntry(
    val channelId: String,
    val title: String,
    val description: String,
    val start: Long,
    val stop: Long,
) {
    val isLive: Boolean get() {
        val now = System.currentTimeMillis() / 1000
        return now in start..stop
    }
    val progressPercent: Float get() {
        val now = System.currentTimeMillis() / 1000
        if (stop <= start) return 0f
        return ((now - start).toFloat() / (stop - start)).coerceIn(0f, 1f)
    }
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startStr = sdf.format(Date(start * 1000))
        val stopStr  = sdf.format(Date(stop  * 1000))
        return "$startStr – $stopStr"
    }
}

data class ChannelEpg(val channel: Channel, val entries: List<EpgEntry>)

@Composable
fun EpgScreen(navController: NavController) {
    val profileId by EngineController.activeProfileId.collectAsState()
    val profileStatus by EngineController.activeProfile.collectAsState()
    val hlsAddr = profileStatus?.hlsAddr ?: ":4600"

    var channelEpgs by remember { mutableStateOf<List<ChannelEpg>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(profileId) {
        if (profileId == 0) { isLoading = false; return@LaunchedEffect }
        isLoading = true
        errorMsg = ""
        try {
            val channels = EngineController.getChannels(profileId, "itv").filter { it.enabled }.take(50)
            val epgData = withContext(Dispatchers.IO) {
                channels.mapNotNull { ch ->
                    try {
                        val encoded = ch.title.encodeUrl()
                        val url = "http://127.0.0.1$hlsAddr/epg/$encoded"
                        val text = URL(url).readText()
                        val entries = parseEpgJson(text)
                        if (entries.isEmpty()) null else ChannelEpg(ch, entries)
                    } catch (_: Exception) { null }
                }
            }
            channelEpgs = epgData
        } catch (e: Exception) {
            errorMsg = e.message ?: "Failed to load EPG"
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080C09)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("TV Guide", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date()), color = Color(0xFF8BA38D), fontSize = 13.sp)
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Loading EPG...", color = Color(0xFF8BA38D), fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Fetching programme guide for ${50} channels", color = Color(0xFF4A6A54), fontSize = 12.sp)
                }
            }
            errorMsg.isNotEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMsg, color = Color(0xFFE85D4D), fontSize = 14.sp)
            }
            channelEpgs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📺", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No EPG data available", color = Color.White, fontSize = 18.sp)
                    Text("Your portal may not support EPG", color = Color(0xFF8BA38D), fontSize = 13.sp)
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(channelEpgs) { channelEpg ->
                    EpgChannelRow(channelEpg)
                }
            }
        }
    }
}

@Composable
fun EpgChannelRow(channelEpg: ChannelEpg) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0C120E)).padding(vertical = 4.dp)) {
        Text(
            channelEpg.channel.title,
            color = Color(0xFF2D8A4E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Row(modifier = Modifier.horizontalScroll(scrollState).padding(horizontal = 8.dp)) {
            channelEpg.entries.forEach { entry ->
                EpgEntryCard(entry)
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun EpgEntryCard(entry: EpgEntry) {
    val bgColor = if (entry.isLive) Color(0xFF1A2C1F) else Color(0xFF111A14)
    val borderColor = if (entry.isLive) Color(0xFF2D8A4E) else Color(0xFF1A2C1F)
    Box(
        modifier = Modifier.width(160.dp).background(bgColor, RoundedCornerShape(8.dp))
            .padding(1.dp).background(bgColor, RoundedCornerShape(7.dp)).padding(10.dp)
    ) {
        Column {
            Text(entry.formattedTime(), color = Color(0xFF8BA38D), fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            Text(entry.title, color = Color.White, fontSize = 12.sp, fontWeight = if (entry.isLive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.isLive) {
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF1A2C1F), RoundedCornerShape(1.dp))) {
                    Box(modifier = Modifier.fillMaxWidth(entry.progressPercent).height(2.dp).background(Color(0xFF2D8A4E), RoundedCornerShape(1.dp)))
                }
                Spacer(Modifier.height(2.dp))
                Text("LIVE", color = Color(0xFF3FB970), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun parseEpgJson(json: String): List<EpgEntry> {
    return try {
        val root = JSONObject(json)
        val entries = mutableListOf<EpgEntry>()
        val data = root.optJSONObject("js")?.optJSONArray("data") ?: return emptyList()
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            entries.add(EpgEntry(
                channelId = item.optString("ch_id"),
                title = item.optString("name").ifEmpty { item.optString("title") },
                description = item.optString("descr"),
                start = item.optLong("start_timestamp").takeIf { it > 0 } ?: item.optLong("time"),
                stop  = item.optLong("stop_timestamp").takeIf { it > 0 } ?: item.optLong("time_to"),
            ))
        }
        entries
    } catch (_: Exception) { emptyList() }
}
