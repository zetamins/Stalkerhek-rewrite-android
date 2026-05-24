package com.stalkerhek.tv.tv
import com.stalkerhek.tv.util.encodeUrl
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.lifecycle.lifecycleScope
import com.stalkerhek.tv.engine.EngineController
import com.stalkerhek.tv.persistence.WatchHistoryRepository
import com.stalkerhek.tv.persistence.WatchHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var streamUrl = ""
    private var channelTitle = ""
    private var channelCmd = ""
    private var profileId = 0
    // Compose state updaters — set from Player.Listener (main thread)
    private var setIsBuffering: ((Boolean) -> Unit)? = null
    private var setErrorMsg: ((String) -> Unit)? = null
    private var setDialDisplay: ((String) -> Unit)? = null
    // Channel number dialling
    private val dialBuffer = StringBuilder()
    private val dialHandler = Handler(Looper.getMainLooper())
    private val dialRunnable = Runnable { commitChannelDial() }
    // Auto-reconnect
    private var reconnectAttempts = 0
    private val maxReconnects = 5
    private val reconnectHandler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        streamUrl = intent.getStringExtra("url") ?: run { finish(); return }
        channelTitle = intent.getStringExtra("title") ?: ""
        channelCmd = intent.getStringExtra("cmd") ?: ""
        profileId = intent.getIntExtra("profileId", 0)
        buildPlayer()
        setContent {
            var isBuffering by remember { mutableStateOf(true) }
            var errorMsg by remember { mutableStateOf("") }
            var showOsd by remember { mutableStateOf(true) }
            var dialDisplay by remember { mutableStateOf("") }
            // Wire state updaters so Player.Listener can update Compose state
            DisposableEffect(Unit) {
                setIsBuffering = { v -> isBuffering = v }
                setErrorMsg = { v -> errorMsg = v }
                setDialDisplay = { v -> dialDisplay = v }
                onDispose { setIsBuffering = null; setErrorMsg = null; setDialDisplay = null }
            }
            // Record watch start
            LaunchedEffect(Unit) {
                if (channelCmd.isNotEmpty() && profileId > 0) {
                    WatchHistoryRepository.record(WatchHistoryEntry(
                        profileId = profileId, cmd = channelCmd, title = channelTitle,
                        genre = "", logo = ""
                    ))
                }
                delay(3000)
                showOsd = false
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = this@PlayerActivity.player
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            useController = false // Custom OSD
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // OSD overlay
                if (showOsd) {
                    Box(Modifier.fillMaxSize().background(Color(0x88000000))) {
                        Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                            Text(channelTitle, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (isBuffering) Text("Buffering...", color = Color(0xFF8BA38D), fontSize = 14.sp)
                        }
                        // PiP button
                        if (supportsPip()) {
                            Text("⊡ PiP", color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp))
                        }
                    }
                }
                // Error overlay
                if (errorMsg.isNotEmpty()) {
                    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠", fontSize = 48.sp, color = Color(0xFFE85D4D))
                            Spacer(Modifier.height(12.dp))
                            Text(errorMsg, color = Color.White, fontSize = 16.sp)
                            Text("Reconnecting... ($reconnectAttempts/$maxReconnects)", color = Color(0xFF8BA38D), fontSize = 13.sp)
                        }
                    }
                }
                // Channel number dial display
                if (dialDisplay.isNotEmpty()) {
                    Box(Modifier.align(Alignment.TopStart).padding(24.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(dialDisplay, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    private fun buildPlayer() {
        player?.release()
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> setIsBuffering?.invoke(true)
                            Player.STATE_READY     -> { setIsBuffering?.invoke(false); setErrorMsg?.invoke(""); reconnectAttempts = 0 }
                            else -> {}
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        if (reconnectAttempts < maxReconnects) {
                            reconnectAttempts++
                            setErrorMsg?.invoke(error.localizedMessage ?: "Playback error")
                            val delayMs = (2000L * reconnectAttempts).coerceAtMost(30_000L)
                            reconnectHandler.postDelayed({
                                player?.let {
                                    it.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
                                    it.prepare()
                                    it.play()
                                }
                            }, delayMs)
                        } else {
                            finish()
                        }
                    }
                })
            }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Channel number dialling via remote number buttons
        if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            val digit = keyCode - KeyEvent.KEYCODE_0
            dialBuffer.append(digit)
            setDialDisplay?.invoke(dialBuffer.toString())
            dialHandler.removeCallbacks(dialRunnable)
            dialHandler.postDelayed(dialRunnable, 2000)
            return true
        }
        // Back = previous channel (handled by system back stack)
        // PiP on menu/options key
        if (keyCode == KeyEvent.KEYCODE_MENU && supportsPip()) {
            enterPip(); return true
        }
        return super.onKeyDown(keyCode, event)
    }
    private fun commitChannelDial() {
        val number = dialBuffer.toString().toIntOrNull() ?: run { dialBuffer.clear(); setDialDisplay?.invoke(""); return }
        dialBuffer.clear()
        setDialDisplay?.invoke("")
        if (number < 1) return
        // Look up the nth enabled channel (1-based) and switch to it
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val channels = EngineController.getChannels(profileId, "itv")
                    .filter { it.enabled }
                val target = channels.getOrNull(number - 1) ?: return@launch
                val hlsAddr = EngineController.activeProfile.value?.hlsAddr ?: ":4600"
                val newUrl = "http://127.0.0.1$hlsAddr/${target.title.encodeUrl()}"
                streamUrl = newUrl
                channelTitle = target.title
                channelCmd = target.cmd
                withContext(Dispatchers.Main) {
                    player?.let {
                        it.setMediaItem(MediaItem.fromUri(Uri.parse(newUrl)))
                        it.prepare()
                        it.play()
                    }
                }
            } catch (_: Exception) {}
        }
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (supportsPip() && player?.isPlaying == true) enterPip()
    }
    private fun supportsPip(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Hide system UI in PiP mode
    }
    override fun onStop() {
        super.onStop()
        // Save watch position for resume
        player?.let { p ->
            if (channelCmd.isNotEmpty() && profileId > 0) {
                WatchHistoryRepository.updatePosition(profileId, channelCmd, p.currentPosition)
            }
        }
        if (!isInPictureInPictureMode) {
            player?.pause()
        }
    }
    override fun onDestroy() {
        reconnectHandler.removeCallbacksAndMessages(null)
        dialHandler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }
}
