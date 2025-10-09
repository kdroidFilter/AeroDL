# Repository Guidelines

## Project Structure & Module Organization
This project is a Kotlin Multiplatform Compose desktop app split into two Gradle modules. `composeApp` hosts the GUI, DI setup, and platform integrations under `composeApp/src/jvmMain/kotlin`, while shared configuration and SQLDelight schemas live in `composeApp/src/commonMain`. The `ytdlp` module wraps the yt-dlp and FFmpeg binaries (`ytdlp/src/jvmMain/kotlin`) and exposes the download API consumed by the UI. Shared icons and certificates reside in `res/`, and `conveyor.conf` defines native packaging metadata. Use `gradle/libs.versions.toml` to introduce dependency upgrades across modules.

## Build, Test, and Development Commands
- `./gradlew build`: Compiles all modules, runs unit tests, and assembles distributable artifacts.
- `./gradlew :composeApp:run`: Launches the AeroDL desktop client with hot reload enabled when supported.
- `./gradlew check`: Runs compilation plus static analysis tasks; prefer this before submitting a pull request.
- `./gradlew :ytdlp:publishToMavenLocal`: Publishes the wrapper for local integration testing if you need to consume it from another project.

## Coding Style & Naming Conventions
Follow the Kotlin official style with 4-space indentation, trailing commas for multiline collections, and explicit visibility modifiers for non-public APIs. Compose `@Composable` functions should use PascalCase and group by feature (e.g., `features/download/manager`). View models end with `ViewModel`, state holders with `State`, and sealed events with `Event` or `Events`. Organize DI definitions near their feature package, and keep SQLDelight queries colocated in `commonMain/sqldelight`. Run IntelliJ’s “Code > Reformat Code” with the Kotlin style profile before committing.

## Testing Guidelines
Unit tests run with Kotlin test/JUnit. Place cross-platform coverage in `composeApp/src/commonTest` (create as needed) and JVM-only cases in `composeApp/src/jvmTest`. When extending the yt-dlp wrapper, add behavioral tests under `ytdlp/src/jvmTest`. Use descriptive test names (`functionName_expectedBehavior_condition`) and prefer faking external processes over hitting real binaries in CI. Execute `./gradlew check` locally to verify new tests.

## Commit & Pull Request Guidelines
Git history follows Conventional Commits (`feat:`, `refactor:`, `fix:`). Scope commits narrowly so reviewers can reason about UI, wrapper, and packaging changes independently. Pull requests should describe the user-facing impact, list notable implementation details, and link to issues or discussions. Include screenshots or short clips when altering Compose UI flows, and call out any manual steps (e.g., Conveyor packaging updates or yt-dlp binary changes) in the PR description.
