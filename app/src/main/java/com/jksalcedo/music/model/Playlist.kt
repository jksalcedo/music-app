package com.jksalcedo.music.model

import android.net.Uri

data class Playlist(
    val id: String,
    val name: String,
    val songIds: MutableList<Long> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    val description: String? = null
) {
    companion object {
        const val ALL_SONGS_PLAYLIST_ID = "all_songs"
        const val ALL_SONGS_PLAYLIST_NAME = "All Songs"
        
        fun createAllSongsPlaylist(): Playlist {
            return Playlist(
                id = ALL_SONGS_PLAYLIST_ID,
                name = ALL_SONGS_PLAYLIST_NAME
            )
        }
    }
}