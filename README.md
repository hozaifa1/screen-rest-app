# ScreenRest — Mobile (Android)

> **Take breaks. Protect your eyes. Stay healthy.**

ScreenRest is an Android app that helps you build healthier screen habits. It runs silently in the background, tracks how long you've been using your phone, and gently reminds you to take breaks by displaying a fullscreen overlay with a Quranic verse or a custom message.

## What It Does

- **Tracks your screen time** — Monitors how long you've been actively using your phone, either per-session (continuous) or across all sessions in a day (cumulative).
- **Reminds you to take breaks** — When you hit your configured usage threshold (e.g. 20 minutes), a fullscreen break screen appears for a set duration (e.g. 30 seconds).
- **Displays Quranic verses** — During breaks, a random ayah is fetched from the [Al Quran Cloud API](https://alquran.cloud/api) and displayed in both Arabic and English (Mohsin Khan/Hilali translation). If offline, a curated local collection is used as fallback.
- **Supports custom messages** — You can add your own break messages. When both are enabled, the app randomly picks between a Quran ayah and a custom message.
- **Location-aware** — Optionally set a geofence so breaks only activate when you're at a specific location (e.g. your office or home).
- **Multiple enforcement levels** — Depending on which permissions you grant, the app adapts its enforcement: FULL (overlay + accessibility blocking), STANDARD (overlay only), BASIC (notification only), or NONE.
- **Guided onboarding** — A step-by-step setup walks you through granting the necessary permissions.
- **Theme support** — Light, dark, or system-default theme.
- **Material Design 3** — Clean, modern UI built with Jetpack Compose.

## Required Permissions

| Permission | Why It's Needed |
|---|---|
| **Usage Stats Access** | Track how long you've been using your phone |
| **Display Over Other Apps** | Show the fullscreen break overlay |
| **Accessibility Service** (optional) | Enhanced app blocking during breaks |
| **Notifications** (optional) | Break reminders and status updates |
| **Location** (optional) | Geofenced break triggers |

---

## Technical Details

### Tech Stack

- **Kotlin** — 100% Kotlin codebase
- **Jetpack Compose** — Declarative UI
- **Hilt** — Dependency injection
- **Room** — Local SQLite database for custom messages
- **DataStore** — Type-safe preferences
- **Retrofit + OkHttp** — Network layer for Quran API
- **Kotlinx Serialization** — JSON parsing
- **Coroutines + Flow** — Async programming and reactive state

### Architecture

The app follows **Clean Architecture** with three layers:

- **Presentation** — Jetpack Compose screens + ViewModels (MVVM)
- **Domain** — Use cases and domain models (no framework dependencies)
- **Data** — Repository implementations, Room DB, DataStore, Retrofit API

### Project Structure

```
app/src/main/java/com/screenrest/app/
├── data/
│   ├── local/
│   │   ├── database/         # Room database, DAOs, entities
│   │   └── datastore/        # DataStore preferences
│   ├── remote/               # Retrofit API services and DTOs
│   └── repository/           # Repository implementations
├── domain/
│   ├── model/                # BreakConfig, Ayah, EnforcementLevel, etc.
│   └── usecase/              # GetRandomDisplayMessageUseCase, etc.
├── presentation/
│   ├── main/                 # Home screen (start/pause tracking)
│   ├── settings/             # Settings screen
│   ├── block/                # Fullscreen break overlay
│   ├── onboarding/           # Permission grant flow
│   ├── components/           # Reusable UI components
│   ├── navigation/           # Navigation graph
│   └── theme/                # Material 3 theme definitions
├── service/                  # UsageTrackingService, ServiceController
├── receiver/                 # Broadcast receivers
└── di/                       # Hilt modules
```

### Key Domain Models

- **BreakConfig** — User configuration: threshold, duration, tracking mode, location
- **TrackingMode** — `CONTINUOUS` (per-session) or `CUMULATIVE` (total daily)
- **EnforcementLevel** — `FULL`, `STANDARD`, `BASIC`, or `NONE`
- **Ayah** — Arabic text, English translation, surah info
- **PermissionStatus** — Current state of all required permissions

### Building

**Prerequisites:** JDK 17+, Android SDK (compileSdk 34, minSdk 26)

```bash
git clone https://github.com/hozaifa1/screen-rest-app.git
cd screen-rest-app/android

# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Build Variants

- **Debug** — Development build with logging
- **Release** — Production build with ProGuard/R8 optimization

## License

MIT License. See [LICENSE](LICENSE) for details.

## Contact

For issues or questions, open an issue or reach out at [20hozaifa02@gmail.com](mailto:20hozaifa02@gmail.com).
