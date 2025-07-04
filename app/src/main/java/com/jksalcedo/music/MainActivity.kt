package com.jksalcedo.music

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jksalcedo.music.adapter.SongAdapter
import com.jksalcedo.music.adapter.PlaylistAdapter
import com.jksalcedo.music.databinding.ActivityMainBinding
import com.jksalcedo.music.model.Song
import com.jksalcedo.music.model.Playlist
import com.jksalcedo.music.service.MusicService
import com.jksalcedo.music.util.AlbumArtManager
import com.jksalcedo.music.util.PlaylistManager
import androidx.core.net.toUri
import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.transition.Visibility
import com.jksalcedo.music.R
import androidx.transition.TransitionManager
import android.widget.EditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var songAdapter: SongAdapter
    private lateinit var playlistManager: PlaylistManager
    private var musicService: MusicService? = null
    private var currentSong: Song? = null
    private var songs: List<Song> = emptyList()
    private var currentPlaylist: Playlist? = null
    private var currentSongIndex = -1
    private var isPlaylistMode = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicService?.setOnSongCompletedListener {
                // Handle song completion based on repeat/shuffle modes
                when {
                    repeatMode == RepeatMode.REPEAT_ONE -> {
                        // Replay the same song
                        currentSong?.let { playSong(it) }
                    }
                    repeatMode == RepeatMode.REPEAT_ALL -> {
                        // Play next song (will loop back to first)
                        playNext()
                    }
                    shuffleMode -> {
                        // Play next song in shuffled order
                        playNext()
                    }
                    else -> {
                        // No repeat, play next if available
                        if (currentSongIndex < getCurrentSongList().size - 1) {
                            playNext()
                        } else {
                            // Song finished, stop playback
                            musicService?.pause()
                            updatePlayPauseButton()
                            // Hide player card
                            TransitionManager.beginDelayedTransition(binding.root)
                            binding.playerCard.visibility = View.GONE
                        }
                    }
                }
            }
            updatePlayPauseButton()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadSongs()
        } else {
            Toast.makeText(this, "Storage permission required to play music", Toast.LENGTH_LONG).show()
        }
    }

    private enum class RepeatMode { NONE, REPEAT_ONE, REPEAT_ALL }
    private enum class SortMode { TITLE, ARTIST, ALBUM, DURATION }

    private var repeatMode = RepeatMode.NONE
    private var shuffleMode = false
    private var sortMode = SortMode.TITLE
    private var shuffledSongs: List<Song> = emptyList()
    private var isUserSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playlistManager = PlaylistManager(this)
        currentPlaylist = Playlist.createAllSongsPlaylist()
        
        setupRecyclerView()
        setupClickListeners()
        setupProgressSlider()
        bindMusicService()
        checkPermissionsAndLoadSongs()
        updateRepeatShuffleButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        unbindService(serviceConnection)
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            this,
            songs = songs,
            onSongClick = { song ->
                playSong(song)
            },
            onSongLongClick = { song ->
                showAddToPlaylistDialog(song)
            }
        )
        binding.playlistRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = songAdapter
        }
    }

    private fun setupClickListeners() {
        binding.playPauseButton.setOnClickListener {
            musicService?.let { service ->
                if (service.isPlaying()) {
                    service.pause()
                } else {
                    service.play()
                }
                updatePlayPauseButton()
            }
        }

        binding.previousButton.setOnClickListener {
            playPrevious()
        }

        binding.nextButton.setOnClickListener {
            playNext()
        }

        binding.repeatShuffleButton.setOnClickListener {
            cycleRepeatShuffleMode()
        }

        binding.sortButton.setOnClickListener {
            showSortDialog()
        }

        binding.playlistsButton.setOnClickListener {
            showPlaylistDialog()
        }

        binding.createPlaylistFab.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun setupProgressSlider() {
        binding.progressSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                isUserSeeking = true
                musicService?.setUserSeeking(true)
                musicService?.seekTo(value.toInt())
            }
        }
        binding.progressSlider.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                isUserSeeking = true
                musicService?.setUserSeeking(true)
            }
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                isUserSeeking = false
                musicService?.setUserSeeking(false)
            }
        })
        
        // Set custom label formatter to show time in MM:SS format
        binding.progressSlider.setLabelFormatter { value ->
            formatTime(value.toLong())
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun bindMusicService() {
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    private fun checkPermissionsAndLoadSongs() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }) {
            loadSongs()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun loadSongs() {
        val loadedSongs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString())
                    .build()

                // Get album art URI using a separate query
                val albumArtUri = if (albumId > 0) {
                    val albumArtProjection = arrayOf(MediaStore.Audio.Albums.ALBUM_ART)
                    val albumArtSelection = "${MediaStore.Audio.Albums._ID} = ?"
                    val albumArtSelectionArgs = arrayOf(albumId.toString())

                    contentResolver.query(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        albumArtProjection,
                        albumArtSelection,
                        albumArtSelectionArgs,
                        null
                    )?.use { albumCursor ->
                        if (albumCursor.moveToFirst()) {
                            val albumArtPath = albumCursor.getString(
                                albumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART)
                            )
                            if (albumArtPath != null) {
                                "file://$albumArtPath".toUri()
                            } else null
                        } else null
                    }
                } else null

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    uri = contentUri,
                    albumArtUri = AlbumArtManager.getAlbumArtForSong(this, id, albumArtUri)
                )
                loadedSongs.add(song)
            }
        }

        this.songs = loadedSongs
        updateCurrentPlaylistTitle()
        sortAndDisplaySongs()
    }

    private fun updateProgress() {
        musicService?.let { service ->
            val currentPosition = service.getCurrentPosition()
            val duration = service.getDuration()
            binding.progressSlider.value = currentPosition.toFloat()
            binding.progressSlider.valueTo = duration.toFloat()
        }
    }

    private fun updatePlayPauseButton() {
        musicService?.let { service ->
            binding.playPauseButton.setImageResource(
                if (service.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }

    private fun cycleRepeatShuffleMode() {
        when {
            !shuffleMode && repeatMode == RepeatMode.NONE -> {
                shuffleMode = true
                repeatMode = RepeatMode.NONE
            }
            shuffleMode -> {
                shuffleMode = false
                repeatMode = RepeatMode.REPEAT_ALL
            }
            repeatMode == RepeatMode.REPEAT_ALL -> {
                repeatMode = RepeatMode.REPEAT_ONE
            }
            repeatMode == RepeatMode.REPEAT_ONE -> {
                repeatMode = RepeatMode.NONE
            }
        }
        updateRepeatShuffleButton()
    }

    private fun updateRepeatShuffleButton() {
        when {
            shuffleMode -> {
                binding.repeatShuffleButton.setImageResource(R.drawable.ic_shuffle)
                binding.repeatShuffleButton.contentDescription = getString(R.string.shuffle)
                binding.repeatShuffleButton.imageAlpha = 255
            }
            repeatMode == RepeatMode.REPEAT_ONE -> {
                binding.repeatShuffleButton.setImageResource(R.drawable.ic_repeat_one)
                binding.repeatShuffleButton.contentDescription = getString(R.string.repeat)
                binding.repeatShuffleButton.imageAlpha = 255
            }
            repeatMode == RepeatMode.REPEAT_ALL -> {
                binding.repeatShuffleButton.setImageResource(R.drawable.ic_repeat)
                binding.repeatShuffleButton.contentDescription = getString(R.string.repeat_all)
                binding.repeatShuffleButton.imageAlpha = 255
            }
            else -> {
                binding.repeatShuffleButton.setImageResource(R.drawable.ic_repeat)
                binding.repeatShuffleButton.contentDescription = getString(R.string.repeat)
                binding.repeatShuffleButton.imageAlpha = 80
            }
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_title),
            getString(R.string.sort_by_artist),
            getString(R.string.sort_by_album),
            getString(R.string.sort_by_duration)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sort))
            .setItems(sortOptions) { _, which ->
                sortMode = when (which) {
                    0 -> SortMode.TITLE
                    1 -> SortMode.ARTIST
                    2 -> SortMode.ALBUM
                    3 -> SortMode.DURATION
                    else -> SortMode.TITLE
                }
                sortAndDisplaySongs()
            }
            .show()
    }

    private fun sortAndDisplaySongs() {
        val currentSongs = currentPlaylist?.let { playlist ->
            playlistManager.getPlaylistSongs(playlist.id, songs)
        } ?: songs
        
        sortAndDisplaySongs(currentSongs)
    }

    private fun sortAndDisplaySongs(songsToSort: List<Song>) {
        val sorted = when (sortMode) {
            SortMode.TITLE -> songsToSort.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> songsToSort.sortedBy { it.artist.lowercase() }
            SortMode.ALBUM -> songsToSort.sortedBy { it.album.lowercase() }
            SortMode.DURATION -> songsToSort.sortedBy { it.duration }
        }
        songAdapter.updateSongs(sorted)
        
        // Update shuffled songs if shuffle mode is active
        shuffledSongs = if (shuffleMode) sorted.shuffled() else sorted
        
        // Update current song index if a song is currently playing
        if (currentSong != null) {
            currentSongIndex = getCurrentSongList().indexOf(currentSong)
            // Update the selected position in the adapter
            songAdapter.setSelectedPosition(currentSongIndex)
        }
    }

    private fun playSong(song: Song) {
        currentSong = song
        currentSongIndex = getCurrentSongList().indexOf(song)
        binding.songTitle.text = song.title
        binding.artistName.text = song.artist
        song.albumArtUri?.let { uri ->
            binding.albumArt.setImageURI(uri)
        } ?: run {
            binding.albumArt.setImageResource(R.drawable.placeholder_album_art)
        }
        musicService?.setSong(song)
        musicService?.play()
        updatePlayPauseButton()
        handler.post(updateProgressRunnable)
        
        // Update the selected position in the adapter
        songAdapter.setSelectedPosition(currentSongIndex)
        
        // Animate player card appearance
        TransitionManager.beginDelayedTransition(binding.root)
        binding.playerCard.visibility = View.VISIBLE
    }

    private fun playNext() {
        val list = getCurrentSongList()
        if (list.isEmpty()) return
        
        when {
            repeatMode == RepeatMode.REPEAT_ONE -> {
                // Stay on the same song
                playSong(list[currentSongIndex])
            }
            else -> {
                // Move to next song
                currentSongIndex = (currentSongIndex + 1) % list.size
                playSong(list[currentSongIndex])
            }
        }
    }

    private fun playPrevious() {
        val list = getCurrentSongList()
        if (list.isEmpty()) return
        
        when {
            repeatMode == RepeatMode.REPEAT_ONE -> {
                // Stay on the same song
                playSong(list[currentSongIndex])
            }
            else -> {
                // Move to previous song
                if (currentSongIndex > 0) {
                    currentSongIndex--
                } else {
                    // Wrap to the end
                    currentSongIndex = list.size - 1
                }
                playSong(list[currentSongIndex])
            }
        }
    }

    private fun showPlaylistDialog() {
        val playlists = playlistManager.playlists
        val playlistNames = playlists.map { it.name }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_playlist)
            .setItems(playlistNames) { _, which ->
                switchToPlaylist(playlists[which])
            }
            .setNeutralButton(R.string.manage_playlists) { _, _ ->
                showManagePlaylistsDialog()
            }
            .show()
    }

    private fun showCreatePlaylistDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.enter_playlist_name)
            setPadding(64, 32, 64, 32)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_playlist)
            .setView(editText)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val playlist = playlistManager.createPlaylist(name)
                    Toast.makeText(this, R.string.playlist_created, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddToPlaylistDialog(song: Song) {
        val playlists = playlistManager.playlists.filter { it.id != Playlist.ALL_SONGS_PLAYLIST_ID }
        if (playlists.isEmpty()) {
            Toast.makeText(this, "Create a playlist first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val playlistNames = playlists.map { it.name }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_to_playlist)
            .setItems(playlistNames) { _, which ->
                val success = playlistManager.addSongToPlaylist(playlists[which].id, song.id)
                if (success) {
                    Toast.makeText(this, R.string.song_added_to_playlist, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showManagePlaylistsDialog() {
        val playlists = playlistManager.playlists.filter { it.id != Playlist.ALL_SONGS_PLAYLIST_ID }
        if (playlists.isEmpty()) {
            Toast.makeText(this, "No playlists to manage", Toast.LENGTH_SHORT).show()
            return
        }
        
        val playlistNames = playlists.map { it.name }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.manage_playlists)
            .setItems(playlistNames) { _, which ->
                showPlaylistOptionsDialog(playlists[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPlaylistOptionsDialog(playlist: Playlist) {
        val options = arrayOf(getString(R.string.rename), getString(R.string.delete))
        
        MaterialAlertDialogBuilder(this)
            .setTitle(playlist.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenamePlaylistDialog(playlist)
                    1 -> showDeletePlaylistDialog(playlist)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRenamePlaylistDialog(playlist: Playlist) {
        val editText = EditText(this).apply {
            setText(playlist.name)
            selectAll()
            setPadding(64, 32, 64, 32)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rename_playlist)
            .setView(editText)
            .setPositiveButton(R.string.rename) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != playlist.name) {
                    val success = playlistManager.updatePlaylistName(playlist.id, newName)
                    if (success) {
                        Toast.makeText(this, R.string.playlist_renamed, Toast.LENGTH_SHORT).show()
                        // Update UI if this is the current playlist
                        if (currentPlaylist?.id == playlist.id) {
                            currentPlaylist = currentPlaylist?.copy(name = newName)
                            updateCurrentPlaylistTitle()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeletePlaylistDialog(playlist: Playlist) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_playlist)
            .setMessage("Delete \"${playlist.name}\"?")
            .setPositiveButton(R.string.delete) { _, _ ->
                val success = playlistManager.deletePlaylist(playlist.id)
                if (success) {
                    Toast.makeText(this, R.string.playlist_deleted, Toast.LENGTH_SHORT).show()
                    // If we deleted the current playlist, switch to "All Songs"
                    if (currentPlaylist?.id == playlist.id) {
                        switchToPlaylist(Playlist.createAllSongsPlaylist())
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun switchToPlaylist(playlist: Playlist) {
        currentPlaylist = playlist
        updateCurrentPlaylistTitle()
        refreshSongList()
    }

    private fun updateCurrentPlaylistTitle() {
        binding.currentPlaylistTitle.text = currentPlaylist?.name ?: getString(R.string.your_playlist)
    }

    private fun refreshSongList() {
        currentPlaylist?.let { playlist ->
            val playlistSongs = playlistManager.getPlaylistSongs(playlist.id, songs)
            sortAndDisplaySongs(playlistSongs)
        }
    }

    private fun getCurrentSongList(): List<Song> {
        val currentSongs = currentPlaylist?.let { playlist ->
            playlistManager.getPlaylistSongs(playlist.id, songs)
        } ?: songs
        
        return if (shuffleMode) {
            if (shuffledSongs.isEmpty()) {
                currentSongs.shuffled().also { shuffledSongs = it }
            } else {
                shuffledSongs
            }
        } else {
            currentSongs
        }
    }
}