# Ghost-Play v2

Ghost-Play is a modern, lightweight, and powerful Android application built with **Kotlin** and **Jetpack Compose**. It allows users to manage and play M3U playlists seamlessly with advanced support for custom User-Agents, Cookies, and DRM-protected streams.

## 🚀 Features

- **Modern UI:** Built entirely with Jetpack Compose and Material Design 3.
- **M3U Playlist Parser:** Robust parser that handles custom headers, redirect URLs, and metadata.
- **DRM Support:** Play `ClearKey` and `Widevine` protected streams directly from playlists using ExoPlayer.
- **Advanced Headers:** Support for custom `User-Agent` and `Cookie` tags per channel or playlist.
- **Favorites System:** Long-press any channel to add it to your local favorites. Favorites are stored persistently.
- **Immersive Player:** Full-screen edge-to-edge video playback with gesture controls for brightness, volume, and zoom.
- **Picture-in-Picture (PiP):** Continue watching your stream while using other apps.
- **Background Playback:** Audio continues playing in the background via a foreground service.

## 🛠️ Technologies Used

- **Language:** Kotlin 2.2
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Media Player:** AndroidX Media3 (ExoPlayer)
- **Image Loading:** Coil (coil-compose)
- **Storage:** SharedPreferences (via Kotlinx Serialization)
- **Navigation:** Navigation Compose

## 📦 How to Build

### Using GitHub Actions (Automated)
This repository is configured with GitHub Actions. 
1. Go to the **Actions** tab in this repository.
2. Select **Build Android APK** from the left sidebar.
3. Click on **Run workflow**.
4. Once the build is complete, you can download the generated APK from the artifacts section.

### Using Android Studio (Manual)
1. Clone this repository:
   ```bash
   git clone https://github.com/divudon21/Ghost-Play-v2.git
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync and download the required dependencies.
4. Click on the **Run** button (Shift + F10) to build and install the app on your connected device or emulator.

## 📝 M3U Playlist Format Support

The app supports standard M3U formats along with Kodi/VLC specific tags for advanced streaming:

```m3u
#EXTM3U
#KODIPROP:inputstream.adaptive.license_type=clearkey
#KODIPROP:inputstream.adaptive.license_key=https://example.com/key.php
#EXTINF:-1 tvg-id="1" tvg-logo="https://example.com/logo.png" group-title="Movies",Movie Channel
#EXTVLCOPT:http-user-agent=CustomUserAgent
https://example.com/stream.mpd|cookie=auth_token=12345
```

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/divudon21/Ghost-Play-v2/issues).

## 📄 License
This project is open-source and available to use freely.
