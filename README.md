# ScreenRest

**A mindful screen break reminder app for Android**

ScreenRest helps you maintain healthy screen usage habits by tracking your device usage and providing timely break reminders with inspirational Quranic verses or custom messages.

## 📱 Features

- **Intelligent Usage Tracking**: Monitor screen time with continuous or cumulative tracking modes
- **Customizable Break Intervals**: Set usage thresholds (1-240 minutes) and break durations (10-300 seconds)
- **Inspirational Messages**: Display random Quranic verses (Arabic + English) or custom messages during breaks
- **Location-Based Reminders**: Optional geofencing to activate breaks only at specific locations
- **Flexible Enforcement Levels**: Multiple permission-based enforcement modes (FULL, STANDARD, BASIC, NONE)
- **Modern Material Design 3 UI**: Clean, intuitive interface with theme support (System/Light/Dark)

## 🏗️ Architecture

ScreenRest follows **Clean Architecture** principles with clear separation of concerns:

### Layers
- **Presentation Layer**: Jetpack Compose UI with MVVM pattern
- **Domain Layer**: Business logic, use cases, and domain models
- **Data Layer**: Repositories, local database (Room), remote API (Retrofit), and preferences (DataStore)

### Key Technologies
- **Kotlin**: 100% Kotlin codebase
- **Jetpack Compose**: Modern declarative UI toolkit
- **Hilt**: Dependency injection
- **Room**: Local database for custom messages
- **DataStore**: Type-safe preferences storage
- **Retrofit + OkHttp**: Network layer for Quran API
- **Kotlinx Serialization**: JSON parsing
- **Coroutines + Flow**: Asynchronous programming

## 📦 Project Structure

```
app/src/main/java/com/screenrest/app/
├── data/
│   ├── local/
│   │   ├── database/         # Room database, DAOs, entities
│   │   └── datastore/        # DataStore preferences
│   ├── remote/               # Retrofit API services and DTOs
│   └── repository/           # Repository implementations
├── domain/
│   ├── model/                # Domain models (BreakConfig, Ayah, etc.)
│   └── usecase/              # Business logic use cases
├── presentation/
│   ├── navigation/           # Navigation graph
│   ├── main/                 # Main screen
│   ├── settings/             # Settings screen
│   ├── block/                # Break/block screen
│   ├── onboarding/           # Onboarding flow
│   ├── components/           # Reusable UI components
│   └── theme/                # Material 3 theme
├── service/                  # Background services, permission helpers
├── receiver/                 # Broadcast receivers
└── di/                       # Hilt dependency injection modules
```

## 🚀 Getting Started

### Prerequisites
- **JDK 17** or higher
- **Android SDK** (compileSdk 34, minSdk 26)
- **Android Studio** Hedgehog or later (recommended)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd screenrest/android
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   ```

## 🔑 Required Permissions

ScreenRest requires the following permissions for full functionality:

### Critical Permissions
- **Usage Stats Access**: Track app usage time
- **Display Over Other Apps**: Show break screen overlay

### Optional Permissions
- **Accessibility Service**: Enhanced app blocking
- **Notifications**: Break reminders
- **Location**: Geofenced break triggers
- **Background Location**: Location tracking while app is in background

## 📚 Domain Models

### Core Models
- **BreakConfig**: User configuration (threshold, duration, tracking mode, location)
- **TrackingMode**: CONTINUOUS (per-session) or CUMULATIVE (total daily)
- **ThemeMode**: SYSTEM, LIGHT, or DARK
- **DisplayMessage**: Custom text or Quranic verse
- **Ayah**: Arabic text, English translation, surah info
- **PermissionStatus**: Current state of all permissions
- **EnforcementLevel**: FULL, STANDARD, BASIC, or NONE

## 🗄️ Data Sources

### Local
- **Room Database**: Stores custom break messages
- **DataStore Preferences**: App settings and configuration
- **Local Ayah JSON**: Fallback Quranic verses (~20 curated ayahs)

### Remote
- **Quran API**: [alquran.cloud](https://alquran.cloud/api) for dynamic verse fetching

## 🧪 Build Variants

- **Debug**: Development build with logging enabled
- **Release**: Production build with ProGuard/R8 optimization

## 📄 License

```
MIT License

Copyright (c) 2026 ScreenRest

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

## 🤝 Contributing

This is a personal project, but suggestions and feedback are welcome!

## 📞 Support

For issues or questions, please open an issue in the repository.

---

**Built with ❤️ using Kotlin & Jetpack Compose**
