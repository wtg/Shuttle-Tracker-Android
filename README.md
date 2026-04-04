# Shuttle Tracker Android

A native Android application for tracking RPI shuttle bus locations, routes, and schedules in real-time.

### Features
*   **Real-time Map:** View live locations of all shuttles on an interactive map.
*   **Route Information:** See the route each shuttle is running and its color-coded path.
*   **Schedules:** Browse shuttle schedules per stop and per route.
*   **ETAs:** Get estimated arrival times for shuttles at each stop.
*   **Announcements:** Read the latest shuttle service announcements.
*   **Analytics:** View aggregated ridership and service data.

### Tech Stack
*   **Language:** Kotlin
*   **UI:** Jetpack Compose + Material 3
*   **Architecture:** MVVM with ViewModels and Kotlin Flows
*   **Dependency Injection:** Hilt
*   **Networking:** Retrofit + OkHttp
*   **Maps:** Google Maps SDK
*   **Build:** Gradle (Kotlin DSL) targeting Android SDK 36

### Project Structure
```
app/src/main/java/edu/rpi/shuttletracker/
├── data/
│   ├── models/         # Data classes for API responses (Route, Stop, Schedule, etc.)
│   ├── network/        # Retrofit API service and helpers
│   └── repositories/   # ApiRepository, UserPreferencesRepository
├── ui/
│   ├── MainActivity.kt
│   ├── announcements/  # Service announcements screen
│   ├── maps/           # Live map view
│   ├── schedule/       # Schedule browser
│   ├── settings/       # User preferences, analytics, about, dev menu
│   └── setup/          # First-run permission setup
└── util/               # Notifications, Firebase, workers
```

### Getting Started
See the [Installation guide](https://github.com/wtg/Shuttle-Tracker-Android/wiki/Installation) on the wiki for setup instructions.

### Related Projects
*   [Shubble](https://github.com/wtg/shubble) – Backend API, web frontend, and ML pipelines
*   [Shuttle Tracker SwiftUI](https://github.com/wtg/Shuttle-Tracker-SwiftUI) – iOS client
