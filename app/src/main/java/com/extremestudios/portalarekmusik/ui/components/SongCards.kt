package com.extremestudios.portalarekmusik.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.extremestudios.portalarekmusik.R
import com.extremestudios.portalarekmusik.data.model.Song

private val PremiumBrushes = listOf(
    Brush.verticalGradient(listOf(Color(0xFFC5A358), Color(0xFF3E321A))),
    Brush.verticalGradient(listOf(Color(0xFF4A90E2), Color(0xFF162A4A))),
    Brush.verticalGradient(listOf(Color(0xFF7B61FF), Color(0xFF312152))),
    Brush.verticalGradient(listOf(Color(0xFFE5B181), Color(0xFF4B3421)))
)

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongCover(song = song, modifier = Modifier.size(56.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (onArtistClick != null) Modifier.clickable { onArtistClick(song.artist) } else Modifier,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.albumName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.albumName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(song.genre) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    border = null,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            if (onAddToPlaylist != null) {
                TextButton(onClick = onAddToPlaylist) {
                    Text("+Playlist")
                }
            }
        }
    }
}

@Composable
fun SongCover(song: Song, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    val fallbackPainter = painterResource(id = R.drawable.app_icon)
    val safeCoverUrl = song.coverUrl.trim()
    val imageModelUrl = safeCoverUrl.toDisplayImageUrl()
    val isRemoteUrl = imageModelUrl.startsWith("http://", ignoreCase = true) || imageModelUrl.startsWith("https://", ignoreCase = true)
    Log.d(
        "SongCover",
        "title=${song.title} cover_url=$safeCoverUrl imageModel=$imageModelUrl audio_url=${song.audioUrl} isRemote=$isRemoteUrl"
    )

    if (isRemoteUrl) {
        AsyncImage(
            model = imageModelUrl,
            contentDescription = "${song.title} cover",
            modifier = modifier
                .clip(shape)
                .background(Color(0x33222222)),
            contentScale = ContentScale.Crop,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            fallback = fallbackPainter,
            onError = { state ->
                Log.e(
                    "SongCover",
                    "Failed loading cover for title=${song.title}, cover_url=$safeCoverUrl, imageModel=$imageModelUrl",
                    state.result.throwable
                )
            },
            onSuccess = {
                Log.d("SongCover", "Loaded cover for title=${song.title}, cover_url=$safeCoverUrl, imageModel=$imageModelUrl")
            }
        )
        return
    }

    Image(
        painter = fallbackPainter,
        contentDescription = "Default artwork",
        modifier = modifier.clip(shape),
        contentScale = ContentScale.Crop
    )
}

private fun String.toDisplayImageUrl(): String {
    val rawUrl = trim()
    if (rawUrl.isBlank()) return ""
    val fileId = rawUrl.extractGoogleDriveFileId() ?: return rawUrl
    return "https://drive.google.com/thumbnail?id=$fileId&sz=w1200"
}

private fun String.extractGoogleDriveFileId(): String? {
    return runCatching {
        val uri = Uri.parse(this)
        val host = uri.host.orEmpty().lowercase()
        if (!host.contains("drive.google.com") && !host.contains("docs.google.com")) return@runCatching null

        uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }?.let { return@runCatching it }

        val segments = uri.pathSegments
        val fileIndex = segments.indexOf("d")
        if (fileIndex >= 0 && fileIndex + 1 < segments.size) {
            return@runCatching segments[fileIndex + 1].takeIf { it.isNotBlank() }
        }

        null
    }.getOrNull()
}
