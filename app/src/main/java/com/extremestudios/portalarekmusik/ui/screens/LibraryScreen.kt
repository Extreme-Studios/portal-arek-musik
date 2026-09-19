package com.extremestudios.portalarekmusik.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.extremestudios.portalarekmusik.R
import com.extremestudios.portalarekmusik.data.model.Playlist
import com.extremestudios.portalarekmusik.data.model.Song
import com.extremestudios.portalarekmusik.ui.components.SongCover

private val LibraryGold = Color(0xFFC5A358)
private val LibraryPanel = Color(0xFF17171B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    genres: List<String>,
    playlists: List<Playlist>,
    recentlyPlayed: List<Song>,
    favoriteSongs: List<Song>,
    errorMessage: String?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String, String) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddSongToPlaylist: (playlistId: String, songId: String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf("Lagu") }
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    val personalSongs = remember(recentlyPlayed, favoriteSongs) { (recentlyPlayed + favoriteSongs).distinctBy { it.id } }
    val matchingSongs = remember(personalSongs, query) {
        personalSongs.filter { query.isBlank() || listOf(it.title, it.artist, it.albumName, it.genre).any { part -> part.contains(query, true) } }
    }
    val artists = remember(matchingSongs) { matchingSongs.map { it.artist }.filter { it.isNotBlank() }.distinct().take(8) }
    val albums = remember(matchingSongs) { matchingSongs.filter { it.albumName.isNotBlank() }.groupBy { it.artist to it.albumName }.entries.take(8) }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier.fillMaxSize().background(Color(0xFF0B0B0F))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Library", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Saya", color = LibraryGold, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                            }
                            Text("Tempat semua musikmu", color = Color.White.copy(alpha = 0.67f), fontSize = 13.sp)
                        }
                        IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, "Cari", tint = Color.White, modifier = Modifier.size(30.dp)) }
                    }
                    if (showSearch) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            placeholder = { Text("Cari lagu, band, album...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LibraryGold, focusedContainerColor = LibraryPanel, unfocusedContainerColor = LibraryPanel)
                        )
                    }
                }
            }
            item {
                LibraryTabs(selected = selectedTab, onSelect = { selectedTab = it })
            }
            item {
                LibrarySection("Baru Diputar") {
                    SongCarousel(matchingSongs.take(8), onSongClick, onToggleFavorite)
                }
            }
            item {
                LibrarySection("Playlist Saya") {
                    if (playlists.isEmpty()) LibraryEmpty("Belum ada playlist. Tambahkan lagu melalui menu playlist.")
                    else PlaylistCarousel(playlists, songs, onSongClick)
                }
            }
            if (matchingSongs.isEmpty() && errorMessage != null) item { LibraryEmpty(errorMessage) }
        }
    }

    songToAdd?.let { selectedSong ->
        AlertDialog(
            onDismissRequest = { songToAdd = null }, title = { Text("Tambah ke Playlist") },
            text = { Column { playlists.forEach { playlist -> TextButton(onClick = { onAddSongToPlaylist(playlist.id, selectedSong.id); songToAdd = null }, modifier = Modifier.fillMaxWidth()) { Text(playlist.name) } } } },
            confirmButton = { TextButton(onClick = { songToAdd = null }) { Text("Tutup") } }
        )
    }
}

@Composable
private fun LibraryTabs(selected: String, onSelect: (String) -> Unit) {
    val tabs = listOf("Lagu" to "music", "Album" to "album", "Playlist" to "playlist")
    Row(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(LibraryPanel).padding(7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEach { (label, icon) ->
            val active = selected == label
            Row(
                modifier = Modifier.clip(RoundedCornerShape(15.dp)).background(if (active) LibraryGold.copy(alpha = 0.17f) else Color.Transparent).clickable { onSelect(label) }.padding(horizontal = 10.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryVectorIcon(icon, if (active) LibraryGold else Color.White.copy(alpha = 0.7f), 21.dp)
                if (active) { Spacer(Modifier.width(6.dp)); Text(label, color = LibraryGold, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun LibrarySection(title: String, body: @Composable () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Lihat Semua ›", color = LibraryGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp)); body()
    }
}

@Composable
private fun SongCarousel(songs: List<Song>, onSongClick: (Song) -> Unit, onToggleFavorite: (Song) -> Unit) {
    if (songs.isEmpty()) { LibraryEmpty("Belum ada lagu."); return }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
        items(songs, key = { it.id }) { song ->
            Card(modifier = Modifier.width(166.dp).clickable { onSongClick(song) }, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = LibraryPanel), border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.13f)))) {
                Column(Modifier.padding(8.dp)) {
                    Box { SongCover(song, Modifier.fillMaxWidth().height(110.dp)); PlayBadge(Modifier.align(Alignment.Center)) }
                    Spacer(Modifier.height(7.dp)); Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun PlaylistCarousel(playlists: List<Playlist>, songs: List<Song>, onSongClick: (Song) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            val firstSong = songs.firstOrNull { it.id in playlist.songIds }
            Card(modifier = Modifier.width(175.dp).clickable { firstSong?.let(onSongClick) }, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = LibraryPanel)) {
                Column(Modifier.padding(8.dp)) {
                    AsyncImage(model = firstSong?.coverUrl?.ifBlank { R.drawable.app_icon } ?: R.drawable.app_icon, contentDescription = playlist.name, contentScale = ContentScale.Crop, placeholder = painterResource(R.drawable.app_icon), error = painterResource(R.drawable.app_icon), modifier = Modifier.fillMaxWidth().height(126.dp).clip(RoundedCornerShape(11.dp)))
                    Spacer(Modifier.height(9.dp)); Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1); Text("${playlist.songIds.size} lagu", color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(song: Song, albumName: String, count: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.width(170.dp).clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = LibraryPanel), border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.13f)))) {
        Column(Modifier.padding(8.dp)) { SongCover(song, Modifier.fillMaxWidth().height(124.dp)); Spacer(Modifier.height(8.dp)); Text(albumName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(song.artist, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp); Text("$count lagu", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) }
    }
}

@Composable
private fun FollowedBandList(artists: List<String>, songs: List<Song>, onArtistClick: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp)).background(LibraryPanel)) {
        artists.take(5).forEachIndexed { index, artist ->
            val artistSongs = songs.filter { it.artist.equals(artist, true) }
            val cover = artistSongs.firstOrNull()
            Row(Modifier.fillMaxWidth().clickable { onArtistClick(artist) }.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                if (cover != null) SongCover(cover, Modifier.size(58.dp)) else Box(Modifier.size(58.dp).background(LibraryGold.copy(.2f), RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(artist, color = Color.White, fontWeight = FontWeight.Bold); Text("${artistSongs.firstOrNull()?.genre ?: "Musik lokal"}  •  ${artistSongs.size} lagu", color = Color.White.copy(alpha = .62f), fontSize = 12.sp) }
                OutlinedButton(onClick = { onArtistClick(artist) }, border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(LibraryGold)), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Following", color = LibraryGold, fontSize = 12.sp) }
            }
            if (index < artists.take(5).lastIndex) HorizontalDivider(color = Color.White.copy(alpha = .08f))
        }
    }
}

@Composable
private fun OfflineCard(songCount: Int, genreCount: Int) {
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(LibraryPanel).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).background(LibraryGold.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) { LibraryVectorIcon("download", LibraryGold, 25.dp) }
        Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text("Download Offline", color = Color.White, fontWeight = FontWeight.Bold); Text("Lagu dan album yang diunduh", color = Color.White.copy(alpha = .6f), fontSize = 13.sp) }
        Text("$songCount lagu • $genreCount genre", color = Color.White.copy(alpha = .62f), fontSize = 12.sp)
    }
}

@Composable private fun PlayBadge(modifier: Modifier = Modifier) { Box(modifier.size(45.dp).background(Color.Black.copy(alpha = .68f), CircleShape).border(1.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { LibraryVectorIcon("play", Color.White, 23.dp) } }
@Composable private fun LibraryEmpty(text: String) { Text(text, color = Color.White.copy(alpha = .58f), modifier = Modifier.padding(horizontal = 20.dp)) }

@Composable
private fun LibraryVectorIcon(kind: String, color: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height; val stroke = Stroke(width = w * .095f, cap = StrokeCap.Round)
        when (kind) {
            "music" -> { drawRoundRect(color, Offset(w*.25f,h*.08f), androidx.compose.ui.geometry.Size(w*.5f,h*.76f), CornerRadius(w*.08f), style = stroke); drawCircle(color, w*.14f, Offset(w*.3f,h*.78f)); drawLine(color, Offset(w*.58f,h*.25f), Offset(w*.73f,h*.2f), strokeWidth=w*.1f, cap=StrokeCap.Round) }
            "album" -> { drawCircle(color, w*.36f, Offset(w*.5f,h*.5f), style=stroke); drawCircle(color, w*.07f, Offset(w*.5f,h*.5f)) }
            "band" -> { drawCircle(color,w*.16f,Offset(w*.36f,h*.35f)); drawCircle(color,w*.13f,Offset(w*.7f,h*.43f)); drawRoundRect(color,Offset(w*.13f,h*.58f),androidx.compose.ui.geometry.Size(w*.48f,h*.22f),CornerRadius(w*.1f)); drawRoundRect(color,Offset(w*.54f,h*.62f),androidx.compose.ui.geometry.Size(w*.34f,h*.18f),CornerRadius(w*.09f)) }
            "playlist" -> repeat(3) { i -> drawLine(color,Offset(w*.15f,h*(.27f+i*.23f)),Offset(w*.82f,h*(.27f+i*.23f)),strokeWidth=w*.1f,cap=StrokeCap.Round) }
            "heart" -> { val p=Path().apply { moveTo(w*.5f,h*.82f); cubicTo(w*.12f,h*.58f,w*.18f,h*.2f,w*.5f,h*.39f); cubicTo(w*.82f,h*.2f,w*.88f,h*.58f,w*.5f,h*.82f) }; drawPath(p,color,style=stroke) }
            "download" -> { drawLine(color,Offset(w*.5f,h*.15f),Offset(w*.5f,h*.63f),strokeWidth=w*.12f,cap=StrokeCap.Round); drawLine(color,Offset(w*.32f,h*.48f),Offset(w*.5f,h*.66f),strokeWidth=w*.12f,cap=StrokeCap.Round); drawLine(color,Offset(w*.68f,h*.48f),Offset(w*.5f,h*.66f),strokeWidth=w*.12f,cap=StrokeCap.Round); drawLine(color,Offset(w*.2f,h*.85f),Offset(w*.8f,h*.85f),strokeWidth=w*.12f,cap=StrokeCap.Round) }
            else -> { val p=Path().apply { moveTo(w*.3f,h*.2f); lineTo(w*.78f,h*.5f); lineTo(w*.3f,h*.8f); close() }; drawPath(p,color) }
        }
    }
}
