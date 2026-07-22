---
title: Architecture
permalink: /architecture/
---

# Architecture

A quick map of the codebase so you know where to look add code. This app is written in Kotlin with [Jetpack Compose](https://developer.android.com/jetpack/compose) for the UI, [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) for dependency injection, and follows an [MVVM](https://developer.android.com/topic/architecture) pattern (Model / View / ViewModel). It's a single-Activity app - everything you see is a Compose screen, not a separate Activity.

Everything lives under the package `edu.rpi.shuttletracker`, at `app/src/main/java/edu/rpi/shuttletracker/`.

## The big picture

```
app/            entry point, DI setup, navigation
core/           small helpers shared by everything (network results, theme, UI bits)
data/           models, the API client, the repository, and saved user settings
feature/        one folder per screen/flow (map, schedule, etas, settings, setup)
background/     notifications and Firebase push handling
widget/         the home screen widget (Jetpack Glance)
```

## How data flows

**Loading data (network → screen):**

```
Backend API
    ↓
data/remote/            ShuttleApi + RetrofitShuttleRemoteDataSource makes the HTTP call
    ↓
data/mapper/             turns the raw DTO into a data/models/ type
    ↓
data/repository/         ShuttleRepository wraps the result in a NetworkResult
    ↓
feature/<name>/<Name>ViewModel.kt    updates its UiState
    ↓
feature/<name>/<Name>Screen.kt       Compose reads UiState and redraws
```

**Handling a user action (screen → network):**

```
feature/<name>/<Name>Screen.kt       user taps something
    ↓
feature/<name>/<Name>ViewModel.kt    the tap calls a function on the ViewModel
    ↓
data/repository/                     ShuttleRepository makes the call
    ↓
data/remote/                         the actual HTTP request
    ↓
Backend API
```

Everything below walks through each of those layers in more detail.

## `app/` - where the app starts

- **`ShuttleTrackerApplication.kt`** - the `Application` class, annotated `@HiltAndroidApp` so Hilt can wire up the rest of the app.
- **`MainActivity.kt`** - the app's one and only `Activity`. It sets the Compose content and decides whether to show setup or the map first.
- **`app/navigation/AppNavigation.kt`** - the nav graph. Each route is a small `@Serializable` object, and `entryProvider { }` maps each route to a feature screen. Screens are given plain callbacks (`onBack`, `onOpenSettings`, ...) instead of a navigation object, which keeps them easy to test and preview on their own.
- **`app/di/`** - Hilt modules (`DataModule`, `NetworkModule`, `PreferencesModule`) that tell Hilt how to build the repository, the Retrofit client, and the preferences store as singletons.

## `data/` - where information comes from

This is the single source of truth every feature reads from. Nothing in `feature/` should talk to the network or DataStore directly - it goes through here.

- **`data/models/`** - plain Kotlin `data classes` shared everywhere: `Vehicle`, `Route`, `Stop`, `Schedule`, `Announcement`. No Android or network types leak in here.
- **`data/remote/`** - `ShuttleApi` (the Retrofit interface describing each HTTP endpoint) and `RetrofitShuttleRemoteDataSource`, which actually makes the calls.
- **`data/mapper/`** - functions that turn raw network DTOs into the `data/models/` types above.
- **`data/repository/`** - `ShuttleRepository` is the interface every feature depends on; `DefaultShuttleRepository` is the real implementation. Every response comes back wrapped in a `NetworkResult` (`Success` or `Failure`) so a dropped connection or a server error never crashes a screen - it just becomes a value the ViewModel can react to.
- **`data/local/preferences/`** - `UserPreferences` (interface) and `DataStoreUserPreferences` (implementation), backed by Jetpack DataStore. This is where things like theme, map type, and developer options live.

Because `ShuttleRepository` and `UserPreferences` are interfaces, tests can swap in a fake instead of hitting the network or disk - see [Testing](#testing) below.

## `feature/` - one folder per screen

Every screen or self-contained flow gets its own folder under `feature/`, e.g. `feature/map/`, `feature/schedule/`, `feature/etas/`, `feature/settings/`, `feature/setup/`. Each one follows the same shape:

```
feature/<name>/
  <Name>Screen.kt       the composable that navigation points to
  <Name>ViewModel.kt     a @HiltViewModel holding that screen's state
  components/            smaller composables used only by this feature
  utils/                 plain Kotlin helper functions (no Compose/Android types)
```

**`<Name>Screen.kt`** is a `@Composable` with a `viewModel: <Name>ViewModel = hiltViewModel()` default parameter. Hilt supplies the real ViewModel at runtime; tests construct the ViewModel by hand and pass it in instead.

**`<Name>ViewModel.kt`** owns a private `MutableStateFlow<XyzUiState>` and exposes it publicly as a read-only `StateFlow`. `XyzUiState` is a small `data class` at the bottom of the same file holding everything that screen needs to render (a list of vehicles, a loading flag, an error, etc). The View never mutates state directly - it calls a function on the ViewModel, and the ViewModel updates the flow, which Compose automatically re-renders from.

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val shuttleRepository: ShuttleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()

    fun doSomething() {
        _uiState.update { it.copy(/* ... */) }
    }
}

data class ExampleUiState(/* ... */)
```

**`components/`** holds smaller `@Composable` functions used only within that feature (cards, rows, sheets). **`utils/`** holds plain Kotlin functions with no Android or Compose dependency - this is deliberate, so that logic (parsing, filtering, math) can be unit tested directly on the JVM without an emulator. See `feature/map/utils/FakeShuttleUtils.kt` or `feature/etas/utils/EtaUtils.kt` for examples.

### Current features

- **`feature/map/`** - the main screen: the Google Map itself, plus the bottom navigation bar that switches between the Map, ETAs, and Schedule tabs.
- **`feature/etas/`** - the ETAs tab: a list of stops with live arrival times, and a details sheet per stop.
- **`feature/schedule/`** - the Schedule tab: the printed weekly schedule.
- **`feature/settings/`** - the settings screen, plus `about/` and `developerMenu/` sub-screens.
- **`feature/setup/`** - the first-run flow (permissions, privacy policy acceptance).

## `background/` - work that isn't a screen

- **`background/service/FirebaseService.kt`** - receives Firebase Cloud Messaging push notifications and figures out where a tap on one should navigate to.
- **`background/notification/Notifications.kt`** - sets up notification channels.

## `widget/` - the home screen widget

Built with [Jetpack Glance](https://developer.android.com/jetpack/androidx/releases/glance), which renders to `RemoteViews` in the launcher's process - no `ViewModel`, no `StateFlow`. Data is fetched separately and the widget just renders whatever's already stored.

- **`EtaWidget.kt`** - the widget's UI. Shows either every stop's soonest arrivals ("all routes"), or one configured stop's full status.
- **`EtaWidgetUpdater.kt`** - fetches live data and saves it into each widget instance's state. Runs on a timer and when the refresh button is tapped.
- **`EtaWidgetRefreshWorker.kt`** - a `WorkManager` job that calls `EtaWidgetUpdater` every 15 minutes.
- **`EtaWidgetActions.kt`** - the widget's tap targets: refresh, route filter, and view toggle.
- **`EtaWidgetConfigureActivity.kt`** - lets a user pick which stop a widget instance shows.
- **`WidgetEntryPoint.kt`** - a Hilt entry point so the worker and actions (which only get a `Context`, not full DI) can reach `ShuttleRepository`.
- **`WidgetSnapshot.kt`** - the data stored in each widget instance's state.
- **`EtaWidgetTheme.kt`** - matches the widget's colors to the app's theme.

Like the map and ETAs tab, the widget shows fake shuttle data in dev mode when there's nothing live to show.

## `core/` - shared building blocks

- **`core/network/`** - `NetworkResult` / `NetworkError`, the wrapper types used everywhere a repository call can fail.
- **`core/ui/`** - the app theme and small shared Compose pieces (error snackbars, etc).
- **`core/util/`** - other small helpers with no natural home elsewhere.

## Testing

- **`app/src/test/`** - plain JVM unit tests (fast, no emulator needed). Fakes for `ShuttleRepository` and `UserPreferences` live in `testing/fakes/`, and sample data builders (`testRoute()`, `testSchedule()`, ...) live in `testing/fixtures/`.
- **`app/src/androidTest/`** - instrumented tests that run on a device/emulator, used for things that need real Compose UI (e.g. tapping through the bottom navigation). This source set has its **own copies** of the same fakes/fixtures, because `test/` and `androidTest/` are separate Gradle source sets that don't share code in this project - if you add a fake to one, you may need to add the matching one to the other.

Run unit tests with `./gradlew testDebugUnitTest`.

## Formatting

[ktlint](https://github.com/pinterest/ktlint) runs as a pre-commit hook and will block a commit that isn't formatted correctly. Run `./gradlew ktlintFormat` to auto-fix most issues before committing.

## Adding a new screen

A quick checklist, based on how the existing features are built:

1. Create `feature/<name>/` with `<Name>Screen.kt` and `<Name>ViewModel.kt`.
2. Give the ViewModel a `<Name>UiState` data class and a `MutableStateFlow`/`StateFlow` pair, like the example above.
3. Inject `ShuttleRepository` and/or `UserPreferences` if the screen needs data.
4. Add a route for it in `app/navigation/AppNavigation.kt` (or, if it's a tab rather than its own destination, wire it into `feature/map/MapsScreen.kt`'s bottom navigation the way ETAs and Schedule are).
5. Add unit tests for the ViewModel under `app/src/test/`, using the existing fakes in `testing/fakes/`.
