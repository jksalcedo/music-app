# Music App - Offline Music Player for Android

## Description

This Android application allows users to play locally stored music files on their devices. It provides a clean user interface to browse songs, control playback, and enjoy music with a persistent notification player. The app focuses on simplicity and a good user experience for offline music listening.

## Features

*   **Local Music Playback:** Plays audio files (MP3, WAV, etc.) stored on the user's device.
*   **Playlist Management:**
    *   View all available songs.
    *   Create and manage custom playlists.
    *   Add/remove songs to/from playlists.
    *   Switch between different playlists.
    *   Rename and delete playlists.
*   **Persistent Music Controls:**
    *   Notification controls (Play, Pause, Next, Previous).
    *   Lock screen controls (via notification).
*   **Playback Functionality:**
    *   Play, Pause, Resume.
    *   Skip to Next/Previous song.
    *   Seek through songs using a progress slider.
    *   Repeat and Shuffle modes.
*   **User Interface:**
    *   Clean and intuitive Material Design.
    *   Displays song title, artist, and album art (with placeholders if not available).
    *   Sort songs (e.g., by title, artist - based on your `sortButton`).
*   **Background Playback:** Continues playing music even when the app is in the background or the screen is off.
*   **Dynamic Album Art:** Uses placeholder album art if original art is missing.

To explore all files and directories, visit the [repository contents](https://github.com/jksalcedo/music-app/tree/main).

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

*   Android Studio (Latest stable version recommended)
*   Android SDK
*   A physical Android device or Emulator (SDK24+)

#### Installation

1. Clone this repository:
   ```bash
   
   git clone https://github.com/jksalcedo/music-app.git
   ```
2. Open the project in Android Studio.
3. Let Gradle sync and download dependencies.
4. Build and run the app on your device or emulator.

## Usage

### Basic Music Playback
1.  Grant necessary permissions (e.g., Read External Storage) when prompted.
2.  The app will scan and display music files from your device.
3.  Tap on a song to start playing.
4.  Use the player controls at the bottom (or in the notification) to manage playback.
5.  Explore sorting options for your playlist.

### Playlist Management
1.  **Creating Playlists**: Tap the floating action button (➕) to create a new playlist.
2.  **Adding Songs to Playlists**: Long-press any song to add it to an existing playlist.
3.  **Switching Playlists**: Tap the playlist icon (🎵) next to the sort button to switch between playlists.
4.  **Managing Playlists**: 
    - Use the "Manage Playlists" option in the playlist selection dialog
    - Rename playlists by selecting "Rename" in the playlist options
    - Delete playlists by selecting "Delete" in the playlist options
5.  **Default Playlist**: "All Songs" shows all music on your device and cannot be modified.

## Known Limitations

*   Currently only supports local music playback; no streaming features.
*   Basic playlist management is now available with creation, editing, and song management features.
*   The `playNext()` and `playPrevious()` in the `MusicService` are currently reliant on `MainActivity` for the core logic of selecting the actual next/previous song.

## Tech Stack

*   **Language:** Kotlin
*   **Architecture:** Standard Android Components
*   **UI:** Android XML Layouts, Material Components
*   **Core Components:**
    *   `Service` for background playback (`MusicService`)
    *   `MediaPlayer` for audio playback
    *   `NotificationCompat` for media notifications
    *   `RecyclerView` for displaying song lists
    *   `ContentResolver` for querying media files

## Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request
