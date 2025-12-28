# Quran Media Player

A focused Android media player for Quran recitation with exceptional, precise seeking and Android Auto support.

## Features

- **Precise Ayah-Level Seeking**: Jump to any ayah with sub-second accuracy
- **Android Auto Integration**: Safe browsing and playback while driving
- **Offline Playback**: Download surahs for offline listening
- **Multiple Reciters**: Support for various Quranic reciters
- **Loop Functionality**: A-B loop for memorization
- **RTL-First Design**: Built with Arabic support from the ground up
- **Accessibility**: Full TalkBack and large text support

## Technical Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Media**: ExoPlayer (Media3)
- **Database**: Room + Proto DataStore
- **DI**: Hilt
- **Architecture**: MVVM + Clean Architecture

## Build Instructions

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or later
- JDK 17
- Android SDK with minSdk 27 (Android 8.1)

### Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/
├── data/          # Data layer (Room, repositories)
├── domain/        # Business logic and models
├── media/         # ExoPlayer and MediaSession
└── presentation/  # UI (Jetpack Compose)
```

## Development Status

Currently in active development following a 6-milestone release plan:

- [x] M1: Foundation (Project setup, basic architecture)
- [ ] M2: Quran Model & Browsing
- [ ] M3: Precision Seeking
- [ ] M4: Android Auto Integration
- [ ] M5: Offline & Downloads
- [ ] M6: Polish & Accessibility

## License

Copyright © 2025. All rights reserved.

## Acknowledgments

Built to serve the Muslim community in their Quran recitation and memorization journey.
