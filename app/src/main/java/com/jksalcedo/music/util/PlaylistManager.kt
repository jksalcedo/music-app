package com.jksalcedo.music.util

import android.content.Context
import com.jksalcedo.music.model.Playlist
import com.jksalcedo.music.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class PlaylistManager(private val context: Context) {
    private val playlistsFile = File(context.filesDir, "playlists.json")
    private val _playlists = mutableListOf<Playlist>()
    
    val playlists: List<Playlist> get() = _playlists.toList()
    
    init {
        loadPlaylists()
        // Ensure "All Songs" playlist always exists
        if (_playlists.none { it.id == Playlist.ALL_SONGS_PLAYLIST_ID }) {
            _playlists.add(0, Playlist.createAllSongsPlaylist())
        }
    }
    
    fun createPlaylist(name: String, description: String? = null): Playlist {
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description
        )
        _playlists.add(playlist)
        savePlaylists()
        return playlist
    }
    
    fun deletePlaylist(playlistId: String): Boolean {
        if (playlistId == Playlist.ALL_SONGS_PLAYLIST_ID) return false
        val removed = _playlists.removeAll { it.id == playlistId }
        if (removed) {
            savePlaylists()
        }
        return removed
    }
    
    fun addSongToPlaylist(playlistId: String, songId: Long): Boolean {
        val playlist = _playlists.find { it.id == playlistId } ?: return false
        if (playlist.songIds.contains(songId)) return false
        
        playlist.songIds.add(songId)
        savePlaylists()
        return true
    }
    
    fun removeSongFromPlaylist(playlistId: String, songId: Long): Boolean {
        if (playlistId == Playlist.ALL_SONGS_PLAYLIST_ID) return false
        val playlist = _playlists.find { it.id == playlistId } ?: return false
        val removed = playlist.songIds.remove(songId)
        if (removed) {
            savePlaylists()
        }
        return removed
    }
    
    fun getPlaylistSongs(playlistId: String, allSongs: List<Song>): List<Song> {
        if (playlistId == Playlist.ALL_SONGS_PLAYLIST_ID) {
            return allSongs
        }
        
        val playlist = _playlists.find { it.id == playlistId } ?: return emptyList()
        return allSongs.filter { song -> playlist.songIds.contains(song.id) }
    }
    
    fun getPlaylist(playlistId: String): Playlist? {
        return _playlists.find { it.id == playlistId }
    }
    
    fun updatePlaylistName(playlistId: String, newName: String): Boolean {
        if (playlistId == Playlist.ALL_SONGS_PLAYLIST_ID) return false
        val playlist = _playlists.find { it.id == playlistId } ?: return false
        val updatedPlaylist = playlist.copy(name = newName)
        val index = _playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            _playlists[index] = updatedPlaylist
            savePlaylists()
            return true
        }
        return false
    }
    
    private fun loadPlaylists() {
        if (!playlistsFile.exists()) return
        
        try {
            val jsonString = playlistsFile.readText()
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val songIdsArray = jsonObject.getJSONArray("songIds")
                val songIds = mutableListOf<Long>()
                
                for (j in 0 until songIdsArray.length()) {
                    songIds.add(songIdsArray.getLong(j))
                }
                
                val playlist = Playlist(
                    id = jsonObject.getString("id"),
                    name = jsonObject.getString("name"),
                    songIds = songIds,
                    createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis()),
                    description = if (jsonObject.has("description")) jsonObject.getString("description") else null
                )
                _playlists.add(playlist)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun savePlaylists() {
        try {
            val jsonArray = JSONArray()
            
            _playlists.forEach { playlist ->
                // Don't save the "All Songs" playlist as it's auto-generated
                if (playlist.id != Playlist.ALL_SONGS_PLAYLIST_ID) {
                    val jsonObject = JSONObject()
                    jsonObject.put("id", playlist.id)
                    jsonObject.put("name", playlist.name)
                    jsonObject.put("createdAt", playlist.createdAt)
                    if (playlist.description != null) {
                        jsonObject.put("description", playlist.description)
                    }
                    
                    val songIdsArray = JSONArray()
                    playlist.songIds.forEach { songId ->
                        songIdsArray.put(songId)
                    }
                    jsonObject.put("songIds", songIdsArray)
                    
                    jsonArray.put(jsonObject)
                }
            }
            
            playlistsFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}