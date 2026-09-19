package com.extremestudios.portalarekmusik.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.extremestudios.portalarekmusik.R
import com.extremestudios.portalarekmusik.data.model.Song
import com.extremestudios.portalarekmusik.ui.components.SongCover

private val HomeGold = Color(0xFFC5A358)
private val HomePanel = Color(0xFF17171B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    songs: List<Song>,
    artists: List<String>,
    errorMessage: String?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredSongs = remember(songs, query) {
        songs.filter { song ->
            query.isBlank() || listOf(song.title, song.artist, song.albumName, song.genre)
                .any { it.contains(query, ignoreCase = true) }
        }
    }
    val latestSongs = remember(filteredSongs) { filteredSongs.take(10) }
    val featuredSong = latestSongs.firstOrNull() ?: songs.firstOrNull()
    val displayArtists = remember(artists, filteredSongs) {
        (artists + filteredSongs.map { it.artist })
            .filter { it.isNotBlank() }
            .distinct()
            .take(10)
    }
    val genres = remember(songs) { songs.map { it.genre }.filter { it.isNotBlank() }.distinct().take(8) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(Color(0xFF0B0B0F))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { HomeHeader(query = query, onQueryChange = { query = it }) }
            item {
                FeaturedBandCard(
                    song = featuredSong,
                    onPlay = { featuredSong?.let(onSongClick) },
                    onArtistClick = { featuredSong?.artist?.let(onArtistClick) }
                )
            }
            item {
                HomeSection(title = "Trending Hari Ini") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(latestSongs, key = { it.id }) { song ->
                            TrendingSongCard(song = song, onClick = { onSongClick(song) })
                        }
                    }
                }
            }
            item {
                HomeSection(title = "Band Baru") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(displayArtists, key = { it }) { artist ->
                            val representativeSong = songs.firstOrNull { it.artist.equals(artist, ignoreCase = true) }
                            ArtistHomeCard(
                                artist = artist,
                                coverUrl = representativeSong?.coverUrl.orEmpty(),
                                genre = representativeSong?.genre.orEmpty(),
                                onClick = { onArtistClick(artist) }
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(modifier = Modifier.weight(1.25f)) {
                        SectionHeading("Lagu Terbaru")
                        Spacer(Modifier.height(12.dp))
                        if (latestSongs.isEmpty()) {
                            HomeEmptyState(if (isLoading) "Memuat lagu..." else "Belum ada lagu yang dipublikasikan.")
                        } else {
                            Column(
                                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(HomePanel)
                            ) {
                                latestSongs.take(4).forEachIndexed { index, song ->
                                    CompactSongRow(song = song, onClick = { onSongClick(song) })
                                    if (index < latestSongs.take(4).lastIndex) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(0.85f)) {
                        SectionHeading("Genre")
                        Spacer(Modifier.height(12.dp))
                        GenreGrid(genres = genres, onGenreClick = { query = it })
                    }
                }
            }
            item {
                HomeSection(title = "Pilihan Untukmu") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(latestSongs.drop(4), key = { it.id }) { song ->
                            TrendingSongCard(song = song, onClick = { onSongClick(song) })
                        }
                    }
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                item { HomeEmptyState(errorMessage) }
            }
        }
    }
}

@Composable
private fun HomeHeader(query: String, onQueryChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(30.dp))
            Icon(Icons.Default.Info, contentDescription = "Informasi", tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Selamat Datang di", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
        Text(
            text = "Portal Arek Musik",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp
        )
        Text("Discover. Connect. Amplify.", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari lagu, band, album...", color = Color.White.copy(alpha = 0.55f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
            trailingIcon = { Icon(Icons.Default.Search, null, tint = HomeGold) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HomePanel,
                unfocusedContainerColor = HomePanel,
                focusedBorderColor = HomeGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
private fun FeaturedBandCard(song: Song?, onPlay: () -> Unit, onArtistClick: () -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(285.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomePanel),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = song?.coverUrl?.takeIf { it.isNotBlank() } ?: R.drawable.app_icon,
                contentDescription = song?.artist ?: "Featured band",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.app_icon),
                error = painterResource(R.drawable.app_icon),
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color(0xE6000000), Color(0x80000000), Color.Transparent))
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("FEATURED BAND", color = HomeGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    song?.artist ?: "Portal Arek Musik",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onArtistClick)
                )
                Text(song?.title ?: "Temukan karya musisi Sidoarjo", color = Color.White.copy(alpha = 0.85f), maxLines = 1)
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = HomeGold, contentColor = Color(0xFF15120B)),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("PUTAR SEKARANG", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HomeSection(title: String, content: @Composable () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionHeading(title)
            Text("Lihat Semua", color = HomeGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SectionHeading(title: String) = Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

@Composable
private fun TrendingSongCard(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(166.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = HomePanel),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box {
                SongCover(song, Modifier.fillMaxWidth().height(118.dp))
                Box(
                    modifier = Modifier.align(Alignment.Center).size(42.dp).background(Color.Black.copy(alpha = 0.66f), CircleShape).border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.PlayArrow, null, tint = Color.White) }
            }
            Spacer(Modifier.height(8.dp))
            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ArtistHomeCard(artist: String, coverUrl: String, genre: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(190.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = HomePanel),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column {
            AsyncImage(
                model = coverUrl.ifBlank { R.drawable.app_icon }, contentDescription = artist,
                modifier = Modifier.fillMaxWidth().height(115.dp), contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.app_icon), error = painterResource(R.drawable.app_icon)
            )
            Column(Modifier.padding(12.dp)) {
                Text(artist, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(genre.ifBlank { "Musisi lokal" }, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onClick, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp), border = BorderStroke(1.dp, HomeGold)) {
                    Text("FOLLOW", color = HomeGold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CompactSongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        SongCover(song, Modifier.size(48.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = Color.White.copy(alpha = 0.56f), fontSize = 12.sp, maxLines = 1)
        }
        Icon(Icons.Default.PlayArrow, contentDescription = "Putar ${song.title}", tint = HomeGold)
    }
}

@Composable
private fun GenreGrid(genres: List<String>, onGenreClick: (String) -> Unit) {
    val rows = genres.chunked(2).ifEmpty { listOf(listOf("Indie", "Rock"), listOf("Pop", "Acoustic")) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { genre ->
                    OutlinedButton(
                        onClick = { onGenreClick(genre) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        border = BorderStroke(1.dp, HomeGold),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(genre, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeEmptyState(message: String) {
    Text(message, color = Color.White.copy(alpha = 0.58f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
}
