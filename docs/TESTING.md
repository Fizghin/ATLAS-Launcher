# Development milestone verification

This report distinguishes implemented code from runtime proof. Development build: 0.4.0,
versionCode 4, Godot 4.4.1, Android API 35, Android 10+ minimum.

Passed locally:

- Godot editor imports and headless scene start (no GDScript parse errors).
- `tests/test_layout.gd`: 500 non-overlapping berths, landmark exclusion, catalog refresh,
  island reassignment, persisted custom building name.
- Gradle `:app:assembleDebug :app:lintDebug`: successful; nonblocking portrait/dependency warnings.
- `tools/validate_apk.py`: actual Godot pack, scene, geometry script, ocean shader,
  original audio, arm64 engine and Kotlin bridge present.
- Desktop GPU rendering with actual scene screenshots (not concept images).

Android smoke-test results and screenshots are saved separately by `tools/android_smoke.py`.
Do not infer a passing Android smoke test from this static report; inspect `smoke.json`.
The first emulator run detected and fixed a real packed-asset startup failure. The pack is
now extracted atomically to app-private storage on APK changes before starting Godot.
SwiftShader indirect on this host also produced driver-level uniform-limit shader errors;
GPU host rendering is used for the local emulator validation.

Unverified release gates:

- Physical Pixel 6 frame-time, memory, temperature and battery profiling; 60 FPS is a target.
- Full TalkBack/large-font accessibility and non-English fallback coverage.
- Touch pinch/rotation/long-press diversity across physical devices.
- Third-party widget binding, configuration and provider lifecycle diversity.
- Audio output quality, competing audio-focus handling, and physical speaker behavior.
- Successful newer signed-release download/install with the persistent signing identity.
- Cinematic asset/art polish, expanded landmark functionality, texture/mesh batching and LOD.

Missing/invalid GitHub release metadata is intentionally shown as an error in the update
sheet rather than an installable update. Nothing is silently installed. No old 2D APK is used.
