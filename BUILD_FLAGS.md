# Runtime Flags

This project uses JVM system properties to control development behaviors. These flags are passed as gradle properties and forwarded to the application at runtime.

## Available Flags

### cleanInstall
- **Default**: `false`
- **Description**: Clean install mode - clears all application settings AND temp directory on startup
- **Use case**: Testing onboarding flow from scratch with a completely clean state

## Usage

### IntelliJ IDEA (Recommended)

Two run configurations are pre-configured in `.run/`:

1. **Run (Normal)** - Normal development mode, preserves all data
2. **Run (Clean Install)** - Clean install mode, clears settings and cache

Simply select the desired configuration from the run configurations dropdown in IntelliJ and click Run.

### Command Line

To run in normal mode:
```bash
./gradlew :composeApp:run
```

To run in clean install mode (test onboarding):
```bash
./gradlew :composeApp:run -PcleanInstall=true
```

## Examples

### Test Onboarding Flow
Using IntelliJ: Select **"Run (Clean Install)"** from the run configurations dropdown

Using command line:
```bash
./gradlew :composeApp:run -PcleanInstall=true
```

### Normal Development
Using IntelliJ: Select **"Run (Normal)"** from the run configurations dropdown

Using command line:
```bash
./gradlew :composeApp:run
```

### Production Build
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```
Production builds always use default values (`cleanInstall=false`).
