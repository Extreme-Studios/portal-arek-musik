package com.extremestudios.portalarekmusik.data.local

import android.content.Context
import com.extremestudios.portalarekmusik.data.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Playlist MV milik perangkat ini saja. Tidak pernah dikirim ke Apps Script/Google Sheet.
 */
class VideoFavoritesLocalStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    private val _videos = MutableStateFlow(readVideos())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    fun addYoutubeVideo(url: String, title: String): AddResult {
        val normalizedUrl = url.trim()
        val youtubeId = VideoItem.extractYoutubeVideoId(normalizedUrl)
            ?: return AddResult.InvalidUrl
        if (_videos.value.any { VideoItem.extractYoutubeVideoId(it.videoUrl) == youtubeId }) {
            return AddResult.AlreadyExists
        }

        val item = VideoItem(
            id = "local_youtube_$youtubeId",
            title = title.trim().ifBlank { "Video YouTube" },
            artist = "Favorit Saya",
            description = "Playlist pribadi di perangkat ini",
            videoUrl = "https://www.youtube.com/watch?v=$youtubeId",
            coverUrl = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg",
            duration = "YouTube",
            isManual = true
        )
        save(listOf(item) + _videos.value)
        return AddResult.Added
    }

    fun remove(videoId: String) {
        save(_videos.value.filterNot { it.id == videoId })
    }

    fun update(videoId: String, url: String, title: String): UpdateResult {
        val youtubeId = VideoItem.extractYoutubeVideoId(url.trim()) ?: return UpdateResult.InvalidUrl
        val updatedId = "local_youtube_$youtubeId"
        if (updatedId != videoId && _videos.value.any { it.id == updatedId }) {
            return UpdateResult.AlreadyExists
        }
        val current = _videos.value.firstOrNull { it.id == videoId } ?: return UpdateResult.NotFound
        val updated = current.copy(
            id = updatedId,
            title = title.trim().ifBlank { current.title },
            videoUrl = "https://www.youtube.com/watch?v=$youtubeId",
            coverUrl = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
        )
        save(_videos.value.map { if (it.id == videoId) updated else it })
        return UpdateResult.Updated
    }

    private fun readVideos(): List<VideoItem> = runCatching {
        val items = JSONArray(preferences.getString(KEY_VIDEOS, "[]"))
        buildList {
            repeat(items.length()) { index ->
                val item = items.optJSONObject(index) ?: return@repeat
                val url = item.optString("videoUrl")
                val youtubeId = VideoItem.extractYoutubeVideoId(url) ?: return@repeat
                add(
                    VideoItem(
                        id = "local_youtube_$youtubeId",
                        title = item.optString("title").ifBlank { "Video YouTube" },
                        artist = "Favorit Saya",
                        description = "Playlist pribadi di perangkat ini",
                        videoUrl = "https://www.youtube.com/watch?v=$youtubeId",
                        coverUrl = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg",
                        duration = "YouTube",
                        isManual = true
                    )
                )
            }
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    private fun save(items: List<VideoItem>) {
        val json = JSONArray().apply {
            items.forEach { video ->
                put(
                    JSONObject()
                        .put("title", video.title)
                        .put("videoUrl", video.videoUrl)
                )
            }
        }
        preferences.edit().putString(KEY_VIDEOS, json.toString()).apply()
        _videos.value = items
    }

    sealed interface AddResult {
        data object Added : AddResult
        data object InvalidUrl : AddResult
        data object AlreadyExists : AddResult
    }

    sealed interface UpdateResult {
        data object Updated : UpdateResult
        data object InvalidUrl : UpdateResult
        data object AlreadyExists : UpdateResult
        data object NotFound : UpdateResult
    }

    private companion object {
        const val PREFERENCE_NAME = "video_favorites_local_store"
        const val KEY_VIDEOS = "videos"
    }
}
