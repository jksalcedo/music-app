package com.jksalcedo.music.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.jksalcedo.music.MainActivity
import com.jksalcedo.music.R
import com.jksalcedo.music.model.Song
import java.util.Locale
import java.util.concurrent.TimeUnit

class SongAdapter(
    private val context: Context,
    private var songs: List<Song> = emptyList(),
    private val onSongClick: (Song) -> Unit,
    private val onSongLongClick: ((Song) -> Unit)? = null,
    private var selectedPosition: Int = RecyclerView.NO_POSITION
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], position == selectedPosition)
    }

    override fun getItemCount() = songs.size

    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(position)
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val albumArt: ShapeableImageView = itemView.findViewById(R.id.albumArt)
        private val songTitle: TextView = itemView.findViewById(R.id.songTitle)
        private val artistName: TextView = itemView.findViewById(R.id.artistName)
        private val duration: TextView = itemView.findViewById(R.id.duration)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSongClick(songs[position])
                    (itemView.context as? MainActivity)?.songAdapter?.setSelectedPosition(position)
                }
            }
            
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSongLongClick?.invoke(songs[position])
                    true
                } else {
                    false
                }
            }
        }

        fun bind(song: Song, isSelected: Boolean) {
            songTitle.text = song.title
            artistName.text = song.artist
            duration.text = formatDuration(song.duration)
            
            // Load album art with fallback
            if (song.albumArtUri != null) {
                albumArt.setImageURI(song.albumArtUri)
                // Set a fallback in case the URI fails to load
                albumArt.setImageDrawable(null) // Clear any previous drawable
                albumArt.setImageURI(song.albumArtUri)
            } else {
                albumArt.setImageResource(R.drawable.ic_music_note)
            }
            
            songTitle.setTextColor(ContextCompat.getColor(context, if(isSelected) R.color.secondary_variant else R.color.white))
        }

        private fun formatDuration(durationMs: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                    TimeUnit.MINUTES.toSeconds(minutes)
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }
} 