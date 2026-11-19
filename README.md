# ATLAS Pirate Launcher

Experimental Android HOME launcher. Current version: native Android Canvas with four island districts, animated water, compass, zoom, app-as-building shortcuts, district assignment, clock, battery and optional synthesized melody.

## APK

After a successful workflow, see https://github.com/Fizghin/ATLAS-Launcher/releases/tag/atlas-preview for the prerelease APK. GitHub Actions also retains a build artifact.

## Limitations

- This is still a **2D map**, not a rendered 3D world.
- Original OBJ meshes in assets/models are **not yet loaded by Android**.
- Real third-party widgets are not supported yet.
- Typography uses the built-in serif face, not a bundled pirate font.
- Debug APKs from separate GitHub runners can have incompatible signing keys; uninstalling an old preview may be necessary. Do not bypass security warnings.
- The APK has not been security-audited or tested across devices.

## Build

GitHub Actions > Build Android APK. Uses JDK 17, Gradle 8.9 and SDK 35. Successful builds upload an artifact and attempt to publish to the atlas-preview prerelease.

## Next milestone

Implement real OpenGL ES rendering, import OBJ meshes and materials, create detailed textured terrain and buildings, camera transitions, 3D app hit-testing, persistent signing, and AppWidgetHost support.
