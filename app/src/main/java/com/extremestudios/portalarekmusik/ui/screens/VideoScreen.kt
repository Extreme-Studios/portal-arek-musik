@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.extremestudios.portalarekmusik.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.extremestudios.portalarekmusik.data.model.VideoItem
import com.extremestudios.portalarekmusik.data.model.VideoBand
import com.extremestudios.portalarekmusik.data.local.VideoFavoritesLocalStore
import com.extremestudios.portalarekmusik.playback.PlaybackManager
import com.extremestudios.portalarekmusik.R
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Custom Vector Icons for Fit Screen
val fitScreenIcon: ImageVector
    @Composable
    get() = remember {
        ImageVector.Builder(
            name = "FitScreen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.White), strokeLineWidth = 2f) {
                // Top-Left corner
                moveTo(4f, 8f)
                lineTo(4f, 4f)
                lineTo(8f, 4f)
                
                // Top-Right corner
                moveTo(16f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 8f)
                
                // Bottom-Left corner
                moveTo(4f, 16f)
                lineTo(4f, 20f)
                lineTo(8f, 20f)
                
                // Bottom-Right corner
                moveTo(16f, 20f)
                lineTo(20f, 20f)
                lineTo(20f, 16f)
            }
        }.build()
    }

val exitFitScreenIcon: ImageVector
    @Composable
    get() = remember {
        ImageVector.Builder(
            name = "ExitFitScreen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.White), strokeLineWidth = 2f) {
                // Top-Left corner pointing inward
                moveTo(8f, 4f)
                lineTo(8f, 8f)
                lineTo(4f, 8f)
                
                // Top-Right corner pointing inward
                moveTo(16f, 4f)
                lineTo(16f, 8f)
                lineTo(20f, 8f)
                
                // Bottom-Left corner pointing inward
                moveTo(8f, 20f)
                lineTo(8f, 16f)
                lineTo(4f, 16f)
                
                // Bottom-Right corner pointing inward
                moveTo(16f, 20f)
                lineTo(16f, 16f)
                lineTo(20f, 16f)
            }
        }.build()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    bands: List<VideoBand>,
    videos: List<VideoItem>,
    selectedArtist: String?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onArtistSelected: (String) -> Unit,
    onBackToBands: () -> Unit,
    deletableChannelArtists: Set<String> = emptySet(),
    onDeleteChannel: (String) -> Unit = {},
    deletableVideoIds: Set<String> = emptySet(),
    onDeleteVideo: (String) -> Unit = {},
    onEditVideo: (String, String) -> Unit = { _, _ -> },
    favoriteVideos: List<VideoItem> = emptyList(),
    onAddFavoriteVideo: (String, String) -> VideoFavoritesLocalStore.AddResult = { _, _ -> VideoFavoritesLocalStore.AddResult.InvalidUrl },
    onRemoveFavoriteVideo: (String) -> Unit = {},
    onEditFavoriteVideo: (String, String, String) -> VideoFavoritesLocalStore.UpdateResult = { _, _, _ -> VideoFavoritesLocalStore.UpdateResult.NotFound },
    onVideoActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
    var channelPendingDeletion by remember { mutableStateOf<String?>(null) }
    var videoPendingDeletion by remember { mutableStateOf<VideoItem?>(null) }
    var videoPendingEdit by remember { mutableStateOf<VideoItem?>(null) }
    var editedVideoTitle by remember { mutableStateOf("") }
    var showFavoritePlaylist by remember { mutableStateOf(false) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var favoriteUrl by remember { mutableStateOf("") }
    var favoriteTitle by remember { mutableStateOf("") }
    var favoriteInputError by remember { mutableStateOf<String?>(null) }
    var favoritePendingEdit by remember { mutableStateOf<VideoItem?>(null) }

    if (showAddFavoriteDialog) {
        AlertDialog(
            onDismissRequest = { showAddFavoriteDialog = false },
            title = { Text("Tambah favorit MV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Simpan link video YouTube ke playlist pribadi perangkat ini.")
                    OutlinedTextField(
                        value = favoriteUrl,
                        onValueChange = { favoriteUrl = it; favoriteInputError = null },
                        label = { Text("Link video YouTube") },
                        singleLine = true,
                        isError = favoriteInputError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = favoriteTitle,
                        onValueChange = { favoriteTitle = it },
                        label = { Text("Judul (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    favoriteInputError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when (onAddFavoriteVideo(favoriteUrl, favoriteTitle)) {
                        VideoFavoritesLocalStore.AddResult.Added -> {
                            favoriteUrl = ""
                            favoriteTitle = ""
                            favoriteInputError = null
                            showFavoritePlaylist = true
                            showAddFavoriteDialog = false
                        }
                        VideoFavoritesLocalStore.AddResult.InvalidUrl -> favoriteInputError = "Masukkan link video YouTube yang valid."
                        VideoFavoritesLocalStore.AddResult.AlreadyExists -> favoriteInputError = "Video ini sudah ada di favorit."
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAddFavoriteDialog = false }) { Text("Batal") } }
        )
    }

    favoritePendingEdit?.let { video ->
        AlertDialog(
            onDismissRequest = { favoritePendingEdit = null },
            title = { Text("Edit favorit MV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = favoriteUrl,
                        onValueChange = { favoriteUrl = it; favoriteInputError = null },
                        label = { Text("Link video YouTube") },
                        singleLine = true,
                        isError = favoriteInputError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = favoriteTitle,
                        onValueChange = { favoriteTitle = it },
                        label = { Text("Judul video") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    favoriteInputError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when (onEditFavoriteVideo(video.id, favoriteUrl, favoriteTitle)) {
                        VideoFavoritesLocalStore.UpdateResult.Updated -> {
                            favoritePendingEdit = null
                            favoriteInputError = null
                        }
                        VideoFavoritesLocalStore.UpdateResult.InvalidUrl -> favoriteInputError = "Masukkan link video YouTube yang valid."
                        VideoFavoritesLocalStore.UpdateResult.AlreadyExists -> favoriteInputError = "Video ini sudah ada di favorit."
                        VideoFavoritesLocalStore.UpdateResult.NotFound -> favoriteInputError = "Favorit sudah tidak ditemukan."
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { favoritePendingEdit = null }) { Text("Batal") } }
        )
    }

    channelPendingDeletion?.let { artist ->
        AlertDialog(
            onDismissRequest = { channelPendingDeletion = null },
            title = { Text("Hapus channel?") },
            text = { Text("Link channel $artist akan dihapus dari daftar MV.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteChannel(artist)
                    channelPendingDeletion = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelPendingDeletion = null }) { Text("Batal") }
            }
        )
    }

    videoPendingDeletion?.let { video ->
        AlertDialog(
            onDismissRequest = { videoPendingDeletion = null },
            title = { Text("Hapus video?") },
            text = { Text("${video.title} akan dihapus dari daftar MV.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteVideo(video.id)
                    videoPendingDeletion = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoPendingDeletion = null }) { Text("Batal") }
            }
        )
    }

    videoPendingEdit?.let { video ->
        AlertDialog(
            onDismissRequest = { videoPendingEdit = null },
            title = { Text("Edit judul video") },
            text = {
                OutlinedTextField(
                    value = editedVideoTitle,
                    onValueChange = { editedVideoTitle = it },
                    label = { Text("Judul video") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editedVideoTitle.isNotBlank()) onEditVideo(video.id, editedVideoTitle.trim())
                    videoPendingEdit = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { videoPendingEdit = null }) { Text("Batal") }
            }
        )
    }

    val isVideoActive = selectedVideo != null
    LaunchedEffect(isVideoActive) {
        onVideoActiveChange(isVideoActive)
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsTrigger by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(controlsTrigger) {
        if (isVideoActive) {
            controlsVisible = true
            delay(1000)
            controlsVisible = false
        }
    }

    LaunchedEffect(selectedVideo) {
        if (selectedVideo != null) {
            controlsTrigger = System.currentTimeMillis()
        }
    }

    fun exitVideo() {
        selectedVideo = null
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    BackHandler(enabled = isVideoActive) {
        exitVideo()
    }
    BackHandler(enabled = selectedArtist != null && !isVideoActive) {
        onBackToBands()
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isLandscape, isVideoActive) {
        val window = activity?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isLandscape && isVideoActive) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Restore system bars and orientation on disposal
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val w = activity?.window ?: return@onDispose
            WindowCompat.getInsetsController(w, w.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0B0F),
                        Color(0xFF160E28),
                        Color(0xFF0F0B18)
                    )
                )
            )
    ) {
        val activeVideo = selectedVideo
        if (activeVideo != null) {
            val youtubeVideoId = remember(activeVideo.videoUrl) {
                VideoItem.extractYoutubeVideoId(activeVideo.videoUrl)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = (if (isLandscape) {
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16 / 9f)
                            .background(Color.Black)
                    }).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(pass = PointerEventPass.Initial)
                                controlsTrigger = System.currentTimeMillis()
                            }
                        }
                    }
                ) {
                    var isVideoLoading by remember { mutableStateOf(true) }
                    var videoPlayerError by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(activeVideo.id) {
                        PlaybackManager.pause()
                    }

                    if (activeVideo.videoUrl.isNotBlank()) {
                        if (youtubeVideoId != null) {
                            YoutubeWebViewContent(
                                liveUrl = activeVideo.videoUrl,
                                youtubeVideoId = youtubeVideoId,
                                onLoadingChange = { isVideoLoading = it },
                                onError = { videoPlayerError = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            NativeVideoPlayer(
                                videoUrl = activeVideo.videoUrl,
                                isLandscape = isLandscape,
                                onLoadingChange = { isVideoLoading = it },
                                onError = { videoPlayerError = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (isVideoLoading && videoPlayerError.isNullOrBlank()) {
                        CircularProgressIndicator(
                            color = Color(0xFFA100FF),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    if (!videoPlayerError.isNullOrBlank()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = videoPlayerError ?: "Gagal memutar video",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Button(
                                    onClick = { openExternalUrl(context, activeVideo.videoUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA100FF))
                                ) {
                                    Text("Buka di Browser", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Fitscreen toggle overlay button
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val targetOrientation = if (isLandscape) {
                                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                    activity?.requestedOrientation = targetOrientation
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) exitFitScreenIcon else fitScreenIcon,
                                    contentDescription = if (isLandscape) "Exit Fullscreen" else "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Spacer to position the fitscreen icon above the play button
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // Back button overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        IconButton(
                            onClick = { exitVideo() },
                            modifier = Modifier
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke daftar",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (!isLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = activeVideo.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeVideo.artist,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA100FF)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        if (activeVideo.description.isNotBlank()) {
                            Text(
                                text = "Deskripsi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeVideo.description,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (youtubeVideoId != null) {
                            Button(
                                onClick = { openExternalUrl(context, activeVideo.videoUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Buka di YouTube", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Video List View
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when {
                                    selectedArtist != null -> selectedArtist
                                    showFavoritePlaylist -> "Favorit MV"
                                    else -> "MV Indie"
                                },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    selectedArtist != null -> "Video dari band/channel terpilih"
                                    showFavoritePlaylist -> "Playlist pribadi di perangkat ini"
                                    else -> "Visual dari musik indie terbaik"
                                },
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFFA100FF),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        if (selectedArtist == null) {
                            TextButton(onClick = { showFavoritePlaylist = !showFavoritePlaylist }) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFC5A358), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (showFavoritePlaylist) "Semua MV" else "Favorit (${favoriteVideos.size})",
                                    color = Color(0xFFC5A358),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (!showFavoritePlaylist && !errorMessage.isNullOrBlank() && (if (selectedArtist == null) bands.isEmpty() else videos.isEmpty())) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(bottom = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(errorMessage, color = Color.White.copy(alpha = 0.7f))
                                        Button(
                                            onClick = onRefresh,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA100FF))
                                        ) {
                                            Text("Coba Lagi")
                                        }
                                    }
                                }
                            }
                        } else if (showFavoritePlaylist && favoriteVideos.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillParentMaxSize().padding(bottom = 100.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Belum ada favorit MV.", color = Color.White.copy(alpha = 0.7f))
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { showAddFavoriteDialog = true }) { Text("Tempel Link YouTube") }
                                }
                            }
                        } else if (!showFavoritePlaylist && ((selectedArtist == null && bands.isEmpty()) || (selectedArtist != null && videos.isEmpty()))) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(bottom = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isRefreshing || isLoading) "Memuat MV di latar belakang..." else "Belum ada video tersedia.",
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else if (showFavoritePlaylist) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Playlist Favorit", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { showAddFavoriteDialog = true }) { Text("+ Tambah", color = Color(0xFFC5A358)) }
                                }
                            }
                            items(favoriteVideos, key = { it.id }) { video ->
                                MvVideoRowCard(
                                    video = video,
                                    onClick = { selectedVideo = video },
                                    onDelete = { onRemoveFavoriteVideo(video.id) },
                                    onEdit = {
                                        favoriteUrl = video.videoUrl
                                        favoriteTitle = video.title
                                        favoriteInputError = null
                                        favoritePendingEdit = video
                                    }
                                )
                            }
                        } else if (selectedArtist == null) {
                            item {
                                MvHeroCard(
                                    band = bands.first(),
                                    onExplore = { onArtistSelected(bands.first().name) }
                                )
                            }
                            item {
                                Text("Berdasarkan Band", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(bands, key = { it.name }) { band ->
                                        MvBandCircleCard(
                                            band = band,
                                            onClick = { onArtistSelected(band.name) },
                                            onDelete = if (band.name.trim().lowercase() in deletableChannelArtists) { { channelPendingDeletion = band.name } } else null
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                MvBandProfileHeader(
                                    artist = selectedArtist.orEmpty(),
                                    videoCount = videos.size,
                                    coverUrl = videos.firstOrNull()?.coverUrl.orEmpty(),
                                    onBack = onBackToBands
                                )
                            }
                            item { Text("Daftar MV", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                            items(videos, key = { it.id }) { video ->
                                MvVideoRowCard(
                                    video = video,
                                    onClick = { selectedVideo = video },
                                    onDelete = if (video.id in deletableVideoIds) {
                                        { videoPendingDeletion = video }
                                    } else null,
                                    onEdit = if (video.id in deletableVideoIds) {
                                        { editedVideoTitle = video.title; videoPendingEdit = video }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MvHeroCard(band: VideoBand, onExplore: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .16f))
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = band.coverUrl.ifBlank { R.drawable.app_icon }, contentDescription = band.name,
                contentScale = ContentScale.Crop, placeholder = painterResource(R.drawable.app_icon), error = painterResource(R.drawable.app_icon),
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xE6000000), Color(0x80000000), Color.Transparent))))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text("PILIH & TONTON", color = Color(0xFFC5A358), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text("MV TERBARU", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Jelajahi video dari ${band.name}", color = Color.White.copy(alpha = .84f), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onExplore, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A358), contentColor = Color(0xFF17120A)), shape = RoundedCornerShape(22.dp)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(5.dp)); Text("Jelajahi MV")
                }
            }
        }
    }
}

@Composable
private fun MvBandCircleCard(band: VideoBand, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(108.dp)) {
        Box(Modifier.size(88.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = .28f), CircleShape).clickable(onClick = onClick)) {
            AsyncImage(model = band.coverUrl.ifBlank { R.drawable.app_icon }, contentDescription = band.name, contentScale = ContentScale.Crop, placeholder = painterResource(R.drawable.app_icon), error = painterResource(R.drawable.app_icon), modifier = Modifier.fillMaxSize())
            if (onDelete != null) IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).size(30.dp).background(Color.Black.copy(alpha=.65f), CircleShape)) { Icon(Icons.Default.Delete, "Hapus ${band.name}", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(band.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${band.videoCount} video", color = Color.White.copy(alpha = .62f), fontSize = 12.sp)
        OutlinedButton(onClick = onClick, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp), border = BorderStroke(1.dp, Color(0xFFC5A358))) { Text("Buka", color = Color(0xFFC5A358), fontSize = 11.sp) }
    }
}

@Composable
private fun VideoArtistCard(
    artist: String,
    videoCount: Int,
    thumbnailUrl: String,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = artist,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.app_icon),
                error = painterResource(R.drawable.app_icon),
                fallback = painterResource(R.drawable.app_icon),
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$videoCount video",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus channel $artist",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Buka video $artist",
                tint = Color(0xFFA100FF)
            )
        }
    }
}

@Composable
private fun YoutubeWebViewContent(
    liveUrl: String,
    youtubeVideoId: String?,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (liveUrl.isBlank()) return

    val targetUrl = if (youtubeVideoId != null)
        "https://m.youtube.com/watch?v=$youtubeVideoId"
    else
        liveUrl

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            WebView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(false)
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        if (url.isBlank()) return false
                        val scheme = Uri.parse(url).scheme.orEmpty().lowercase()
                        if (scheme.isNotBlank() && scheme != "http" && scheme != "https") {
                            openExternalUrl(viewContext, url)
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChange(false)
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: android.graphics.Bitmap?
                    ) {
                        onLoadingChange(true)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onError("Gagal membuka video.")
                            onLoadingChange(false)
                        }
                    }
                }

                // Custom WebChromeClient to support HTML5 fullscreen video inside webview
                var fullscreenView: View? = null
                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        fullscreenView = view
                        val decorView = (viewContext as? Activity)?.window?.decorView as? ViewGroup
                        decorView?.addView(
                            view,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        val window = (viewContext as? Activity)?.window
                        if (window != null) {
                            WindowCompat.getInsetsController(window, window.decorView).apply {
                                hide(WindowInsetsCompat.Type.systemBars())
                                systemBarsBehavior =
                                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                        }
                    }

                    override fun onHideCustomView() {
                        val decorView = (viewContext as? Activity)?.window?.decorView as? ViewGroup
                        fullscreenView?.let { decorView?.removeView(it) }
                        fullscreenView = null
                        val window = (viewContext as? Activity)?.window
                        if (window != null) {
                            WindowCompat.getInsetsController(window, window.decorView)
                                .show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                }

                loadUrl(targetUrl)
            }
        },
        update = { /* no-op: URL set in factory */ }
    )
}

@Composable
private fun MvBandProfileHeader(artist: String, videoCount: Int, coverUrl: String, onBack: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17171B)), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            AsyncImage(model = coverUrl.ifBlank { R.drawable.app_icon }, contentDescription = artist, contentScale = ContentScale.Crop, placeholder = painterResource(R.drawable.app_icon), error = painterResource(R.drawable.app_icon), modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE9000000)))))
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White); Text("Semua Band", color = Color.White) }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(artist, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text("$videoCount video dari band/channel terpilih", color = Color.White.copy(alpha = .78f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MvVideoRowCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .055f)), border = BorderStroke(1.dp, Color.White.copy(alpha = .12f))) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 146.dp, height = 100.dp).clip(RoundedCornerShape(11.dp))) {
                AsyncImage(model = video.coverUrl.ifBlank { R.drawable.app_icon }, contentDescription = video.title, contentScale = ContentScale.Crop, placeholder = painterResource(R.drawable.app_icon), error = painterResource(R.drawable.app_icon), modifier = Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.Center).size(42.dp).background(Color.Black.copy(alpha=.7f), CircleShape).border(1.dp, Color(0xFFC5A358), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = Color.White) }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(video.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(video.artist, color = Color.White.copy(alpha=.63f), fontSize = 13.sp, maxLines = 1)
                Text("Official MV", color = Color(0xFFC5A358), fontSize = 12.sp, modifier = Modifier.border(1.dp, Color(0xFFC5A358), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (onEdit != null) IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Edit, "Edit", tint = Color.White.copy(alpha=.8f), modifier = Modifier.size(17.dp)) }
                if (onDelete != null) IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp)) }
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f)
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = video.coverUrl.ifBlank { R.drawable.app_icon },
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.app_icon),
                    error = painterResource(R.drawable.app_icon),
                    fallback = painterResource(R.drawable.app_icon),
                    modifier = Modifier.fillMaxSize()
                )
                if (onDelete != null || onEdit != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(22.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (onEdit != null) IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Edit ${video.title}", tint = Color.White)
                        }
                        if (onDelete != null) IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Hapus ${video.title}", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
                // Play Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        .size(54.dp)
                        .border(1.5.dp, Color(0xFFA100FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Video Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = video.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (video.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = video.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
private fun openExternalUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun NativeVideoPlayer(
    videoUrl: String,
    isLandscape: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val streamUrl = remember(videoUrl) { getDirectVideoUrl(videoUrl) }
    android.util.Log.d("VideoPlayer", "Original videoUrl: $videoUrl -> Converted streamUrl: $streamUrl")

    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> onLoadingChange(true)
                    Player.STATE_READY -> {
                        onLoadingChange(false)
                        onError(null)
                    }
                    Player.STATE_ENDED -> onLoadingChange(false)
                    else -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onLoadingChange(false)
                onError("Gagal memutar video: ${error.localizedMessage}")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                player = exoPlayer
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
            playerView.resizeMode = if (isLandscape) {
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            } else {
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        modifier = modifier
    )
}

private fun getDirectVideoUrl(url: String): String {
    var directUrl = url.trim()

    // Google Drive: drive.google.com/file/d/FILE_ID/view...
    val drivePathRegex = Regex("drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)")
    val drivePathMatch = drivePathRegex.find(directUrl)
    if (drivePathMatch != null) {
        val fileId = drivePathMatch.groupValues[1]
        return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
    }

    // Google Drive: drive.google.com/open?id=FILE_ID
    val driveQueryRegex = Regex("drive\\.google\\.com/.*[?&]id=([a-zA-Z0-9_-]+)")
    val driveQueryMatch = driveQueryRegex.find(directUrl)
    if (driveQueryMatch != null) {
        val fileId = driveQueryMatch.groupValues[1]
        return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
    }

    // Dropbox
    if (directUrl.contains("dropbox.com")) {
        if (directUrl.endsWith("dl=0")) {
            directUrl = directUrl.substring(0, directUrl.length - 4) + "raw=1"
        } else if (!directUrl.contains("raw=1") && !directUrl.contains("dl=1")) {
            directUrl = if (directUrl.contains("?")) {
                "$directUrl&raw=1"
            } else {
                "$directUrl?raw=1"
            }
        }
    }

    return directUrl
}
