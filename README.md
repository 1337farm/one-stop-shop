# ForgeRig

ForgeRig (formerly OneStopShop / OpenCode) is a standalone ARM64 Linux engine providing an isolated Linux userland on unrooted Android devices.

## Features

- **Isolated Linux Userland:** Runs a minimal Ubuntu rootfs using PRoot on ARM64 architecture, all extracted dynamically at launch.
- **Persistent Daemon Orchestration:** Runs a background container service with a wake lock to ensure the Linux environment and WebSockets don't sleep.
- **Embedded Web UI:** Connects to localhost WebSockets for UI rendering and workspace interaction via an embedded WebView.
- **Seamless GitHub Integration:** Uses custom URI scheme routing (`opencode://oauth-callback`) to silently exchange auth codes and inject access tokens into the container's `.gitconfig`.

## Development

The Android host is built with Kotlin, and the environment can be built using standard Gradle commands.

### Build and Test

```bash
./gradlew assembleDebug
./gradlew test
```

### CI / CD

A GitHub Actions workflow is provided to build and test the application on pushes to the `main` branch. Artifacts (APK) are uploaded to a `latest` release tag.
