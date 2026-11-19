package com.atlas.bridge

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Android RemoteViews must remain native; never render a fake widget into Godot. */
class WidgetActivity : Activity() {
    private lateinit var host: AppWidgetHost
    private lateinit var manager: AppWidgetManager
    private lateinit var content: LinearLayout
    private val prefs get() = getSharedPreferences("widgets", MODE_PRIVATE)
    private var pending = -1
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        host = AppWidgetHost(this, 1042); manager = AppWidgetManager.getInstance(this)
        pending = state?.getInt("pending", -1) ?: -1
        render()
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putInt("pending", pending); super.onSaveInstanceState(outState) }
    override fun onStart() { super.onStart(); host.startListening() }
    override fun onStop() { host.stopListening(); super.onStop() }
    private fun ids() = prefs.getStringSet("ids", emptySet())!!.mapNotNull { it.toIntOrNull() }
    private fun render() {
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 32, 24, 32) }
        setContentView(ScrollView(this).apply { addView(content) })
        content.addView(TextView(this).apply { text = "Captain’s cabin · Android widgets"; textSize = 24f })
        content.addView(Button(this).apply { text = "Return to islands"; setOnClickListener { finish() } })
        content.addView(Button(this).apply { text = "Add a widget"; setOnClickListener { choose() } })
        if (ids().isEmpty()) content.addView(TextView(this).apply { text = "Choose a widget installed on this device. Android may ask for binding permission." })
        ids().forEach { id ->
            val info = manager.getAppWidgetInfo(id)
            if (info == null) { forget(id); return@forEach }
            content.addView(host.createView(this, id, info).apply {
                setAppWidget(id, info)
                val width = resources.displayMetrics.widthPixels / resources.displayMetrics.density - 48
                updateAppWidgetSize(null, width.toInt(), 180, width.toInt(), 300)
            }, LinearLayout.LayoutParams(-1, (220 * resources.displayMetrics.density).toInt()))
            content.addView(Button(this).apply { text = "Remove ${info.loadLabel(packageManager)}"; setOnClickListener {
                AlertDialog.Builder(this@WidgetActivity).setMessage("Remove this widget from ATLAS?")
                    .setNegativeButton("Keep", null).setPositiveButton("Remove") { _, _ -> forget(id); render() }.show()
            } })
        }
    }
    private fun forget(id: Int) { host.deleteAppWidgetId(id); prefs.edit().putStringSet("ids", ids().filter { it != id }.map { it.toString() }.toSet()).apply() }
    private fun choose() {
        val providers = manager.installedProviders.sortedBy { it.loadLabel(packageManager) }
        AlertDialog.Builder(this).setTitle("Choose an installed widget")
            .setItems(providers.map { it.loadLabel(packageManager) }.toTypedArray()) { _, index ->
                pending = host.allocateAppWidgetId()
                val provider = providers[index].provider
                if (manager.bindAppWidgetIdIfAllowed(pending, provider)) configure()
                else @Suppress("DEPRECATION") startActivityForResult(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pending).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider), 1)
            }.setNegativeButton("Cancel", null).show()
    }
    private fun configure() {
        val info = manager.getAppWidgetInfo(pending) ?: run { cancel(); return }
        if (info.configure != null) {
            try { host.startAppWidgetConfigureActivityForResult(this, pending, 0, 2, null) }
            catch (_: Exception) { cancel() }
        } else save()
    }
    private fun save() { prefs.edit().putStringSet("ids", (ids() + pending).map { it.toString() }.toSet()).apply(); pending = -1; render() }
    private fun cancel() { if (pending >= 0) host.deleteAppWidgetId(pending); pending = -1; render() }
    @Deprecated("Android activity result bridge")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 || requestCode == 2) { if (resultCode != RESULT_OK) cancel() else if (requestCode == 1) configure() else save() }
    }
}
