# VoiceApp — Audio-First Social Feed

An Android MVP demonstrating audio recording and a scrollable audio feed, built with Kotlin + Jetpack Compose.

---

## Build & Run

```bash
# Clone and open in Android Studio Hedgehog (2023.1.1) or newer
# Min SDK 24 · Target SDK 36 · Kotlin 2.0.21

./gradlew assembleDebug
# or just press ▶ Run in Android Studio
```

The app requires a device or emulator running **Android 7.0+ (API 24)**. Recording requires a real device (emulator mic support is unreliable).

---

## Architecture

The project follows **Clean Architecture** layered inside **MVVM**, with manual constructor injection (no Hilt — chosen deliberately to keep the dependency graph visible and the setup lean for an MVP).

```
presentation/          ← Compose screens + ViewModels
  feed/                  FeedScreen, FeedViewModel
  record/                RecordScreen, RecordViewModel
  navigation/            Compose NavHost wiring

domain/                ← Pure Kotlin. Zero Android imports.
  model/                 AudioPost
  repository/            AudioRepository (interface)
  usecase/               GetAudioFeedUseCase, SaveRecordingUseCase

data/                  ← Repository impl, local data sources
  local/                 MockAudioDataSource, SampleAudioGenerator
  repository/            AudioRepositoryImpl

player/                ← AudioPlayer, AudioFocusManager, PlaybackState
recorder/              ← AudioRecorder, RecordingState
service/               ← AudioPlaybackService (foreground)
di/                    ← AppContainer (manual DI root)
```

`AppContainer` is lazily instantiated inside `VoiceApp` (the `Application` class) and acts as the single composition root. Every ViewModel pulls its dependencies from there via a custom `ViewModelProvider.Factory`.

---

## How Single-Audio Playback Works

`AudioPlayer` is an **application-scoped singleton** that owns one `MediaPlayer` instance at a time.

When `play(postId, filePath)` is called:

- **Same post, currently playing** → pause it
- **Same post, currently paused** → resume it
- **Different post** → `tearDownCurrentPlayer()` releases the existing `MediaPlayer`, then creates and `prepareAsync()`s a fresh one for the new post

The current playback identity (`activePostId`) is compared on every tap. Because only one `MediaPlayer` can ever be "alive" at a time, there is no race condition between simultaneous taps — the previous player is synchronously torn down before the new one is created. All operations happen on `Dispatchers.Main`.

State is exposed as a `StateFlow<PlaybackState>` which the `FeedViewModel` merges with the post list via `combine(...)`. Each `AudioPostCard` compares its own post ID against `playbackState.activePostId` to decide which icon and waveform progress to render — no shared mutable state in the UI layer.

---

## How Lifecycle Changes Are Handled

| Scenario | Behaviour |
|---|---|
| Scroll the feed | Audio continues — the player is decoupled from scroll position |
| Tap a different post | Current player is released; new one starts |
| Another app requests audio focus | `AudioFocusManager` callback triggers `pause()` |
| App goes to background (audio playing) | `AudioPlaybackService` holds a foreground notification; MediaPlayer keeps running |
| App returns to foreground | Nothing changes — user resumes control via the feed |
| Activity destroyed (config change) | `isChangingConfigurations` guard skips `release()` |
| Activity permanently destroyed | `release()` is called; MediaPlayer and coroutine scope are cleaned up |

`AudioFocusManager` wraps `AudioFocusRequest` (API 26+) with a legacy fallback for API 24–25. Focus is requested before every `start()`/`resume()` and abandoned on pause or completion.

---

## Tradeoffs Made

**No Hilt** — The manual `AppContainer` keeps the dependency graph explicit and avoids annotation processing overhead. For a team or a larger feature set, Hilt would be the obvious next step.

**Application-scoped `AudioPlayer`** — Simpler than a bound service for the scope of an MVP. The downside is that if Android kills the process while audio is paused, the state is lost. With a proper `MediaSessionService` (Jetpack Media3) this would be solved correctly.

**`MediaPlayer` over ExoPlayer** — The spec forbids third-party audio SDKs. `MediaPlayer` covers the requirements. In production, ExoPlayer (which is first-party Jetpack Media3) would offer better buffering, gapless playback, and adaptive streaming.

**WAV sample files generated on first launch** — Avoids bundling binary assets. A real app would stream from a CDN; the generator is purely for demo purposes.

**Waveform from random data** — Parsing real PCM amplitudes from a `MediaPlayer` source requires a separate decode pass. For the feed, seeded-random bars give a visually plausible result. For recordings, we poll `MediaRecorder.getMaxAmplitude()` at 80ms intervals for live amplitude.

**No pagination** — The feed loads all posts at once from an in-memory list. A real app would use Paging 3.

---

## What I'd Improve With More Time

1. **Replace `MediaPlayer` with Jetpack Media3 / ExoPlayer** — handles audio session routing, gapless playback, and network streaming correctly.
2. **Proper `MediaSessionService`** — replaces the thin `AudioPlaybackService` with a fully spec-compliant media session that integrates with lock screen controls, Bluetooth HFP, and Android Auto.
3. **Real waveform extraction** — decode the audio file off-thread and compute RMS amplitude per frame; display an accurate waveform instead of a random one.
4. **Paging 3** for infinite-scroll feed with server pagination.
5. **Room** to persist recordings and feed state across cold starts.
6. **Hilt** for dependency injection once the feature count justifies it.
7. **Navigation argument safety** with type-safe routes (Compose Navigation 2.8 sealed route API).
8. **Accessibility** — content descriptions on waveforms, minimum touch target sizes enforced.

---

## How This Would Scale in a Real App

**Media layer** — Move to a `MediaSessionService` (Jetpack Media3). It handles audio focus, Bluetooth headset events, notification controls, and background playback correctly at scale. The `AudioPlayer` abstraction already provides the right interface; swapping the implementation is straightforward.

**Feed** — Replace `MockAudioDataSource` with a `RemoteAudioDataSource` backed by a Retrofit/OkHttp network layer and a Room cache. The `AudioRepository` interface and the use cases don't change. Introduce Paging 3 for cursor-based server pagination.

**State management** — `StateFlow` + `combine` already handles concurrent updates cleanly. For more complex cross-feature state (e.g., notifications, DMs), a shared event bus (Kotlin `SharedFlow`) sits cleanly at the domain layer without leaking into ViewModels.

**Dependency injection** — Drop `AppContainer` in favour of Hilt modules. The constructor injection style throughout means the migration is mechanical.

**Multi-module** — Split `domain`, `data`, `player`, and `presentation` into separate Gradle modules once the team grows. The current package structure maps 1:1 to that future split.
