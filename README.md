# ATLAS 3D Android launcher ? development milestone 0.4.0

ATLAS now hosts Godot 4 as its Android HOME screen. The previous Canvas interface has
been removed. This is a development APK milestone, not the finished cinematic edition.

## Implemented

- Real four-island 3D world, original modeled houses/landmarks, palms, ships and animated ocean.
- Kotlin Godot plugin: launchable activities from PackageManager, component-specific launching,
  package changes, HOME selection, battery, real native AppWidgetHost cabin, update manager.
- Each discovered launchable activity gets a distinct building with its app name; no demo apps
  are inserted on Android. Desktop preview examples are explicitly labeled.
- Tap island to fly closer; tap a building to open its app; Back returns to overview.
  Drag rotates; pinch/wheel zooms. Hold a building for rename, reassignment or next-berth move.
- Saved layout, audio, volume, reduced motion, shadow and 30/60 FPS preferences.
- Original looping harbor music, surf and optional action bell. Audio pauses with activity lifecycle.
- Update checks require release metadata, actual APK version codes, SHA256 and signing match;
  installation always uses Android consent. Missing metadata is reported instead of guessed.

Weather/calendar/notification content is provided through user-selected real Android widgets
in the native Captain?s cabin. These are not rendered onto 3D surfaces. Device/provider testing
and final environment polish remain open. See [verification limits](docs/TESTING.md).

## Build

Requirements: JDK 17, Gradle 8.9, Android SDK 35/build-tools, Python 3, Godot **4.4.1**.
Both app and native runtime use the same pinned engine version. The Godot Maven AAR embeds
the Android engine; a separate Godot Android template installation is not needed for this
embedded architecture. See [Godot Android library documentation](https://docs.godotengine.org/en/4.4/tutorials/platform/android/android_library.html).

```sh
python tools/generate_audio.py
godot --headless --path godot --editor --import --quit
godot --headless --path godot --script tests/test_layout.gd
mkdir -p app/src/main/assets
godot --headless --path godot --export-pack Pack ../app/src/main/assets/atlas.pck --quit
gradle :atlas-plugin:assembleDebug :app:assembleDebug :app:lintDebug
python tools/validate_apk.py app/build/outputs/apk/debug/app-debug.apk
```

Set `ANDROID_HOME` or untracked `local.properties` with `sdk.dir`. On Windows use the
Godot console executable, and create the assets directory through PowerShell if needed.
The Gradle pre-build gate refuses builds without a Godot pack. Re-export after scene edits.
The activity extracts the compressed pack to private storage on APK changes for seekable access.

## Install development APK

Android 10+; arm64-v8a for Pixel 6 and x86_64 for emulator. Install the development APK using
Android?s package installer or `adb install -r app-debug.apk`. Choose ATLAS under Android
Settings ? Apps ? Default apps ? Home app, or use ATLAS Settings ? Choose ATLAS as home.
To return to your previous launcher, select it in Android Home settings.
Development builds use the local Android debug signing identity. A later signed release may
require uninstalling the development build (which deletes its saved layout). Back up manually
before uninstalling if needed. Never commit the debug key or private release signing material.

## Automated Android smoke test

Use a disposable emulator only: the test changes its default HOME application.

```sh
python tools/android_smoke.py app/build/outputs/apk/debug/app-debug.apk --output smoke
```

It verifies the native bridge, catalog/building counts, HOME resolution, island camera focus,
launching an installed app through a building tap, and returning HOME. `smoke.json`, logcat,
and screenshots are actual runtime evidence. Layout tests are separate from Android proof.

## CI and release signing

The new workflow exports Godot, builds the Kotlin plugin and APK, runs lint/content/signature
validation, and runs emulator smoke testing. Development APKs are Actions artifacts.
Version tags (`v0.4.0`, matching the built versionName) require repository secrets:

- `ATLAS_KEYSTORE_BASE64`
- `ATLAS_STORE_PASSWORD`
- `ATLAS_KEY_ALIAS`
- `ATLAS_KEY_PASSWORD`

The release key is decoded only into runner temporary storage and removed after signing.
Release publishing requires the signed APK smoke test to pass. `atlas-update.json` is generated
from actual APK metadata and digest. Increment versionCode and versionName together before
future releases. No final release is published by this local development delivery.

## Repository map

`godot/` is the complete Godot scene and scripts. `atlas-plugin/` is the Kotlin Android library.
`app/` is the Godot HOME host. `tools/` contains asset generation and verification utilities.
[Asset licenses](ASSET_CREDITS.md), [design](DESIGN.md), [behavior](UX-CONTRACT.md),
and [test limits](docs/TESTING.md) distinguish functionality from remaining release gates.
