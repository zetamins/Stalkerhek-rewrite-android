package com.stalkerhek.tv.tv

import com.stalkerhek.tv.util.encodeUrl

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.stalkerhek.tv.engine.Channel
import com.stalkerhek.tv.engine.EngineController
import com.stalkerhek.tv.persistence.FavouritesRepository

@Composable
fun FavouritesScreen(navController: NavController) {
    val context = LocalContext.current
    val profileId by EngineController.activeProfileId.collectAsState()
    val profileStatus by EngineController.activeProfile.collectAsState()
    val hlsAddr = profileStatus?.hlsAddr ?: ":4600"

    var favChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(profileId) {
        if (profileId == 0) { isLoading = false; return@LaunchedEffect }
        isLoading = true
        val allChannels = try { EngineController.getChannels(profileId, "itv") } catch (_: Exception) { emptyList() }
        val favCmds = FavouritesRepository.getFavourites(profileId)
        favChannels = allChannels.filter { it.cmd in favCmds }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080C09)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⭐ Favourites", color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Text("${favChannels.size} channels", color = Color(0xFF8BA38D), fontSize = 13.sp)
        }

        when {
            isLoading -> LoadingGrid()
            favChannels.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No favourites yet", color = Color.White, fontSize = 18.sp)
                    Text("Long-press a channel to add it to favourites", color = Color(0xFF8BA38D), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favChannels) { channel ->
                    FavChannelCard(channel) {
                        context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                            putExtra("url", "http://127.0.0.1$hlsAddr/${channel.title.encodeUrl()}")
                            putExtra("title", channel.title)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun FavChannelCard(channel: Channel, onClick: () -> Unit) {
    Box(
        modifier = Modifier.aspectRatio(16f / 9f).clickable(onClick = onClick)
            .background(Color(0xFF111A14), RoundedCornerShape(8.dp)).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AsyncImage(model = channel.logo.ifEmpty { null }, contentDescription = channel.title,
                modifier = Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(4.dp))
            Text(channel.title, color = Color.White, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}
