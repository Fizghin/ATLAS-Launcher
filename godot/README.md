# ATLAS Godot rebuild (work in progress)

This is a **separate 3D project**. It is not yet the Android launcher APK and does not replace the current Android code.

## Open
Install Godot 4.3 or later. Import `godot/project.godot`, open `scenes/archipelago.tscn`, and run. The current scene uses **procedural geometry and placeholder materials**; the detailed OBJ files in `../assets/models` are not yet imported.

## Implemented in this first commit
- Four 3D island scenes with irregular shoreline geometry, layered beaches, terrain, vegetation, docks, and district landmarks.
- Separate mesh + collision body + floating sign for each entry in an app catalog.
- Raycast interaction for selecting individual app buildings.
- Tweened camera flight to an island and back to the archipelago.
- A prototype clock and simple 3D sea surface.

## Android integration needed
A Godot 4 Android plugin registered as `AtlasAndroid` must implement:
- `getLaunchableApps() -> String` returning JSON with `label`, `package`, `activity` fields.
- `launchApp(packageName: String)` using the Android package manager and explicit launch intent.
- `checkForUpdates()` to query signed GitHub releases and request user-approved APK installation.
- `getBatteryPercent()`, `getWeatherInfo()` as applicable.
- Widget hosting through native Android `AppWidgetHost`, presented as a surface/panel inside the Godot UI. Widgets require user permission.

**Important:** The current demo catalog contains fake packages for testing. It does not enumerate installed apps. Android 11+ package visibility and HOME intent-filter integration must be configured and tested.

## Quality bar before release
1. Import and texture detailed GLB/GLTF assets, including tavern, shipyard, fortress, observatory, houses, vegetation, boats, and cliffs.
2. Replace flat water with a mobile-friendly animated shader and shore foam.
3. Add baked lighting, shadows, LOD, and frame-time profiling on Pixel 6.
4. Make the scene populate from real installed apps and persist user building placement.
5. Build and test an Android launcher with Godot Android plugin, proper HOME role, and system Back navigation.
6. Sign releases with a persistent signing key and verify update compatibility.
7. Publish an APK to GitHub Releases **only after a successful Android build and device testing**.

Do not present this project as a complete cinematic 3D launcher until these requirements are met.
