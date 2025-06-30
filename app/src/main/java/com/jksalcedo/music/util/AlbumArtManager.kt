package com.jksalcedo.music.util

import android.content.Context
import android.net.Uri
import com.jksalcedo.music.R
import java.util.Random
import androidx.core.net.toUri

object AlbumArtManager {
    private val random = Random()
    private val albumArtResources = listOf(
        R.drawable.album_art_1,
        R.drawable.album_art_2,
        R.drawable.album_art_3,
        R.drawable.album_art_4,
        R.drawable.album_art_5,
        R.drawable.album_art_6,
        R.drawable.album_art_7,
        R.drawable.album_art_8,
        R.drawable.album_art_9,
        R.drawable.album_art_10
    )
    
    // Cache to ensure consistent album art for the same song
    private val songAlbumArtCache = mutableMapOf<Long, Int>()
    
    fun getAlbumArtForSong(context: Context, songId: Long, originalAlbumArtUri: Uri?): Uri? {
        // If song has original album art, use it
        if (originalAlbumArtUri != null) {
            return originalAlbumArtUri
        }
        
        // Check if we already assigned an album art to this song
        val cachedResourceId = songAlbumArtCache[songId]
        if (cachedResourceId != null) {
            return getResourceUri(context, cachedResourceId)
        }
        
        // Assign a random album art and cache it
        val randomResourceId = albumArtResources[random.nextInt(albumArtResources.size)]
        songAlbumArtCache[songId] = randomResourceId
        
        return getResourceUri(context, randomResourceId)
    }
    
    private fun getResourceUri(context: Context, resourceId: Int): Uri {
        return "android.resource://${context.packageName}/$resourceId".toUri()
    }
    
    fun clearCache() {
        songAlbumArtCache.clear()
    }
    
    fun getAlbumArtCount(): Int = albumArtResources.size
} 