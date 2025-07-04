package com.jksalcedo.music.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jksalcedo.music.R
import com.jksalcedo.music.model.Playlist

class PlaylistAdapter(
    private var playlists: List<Playlist> = emptyList(),
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onPlaylistOptionsClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updatePlaylists(newPlaylists: List<Playlist>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount() = playlists.size

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playlistName: TextView = itemView.findViewById(R.id.playlistName)
        private val songCount: TextView = itemView.findViewById(R.id.songCount)
        private val optionsButton: ImageButton = itemView.findViewById(R.id.optionsButton)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlaylistClick(playlists[position])
                }
            }

            optionsButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlaylistOptionsClick(playlists[position])
                }
            }
        }

        fun bind(playlist: Playlist) {
            playlistName.text = playlist.name
            val count = playlist.songIds.size
            songCount.text = if (playlist.id == Playlist.ALL_SONGS_PLAYLIST_ID) {
                ""  // Don't show count for "All Songs" as it's dynamic
            } else {
                "$count ${if (count == 1) "song" else "songs"}"
            }
            
            // Hide options button for "All Songs" playlist
            optionsButton.visibility = if (playlist.id == Playlist.ALL_SONGS_PLAYLIST_ID) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }
}