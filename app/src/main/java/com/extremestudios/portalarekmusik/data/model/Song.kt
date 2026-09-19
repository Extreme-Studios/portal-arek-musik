package com.extremestudios.portalarekmusik.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val description: String,
    val coverUrl: String,
    val audioUrl: String,
    val uploadedAt: String,
    val status: String,
    val releaseYear: String = "",
    val albumName: String = "",
    val trackNumber: Int? = null
)
