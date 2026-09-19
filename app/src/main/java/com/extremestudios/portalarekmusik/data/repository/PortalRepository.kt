package com.extremestudios.portalarekmusik.data.repository

import android.util.Log
import com.extremestudios.portalarekmusik.data.model.PortalPost
import com.extremestudios.portalarekmusik.data.model.PortalUserSession
import com.extremestudios.portalarekmusik.data.model.VideoItem
import com.extremestudios.portalarekmusik.data.model.VideoBand
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PortalRepository {
    fun getCategories(): List<String> = listOf(
        "All",
        "Event",
        "Live Perform",
        "Band Profile",
        "Promo Band",
        "Open Booking",
        "New Release",
        "Announcement",
        "Festival",
        "Community"
    )

    fun getSubmissionCategories(): List<String> = getCategories().filter { it != "All" }

    // ─── Admin Auth ───────────────────────────────────────────────────────────

    suspend fun loginAdmin(username: String, password: String): Result<PortalUserSession> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("type", "admin_login")
                put("username", username.trim())
                put("password", password)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(response.optString("message").ifBlank { "Login gagal." })
            }
            val user = response.optJSONObject("user")
                ?: throw IllegalStateException("Akun admin tidak ditemukan.")
            PortalUserSession(
                username = user.optString("username").ifBlank { username.trim() },
                status = user.optString("status").ifBlank { "ACTIVE" },
                createdAt = user.optString("createdAt"),
                password = password
            )
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Login gagal.")
        }
    }

    suspend fun checkAdminStatus(username: String, password: String): Result<PortalUserSession> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("type", "admin_check_status")
                put("username", username.trim())
                put("password", password)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(response.optString("message").ifBlank { "Gagal verifikasi status admin." })
            }
            val user = response.optJSONObject("user")
                ?: throw IllegalStateException("Akun admin tidak ditemukan.")
            PortalUserSession(
                username = user.optString("username").ifBlank { username.trim() },
                status = user.optString("status").ifBlank { "ACTIVE" },
                createdAt = user.optString("createdAt"),
                password = password
            )
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal verifikasi status admin.")
        }
    }

    suspend fun updateAdminCredentials(
        currentUsername: String,
        newUsername: String,
        newPassword: String
    ): Result<PortalUserSession> = withContext(Dispatchers.IO) {
        runCatching {
            if (currentUsername.trim().isBlank()) throw IllegalStateException("Sesi admin tidak valid.")
            if (newUsername.trim().isBlank()) throw IllegalStateException("Username baru wajib diisi.")
            if (newPassword.isBlank()) throw IllegalStateException("Password baru wajib diisi.")

            val payload = JSONObject().apply {
                put("type", "admin_update_credentials")
                put("username", currentUsername.trim())
                put("new_username", newUsername.trim())
                put("new_password", newPassword)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(response.optString("message").ifBlank { "Gagal update akun admin." })
            }
            val user = response.optJSONObject("user")
                ?: throw IllegalStateException("Akun admin tidak ditemukan setelah update.")
            PortalUserSession(
                username = user.optString("username").ifBlank { newUsername.trim() },
                status = user.optString("status").ifBlank { "ACTIVE" },
                createdAt = user.optString("createdAt"),
                password = newPassword
            )
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal update akun admin.")
        }
    }

    // ─── Portal Posts ─────────────────────────────────────────────────────────

    suspend fun fetchActivePosts(): Result<List<PortalPost>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL("$GOOGLE_APPS_SCRIPT_ENDPOINT?type=portal_posts").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code saat mengambil data portal.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val root = JSONObject(body)
            val candidates = listOfNotNull(
                root.optJSONArray("portal_posts"),
                root.optJSONArray("posts"),
                root.optJSONArray("portalPosts")
            )
            val postsArray = candidates.firstOrNull() ?: JSONArray()
            buildList {
                for (i in 0 until postsArray.length()) {
                    val item = postsArray.optJSONObject(i) ?: continue
                    val status = item.optString("status").ifBlank { "PENDING" }
                    if (!status.equals("ACTIVE", ignoreCase = true)) continue
                    val posterUrl = item.optString("poster_url")
                    if (posterUrl.isBlank()) continue
                    add(
                        PortalPost(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            title = item.optString("title").ifBlank {
                                item.optString("band_name").ifBlank { item.optString("category") }
                            },
                            submitterName = item.optString("submitterName").ifBlank {
                                item.optString("band_name").ifBlank {
                                    item.optString("submitter_name").ifBlank { "Portal Kita" }
                                }
                            },
                            category = item.optString("category"),
                            posterUrl = posterUrl,
                            targetUrl = item.optString("target_url"),
                            shortCaption = item.optString("short_caption").ifBlank { item.optString("caption") },
                            eventDate = item.optString("event_date"),
                            status = status,
                            createdAt = item.optString("created_at")
                        )
                    )
                }
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal memuat Portal Kita.")
        }
    }

    // ─── Videos ───────────────────────────────────────────────────────────────

    suspend fun fetchVideoBands(): Result<List<VideoBand>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL("$GOOGLE_APPS_SCRIPT_ENDPOINT?type=video_bands").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code saat mengambil daftar band MV.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val bands = JSONObject(body).optJSONArray("bands") ?: JSONArray()
            buildList {
                for (i in 0 until bands.length()) {
                    val item = bands.optJSONObject(i) ?: continue
                    val name = item.optString("artist").trim()
                    if (name.isBlank()) continue
                    add(VideoBand(name, item.optInt("video_count", 0), item.optString("cover_url")))
                }
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal memuat daftar band MV.")
        }
    }

    suspend fun fetchActiveVideos(artist: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedArtist = java.net.URLEncoder.encode(artist, StandardCharsets.UTF_8.name())
            val connection = (URL("$GOOGLE_APPS_SCRIPT_ENDPOINT?type=videos&artist=$encodedArtist").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code saat mengambil data video.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val root = JSONObject(body)
            val candidates = listOfNotNull(
                root.optJSONArray("videos"),
                root.optJSONArray("data")
            )
            val videosArray = candidates.firstOrNull() ?: JSONArray()
            buildList {
                for (i in 0 until videosArray.length()) {
                    val item = videosArray.optJSONObject(i) ?: continue
                    val status = item.optString("status").ifBlank { "INACTIVE" }
                    if (!status.equals("ACTIVE", ignoreCase = true) &&
                        !status.equals("APPROVED", ignoreCase = true)
                    ) continue
                    val videoUrl = item.optString("video_url").ifBlank { item.optString("videoUrl").ifBlank { item.optString("url") } }
                    if (videoUrl.isBlank()) continue

                    val youtubeVideoId = VideoItem.extractYoutubeVideoId(videoUrl)
                    val coverUrl = item.optString("cover_url").ifBlank { item.optString("coverUrl") }.ifBlank {
                        if (youtubeVideoId != null) {
                        "https://img.youtube.com/vi/$youtubeVideoId/hqdefault.jpg"
                        } else {
                        ""
                        }
                    }

                    val videoId = item.optString("id").ifBlank { UUID.randomUUID().toString() }
                    add(
                        VideoItem(
                            id = videoId,
                            title = item.optString("title").ifBlank { "Video Klip" },
                            artist = item.optString("artist").ifBlank { "Indie Band" },
                            description = item.optString("description").ifBlank { "" },
                            videoUrl = videoUrl,
                            coverUrl = coverUrl,
                            duration = "Video",
                            isManual = item.optBoolean("is_manual", false) || videoId.startsWith("vid_")
                        )
                    )
                }
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal memuat video.")
        }
    }

    // ─── Submit Poster ────────────────────────────────────────────────────────

    suspend fun submitPoster(
        session: PortalUserSession?,
        deviceId: String,
        category: String,
        posterImageBase64: String,
        posterImageFileName: String,
        posterImageMimeType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (deviceId.isBlank()) throw IllegalStateException("Identitas perangkat tidak tersedia. Coba buka ulang aplikasi.")
            val payload = JSONObject().apply {
                put("type", "portal_post")
                session?.takeIf { it.status.equals("ACTIVE", ignoreCase = true) }?.let { activeSession ->
                    put("username", activeSession.username)
                    put("password", activeSession.password)
                }
                put("device_id", deviceId)
                put("category", category)
                put("posterImageBase64", posterImageBase64)
                put("posterImageFileName", posterImageFileName)
                put("posterImageMimeType", posterImageMimeType)
            }
            Log.d("PortalKirim", "payload={type=portal_post,category=$category}")
            val response = postJson(payload)
            Log.d("PortalKirim", "raw response=${response}")
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(
                    response.optString("message").ifBlank { "Gagal mengirim poster." }
                )
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal mengirim poster.")
        }
    }

    // ─── Upload Lagu ──────────────────────────────────────────────────────────

    suspend fun deletePortalPost(session: PortalUserSession, postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!session.status.equals("ACTIVE", ignoreCase = true)) {
                throw IllegalStateException("Login admin aktif diperlukan untuk menghapus poster.")
            }
            if (postId.isBlank()) throw IllegalStateException("ID poster tidak valid.")
            val payload = JSONObject().apply {
                put("type", "portal_delete")
                put("username", session.username)
                put("password", session.password)
                put("postId", postId)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(response.optString("message").ifBlank { "Gagal menghapus poster." })
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal menghapus poster.")
        }
    }

    suspend fun submitSong(
        session: PortalUserSession,
        artistName: String,
        songTitle: String,
        genre: String,
        city: String,
        releaseYear: String,
        description: String,
        audioUrl: String,
        coverImageBase64: String?,
        coverImageFileName: String?,
        coverImageMimeType: String?,
        coverArtworkLink: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (session.username.trim().isBlank()) {
                throw IllegalStateException("Sesi admin tidak valid. Silakan login ulang.")
            }
            val payload = JSONObject().apply {
                put("type", "song_registration")
                put("username", session.username)
                put("password", session.password)
                put("bandName", artistName)
                put("songTitle", songTitle)
                put("genre", genre)
                put("city", city)
                put("releaseYear", releaseYear)
                put("description", description)
                put("audioUrl", audioUrl)
                put("coverArtworkLink", coverArtworkLink)
                if (!coverImageBase64.isNullOrBlank()) {
                    put("coverImageBase64", coverImageBase64)
                    put("coverImageFileName", coverImageFileName ?: "cover.jpg")
                    put("coverImageMimeType", coverImageMimeType ?: "image/jpeg")
                }
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(
                    response.optString("message").ifBlank { "Gagal menyimpan lagu." }
                )
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal menyimpan lagu.")
        }
    }

    // ─── Upload Video ─────────────────────────────────────────────────────────

    suspend fun submitVideo(
        session: PortalUserSession,
        title: String,
        artist: String,
        videoUrl: String,
        description: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (session.username.trim().isBlank()) {
                throw IllegalStateException("Sesi admin tidak valid. Silakan login ulang.")
            }
            val payload = JSONObject().apply {
                put("type", "video_registration")
                put("username", session.username)
                put("password", session.password)
                put("title", title)
                put("artist", artist)
                put("video_url", videoUrl)
                put("description", description)
            }
            val response = postJson(payload)
            if (!response.optBoolean("success", false)) {
                throw IllegalStateException(
                    response.optString("message").ifBlank { "Gagal menyimpan video." }
                )
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal menyimpan video.")
        }
    }

    // ─── HTTP Helpers ─────────────────────────────────────────────────────────

    private fun postJson(payload: JSONObject): JSONObject {
        val connection = (URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(payload.toString())
        }

        val responseCode = connection.responseCode
        val responseBody = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()

        if (responseCode !in 200..299) {
            throw IllegalStateException("HTTP $responseCode saat mengirim data. $responseBody")
        }
        return JSONObject(responseBody.ifBlank { "{\"success\":false}" })
    }

    companion object {
        private const val GOOGLE_APPS_SCRIPT_ENDPOINT =
            "https://script.google.com/macros/s/AKfycbz1vWq1Ged6Qk5bfZUYUDvCeTfZicwBBZcvcwL5iHjrCiL1cLEQ4tCfKGt5IaAhJ69Mhw/exec"
    }
}
