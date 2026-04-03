# 🕌 ScreenRest — Your Phone's Spiritual Break Companion

> **Take mindful breaks. Protect your eyes. Nurture your soul.**

Tired of endless scrolling? ScreenRest transforms your phone habits into moments of spiritual reflection. When you've been using your phone too long, it gently interrupts with beautiful Quranic verses and Islamic reminders - turning each break into a moment of peace and connection with Allah.

---

## ✨ Why You'll Love ScreenRest

### 📱 **Smart Break Reminders**
ScreenRest silently watches your phone usage and reminds you to take breaks when you need them most. No more losing track of time!

### 🕌 **Spiritual Content**
Instead of boring break screens, you'll see:
- **Beautiful Quranic verses** in Arabic with English translations
- **Inspirational Islamic reminders** to refresh your faith
- **Personalized messages** that you can add yourself

### 🎨 **Gorgeous Visuals**
Every break screen appears with a different beautiful color, making each interruption a pleasant visual experience.

### ⚙️ **Works Your Way**
- **Timer Exceptions**: Add important apps (like prayer apps or work tools) to a whitelist so breaks don't interrupt you
- **Flexible Timing**: Set breaks from 10 seconds to 2 hours of usage
- **Break Duration**: Choose how long each break lasts (30 seconds to 30 minutes)

### � **Simple & Automatic**
- Runs quietly in the background
- Starts automatically when you turn on your phone
- No complicated setup - just install and go!

---

## 📲 How to Get ScreenRest on Your Phone

### **⚠️ Important: Installation Warning**
Since ScreenRest isn't on the Google Play Store, Android might show a security warning. This is normal! The app is completely safe - it just means Google hasn't reviewed it.

### **Step 1: Enable Installation from Unknown Sources**
1. Go to your phone's **Settings**
2. Search for "Install unknown apps" or "Unknown sources"
3. Find your web browser (Chrome, Firefox, etc.)
4. **Allow** installation from that browser

### **Step 2: Download & Install**
1. Download the ScreenRest APK file from the [outputs directory](app/build/outputs/apk/) (or build from source as shown below)
   - **Note**: I don't use GitHub releases. APK files are available directly in the repository's outputs folder after building.
2. Open the downloaded file
3. If you see a security warning, tap **"Install anyway"** or **"Proceed"**
4. Follow the installation prompts

### **Step 3: Grant Permissions**
ScreenRest needs a few permissions to work properly:
- **Usage Access**: To see how long you've been using your phone
- **Display Over Other Apps**: To show break screens
- **Notifications**: To tell you when breaks are coming
- **Accessibility Service** (optional): For stronger break enforcement

Don't worry - the app will guide you through each permission step by step!

### **Step 4: Start Your Journey**
1. Open ScreenRest
2. Follow the simple setup guide
3. Set your preferred break timing
4. Add your favorite Islamic verses and reminders
5. Toggle the service ON and you're ready!

---

## 🎯 Perfect For Everyone

### **Students & Professionals**
Set 20-minute usage reminders with short 30-second spiritual breaks during study sessions or work.

### **Digital Wellness**
Use longer breaks (2-5 minutes) to rest your eyes and reconnect with your faith throughout the day.

### **Spiritual Growth**
Curate your favorite Quranic verses and reminders that speak to your heart and strengthen your iman.

### **Family & Work**
Add important apps to your whitelist (like family messaging or work tools) so breaks don't interrupt important moments.

---

## 🛠️ For Developers & Tech Enthusiasts

### **Technical Architecture**

ScreenRest follows Clean Architecture principles with modern Android development practices:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │   Main UI   │ │   Settings  │ │   Block Screen  │   │
│  │  (Compose)  │ │  (Compose)  │ │   (Compose)     │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │   Models    │ │  Use Cases  │ │  Repository     │   │
│  │             │ │             │ │  Interfaces     │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │ Room DB     │ │ DataStore   │ │  Retrofit API   │   │
│  │ (Ayat/Msgs) │ │ (Settings)  │ │ (Quran API)     │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### **Technology Stack**
- **Language**: Kotlin 100% (Modern, safe, concise)
- **UI Framework**: Jetpack Compose (Beautiful, performant UI)
- **Architecture**: MVVM + Clean Architecture (Testable, maintainable)
- **Dependency Injection**: Hilt (Official Android DI solution)
- **Database**: Room (Local SQLite storage)
- **Preferences**: DataStore (Modern settings storage)
- **Networking**: Retrofit + OkHttp (API integration)
- **Async**: Coroutines + Flow (Smooth, responsive operations)

### **Core Services**
- **UsageTrackingService**: Background monitoring of screen time
- **BlockOverlayService**: Manages break screen display and themes
- **BlockAccessibilityService**: Enhanced enforcement (optional)
- **BootReceiver**: Auto-start functionality

### **Required Permissions**

| Permission | Why It's Needed | User Impact |
|------------|-----------------|-------------|
| **Usage Stats Access** | Track screen time usage | Core functionality |
| **Display Over Other Apps** | Show fullscreen break overlay | Required for breaks |
| **Accessibility Service** | Block navigation during breaks | Optional - stronger enforcement |
| **Notifications** | Status updates and reminders | Optional - basic enforcement |

---

## 🚀 Building from Source

### **Prerequisites**
- **JDK 17+** (Development environment)
- **Android SDK** (API level 26-34)
- **Physical Device** (Recommended for testing permissions)

### **Build Instructions**
```bash
# Clone the repository
git clone https://github.com/hozaifa1/screen-rest-app.git
cd screen-rest-app/android

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or install directly
./gradlew installDebug
```

---

## 🔧 Advanced Configuration

### **Message Management**
```kotlin
// Sequential display logic
Ayah #1 → Reminder #1 → Ayah #2 → Reminder #2 →循环
```

### **Theme Rotation**
```kotlin
// Automatic color cycling
TEAL → BLUE → INDIGO → PURPLE → PINK → RED → 
ORANGE → AMBER → GREEN → CYAN → TEAL
```

### **Whitelist Functionality**
```kotlin
// Timer automatically pauses for whitelisted apps
if (whitelistApps.contains(currentApp)) {
    pauseTimer()
    showStatus("Timer paused (whitelist app)")
} else {
    resumeTimer()
}
```

---

## 📊 Project Statistics

- **Total Files**: 70+ Kotlin files
- **Lines of Code**: 5,000+ lines
- **Domain Models**: 12 models
- **Use Cases**: 8 use cases
- **Repositories**: 5 repositories
- **Services**: 3 background services
- **UI Screens**: 10+ Compose screens
- **Database Entities**: 3 entities (Ayah, Reminder, Custom Message)
- **API Integration**: Quran Cloud API with local fallback

---

## 🧪 Testing

### **Manual Testing Checklist**
- [ ] Onboarding flow with all permissions
- [ ] Service start/stop functionality
- [ ] Break screen trigger and display
- [ ] Message sequencing (Ayah → Reminder)
- [ ] Theme color rotation
- [ ] Settings persistence
- [ ] Database CRUD operations
- [ ] Permission status updates
- [ ] Whitelist functionality (add/remove apps)
- [ ] Timer pause/resume when entering/leaving whitelist apps
- [ ] Status display when timer is paused

### **Automated Verification**
```bash
# Run verification scripts
python verify_ui_implementation.py
python verify_part2.py
```

---

## 🔒 Privacy & Security

- **Local Storage**: All data stored locally on device
- **No Analytics**: No usage tracking or data collection
- **Minimal Permissions**: Only requests necessary permissions
- **Open Source**: Full code transparency
- **No Network Required**: Core functionality works offline

---

## 📱 Device Compatibility

- **Minimum Android Version**: 8.0 (API 26)
- **Target Android Version**: 14 (API 34)
- **Recommended**: Android 10+ for best experience
- **Architecture**: ARM64, ARM32, x86, x86_64

---

## 🤝 Contributing

We welcome contributions! Please see our contributing guidelines:

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Submit a pull request

### **Areas for Contribution**
- Additional language translations
- New reminder categories
- UI/UX improvements
- Performance optimizations
- Accessibility enhancements

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) for details.

```
Copyright (c) 2026 ScreenRest

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 📞 Support & Contact

- **Issues**: [GitHub Issues](https://github.com/hozaifa1/screen-rest-app/issues)
- **Email**: [20hozaifa02@gmail.com](mailto:20hozaifa02@gmail.com)
- **Discussions**: [GitHub Discussions](https://github.com/hozaifa1/screen-rest-app/discussions)

---

## 🌟 Special Thanks

- **Quran Cloud API** for providing Quranic verses and translations
- **Android Jetpack** team for amazing modern development tools
- **Material Design** team for the beautiful design system
- **Open Source Community** for inspiration and feedback

---

## 📈 Roadmap

### **Planned Features**
- [ ] Wear OS companion app
- [ ] Statistics and usage analytics
- [ ] Family sharing features
- [ ] More language support
- [ ] Integration with calendar apps
- [ ] Smart break suggestions based on usage patterns

### **Version History**
- **v1.0.0** - Core functionality with Islamic content integration
- **Future versions** - Enhanced features and community requests

---

**🕌 ScreenRest - Transform your screen breaks into moments of spiritual reflection and digital wellness.**

*Built with ❤️ for the Muslim community seeking balance in the digital age.*
