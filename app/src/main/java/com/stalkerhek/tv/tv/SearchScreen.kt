package com.stalkerhek.tv.tv

import com.stalkerhek.tv.util.encodeUrl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.stalkerhek.tv.engine.Channel
import com.stalkerhek.tv.engine.EngineController
import kotlinx.coroutines.delay
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val profileId by EngineController.activeProfileId.collectAsState()
    val profileStatus by EngineController.activeProfile.collectAsState()
    val hlsAddr = profileStatus?.hlsAddr ?: ":4600"

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var cachedChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }

    // Load channel list once when screen appears or profile changes
    LaunchedEffect(profileId) {
        if (profileId == 0) return@LaunchedEffect
        cachedChannels = try { EngineController.getChannels(profileId, "itv").filter { it.enabled } }
                         catch (_: Exception) { emptyList() }
    }

    LaunchedEffect(query) {
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(300)
        isSearching = true
        // Search in-memory cached list — no JNI call per keystroke
        results = cachedChannels.filter {
            it.title.contains(query, ignoreCase = true) || it.genre.contains(query, ignoreCase = true)
        }.take(100)
        isSearching = false
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080C09)).padding(24.dp)) {
        Text("Search Channels", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(bottom = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111A14), RoundedCornerShape(12.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(Color(0xFF2D8A4E)),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Type to search channels...", color = Color(0xFF8BA38D), fontSize = 18.sp)
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                Text("✕", color = Color(0xFF8BA38D), fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp).clickable { query = "" })
            }
        }

        Spacer(Modifier.height(8.dp))
        if (isSearching) {
            Text("Searching...", color = Color(0xFF8BA38D), fontSize = 14.sp, modifier = Modifier.padding(8.dp))
        } else if (results.isEmpty() && query.length >= 2) {
            Text("No channels found for \"$query\"", color = Color(0xFF8BA38D), fontSize = 14.sp, modifier = Modifier.padding(8.dp))
        } else {
            Text("${results.size} results", color = Color(0xFF8BA38D), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(results) { ch ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF111A14), RoundedCornerShape(10.dp))
                        .clickable {
                            val url = "http://127.0.0.1$hlsAddr/${ch.title.encodeUrl()}"
                            context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                                putExtra("url", url); putExtra("title", ch.title)
                            })
                        }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ch.title, color = Color.White, fontSize = 14.sp)
                        Text(ch.genre, color = Color(0xFF8BA38D), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
