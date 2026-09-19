package com.extremestudios.portalarekmusik.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class LibraryLocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("personal_library_store", Context.MODE_PRIVATE)
    private val _historyIds = MutableStateFlow(readIds("history_ids"))
    val historyIds: StateFlow<List<String>> = _historyIds.asStateFlow()
    private val _favoriteIds = MutableStateFlow(readIds("favorite_ids"))
    val favoriteIds: StateFlow<List<String>> = _favoriteIds.asStateFlow()

    fun recordPlayed(songId: String) {
        if (songId.isBlank()) return
        save("history_ids", listOf(songId) + _historyIds.value.filterNot { it == songId }.take(99)) { _historyIds.value = it }
    }

    fun toggleFavorite(songId: String) {
        if (songId.isBlank()) return
        val updated = if (songId in _favoriteIds.value) _favoriteIds.value - songId else listOf(songId) + _favoriteIds.value
        save("favorite_ids", updated) { _favoriteIds.value = it }
    }

    private fun readIds(key: String): List<String> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun save(key: String, ids: List<String>, update: (List<String>) -> Unit) {
        prefs.edit().putString(key, JSONArray(ids).toString()).apply()
        update(ids)
    }
}
