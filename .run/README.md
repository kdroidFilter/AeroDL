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

## How to Use

1. Open the project in IntelliJ IDEA
2. The configurations will appear automatically in the run configurations dropdown (top-right)
3. Select the desired configuration
4. Click the Run button (▶️) or press `Shift+F10`

## Technical Details

- **Normal mode**: Runs with default settings (`cleanInstall=false`) and logs enabled (`-PdebugLogs=true`)
- **Clean Install mode**: Runs with `-PcleanInstall=true` and logs enabled (`-PdebugLogs=true`)
  - Clears Java temp directory (`/tmp`)
  - Clears all application settings (forces onboarding)

These configurations are version controlled and shared across the team for consistency.
