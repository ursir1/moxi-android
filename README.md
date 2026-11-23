# Moxi Android - Modern Native Media Streaming Client

Native Android client for Moxi built with Kotlin, Jetpack Compose, Kotlin Multiplatform, ExoPlayer, MPV, and a high-performance local streaming server.

[![Android CI](https://github.com/ursir1/moxi-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/ursir1/moxi-android/actions/workflows/android-ci.yml)
[![Release APK](https://github.com/ursir1/moxi-android/actions/workflows/release-apk.yml/badge.svg)](https://github.com/ursir1/moxi-android/actions/workflows/release-apk.yml)

## Overview

**Moxi Android** is a modern, high-performance Android streaming client designed for phones and tablets. It combines full ecosystem support with a sleek Jetpack Compose UI, local torrent/HTTP streaming, dual internal player engines (ExoPlayer & MPV), advanced subtitle/audio controls, and a customizable Liquid Glass interface.

### Highlights

- **⚡ Blazing Fast UI**: Built from the ground up with Kotlin and Jetpack Compose for smooth, responsive 120Hz interactions.
- **🎬 Dual Player Engines**: Choose between **ExoPlayer** (native hardware acceleration) and **MPV** (advanced subtitle rendering, ASS styling, and precision audio sync).
- **🚀 Built-in Streaming Server**: High-performance local engine for instant seeking, HLS transcoding, and torrent streaming with minimal memory overhead (~50MB RAM).
- **✨ Liquid Glass Interface**: Customizable modern UI with translucent glass surfaces, dynamic theme adaptation, and haptic feedback.
- **🔄 Cross-Platform Continuity**: Seamless library synchronization, addon management, and watched progress tracking across your devices.

---

## Features

- **Jetpack Compose Native UI**: Fluid navigation, hero cards, bottom bars, and clean settings panels.
- **In-App Playback**: Embedded playback with gesture controls for volume, brightness, and seeking.
- **Rich Audio & Subtitle Controls**: Embedded track selector, automatic language preferences, external subtitle import, delay adjustments, and custom typography.
- **Addon Ecosystem**: Browse, install, and manage addons directly from your phone.
- **Multi-Architecture Support**: Native optimizations for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
- **Automated CI/CD**: Automatic GitHub Actions release packaging for signed APKs with SHA256 checksums.

---

## Architecture

```text
MoxiAndroid
|-- app/                              Main Android application module
|   |-- player/                       Playback abstractions, ExoPlayer & MPV backends
|   |-- presentation/                 Jetpack Compose screens & Liquid Glass UI
|   |-- data/                         Addon & catalog repositories, local preferences
|   |-- server/                       Streaming server controller & background service
|   `-- src/main/assets/              Language mappings & assets
|-- third_party/mpv-android-lib/       Embedded MPV Android library
|-- streamio-core-kotlin/              Core protocol bridge
|-- stream-server/                    High-performance streaming engine
`-- .github/workflows/                CI builds & automated APK releases
```

---

## Download & Releases

Download the latest ready-to-install APK directly from the **[Releases](https://github.com/ursir1/moxi-android/releases)** tab:

| APK File | Target Device |
|---|---|
| `Moxi-vX.Y.Z-arm64-v8a-release.apk` | Most modern Android phones & tablets (Recommended) |
| `Moxi-vX.Y.Z-universal-release.apk` | Works on all Android devices |
| `Moxi-vX.Y.Z-armeabi-v7a-release.apk` | Older 32-bit ARM devices |
| `Moxi-vX.Y.Z-x86_64-release.apk` | 64-bit Android emulators |

---

## Building from Source

### Prerequisites
- JDK 21
- Android Studio or Android SDK Command-line Tools (API 37)
- Android NDK `29.0.13846066`

### Clone
```bash
git clone --recurse-submodules https://github.com/ursir1/moxi-android.git
cd moxi-android
```

### Build APK
- **Debug build:**
  ```powershell
  .\gradlew :app:assembleDebug
  ```
- **Release build:**
  ```powershell
  .\gradlew :mpv-android-lib:assembleRelease :app:assembleRelease
  ```

---

## Ecosystem

- **Moxi Desktop (Windows/macOS/Linux)**: [ursir1/moxi](https://github.com/ursir1/moxi)
- **Moxi Android**: [ursir1/moxi-android](https://github.com/ursir1/moxi-android)

---

## License & Credits

Built with open-source technologies including Kotlin, Jetpack Compose, Media3, MPV, and community streaming libraries.
