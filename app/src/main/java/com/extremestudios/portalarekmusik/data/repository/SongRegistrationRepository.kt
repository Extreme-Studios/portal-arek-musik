package com.extremestudios.portalarekmusik.data.repository
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.extremestudios.portalarekmusik.data.model.SongRegistrationPayload
import com.extremestudios.portalarekmusik.data.model.MvRegistrationPayload
import com.extremestudios.portalarekmusik.data.model.RegisteredChannel
import com.extremestudios.portalarekmusik.data.model.PortalUserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedInputStream
import java.io.OutputStreamWriter
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class SongRegistrationRepository {
    companion object {
        const val GOOGLE_APPS_SCRIPT_ENDPOINT = "https://script.google.com/macros/s/AKfycbz1vWq1Ged6Qk5bfZUYUDvCeTfZicwBBZcvcwL5iHjrCiL1cLEQ4tCfKGt5IaAhJ69Mhw/exec"
        private const val AUDIO_UPLOAD_ENDPOINT = "https://smh-audio.3xtremestudios.workers.dev/admin-upload"
        private const val PUBLIC_AUDIO_UPLOAD_ENDPOINT = "https://smh-audio.3xtremestudios.workers.dev/public-upload"
        private const val MAX_AUDIO_BYTES = 10L * 1024L * 1024L
    }

    suspend fun uploadAdminAudio(
        session: PortalUserSession,
        contentResolver: ContentResolver,
        uri: Uri,
        artistName: String,
        songTitle: String,
        releaseDate: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!session.status.equals("ACTIVE", ignoreCase = true) || session.username.isBlank() || session.password.isBlank()) {
                throw IllegalStateException("Login admin aktif diperlukan untuk upload MP3.")
            }
            if (artistName.isBlank() || songTitle.isBlank()) {
                throw IllegalStateException("Nama band dan judul lagu wajib diisi sebelum upload MP3.")
            }

            val audioMeta = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    Pair(
                        if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "audio.mp3",
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
                    )
                } else Pair("audio.mp3", -1L)
            } ?: Pair("audio.mp3", -1L)
            val audioSize = audioMeta.second
            if (audioSize <= 0L) throw IllegalStateException("Ukuran file MP3 tidak dapat dibaca.")
            if (audioSize > MAX_AUDIO_BYTES) throw IllegalStateException("Ukuran MP3 maksimal 10 MB.")

            val query = "artist=${URLEncoder.encode(artistName.trim(), "UTF-8")}&title=${URLEncoder.encode(songTitle.trim(), "UTF-8")}&releaseDate=${URLEncoder.encode(releaseDate.trim(), "UTF-8")}" 
            val connection = (URL("$AUDIO_UPLOAD_ENDPOINT?$query").openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setFixedLengthStreamingMode(audioSize)
                setRequestProperty("Content-Type", contentResolver.getType(uri) ?: "audio/mpeg")
                setRequestProperty("X-Admin-Username", session.username)
                setRequestProperty("X-Admin-Password", session.password)
                setRequestProperty("X-File-Name", audioMeta.first)
            }
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    BufferedInputStream(input).use { bufferedInput ->
                        connection.outputStream.use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = bufferedInput.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                } ?: throw IllegalStateException("File MP3 tidak dapat dibuka.")

                val responseCode = connection.responseCode
                val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                val response = JSONObject(responseText.ifBlank { "{}" })
                if (responseCode !in 200..299 || !response.optBoolean("success", false)) {
                    throw IllegalStateException(response.optString("message").ifBlank { "Upload MP3 gagal (HTTP $responseCode)." })
                }
                response.optString("audioUrl").ifBlank {
                    throw IllegalStateException("Worker tidak mengembalikan link audio.")
                }
            } finally {
                connection.disconnect()
            }
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Gagal upload MP3.")
        }
    }

    suspend fun uploadPublicAudio(
        contentResolver: ContentResolver,
        uri: Uri,
        artistName: String,
        songTitle: String,
        releaseDate: String,
        deviceId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val audioMeta = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    Pair(
                        if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "audio.mp3",
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
                    )
                } else Pair("audio.mp3", -1L)
            } ?: Pair("audio.mp3", -1L)
            val audioSize = audioMeta.second
            if (audioSize <= 0L) throw IllegalStateException("Ukuran file MP3 tidak dapat dibaca.")
            if (audioSize > MAX_AUDIO_BYTES) throw IllegalStateException("Ukuran MP3 maksimal 10 MB.")

            val query = "artist=${URLEncoder.encode(artistName.trim(), "UTF-8")}&title=${URLEncoder.encode(songTitle.trim(), "UTF-8")}&releaseDate=${URLEncoder.encode(releaseDate.trim(), "UTF-8")}" 
            val connection = (URL("$PUBLIC_AUDIO_UPLOAD_ENDPOINT?$query").openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setFixedLengthStreamingMode(audioSize)
                setRequestProperty("Content-Type", contentResolver.getType(uri) ?: "audio/mpeg")
                setRequestProperty("X-Device-ID", deviceId)
                setRequestProperty("X-File-Name", audioMeta.first)
            }
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    BufferedInputStream(input).use { bufferedInput ->
                        connection.outputStream.use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = bufferedInput.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                } ?: throw IllegalStateException("File MP3 tidak dapat dibuka.")
                val responseCode = connection.responseCode
                val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                val response = JSONObject(responseText.ifBlank { "{}" })
                if (responseCode !in 200..299 || !response.optBoolean("success", false)) {
                    throw IllegalStateException(response.optString("message").ifBlank { "Upload MP3 gagal (HTTP $responseCode)." })
                }
                response.optString("audioUrl").ifBlank {
                    throw IllegalStateException("Worker tidak mengembalikan link audio.")
                }
            } finally {
                connection.disconnect()
            }
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Gagal upload MP3.")
        }
    }

    suspend fun submitRegistration(
        payload: SongRegistrationPayload,
        adminSession: PortalUserSession? = null,
        deviceId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!GOOGLE_APPS_SCRIPT_ENDPOINT.startsWith("http")) {
            return@withContext Result.failure(IllegalStateException("Endpoint Google Apps Script belum diatur."))
        }

        runCatching {
            val connection = (URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().apply {
                put("type", "song_registration")
                put("bandName", payload.bandName)
                put("songTitle", payload.songTitle)
                put("genre", payload.genre)
                put("city", payload.city)
                put("releaseYear", payload.releaseYear)
                if (!payload.mp3Link.isNullOrBlank()) {
                    put("mp3Link", payload.mp3Link)
                }
                if (!payload.coverImageBase64.isNullOrBlank()) {
                    put("coverImageBase64", payload.coverImageBase64)
                    put("coverImageFileName", payload.coverImageFileName.orEmpty())
                    put("coverImageMimeType", payload.coverImageMimeType.orEmpty())
                }
                adminSession?.takeIf { it.status.equals("ACTIVE", ignoreCase = true) }?.let { session ->
                    put("username", session.username)
                    put("password", session.password)
                }
                if (deviceId?.isNotBlank() == true) put("device_id", deviceId)
            }.toString()

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                throw IllegalStateException("Gagal kirim data (HTTP $responseCode). $errorBody")
            }
            val responseBody = connection.inputStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            val responseJson = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
            val success = responseJson.optBoolean("success", false)

            if (!success) {
                val message = responseJson.optString("message").ifBlank {
                    "Registrasi lagu gagal diproses."
                }
                throw IllegalStateException(message)
            }
            connection.disconnect()
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Terjadi kesalahan saat mengirim registrasi.")
        }
    }

    suspend fun submitMvRegistration(
        payload: MvRegistrationPayload,
        adminSession: PortalUserSession? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!GOOGLE_APPS_SCRIPT_ENDPOINT.startsWith("http")) {
            return@withContext Result.failure(IllegalStateException("Endpoint Google Apps Script belum diatur."))
        }

        runCatching {
            val connection = (URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().apply {
                put("type", "video_registration")
                put("title", payload.title)
                put("artist", payload.artist)
                put("videoUrl", payload.videoUrl)
                put("video_url", payload.videoUrl)
                put("releaseYear", payload.releaseYear)
                put("release_year", payload.releaseYear)
                put("albumName", payload.albumName)
                put("album_name", payload.albumName)
                put("genre", payload.genre)
                adminSession?.takeIf { it.status.equals("ACTIVE", ignoreCase = true) }?.let { session ->
                    put("username", session.username)
                    put("password", session.password)
                }
            }.toString()

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                throw IllegalStateException("Gagal kirim data (HTTP $responseCode). $errorBody")
            }
            val responseBody = connection.inputStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            val responseJson = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
            val success = responseJson.optBoolean("success", false)

            if (!success) {
                val message = responseJson.optString("message").ifBlank {
                    "Registrasi MV gagal diproses."
                }
                throw IllegalStateException(message)
            }
            connection.disconnect()
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Terjadi kesalahan saat mengirim registrasi.")
        }
    }

    suspend fun submitYoutubeChannel(
        artist: String,
        channelUrl: String,
        adminSession: PortalUserSession? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!GOOGLE_APPS_SCRIPT_ENDPOINT.startsWith("http")) {
            return@withContext Result.failure(IllegalStateException("Endpoint Google Apps Script belum diatur."))
        }

        runCatching {
            val connection = (URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().apply {
                put("type", "channel_import")
                put("artist", artist.trim())
                put("channelUrl", channelUrl.trim())
                put("channel_url", channelUrl.trim())
                adminSession?.takeIf { it.status.equals("ACTIVE", ignoreCase = true) }?.let { session ->
                    put("username", session.username)
                    put("password", session.password)
                }
            }.toString()
            android.util.Log.d("YoutubeChannel", "Submitting channel for artist=\${artist.trim()}")
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }

            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (responseCode !in 200..299) {
                throw IllegalStateException("Gagal kirim channel (HTTP \$responseCode).")
            }
            val responseJson = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Channel YouTube gagal disimpan." })
            }
            android.util.Log.d("YoutubeChannel", "Channel saved: \$responseBody")
            connection.disconnect()
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Terjadi kesalahan saat menambah channel.")
        }
    }

    suspend fun fetchChannels(): Result<List<RegisteredChannel>> = withContext(Dispatchers.IO) { runCatching {
        val body = (URL("$GOOGLE_APPS_SCRIPT_ENDPOINT?type=video_channels").openConnection() as HttpURLConnection).inputStream.bufferedReader().use { it.readText() }
        val items = JSONObject(body).optJSONArray("channels") ?: org.json.JSONArray()
        buildList { for (i in 0 until items.length()) { val item=items.optJSONObject(i) ?: continue; add(RegisteredChannel(item.optString("id"), item.optString("artist"), item.optString("channel_url"))) } }
    } }

    suspend fun deleteChannel(session: PortalUserSession, id: String): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val connection=(URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply { requestMethod="POST"; doOutput=true; setRequestProperty("Content-Type","application/json; charset=UTF-8") }
        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(JSONObject().put("type","channel_delete").put("id",id).put("username",session.username).put("password",session.password).toString()) }
        val response=JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        if (!response.optBoolean("success",false)) throw IllegalStateException(response.optString("message"))
    } }

    suspend fun deleteVideo(session: PortalUserSession, id: String): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val connection=(URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply { requestMethod="POST"; doOutput=true; setRequestProperty("Content-Type","application/json; charset=UTF-8") }
        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(JSONObject().put("type","video_delete").put("id",id).put("username",session.username).put("password",session.password).toString()) }
        val response=JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        if (!response.optBoolean("success",false)) throw IllegalStateException(response.optString("message"))
    } }

    suspend fun updateVideoTitle(session: PortalUserSession, id: String, title: String): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val connection=(URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply { requestMethod="POST"; doOutput=true; setRequestProperty("Content-Type","application/json; charset=UTF-8") }
        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(JSONObject().put("type","video_update").put("id",id).put("title",title).put("username",session.username).put("password",session.password).toString()) }
        val response=JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        if (!response.optBoolean("success",false)) throw IllegalStateException(response.optString("message"))
    } }
}
