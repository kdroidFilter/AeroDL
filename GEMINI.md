# gemini.md

This file provides guidance to Gemini when working with code in this repository.

## Project Overview

This is a **Kotlin Multiplatform** desktop application (JVM target) that provides a GUI for yt-dlp, a powerful video/audio downloading tool. The project is split into two modules:

- **composeApp**: The main desktop application built with Compose Multiplatform
- **ytdlp**: A standalone Kotlin wrapper library for yt-dlp with automatic binary management

## Build Commands

```bash
# Run the desktop application
./gradlew :composeApp:run

# Build the project
./gradlew build

# Run tests
./gradlew jvmTest        # Run tests for JVM target
./gradlew allTests       # Run all tests and create aggregated report
./gradlew check          # Run all checks

# Package the application
./gradlew :composeApp:createDistributable
./gradlew :composeApp:packageDistributionForCurrentOS
./gradlew :composeApp:packageDmg    # macOS
./gradlew :composeApp:packageMsi    # Windows
./gradlew :composeApp:packageDeb    # Linux
```

## Architecture

### Module Structure

**ytdlp module**: Independent library that wraps yt-dlp
- `YtDlpWrapper.kt`: Main API entry point
- `core/`: Core download logic and event handling
- `model/`: Data models (VideoInfo, PlaylistInfo, etc.)
- `util/`: Platform utilities, network helpers, archive handling
- Automatically downloads and manages yt-dlp and FFmpeg binaries
- Provides asynchronous, event-driven API using Kotlin Coroutines
- See `ytdlp/README.md` for detailed API documentation

**composeApp module**: Desktop GUI application
- `src/jvmMain/kotlin/io/github/kdroidfilter/ytdlpgui/`
  - `main.kt` & `App.kt`: Application entry point and root composable
  - `core/`: Cross-cutting concerns organized by layer (Clean Architecture)
    - `domain/manager/`: Business logic managers (DownloadManager, ClipboardMonitorManager)
    - `navigation/`: Navigation system (Navigator, Destination, NavigationAction, animations)
    - `design/`: Design system - reusable UI primitives
      - `components/`: Generic UI components (Switcher)
      - `icons/`: Custom icons (Logo, Browser icons, etc.)
    - `ui/`: Application-specific UI components (Footer, Header)
    - `platform/`: Platform-specific integrations
      - `browser/`: Browser operations (openUrlInBrowser)
      - `filesystem/`: File system utilities (FileExplorerUtils)
      - `notifications/`: Notification helpers (NotificationThumbUtils)
    - `config/`: Application configuration (SettingsKeys)
  - `data/`: Data layer (repositories for download history and supported sites)
  - `di/`: Dependency injection setup (Koin modules)
  - `features/`: Feature-based organization by domain
    - `init/`: Initial yt-dlp/FFmpeg setup (InitScreen, InitViewModel, InitState)
    - `onboarding/`: Multi-step onboarding flow (OnboardingFlow.kt)
    - `home/`: Home screen and dashboard (HomeScreen, HomeViewModel, HomeState, HomeEvents)
    - `download/`: Download management features
      - `manager/`: Main download manager screen (DownloaderScreen, DownloadViewModel)
      - `single/`: Single video/audio download (SingleDownloadScreen, SingleDownloadViewModel)
      - `bulk/`: Bulk/playlist download (BulkDownloadScreen, BulkDownloadViewModel)
    - `system/`: System-level screens
      - `settings/`: Application settings (SettingsScreen, SettingsViewModel)
      - `about/`: About/info screen (AboutScreen, AboutViewModel)
- `src/commonMain/sqldelight/`: SQLDelight schema files
  - `Database.sq`: Tables for download_history and supported_sites

### Key Dependencies

- **Compose Multiplatform**: Desktop UI framework
- **Koin**: Dependency injection
- **SQLDelight**: Type-safe database queries
- **Navigation Compose**: Screen navigation
- **Compose Fluent**: UI components following Fluent design
- **Ktor**: HTTP client (used in ytdlp module)
- **kotlinx.serialization**: JSON parsing
- **multiplatform-settings**: Cross-platform settings storage
- **auto-launch**: Auto-start on system boot
- **filekit**: File picker dialogs
- **knotify**: Desktop notifications

### Navigation Architecture

The app uses a custom Navigator abstraction (`DefaultNavigator`) with type-safe destinations defined in `core.ui.navigation.Destination`. Navigation flows include:
- InitScreen → Onboarding steps → MainNav (Home/Downloader)
- MainNav can navigate to SecondaryNav (Settings/About) or Download screens

### State Management

- ViewModels use `kotlinx.coroutines.flow.StateFlow` for reactive state
- Koin provides ViewModels via `viewModel` and `single` scopes
- Business logic is centralized in managers (DownloadManager, ClipboardMonitorManager)

### Data Persistence

- SQLDelight generates type-safe database queries from `.sq` files
- DownloadHistoryRepository manages download history
- SupportedSitesRepository caches yt-dlp extractor information
- Settings stored via `multiplatform-settings` library

## Browser Cookies Support

The ytdlp wrapper supports using browser cookies for authenticated/age-restricted content via `--cookies-from-browser`:
- Default browser is set to `firefox` in InitViewModel during initialization
- Can be configured globally via `YtDlpWrapper.cookiesFromBrowser`
- Can be overridden per download via `Options.cookiesFromBrowser`

## Development Notes

- JVM toolchain version: 21
- Package name: `io.github.kdroidfilter.ytdlpgui`
- Main class: `io.github.kdroidfilter.ytdlpgui.MainKt`
- The ytdlp module is a standalone library that can be reused outside this GUI
- All source code is in `jvmMain` (no iOS/Android targets currently)
- SQLDelight dialect: SQLite 3.24
