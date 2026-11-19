package com.atlas.bridge

import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

/** All bridge calls may originate on Godot's render thread. Android UI stays on main. */
class AtlasAndroid(godot: Godot) : GodotPlugin(godot) {
    private val context: android.app.Activity get() = activity!!
    private val prefs get() = context.getSharedPreferences("atlas", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var revision = 0
    @Volatile private var update = JSONObject().put("state", "idle").toString()
    private var registered = false
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { revision++ }
    }
    override fun getPluginName() = "AtlasAndroid"
    override fun onMainCreate(activity: android.app.Activity): android.view.View? {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(packages, filter, Context.RECEIVER_EXPORTED)
        else @Suppress("DEPRECATION") activity.registerReceiver(packages, filter)
        registered = true
        return null
    }
    override fun onMainResume() { revision++ }
    override fun onMainDestroy() {
        if (registered) context.unregisterReceiver(packages)
        executor.shutdownNow()
    }
    @UsedByGodot fun catalogRevision() = revision
    @UsedByGodot fun getApps(): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val activities = pm.queryIntentActivities(intent, 0)
        val result = JSONArray()
        activities.filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.ROOT) }.forEach {
                val info = it.activityInfo
                val label = it.loadLabel(pm).toString()
                val component = ComponentName(info.packageName, info.name).flattenToString()
                val district = when (info.applicationInfo.category) {
                    ApplicationInfo.CATEGORY_SOCIAL -> 0
                    ApplicationInfo.CATEGORY_MAPS -> 1
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> 2
                    else -> classify("${info.packageName} $label")
                }
                result.put(JSONObject().put("package", info.packageName).put("component", component)
                    .put("id", component).put("label", label).put("district", district))
            }
        return result.toString()
    }
    private fun classify(value: String): Int {
        val s = value.lowercase(Locale.ROOT)
        return when {
            Regex("chat|message|mail|social|phone|contact|whatsapp|telegram|discord|instagram").containsMatchIn(s) -> 0
            Regex("map|travel|uber|lyft|train|flight|auto|navigation|booking").containsMatchIn(s) -> 1
            Regex("file|document|office|note|drive|learn|book|school|calendar|task").containsMatchIn(s) -> 2
            else -> 3
        }
    }
    @UsedByGodot fun launchApp(component: String): Boolean {
        val name = ComponentName.unflattenFromString(component) ?: return false
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try { context.startActivity(intent); true } catch (_: Exception) { false }
    }
    @UsedByGodot fun getBattery(): Int =
        (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    @UsedByGodot fun openWidgets() { context.runOnUiThread { context.startActivity(Intent(context, WidgetActivity::class.java)) } }
    @UsedByGodot fun selectDefaultHome() {
        context.runOnUiThread {
            val roles = context.getSystemService(RoleManager::class.java)
            if (Build.VERSION.SDK_INT >= 29 && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                @Suppress("DEPRECATION") context.startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 901)
            } else context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }
    @UsedByGodot fun checkUpdates(force: Boolean) {
        if (!force && System.currentTimeMillis() - prefs.getLong("lastUpdate", 0) < 6 * 60 * 60 * 1000) return
        update = JSONObject().put("state", "checking").toString()
        executor.execute {
            try {
                val release = JSONObject(fetch("https://api.github.com/repos/Fizghin/ATLAS-Launcher/releases/latest"))
                val assets = release.getJSONArray("assets")
                val manifestAsset = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name") == "atlas-update.json" }
                    ?: error("Latest release has no verified ATLAS update manifest")
                val manifest = JSONObject(fetch(manifestAsset.getString("browser_download_url")))
                val url = manifest.getString("url")
                require(url.startsWith("https://github.com/Fizghin/ATLAS-Launcher/releases/download/"))
                require(manifest.getString("sha256").matches(Regex("[a-fA-F0-9]{64}")))
                @Suppress("DEPRECATION") val current = context.packageManager.getPackageInfo(context.packageName, 0).let {
                    if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
                }
                update = manifest.put("state", if (manifest.getLong("versionCode") > current) "available" else "current")
                    .put("notes", release.optString("body").take(12000)).toString()
                prefs.edit().putLong("lastUpdate", System.currentTimeMillis()).apply()
            } catch (e: Exception) { update = JSONObject().put("state", "error").put("message", e.message ?: "Network error").toString() }
        }
    }
    private fun fetch(url: String): String {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 15000; connection.readTimeout = 20000
        connection.setRequestProperty("User-Agent", "ATLAS-Launcher/0.4")
        return try { require(connection.responseCode == 200) { "Update server returned ${connection.responseCode}" }; connection.inputStream.bufferedReader().use { it.readText() } }
        finally { connection.disconnect() }
    }
    @UsedByGodot fun updateStatus() = update
    @UsedByGodot fun downloadUpdate(): String {
        return try {
            val data = JSONObject(update)
            require(data.getString("state") == "available")
            val file = java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "atlas-${data.getLong("versionCode")}-${System.currentTimeMillis()}.apk")
            val request = DownloadManager.Request(android.net.Uri.parse(data.getString("url")))
                .setTitle("ATLAS ${data.getString("versionName")}").setDestinationUri(android.net.Uri.fromFile(file))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            prefs.edit().putLong("downloadId", id).putString("downloadPath", file.path)
                .putString("downloadHash", data.getString("sha256")).putLong("downloadVersion", data.getLong("versionCode")).apply()
            "Download started. Choose Install downloaded update when it finishes."
        } catch (e: Exception) { e.message ?: "Download failed" }
    }
    @UsedByGodot fun installUpdate(): String {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = prefs.getLong("downloadId", -1)
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst() || cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_SUCCESSFUL) return "No completed download. Try again when downloading finishes."
        }
        executor.execute {
            try {
                val file = java.io.File(prefs.getString("downloadPath", "")!!)
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                require(hash.equals(prefs.getString("downloadHash", ""), true)) { "APK checksum mismatch" }
                val pm = context.packageManager
                @Suppress("DEPRECATION") val archive = pm.getPackageArchiveInfo(file.path, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES) ?: error("Invalid APK")
                @Suppress("DEPRECATION") val own = pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                require(archive.packageName == context.packageName && archive.longVersionCode == prefs.getLong("downloadVersion", -1) && archive.longVersionCode > own.longVersionCode) { "Wrong package or version" }
                val ownSigners = own.signingInfo!!.apkContentsSigners.map { it.toCharsString() }.toSet()
                require(archive.signingInfo!!.apkContentsSigners.map { it.toCharsString() }.toSet() == ownSigners) { "Signing certificate is incompatible" }
                context.runOnUiThread {
                    if (!pm.canRequestPackageInstalls()) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:${context.packageName}")))
                    } else {
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    }
                }
                update = JSONObject().put("state", "verified").put("message", "APK verified. Approve installation in Android; retry Install after granting permission.").toString()
            } catch (e: Exception) { update = JSONObject().put("state", "error").put("message", e.message).toString() }
        }
        return "Verifying downloaded APK…"
    }
}
