# ATLAS behavior contract

Source: user-supplied Project Atlas brief; existing repository HOME functionality;
Android platform consent APIs. Visual rules: [DESIGN.md](DESIGN.md).

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Navigation | `world.gd::_fly`, `_notification` | Project Atlas brief | Overview → district → building. Back closes modal, then returns overview. HOME uses a singleTask activity. | Android smoke + manual gestures |
| Form | `_edit`, ConfigFile | Project Atlas brief | Name must be nonempty, at most 60 characters. Save updates building; Close discards input. | Layout persistence test; emulator edit |
| Select/Listbox | `_edit`, `_search` | Project Atlas brief | Authored Godot button rows. Native provider picker only in Android widget cabin. | Emulator modal interaction |
| Scrollbar | Godot Theme / ScrollContainer | Project Atlas brief | Visible engine scrollbars, bounded dialogs, full results accessible. No web stylesheet. | Narrow portrait review |
| Toast | `status` label / update dialog message | Project Atlas brief | One fixed scene status area; update errors remain inside update sheet. | Runtime error-path check |
| CRUD | `_catalog`, `_edit`, Kotlin PackageManager | Project Atlas brief | Component ID is stable identity. Retain saved overrides across removal/reinstall. Never truncate installed launchable activities. | Layout tests and package changes |
| Permissions | Kotlin platform APIs | Project Atlas brief | Request default HOME role; Android widget binding/configuration; unknown-source approval and installer. Never bypass system approval. | Emulator where supported |
| Updates | `AtlasAndroid` | Project Atlas brief | Six-hour check on launch; latest non-prerelease release manifest; actual APK version code, SHA256, package and signing identity verification. Explicit download and install. | Live check; verified install needs signed newer release |

English UI initially; device-local clock. App labels retain their source language. No calendar,
notification or location permission is collected. Weather/calendar/notification widgets run as
real Android RemoteViews in the native cabin, subject to the installed provider's capabilities.
The cabin is secondary to the world; no claim that native RemoteViews are mapped onto 3D meshes.

App installation/removal causes catalog reconciliation; names and assignments persist in
`user://captains_log.cfg`. Every activity has one collision-free grid berth outside the landmark.
Reassigning selects the first available berth; Next berth swaps if occupied. No destructive
app uninstall action exists in the Godot surface. Removing a widget uses native confirmation.
Menus close using Back/Escape. Audio pauses when ATLAS loses foreground and resumes with it.
Local settings work without internet. Update failures never prevent launching installed apps.

Development limits: full screen-reader navigation through the 3D world, large-catalog frame-time
and memory profiling, physical Pixel 6 gestures, widget configuration diversity, release signing
and end-to-end APK update installation remain release gates, not inferred successes.
