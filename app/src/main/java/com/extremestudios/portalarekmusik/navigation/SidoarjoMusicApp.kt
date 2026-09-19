package com.extremestudios.portalarekmusik.navigation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.extremestudios.portalarekmusik.data.local.PortalSessionStore
import com.extremestudios.portalarekmusik.data.local.LibraryLocalStore
import com.extremestudios.portalarekmusik.data.local.PlaylistLocalStore
import com.extremestudios.portalarekmusik.data.local.VideoFavoritesLocalStore
import com.extremestudios.portalarekmusik.data.model.PortalPost
import com.extremestudios.portalarekmusik.data.model.RegisteredChannel
import com.extremestudios.portalarekmusik.data.model.Song
import com.extremestudios.portalarekmusik.data.model.VideoItem
import com.extremestudios.portalarekmusik.data.model.VideoBand
import com.extremestudios.portalarekmusik.data.repository.ApiSongRepository
import com.extremestudios.portalarekmusik.data.repository.PortalRepository
import com.extremestudios.portalarekmusik.data.repository.PlaylistRepository
import com.extremestudios.portalarekmusik.data.repository.SongRegistrationRepository
import com.extremestudios.portalarekmusik.playback.PlaybackManager
import com.extremestudios.portalarekmusik.ui.components.SongCover
import com.extremestudios.portalarekmusik.ui.components.TopBannerAd
import com.extremestudios.portalarekmusik.ui.screens.AboutScreen
import com.extremestudios.portalarekmusik.ui.screens.AlbumDetailScreen
import com.extremestudios.portalarekmusik.ui.screens.ArtistDetailScreen
import com.extremestudios.portalarekmusik.ui.screens.HomeScreen
import com.extremestudios.portalarekmusik.ui.screens.LibraryScreen
import com.extremestudios.portalarekmusik.ui.screens.NowPlayingScreen
import com.extremestudios.portalarekmusik.ui.screens.PortalPosterSubmissionScreen
import com.extremestudios.portalarekmusik.ui.screens.PortalKitaScreen
import com.extremestudios.portalarekmusik.ui.screens.PortalPostDetailScreen
import com.extremestudios.portalarekmusik.ui.screens.SubmitMvScreen
import com.extremestudios.portalarekmusik.ui.screens.SubmitSongScreen
import com.extremestudios.portalarekmusik.ui.screens.VideoScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AppScreen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : AppScreen("home", "Home", Icons.Default.Home)
    data object Library : AppScreen("library", "Library", Icons.AutoMirrored.Filled.List)
    data object Video : AppScreen("video", "MV", Icons.Default.PlayArrow)
    data object NowPlaying : AppScreen("now_playing", "Now Playing", Icons.Default.PlayArrow)
    data object PortalKita : AppScreen("portal_kita", "Portal Kita", Icons.AutoMirrored.Filled.List)
    data object About : AppScreen("about", "About", Icons.Default.Info)
}

@Composable
fun PortalArekMusicApp(repository: ApiSongRepository = ApiSongRepository()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val playlistRepository = remember { PlaylistRepository(PlaylistLocalStore(context)) }
    val libraryStore = remember { LibraryLocalStore(context) }
    val videoFavoritesStore = remember { VideoFavoritesLocalStore(context) }
    val portalRepository = remember { PortalRepository() }
    val channelRepository = remember { SongRegistrationRepository() }
    val portalSessionStore = remember { PortalSessionStore(context) }
    val portalSession by portalSessionStore.sessionFlow.collectAsState(initial = null)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var isVideoActiveByPlayer by remember { mutableStateOf(false) }

    val tabs = listOf(
        AppScreen.Home,
        AppScreen.Library,
        AppScreen.Video,
        AppScreen.PortalKita,
        AppScreen.About
    )
    var currentTab by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    val tabHistory = remember { mutableStateListOf<AppScreen>().apply { add(AppScreen.Home) } }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedPortalPost by remember { mutableStateOf<PortalPost?>(null) }
    var showPortalSubmission by remember { mutableStateOf(false) }
    var showSongSubmission by remember { mutableStateOf(false) }
    var showMvSubmission by remember { mutableStateOf(false) }
    var songs by remember { mutableStateOf(repository.getFallbackSongs()) }
    var isLoadingSongs by remember { mutableStateOf(true) }
    var isRefreshingSongs by remember { mutableStateOf(false) }
    var songsError by remember { mutableStateOf<String?>(null) }
    val genres = remember(songs) { repository.getGenres(songs) }
    val artists = remember(songs) { repository.getArtists(songs) }
    val playlists by playlistRepository.playlists.collectAsState(initial = emptyList())
    val playedSongIds by libraryStore.historyIds.collectAsState(initial = emptyList())
    val favoriteSongIds by libraryStore.favoriteIds.collectAsState(initial = emptyList())
    val favoriteVideos by videoFavoritesStore.videos.collectAsState(initial = emptyList())
    var portalPosts by remember { mutableStateOf<List<PortalPost>>(emptyList()) }
    var isLoadingPortal by remember { mutableStateOf(true) }
    var portalError by remember { mutableStateOf<String?>(null) }
    var portalVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var portalVideoBands by remember { mutableStateOf<List<VideoBand>>(emptyList()) }
    var registeredChannels by remember { mutableStateOf<List<RegisteredChannel>>(emptyList()) }
    var selectedVideoBand by remember { mutableStateOf<String?>(null) }
    var isLoadingVideos by remember { mutableStateOf(true) }
    var isRefreshingVideos by remember { mutableStateOf(false) }
    var videosError by remember { mutableStateOf<String?>(null) }
    var isRefreshingPortal by remember { mutableStateOf(false) }
    var isRefreshingAbout by remember { mutableStateOf(false) }
    val portalCategories = remember { portalRepository.getCategories() }
    val currentPlayingSong by PlaybackManager.currentSong.collectAsState()
    val isPlayingNow by PlaybackManager.isPlaying.collectAsState()
    val currentPositionMs by PlaybackManager.positionMs.collectAsState()
    val currentDurationMs by PlaybackManager.durationMs.collectAsState()

    fun navigateTo(screen: AppScreen) {
        selectedArtist = null
        selectedAlbum = null
        selectedPortalPost = null
        showPortalSubmission = false
        showSongSubmission = false
        showMvSubmission = false
        if (currentTab != screen) {
            currentTab = screen
            tabHistory.add(screen)
        }
    }

    fun openSong(song: Song) {
        libraryStore.recordPlayed(song.id)
        selectedArtist = null
        selectedAlbum = null
        if (song.audioUrl.isNotBlank()) {
            PlaybackManager.playSong(songs, song, autoPlay = true)
            navigateTo(AppScreen.NowPlaying)
        } else {
            navigateTo(AppScreen.NowPlaying)
        }
    }

    fun openSongFromQueue(queue: List<Song>, song: Song) {
        libraryStore.recordPlayed(song.id)
        selectedArtist = null
        selectedAlbum = null
        if (song.audioUrl.isNotBlank()) {
            PlaybackManager.playSong(queue, song, autoPlay = true)
        }
        navigateTo(AppScreen.NowPlaying)
    }

    fun refreshSongs(showBlocking: Boolean) {
        scope.launch {
            if (showBlocking) isLoadingSongs = true else isRefreshingSongs = true
            val result = repository.fetchSongs()
            result.onSuccess { apiSongs ->
                songs = apiSongs
                songsError = null
                Log.d("PortalArekMusicApp", "API success. songs count=${apiSongs.size}")
                Log.d("PortalArekMusicApp", "API success overwrote song state/cache. fullList=${apiSongs.size}")
                Log.d("PortalArekMusicApp", "Home song count=${apiSongs.take(10).size}")
                Log.d("PortalArekMusicApp", "Library song count=${apiSongs.size}")
                if (apiSongs.isEmpty()) {
                    Log.d("PortalArekMusicApp", "Rendering empty song state")
                }
            }.onFailure { err ->
                songsError = err.message ?: "Gagal mengambil data lagu dari server."
                if (songs.isEmpty()) {
                    val fallbackSongs = repository.getFallbackSongs()
                    songs = fallbackSongs
                    Log.d("PortalArekMusicApp", "API failed. Fallback cache used. count=${fallbackSongs.size}")
                } else {
                    Log.d("PortalArekMusicApp", "API failed. Keeping current in-memory list as fallback. count=${songs.size}")
                }
            }
            isLoadingSongs = false
            isRefreshingSongs = false
        }
    }

    fun refreshPortalPosts(showBlocking: Boolean = true) {
        scope.launch {
            if (showBlocking) isLoadingPortal = true else isRefreshingPortal = true
            val result = portalRepository.fetchActivePosts()
            result.onSuccess { posts ->
                portalPosts = posts
                portalError = null
            }.onFailure { error ->
                portalError = error.message ?: "Gagal memuat poster Portal Kita."
                if (showBlocking && portalPosts.isEmpty()) {
                    portalPosts = emptyList()
                }
            }
            isLoadingPortal = false
            isRefreshingPortal = false
        }
    }

    fun refreshVideoBands(showBlocking: Boolean = true) {
        scope.launch {
            if (showBlocking) isLoadingVideos = true else isRefreshingVideos = true
            val result = portalRepository.fetchVideoBands()
            result.onSuccess { list ->
                portalVideoBands = list
                videosError = null
            }.onFailure { error ->
                videosError = error.message ?: "Gagal memuat daftar band MV."
                if (showBlocking && portalVideoBands.isEmpty()) {
                    portalVideoBands = emptyList()
                }
            }
            isLoadingVideos = false
            isRefreshingVideos = false
        }
    }

    fun refreshRegisteredChannels() {
        if (!portalSession?.status.equals("ACTIVE", ignoreCase = true)) {
            registeredChannels = emptyList()
            return
        }
        scope.launch {
            channelRepository.fetchChannels().onSuccess { registeredChannels = it }
        }
    }

    fun refreshPortalVideos(artist: String, showBlocking: Boolean = true) {
        scope.launch {
            if (showBlocking) isLoadingVideos = true else isRefreshingVideos = true
            val result = portalRepository.fetchActiveVideos(artist)
            result.onSuccess { list ->
                portalVideos = list
                videosError = null
            }.onFailure { error ->
                videosError = error.message ?: "Gagal memuat video."
                if (showBlocking && portalVideos.isEmpty()) portalVideos = emptyList()
            }
            isLoadingVideos = false
            isRefreshingVideos = false
        }
    }

    LaunchedEffect(Unit) {
        // Login admin/member sudah dihapus dari aplikasi; jangan pulihkan sesi lama.
        portalSessionStore.clearSession()
        PlaybackManager.init(context)
        refreshSongs(showBlocking = true)
        refreshPortalPosts(showBlocking = true)
        refreshVideoBands(showBlocking = false)
    }

    LaunchedEffect(portalSession?.status) {
        refreshRegisteredChannels()
    }

    LaunchedEffect(currentTab) {
        if (currentTab == AppScreen.Video) refreshRegisteredChannels()
    }

    LaunchedEffect(currentTab, songs.size) {
        Log.d("PortalArekMusicApp", "Selected tab=${currentTab.route}, songsCount=${songs.size}")
    }

    LaunchedEffect(Unit) {
        while (true) {
            PlaybackManager.tick()
            delay(500)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose { }
    }

    BackHandler {
        when {
            showPortalSubmission -> showPortalSubmission = false
            showSongSubmission -> showSongSubmission = false
            showMvSubmission -> showMvSubmission = false
            selectedPortalPost != null -> selectedPortalPost = null
            selectedAlbum != null -> selectedAlbum = null
            selectedArtist != null -> selectedArtist = null
            selectedVideoBand != null -> {
                selectedVideoBand = null
                portalVideos = emptyList()
                videosError = null
            }
            tabHistory.size > 1 -> {
                tabHistory.removeAt(tabHistory.lastIndex)
                currentTab = tabHistory.last()
            }
            else -> activity?.moveTaskToBack(true)
        }
    }

    val hideBars = isLandscape && currentTab == AppScreen.Video && isVideoActiveByPlayer

    Scaffold(
        topBar = {
            if (!hideBars) {
                TopBannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                )
            }
        },
        bottomBar = {
            if (!hideBars) {
                Column {
                    if (currentPlayingSong != null && currentTab != AppScreen.NowPlaying) {
                        MiniPlayerBar(
                            song = currentPlayingSong ?: return@Column,
                            isPlaying = isPlayingNow,
                            progress = if (currentDurationMs > 0L) {
                                (currentPositionMs.toFloat() / currentDurationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            onOpen = { navigateTo(AppScreen.NowPlaying) },
                            onPlayPause = { PlaybackManager.togglePlayPause() },
                            onPrevious = { PlaybackManager.previous() },
                            onNext = { PlaybackManager.next() }
                        )
                    }
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp
                    ) {
                        tabs.forEach { screen ->
                            NavigationBarItem(
                                selected = currentTab == screen,
                                onClick = { navigateTo(screen) },
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = if (screen.label.isBlank()) null else ({
                                    Text(
                                        text = screen.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }),
                                alwaysShowLabel = screen.label.isNotBlank(),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val modifier = if (hideBars) Modifier.fillMaxSize() else Modifier.padding(innerPadding)
        if (showSongSubmission) {
            SubmitSongScreen(
                modifier = modifier,
                onBack = { showSongSubmission = false },
                adminSession = portalSession
            )
        } else if (showMvSubmission) {
            SubmitMvScreen(
                modifier = modifier,
                onBack = { showMvSubmission = false },
                adminSession = portalSession
            )
        } else if (showPortalSubmission) {
            PortalPosterSubmissionScreen(
                modifier = modifier,
                onBack = { showPortalSubmission = false },
                onSubmitted = {
                    showPortalSubmission = false
                    refreshPortalPosts(showBlocking = true)
                }
            )
        } else selectedPortalPost?.let { post ->
            PortalPostDetailScreen(
                post = post,
                modifier = modifier,
                onBack = { selectedPortalPost = null }
            )
        } ?: selectedAlbum?.let { (artistName, albumName) ->
            AlbumDetailScreen(
                artistName = artistName,
                albumName = albumName,
                songs = songs,
                modifier = modifier,
                onBack = { selectedAlbum = null },
                onSongClick = { song, queue -> openSongFromQueue(queue, song) }
            )
        } ?: selectedArtist?.let { artistName ->
            ArtistDetailScreen(
                artistName = artistName,
                songs = songs,
                errorMessage = songsError,
                modifier = modifier,
                onBack = { selectedArtist = null },
                onSongClick = { openSong(it) },
                onAlbumClick = { albumName -> selectedAlbum = artistName to albumName }
            )
        } ?: when (currentTab) {
            AppScreen.Home -> HomeScreen(
                songs = songs,
                artists = artists,
                errorMessage = songsError,
                isLoading = isLoadingSongs,
                isRefreshing = isRefreshingSongs,
                onRefresh = { refreshSongs(showBlocking = false) },
                modifier = modifier,
                onSongClick = { openSong(it) },
                onArtistClick = { selectedArtist = it }
            )
            AppScreen.Library -> LibraryScreen(
                songs = songs,
                genres = genres,
                playlists = playlists,
                recentlyPlayed = playedSongIds.mapNotNull { id -> songs.firstOrNull { it.id == id } },
                favoriteSongs = favoriteSongIds.mapNotNull { id -> songs.firstOrNull { it.id == id } },
                errorMessage = songsError,
                isLoading = isLoadingSongs,
                isRefreshing = isRefreshingSongs,
                onRefresh = { refreshSongs(showBlocking = false) },
                modifier = modifier,
                onSongClick = { openSong(it) },
                onArtistClick = { selectedArtist = it },
                onAlbumClick = { artist, album -> selectedAlbum = artist to album },
                onToggleFavorite = { libraryStore.toggleFavorite(it.id) },
                onAddSongToPlaylist = { playlistId, songId ->
                    scope.launch { playlistRepository.addSongToPlaylist(playlistId, songId) }
                }
            )
            AppScreen.NowPlaying -> NowPlayingScreen(
                playlists = playlists,
                modifier = modifier,
                onOpenLibrary = { navigateTo(AppScreen.Library) },
                onArtistClick = { selectedArtist = it },
                onAddSongToPlaylist = { playlistId, songId ->
                    scope.launch { playlistRepository.addSongToPlaylist(playlistId, songId) }
                },
                onCreatePlaylistWithSong = { name, songId ->
                    scope.launch {
                        val playlist = playlistRepository.createPlaylist(name)
                        playlistRepository.addSongToPlaylist(playlist.id, songId)
                    }
                }
            )
            AppScreen.Video -> VideoScreen(
                bands = portalVideoBands,
                videos = portalVideos,
                selectedArtist = selectedVideoBand,
                isLoading = isLoadingVideos,
                isRefreshing = isRefreshingVideos,
                errorMessage = videosError,
                onRefresh = {
                    refreshRegisteredChannels()
                    selectedVideoBand?.let { refreshPortalVideos(it, showBlocking = false) }
                        ?: refreshVideoBands(showBlocking = false)
                },
                onArtistSelected = { artist ->
                    selectedVideoBand = artist
                    portalVideos = emptyList()
                    refreshPortalVideos(artist, showBlocking = false)
                },
                onBackToBands = {
                    selectedVideoBand = null
                    portalVideos = emptyList()
                    videosError = null
                },
                deletableChannelArtists = if (portalSession?.status.equals("ACTIVE", ignoreCase = true)) {
                    registeredChannels.map { it.artist.trim().lowercase() }.toSet()
                } else emptySet(),
                onDeleteChannel = { artist ->
                    val session = portalSession ?: return@VideoScreen
                    val channel = registeredChannels.firstOrNull {
                        it.artist.trim().equals(artist.trim(), ignoreCase = true)
                    } ?: return@VideoScreen
                    scope.launch {
                        channelRepository.deleteChannel(session, channel.id)
                            .onSuccess {
                                registeredChannels = registeredChannels.filterNot { it.id == channel.id }
                                refreshVideoBands(showBlocking = false)
                            }
                            .onFailure { error ->
                                videosError = error.message ?: "Gagal menghapus channel."
                        }
                    }
                },
                deletableVideoIds = if (portalSession?.status.equals("ACTIVE", ignoreCase = true)) {
                    portalVideos.filterNot { it.id.startsWith("yt_") }.map { it.id }.toSet()
                } else emptySet(),
                onDeleteVideo = { videoId ->
                    val session = portalSession ?: return@VideoScreen
                    scope.launch {
                        channelRepository.deleteVideo(session, videoId)
                            .onSuccess {
                                portalVideos = portalVideos.filterNot { it.id == videoId }
                                selectedVideoBand?.let { refreshPortalVideos(it, showBlocking = false) }
                                refreshVideoBands(showBlocking = false)
                            }
                            .onFailure { error ->
                                videosError = error.message ?: "Gagal menghapus video."
                        }
                    }
                },
                onEditVideo = { videoId, title ->
                    val session = portalSession ?: return@VideoScreen
                    scope.launch {
                        channelRepository.updateVideoTitle(session, videoId, title)
                            .onSuccess {
                                portalVideos = portalVideos.map { video ->
                                    if (video.id == videoId) video.copy(title = title) else video
                                }
                                refreshVideoBands(showBlocking = false)
                            }
                            .onFailure { error ->
                                videosError = error.message ?: "Gagal mengubah judul video."
                            }
                    }
                },
                favoriteVideos = favoriteVideos,
                onAddFavoriteVideo = { url, title ->
                    videoFavoritesStore.addYoutubeVideo(url, title)
                },
                onRemoveFavoriteVideo = { videoId ->
                    videoFavoritesStore.remove(videoId)
                },
                onEditFavoriteVideo = { videoId, url, title ->
                    videoFavoritesStore.update(videoId, url, title)
                },
                onVideoActiveChange = { isVideoActiveByPlayer = it },
                modifier = modifier
            )
            AppScreen.PortalKita -> PortalKitaScreen(
                posts = portalPosts,
                categories = portalCategories,
                isLoading = isLoadingPortal,
                isRefreshing = isRefreshingPortal,
                errorMessage = portalError,
                modifier = modifier,
                onRefresh = { refreshPortalPosts(showBlocking = false) },
                onPostPreview = { selectedPortalPost = it },
                onOpenPosterSubmission = { showPortalSubmission = true },
                canDeletePosts = portalSession?.status.equals("ACTIVE", ignoreCase = true),
                onDeletePost = { post ->
                    val session = portalSession ?: return@PortalKitaScreen
                    scope.launch {
                        portalRepository.deletePortalPost(session, post.id)
                            .onSuccess {
                                portalPosts = portalPosts.filterNot { it.id == post.id }
                                refreshPortalPosts(showBlocking = false)
                            }
                            .onFailure { error ->
                                portalError = error.message ?: "Gagal menghapus poster."
                            }
                    }
                }
            )
            AppScreen.About -> AboutScreen(
                isRefreshing = isRefreshingAbout,
                onOpenSongUpload = {
                    showSongSubmission = true
                },
                onOpenMusicVideoUpload = {
                    showMvSubmission = true
                },
                onRefresh = {
                    scope.launch {
                        isRefreshingAbout = true
                        delay(1000)
                        isRefreshingAbout = false
                    }
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongCover(song = song, modifier = Modifier.size(44.dp))
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_media_previous),
                            contentDescription = "Previous"
                        )
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_media_next),
                            contentDescription = "Next"
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
