package com.atlas.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val parchment = Color.rgb(233, 212, 165)
    private val ink = Color.rgb(55, 39, 25)
    private var district: String? = null
    private lateinit var root: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(parchment)
            setPadding(22, 30, 22, 16)
        }
        setContentView(root)
        showMap()
    }
    private fun label(text: String, size: Float = 19f): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(ink)
            typeface = Typeface.create("serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(8, 18, 8, 18)
        }
    private fun action(text: String, callback: () -> Unit) {
        root.addView(label(text).apply { setOnClickListener { callback() } },
            LinearLayout.LayoutParams(-1, 0, 1f))
    }
    private fun showMap() {
        district = null
        root.removeAllViews()
        root.addView(label("ATLAS", 34f))
        root.addView(label("THE CARTOGRAPHER'S HOME", 13f))
        action("⚓  THE TAVERN\nSocial") { showDistrict("THE TAVERN") }
        action("⚓  THE SHIPYARD\nTravel") { showDistrict("THE SHIPYARD") }
        action("⚓  THE VAULT\nFiles") { showDistrict("THE VAULT") }
        action("⚓  THE OBSERVATORY\nUtilities") { showDistrict("THE OBSERVATORY") }
        val prefs = getSharedPreferences("atlas", MODE_PRIVATE)
        val music = label(if (prefs.getBoolean("music", false)) "SOUND: ON" else "SOUND: OFF", 14f)
        music.setOnClickListener {
            val enabled = !prefs.getBoolean("music", false)
            prefs.edit().putBoolean("music", enabled).apply()
            music.text = if (enabled) "SOUND: ON (audio not bundled)" else "SOUND: OFF"
        }
        root.addView(music)
        root.addView(label("CAPTAIN'S LOG • " +
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()), 13f))
    }
    private fun showDistrict(name: String) {
        district = name
        root.removeAllViews()
        root.addView(label("‹ BACK TO MAP").apply { setOnClickListener { showMap() } })
        root.addView(label(name, 26f))
        root.addView(label("BUILDINGS • INSTALLED APPS", 13f))
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        for (app in apps) {
            list.addView(label("⌂  " + app.loadLabel(packageManager).toString(), 17f).apply {
                gravity = Gravity.START
                setOnClickListener {
                    packageManager.getLaunchIntentForPackage(app.activityInfo.packageName)
                        ?.let { intent -> startActivity(intent) }
                }
            })
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (district != null) showMap() else super.onBackPressed()
    }
}
