# IntelliJ Run Configurations

This directory contains pre-configured run configurations for IntelliJ IDEA / Android Studio.

## Available Configurations

### 🚀 Run (Normal)
- **Purpose**: Normal development mode
- **Settings**: Preserves all application settings and cache
- **Use when**: Regular development, testing features with existing data
- **Logs**: Enabled via `-PdebugLogs=true`

### 🧹 Run (Clean Install)
- **Purpose**: Clean installation mode for testing onboarding
- **Settings**: Clears all settings and cache on startup
- **Use when**: Testing the onboarding flow from scratch
- **Logs**: Enabled via `-PcleanInstall=true -PdebugLogs=true`

### 🔒 Run (Release, Logs)
- Purpose: Package and run the release build with ProGuard optimizations enabled
- Use when: Reproducing release-only issues (e.g., shrink/opt related)
- Logs: Enabled via `-PdebugLogs=true`

### 🧼 Run (Release Clean Install, Logs)
- Purpose: Same as above, but starts from a clean state
- Use when: Testing onboarding or first-run behavior in release
- Logs: Enabled via `-PcleanInstall=true -PdebugLogs=true`

## How to Use

1. Open the project in IntelliJ IDEA
2. The configurations will appear automatically in the run configurations dropdown (top-right)
3. Select the desired configuration
4. Click the Run button (▶️) or press `Shift+F10`

Notes
-----
- The Release run configs package the app then execute the produced binary under
  `composeApp/build/compose/binaries/main/app/AeroDl/bin/AeroDl` (Linux).
  They respect `-PcleanInstall` and `-PdebugLogs` via Compose `jvmArgs`.
  On non-Linux OS, adapt the run task in `composeApp/build.gradle.kts` if needed.

## Technical Details

- **Normal mode**: Runs with default settings (`cleanInstall=false`) and logs enabled (`-PdebugLogs=true`)
- **Clean Install mode**: Runs with `-PcleanInstall=true` and logs enabled (`-PdebugLogs=true`)
  - Clears Java temp directory (`/tmp`)
  - Clears all application settings (forces onboarding)

These configurations are version controlled and shared across the team for consistency.
