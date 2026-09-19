package com.extremestudios.portalarekmusik.data.model

data class SongRegistrationPayload(
    val bandName: String,
    val songTitle: String,
    val genre: String,
    val city: String,
    val releaseYear: String,
    val mp3Link: String? = null,
    val coverImageBase64: String? = null,
    val coverImageFileName: String? = null,
    val coverImageMimeType: String? = null
)
