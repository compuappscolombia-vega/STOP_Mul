# STOP! Multiplayer Category Rush (Android)

Native Android rewrite of **STOP! Multiplayer Category Rush**, built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**.

## Features
- **Title Screen & Player Hub**: Animated neon letter badge, quick profile & auth pills, daily challenge access, rankings, mute toggle, and adjustable AI bot pace (`1s–6s` per category) & difficulty (`Easy`, `Medium`, `Hard`).
- **Practice Sprint & Daily Challenge**:
  - Spinning neon **Letter Roulette** wheel that locks onto a valid letter followed by a `3-2-1-STOP!` countdown with synthesized sound effects and Text-to-Speech announcements.
  - Real-time category board with starting-letter validation, keyboard `Next` field chaining, live bot progress indicators, inline bot speed adjustment, and the `STOP!` slap button.
  - Round breakdown modal with confetti burst, `Unique (+10)`, `Shared (+5)`, `+5` first-STOP bonus, and match standings.
  - Deterministic daily challenges keyed by `YYYY-MM-DD`.
- **Real-Time Online Multiplayer Lobby & Custom Rooms (Firebase Firestore)**:
  - Create custom rooms (name, rounds, timer, 3–8 of 12 categories, public/private visibility, optional AI challenger bot) or join public rooms / private rooms by code (`STOP-XXXX`).
  - Live cross-device pre-round lobby, synchronized active rounds, live player answers, first-STOP lock, round summaries, and real-time Match Chat with quick emoji reactions.
- **Google Sign-In, Cloud Profiles & Cross-Device Leaderboards**:
  - Authenticated access with Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption`).
  - Cloud-synced player profiles, match stats, and public Global All-Time and Weekly champion leaderboards powered by Firebase Firestore with zero-trust security rules.

## Architecture
- **UI Layer**: Jetpack Compose screens (`AuthScreen`, `TitleScreen`, `GamePlayScreen`, `MultiplayerLobbyScreen`, `RoomViewScreen`) and modular dialogs/components under `com.example.ui`.
- **State Management**: `StopViewModel` exposing `StateFlow<StopUiState>` with lifecycle-aware collection, real-time Firestore `snapshots()` observers, and coroutine timers.
- **Domain & Rules**: `GameRules` and `GameModels` implementing scoring, word normalization, bot word generation, and daily seeding.
- **Data, Auth & Audio**: `FirebaseRepository`, `GoogleAuthManager`, `FirestoreMappers`, `FirestoreErrorHandler`, and `SoundEngine` (`AudioTrack` PCM synthesis + Android `TextToSpeech`).

## Building
Build the debug APK using Gradle (Kotlin DSL):
```bash
gradle :app:assembleDebug
```
