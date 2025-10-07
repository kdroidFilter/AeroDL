# Repository Guidelines

## Project Structure & Module Organization
This repository hosts a Kotlin Multiplatform desktop client built with Compose Multiplatform. Shared UI and domain code live in `composeApp/src/commonMain`, while desktop-specific views reside in `composeApp/src/jvmMain/kotlin` alongside `composeResources` for platform assets. SQLDelight schema files are under `composeApp/src/commonMain/sqldelight`, and the `ytdlp` module wraps yt-dlp integration and release tooling. Shared assets and certificates sit in `res/`, and packaging details are governed by `conveyor.conf`.

## Build, Test, and Development Commands
Use Gradle wrappers from the project root:
```bash
./gradlew :composeApp:run                      # launch the desktop app with hot reload
./gradlew :composeApp:check                    # compile, run unit tests, validate SQLDelight schema
./gradlew :ytdlp:build                         # build the yt-dlp helper module
./gradlew :composeApp:packageReleaseDistributionForCurrentOS  # assemble distributables for your OS
```
Prefer IDE run configs for UI iteration but keep commands reproducible in PR discussions.

## Coding Style & Naming Conventions
Follow standard Kotlin style: four-space indentation, trailing commas where the compiler allows, and favor `val` over `var`. Compose screens end in `Screen`, navigation destinations live under `core/ui/navigation`, and view models adopt the `*ViewModel` suffix. Extend SQLDelight schemas with lower_snake_case table names and add migrations in versioned `.sq` files. Keep platform-specific implementations in their respective source set folders and avoid sharing JVM-only APIs via `commonMain`.

## Testing Guidelines
Write unit tests with `kotlin.test` in `composeApp/src/commonTest/kotlin` for shared logic or `composeApp/src/jvmTest/kotlin` when JVM APIs are required. Name test files after the class under test and describe behavior in `should...` function names. Run `./gradlew :composeApp:check` locally before opening a PR; add snapshots or mock data in `composeResources` to keep tests deterministic. Target high-level scenarios around download scheduling, cookie handling, and SQLDelight repositories.

## Commit & Pull Request Guidelines
Recent history favors imperative, descriptive commit titles (e.g., “Refactor SingleDownloadScreen layout logic”). Group related changes per commit and mention affected modules. Pull requests should summarize user-facing impact, link to issues when applicable, and include screenshots or screen recordings for UI updates. Note configuration changes (cookies, Conveyor, certificates) explicitly so reviewers can re-run the corresponding Gradle task.

## Packaging & Configuration Tips
Conveyor configuration lives in `conveyor.conf`; update release metadata there and regenerate icons via the associated Gradle tasks before packaging. Keep trusted certificates in `res/netfree-ca.crt` and document any additions. When touching yt-dlp integration, ensure the `ytdlp` module stays version-locked and update download defaults in `InitViewModel`.
