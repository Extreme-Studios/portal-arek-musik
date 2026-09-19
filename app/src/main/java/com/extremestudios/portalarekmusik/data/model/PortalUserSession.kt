package com.extremestudios.portalarekmusik.data.model

data class PortalUserSession(
    val username: String,
    val status: String,
    val createdAt: String = "",
    val password: String = ""
)
