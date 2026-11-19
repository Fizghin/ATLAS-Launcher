package com.atlas.launcher
import org.godotengine.godot.GodotActivity
class MainActivity : GodotActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        // Gradle may compress APK assets. Give Godot a seekable private pack file.
        val pack = java.io.File(filesDir, "atlas.pck")
        val stamp = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        val prefs = getSharedPreferences("pack", MODE_PRIVATE)
        if (!pack.isFile || prefs.getLong("stamp", -1) != stamp) {
            val staging = java.io.File(filesDir, "atlas.pck.tmp")
            assets.open("atlas.pck").use { input -> staging.outputStream().use { input.copyTo(it) } }
            check(staging.renameTo(pack)) { "Cannot prepare Godot project pack" }
            prefs.edit().putLong("stamp", stamp).apply()
        }
        super.onCreate(savedInstanceState)
    }
    override fun getCommandLine() = super.getCommandLine().apply {
        addAll(0, listOf("--main-pack", java.io.File(filesDir, "atlas.pck").absolutePath, "--rendering-method", "gl_compatibility"))
    }
}
