package com.extremestudios.portalarekmusik.data.model

data class MvRegistrationPayload(
    val title: String,
    val artist: String,
    val releaseYear: String,
    val albumName: String,
    val genre: String,
    val videoUrl: String
)
