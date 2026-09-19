package com.extremestudios.portalarekmusik.data.repository

import android.util.Log
import com.extremestudios.portalarekmusik.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ApiSongRepository {
    suspend fun fetchSongs(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = getJson("$SONGS_ENDPOINT?ts=${System.currentTimeMillis()}")
            val root = JSONObject(body)
            if (!root.optBoolean("success", false)) throw IllegalStateException("Response API tidak sukses.")
            val songsArray = root.optJSONArray("songs")
                ?: root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("songs")
                ?: throw IllegalStateException("Field songs tidak ditemukan.")

            Log.d(TAG, "Raw songs from API: ${songsArray.length()}")
            var activeCount = 0
            val songs = buildList {
                for (i in 0 until songsArray.length()) {
                    val item = songsArray.optJSONObject(i) ?: continue
                    val title = readString(item, "title", "song_title", "judul_lagu")
                    val artist = readString(item, "artist", "band_name", "artist_name", "bandName", "submitterName")
                    val status = readString(item, "status").ifBlank { "ACTIVE" }
                    val isActive = status.equals("active", ignoreCase = true) ||
                        status.equals("published", ignoreCase = true) ||
                        status.equals("ready", ignoreCase = true)
                    if (!isActive) continue
                    activeCount++

                    val rawId = readString(item, "song_id", "id")
                    val createdAt = readString(item, "created_at", "uploaded_at", "uploadedAt")
                    val coverUrl = readString(item, "cover_url", "coverUrl", "cover")
                    val audioUrl = readString(item, "audio_url", "audioUrl", "audio")
                    val albumName = readString(item, "album_name", "albumName")
                    val trackNumber = readString(item, "track_number", "trackNumber").toIntOrNull()
                    val resolvedId = rawId.ifBlank {
                        listOf(title, artist, createdAt, i.toString()).joinToString("#")
                    }
                    Log.d(TAG, "Song from API title=$title cover_url=$coverUrl audio_url=$audioUrl album_name=$albumName track_number=$trackNumber")

                    add(
                        Song(
                            id = resolvedId,
                            title = title,
                            artist = artist,
                            genre = readString(item, "genre"),
                            description = readString(item, "description", "desc", "notes"),
                            coverUrl = coverUrl,
                            audioUrl = audioUrl,
                            uploadedAt = createdAt,
                            status = status,
                            releaseYear = readString(item, "release_year", "year", "releaseYear"),
                            albumName = albumName,
                            trackNumber = trackNumber
                        )
                    )
                }
            }
                .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<Song> { parseCreatedAtMillis(it.uploadedAt) }
                        .thenBy { it.title.lowercase(Locale.US) }
                )

            Log.d(TAG, "Active songs after status filter: $activeCount")
            Log.d(TAG, "Songs after validation/dedupe/sort: ${songs.size}")
            songs
        }.onFailure { error -> Log.e(TAG, "Gagal memuat lagu dari API.", error) }
    }

    private fun getJson(requestUrl: String): String {
        var lastError: Exception? = null
        repeat(MAX_API_ATTEMPTS) { attempt ->
            try {
                return getJsonOnce("$requestUrl&retry=$attempt-${System.nanoTime()}")
            } catch (error: Exception) {
                lastError = error
                Log.w(TAG, "Percobaan API lagu ${attempt + 1}/$MAX_API_ATTEMPTS gagal: ${error.message}")
            }
        }
        throw lastError ?: IllegalStateException("Gagal mengambil data lagu.")
    }

    private fun getJsonOnce(requestUrl: String): String {
        var url = URL(requestUrl)
        repeat(MAX_REDIRECTS) {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("User-Agent", "PortalArekMusik/2.9 (Android)")
            }
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")?.trim()
                        ?: throw IllegalStateException("Redirect API tidak memiliki alamat tujuan.")
                    url = URL(url, location)
                    return@repeat
                }
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code saat mengambil data lagu. $body")
                }
                if (body.isBlank()) throw IllegalStateException("Respons API lagu kosong.")
                return body
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalStateException("Terlalu banyak redirect saat mengambil data lagu.")
    }

    fun getGenres(songs: List<Song>): List<String> = listOf("Semua") + songs.map { it.genre }.filter { it.isNotBlank() }.distinct().sorted()
    fun getArtists(songs: List<Song>): List<String> = songs.map { it.artist }.filter { it.isNotBlank() }.distinct().sorted()

    fun getFallbackSongs(): List<Song> = emptyList()

    companion object {
        private const val TAG = "ApiSongRepository"
        private const val MAX_REDIRECTS = 5
        private const val MAX_API_ATTEMPTS = 3
        private const val SONGS_ENDPOINT = "https://script.google.com/macros/s/AKfycbxrWPxN_Az4iBYy6QWqN9X2gkjlCSrMC-eqU2gLscD7gTcRvd62nL7l2cDmcmQa2B_6mA/exec"
    }
}

private fun readString(item: JSONObject, vararg keys: String): String {
    keys.forEach { key ->
        if (item.has(key)) {
            val value = item.opt(key)
            if (value != null && value != JSONObject.NULL) {
                val text = value.toString().trim()
                if (text.isNotBlank()) return text
            }
        }
    }
    return ""
}

private fun parseCreatedAtMillis(value: String): Long {
    if (value.isBlank()) return Long.MIN_VALUE
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(value)?.time
        }.getOrNull()
    } ?: Long.MIN_VALUE
}
