package com.extremestudios.portalarekmusik.data.repository

import com.extremestudios.portalarekmusik.data.local.PlaylistLocalStore
import com.extremestudios.portalarekmusik.data.model.Playlist
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val localStore: PlaylistLocalStore) {
    val playlists: Flow<List<Playlist>> = localStore.playlistsFlow

    suspend fun createPlaylist(name: String): Playlist = localStore.createPlaylist(name)
    suspend fun renamePlaylist(id: String, newName: String) = localStore.renamePlaylist(id, newName)
    suspend fun deletePlaylist(id: String) = localStore.deletePlaylist(id)
    suspend fun addSongToPlaylist(playlistId: String, songId: String) = localStore.addSongToPlaylist(playlistId, songId)
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = localStore.removeSongFromPlaylist(playlistId, songId)
}
