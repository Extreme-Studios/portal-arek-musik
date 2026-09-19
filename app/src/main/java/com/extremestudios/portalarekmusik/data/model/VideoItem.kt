package com.extremestudios.portalarekmusik.data.model

data class VideoItem(
    val id: String,
    val title: String,
    val artist: String,
    val description: String,
    val videoUrl: String,
    val coverUrl: String,
    val duration: String,
    val isManual: Boolean = false
) {
    companion object {
        fun extractYoutubeVideoId(url: String): String? {
            val trimmed = url.trim()
            val shortMatch = Regex("youtu\\.be/([A-Za-z0-9_-]{11})").find(trimmed)
            if (shortMatch != null) return shortMatch.groupValues[1]
            val longMatch = Regex("[?&]v=([A-Za-z0-9_-]{11})").find(trimmed)
            if (longMatch != null) return longMatch.groupValues[1]
            val embedMatch = Regex("/embed/([A-Za-z0-9_-]{11})").find(trimmed)
            if (embedMatch != null) return embedMatch.groupValues[1]
            return null
        }
    }
}
