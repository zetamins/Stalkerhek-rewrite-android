package com.stalkerhek.tv.tv

import com.stalkerhek.tv.util.encodeUrl

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.stalkerhek.tv.engine.Channel
import com.stalkerhek.tv.engine.EngineController

@Composable
fun VodScreen(navController: NavController) {
    val context = LocalContext.current
    val profileId by EngineController.activeProfileId.collectAsState()
    val profileStatus by EngineController.activeProfile.collectAsState()
    val hlsAddr = profileStatus?.hlsAddr ?: ":4600"
    val proxyAddr = profileStatus?.proxyAddr ?: ":4800"

    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("") }
    var allVodChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Derived item list — filtering happens in-memory, no second network call
    val items = remember(allVodChannels, selectedCategory) {
        if (selectedCategory.isEmpty()) allVodChannels
        else allVodChannels.filter { it.genre == selectedCategory }
    }

    LaunchedEffect(profileId) {
        if (profileId == 0) { isLoading = false; return@LaunchedEffect }
        isLoading = true
        val allVod = try { EngineController.getChannels(profileId, "vod") } catch (_: Exception) { emptyList() }
        allVodChannels = allVod
        categories = allVod.map { it.genre }.distinct().filter { it.isNotEmpty() }.sorted()
        selectedCategory = categories.firstOrNull() ?: ""
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080C09))) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎬 VOD", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${items.size} titles", color = Color(0xFF8BA38D), fontSize = 13.sp)
        }

        // Category filter strip
        if (categories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChip("All", selectedCategory.isEmpty()) { selectedCategory = "" }
                }
                items(categories) { cat ->
                    CategoryChip(cat, selectedCategory == cat) { selectedCategory = cat }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        when {
            isLoading -> LoadingGrid()
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎬", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No VOD content available", color = Color.White, fontSize = 18.sp)
                    Text("Your portal may not support VOD", color = Color(0xFF8BA38D), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { vod ->
                    VodCard(vod) {
                        // VOD uses proxy stream URL
                        val url = "http://127.0.0.1$proxyAddr/c/?action=create_link&type=vod&cmd=${vod.cmd.encodeUrl()}"
                        context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                            putExtra("url", url); putExtra("title", vod.title)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clickable(onClick = onClick).background(
            if (selected) Color(0xFF2D8A4E) else Color(0xFF111A14), RoundedCornerShape(20.dp)
        ).padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) Color.White else Color(0xFF8BA38D), fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun VodCard(vod: Channel, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).background(Color(0xFF111A14), RoundedCornerShape(8.dp)).padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = vod.logo.ifEmpty { null },
            contentDescription = vod.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(Color(0xFF0C120E), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(6.dp))
        Text(vod.title, color = Color.White, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp))
        Text(vod.genre, color = Color(0xFF8BA38D), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
