@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.stalkerhek.tv.tv
import com.stalkerhek.tv.util.encodeUrl
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.stalkerhek.tv.engine.Channel
import com.stalkerhek.tv.engine.EngineController
import com.stalkerhek.tv.persistence.FavouritesRepository
import com.stalkerhek.tv.persistence.WatchHistoryRepository
import com.stalkerhek.tv.persistence.WatchHistoryEntry
import kotlinx.coroutines.delay
enum class ChannelView { ALL, GENRE, FAVOURITES, HISTORY }
@Composable
fun ChannelGridScreen(navController: NavController) {
    val context = LocalContext.current
    val profileId by EngineController.activeProfileId.collectAsState()
    val profileStatus by EngineController.activeProfile.collectAsState()
    val hlsAddr = profileStatus?.hlsAddr ?: ":4600"
    var allChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf("") }
    var currentView by remember { mutableStateOf(ChannelView.ALL) }
    var isLoading by remember { mutableStateOf(true) }
    var toastMsg by remember { mutableStateOf("") }
    var favouriteCount by remember { mutableStateOf(0) }
    val displayChannels = remember(allChannels, selectedGenre, currentView, profileId) {
        var list = allChannels.filter { it.enabled }
        when (currentView) {
            ChannelView.FAVOURITES -> {
                val favs = FavouritesRepository.getFavourites(profileId)
                list = list.filter { it.cmd in favs }
            }
            ChannelView.HISTORY -> {
                val history = WatchHistoryRepository.getHistory(profileId).map { it.cmd }
                list = history.mapNotNull { cmd -> list.find { it.cmd == cmd } }
            }
            else -> {
                if (selectedGenre.isNotEmpty()) list = list.filter { it.genre == selectedGenre }
            }
        }
        list
    }
    LaunchedEffect(profileId) {
        if (profileId == 0) { isLoading = false; return@LaunchedEffect }
        isLoading = true
        allChannels = try { EngineController.getChannels(profileId, "itv") } catch (_: Exception) { emptyList() }
        genres = allChannels.map { it.genre }.filter { it.isNotEmpty() }.distinct().sorted()
        favouriteCount = FavouritesRepository.getFavourites(profileId).size
        isLoading = false
    }
    // Toast auto-dismiss
    LaunchedEffect(toastMsg) {
        if (toastMsg.isNotEmpty()) {
            kotlinx.coroutines.delay(2000)
            toastMsg = ""
        }
    }
    fun playChannel(channel: Channel) {
        WatchHistoryRepository.record(WatchHistoryEntry(
            profileId = profileId, cmd = channel.cmd, title = channel.title,
            genre = channel.genre, logo = channel.logo
        ))
        context.startActivity(Intent(context, PlayerActivity::class.java).apply {
            putExtra("url", "http://127.0.0.1$hlsAddr/${channel.title.encodeUrl()}")
            putExtra("title", channel.title)
            putExtra("cmd", channel.cmd)
            putExtra("profileId", profileId)
        })
    }
    fun toggleFavourite(channel: Channel) {
        val added = FavouritesRepository.toggle(profileId, channel.cmd)
        favouriteCount = FavouritesRepository.getFavourites(profileId).size
        toastMsg = if (added) "⭐ Added to favourites" else "Removed from favourites"
    }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080C09))) {
        // Top bar with tabs
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0C120E)).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // View tabs
            listOf(
                ChannelView.ALL to "📺 All",
                ChannelView.FAVOURITES to "⭐ Fav ${if (favouriteCount > 0) "($favouriteCount)" else ""}",
                ChannelView.HISTORY to "🕐 History",
            ).forEach { (view, label) ->
                val selected = currentView == view
                Box(
                    modifier = Modifier.clickable { currentView = view; selectedGenre = "" }
                        .background(if (selected) Color(0xFF2D8A4E) else Color(0xFF111A14), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, color = if (selected) Color.White else Color(0xFF8BA38D), fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.weight(1f))
            // Search icon
            Box(
                modifier = Modifier.clickable { navController.navigate("search") }
                    .background(Color(0xFF111A14), RoundedCornerShape(8.dp)).padding(10.dp)
            ) {
                Text("🔍", fontSize = 16.sp)
            }
            // EPG icon
            Box(
                modifier = Modifier.clickable { navController.navigate("epg") }
                    .background(Color(0xFF111A14), RoundedCornerShape(8.dp)).padding(10.dp)
            ) {
                Text("📅", fontSize = 16.sp)
            }
            // VOD icon
            Box(
                modifier = Modifier.clickable { navController.navigate("vod") }
                    .background(Color(0xFF111A14), RoundedCornerShape(8.dp)).padding(10.dp)
            ) {
                Text("🎬", fontSize = 16.sp)
            }
        }
        // Genre strip (only in ALL view)
        if (currentView == ChannelView.ALL && genres.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0F0B)).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { CategoryChip("All", selectedGenre.isEmpty()) { selectedGenre = "" } }
                items(genres) { genre -> CategoryChip(genre, selectedGenre == genre) { selectedGenre = genre } }
            }
        }
        // Channel count
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("${displayChannels.size} channels", color = Color(0xFF4A6A54), fontSize = 11.sp)
        }
        when {
            isLoading -> LoadingGrid()
            displayChannels.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (currentView == ChannelView.FAVOURITES) "⭐" else if (currentView == ChannelView.HISTORY) "🕐" else "📺", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(when (currentView) {
                        ChannelView.FAVOURITES -> "No favourites yet
Long-press a channel to add"
                        ChannelView.HISTORY -> "No watch history yet"
                        else -> "No channels found"
                    }, color = Color(0xFF8BA38D), fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayChannels, key = { it.cmd }) { channel ->
                    val isFav = FavouritesRepository.isFavourite(profileId, channel.cmd)
                    ChannelCard(
                        channel = channel,
                        isFavourite = isFav,
                        onClick = { playChannel(channel) },
                        onLongClick = { toggleFavourite(channel) }
                    )
                }
            }
        }
    }
    // Toast
    if (toastMsg.isNotEmpty()) {
        Box(Modifier.fillMaxSize().padding(bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier.background(Color(0xEE111A14), RoundedCornerShape(12.dp)).padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(toastMsg, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
@Composable
fun ChannelCard(channel: Channel, isFavourite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier.aspectRatio(16f / 9f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(Color(0xFF111A14), RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = channel.logo.ifEmpty { null },
            contentDescription = channel.title,
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentScale = ContentScale.Fit
        )
        // Title overlay at bottom
        Box(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color(0xCC000000), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(channel.title, color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Favourite star
        if (isFavourite) {
            Text("⭐", fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}
@Composable
fun LoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(32) {
            Box(
                modifier = Modifier.aspectRatio(16f / 9f).background(Color(0xFF111A14), RoundedCornerShape(8.dp))
            )
        }
    }
}
