package com.atlas.launcher

import android.app.Activity
import android.content.*
import android.content.pm.ResolveInfo
import android.graphics.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.*
import android.view.*
import android.widget.*
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var map: PirateMap
    private lateinit var world: Atlas3DView
    private var in3D = true
    private val prefs by lazy { getSharedPreferences("atlas", MODE_PRIVATE) }
    private val parchment = Color.rgb(235, 214, 168)
    private val ink = Color.rgb(60, 42, 27)
    private val sea = Color.rgb(57, 108, 119)
    private var audio: SeaAudio? = null
    private val districts = arrayOf("TAVERN", "SHIPYARD", "VAULT", "OBSERVATORY")
    private val descriptions = arrayOf("SOCIAL & MESSAGES", "TRAVEL & MOTORS", "FILES & STUDY", "TOOLS & WEATHER")
    private val apps by lazy {
        packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName + "/" + it.activityInfo.name }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = sea
        window.navigationBarColor = parchment
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        map = PirateMap()
        world = Atlas3DView(this) { index ->
            in3D = false
            setContentView(map)
            map.openDistrict(index)
        }
        show3D()
        checkUpdates(false)
        if (prefs.getBoolean("music", false)) startMusic()
    }
    override fun onPause() { super.onPause(); if(in3D) world.onPause(); audio?.stop(); audio = null }
    override fun onResume() { super.onResume(); if(::world.isInitialized && in3D) world.onResume(); if (prefs.getBoolean("music", false)) startMusic() }
    override fun onDestroy() { audio?.stop(); audio = null; super.onDestroy() }
    private fun startMusic() { if (audio == null) audio = SeaAudio().also { it.start() } }
    private fun toggleMusic() {
        val enabled = !prefs.getBoolean("music", false)
        prefs.edit().putBoolean("music", enabled).apply()
        if (enabled) startMusic() else { audio?.stop(); audio = null }
        map.invalidate()
    }
    private fun launch(app: ResolveInfo) {
        try {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(app.activityInfo.packageName, app.activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) { Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show() }
    }
    private fun assigned(index: Int): List<ResolveInfo> {
        val explicit = prefs.getString("district_$index", null)
        if (explicit != null) {
            val ids = explicit.split("|").filter { it.isNotBlank() }
            return ids.mapNotNull { id -> apps.find { it.activityInfo.packageName + "/" + it.activityInfo.name == id } }
        }
        val result = apps.filter {
            val name = (it.loadLabel(packageManager).toString() + " " + it.activityInfo.packageName).lowercase()
            when(index) {
                0 -> listOf("whatsapp","instagram","telegram","discord","reddit","messages","gmail","snapchat","facebook","signal","chatgpt","claude").any(name::contains)
                1 -> listOf("maps","car","uber","ola","travel","drive","parking","vehicle","rail","flight","waze","nfs").any(name::contains)
                2 -> listOf("file","document","pdf","note","keep","classroom","school","calendar","drive","office","docs","sheet","study").any(name::contains)
                else -> listOf("camera","clock","weather","calculator","battery","settings","phone","chrome","browser","gallery","photos","music").any(name::contains)
            }
        }
        return result.take(24)
    }
    private fun chooseApps(index: Int) {
        val ids = apps.map { it.activityInfo.packageName + "/" + it.activityInfo.name }
        val saved = prefs.getString("district_$index", null)?.split("|")?.filter { it.isNotBlank() }
        val current = saved ?: assigned(index).map { it.activityInfo.packageName + "/" + it.activityInfo.name }
        val checks = BooleanArray(apps.size) { ids[it] in current }
        android.app.AlertDialog.Builder(this)
            .setTitle("Assign buildings to " + districts[index])
            .setMultiChoiceItems(apps.map { it.loadLabel(packageManager).toString() }.toTypedArray(), checks) { _, which, selected -> checks[which] = selected }
            .setPositiveButton("SAVE") { _, _ ->
                prefs.edit().putString("district_$index", ids.filterIndexed { i, _ -> checks[i] }.joinToString("|")).apply()
                map.invalidate()
            }.setNegativeButton("CANCEL", null).show()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if(!in3D) show3D() else super.onBackPressed() }

    private fun show3D() {
        in3D = true
        val shell = android.widget.FrameLayout(this)
        (world.parent as? android.view.ViewGroup)?.removeView(world)
        shell.addView(world)
        val header = android.widget.TextView(this).apply {
            text = "A T L A S    ✦    THE LIVING ARCHIPELAGO"
            textSize = 18f
            setTextColor(android.graphics.Color.rgb(245,222,175))
            setBackgroundColor(0xBB203E46.toInt())
            gravity = android.view.Gravity.CENTER
            setPadding(8,24,8,24)
        }
        shell.addView(header, android.widget.FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP))
        val bottom = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setBackgroundColor(0xDD203E46.toInt())
        }
        val labels = arrayOf("2D MAP","CHECK UPDATES","ALL APPS")
        for (i in labels.indices) {
            val b=android.widget.Button(this).apply {
                text=labels[i];textSize=11f
                setOnClickListener {
                    when(i) {
                        0 -> {in3D=false;setContentView(map)}
                        1 -> checkUpdates(true)
                        else -> {in3D=false;setContentView(map);map.showAppDrawer()}
                    }
                }
            }
            bottom.addView(b,android.widget.LinearLayout.LayoutParams(0,-2,1f))
        }
        shell.addView(bottom,android.widget.FrameLayout.LayoutParams(-1,-2,android.view.Gravity.BOTTOM))
        setContentView(shell)
        world.onResume()
    }

    private fun checkUpdates(manual: Boolean) {
        Thread {
            try {
                val conn = (java.net.URL("https://api.github.com/repos/Fizghin/ATLAS-Launcher/releases/tags/atlas-preview").openConnection() as java.net.HttpURLConnection)
                conn.connectTimeout=7000;conn.readTimeout=7000
                conn.setRequestProperty("Accept","application/vnd.github+json")
                val json=org.json.JSONObject(conn.inputStream.bufferedReader().use{it.readText()})
                conn.disconnect()
                val assets=json.optJSONArray("assets")
                var apkUrl=""\n                var releaseStamp=""
                if(assets!=null)for(i in 0 until assets.length()){
                    val a=assets.getJSONObject(i)
                    if(a.optString("name").endsWith(".apk")) {\n                        apkUrl=a.optString("browser_download_url")\n                        releaseStamp=a.optString("updated_at")\n                    }
                }
                val published=releaseStamp.ifBlank { json.optString("published_at") }
                val seen=prefs.getString("last_seen_release","") ?: ""
                val url=apkUrl
                runOnUiThread {
                    if(url.isNotBlank() && (manual || published!=seen)) {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("ATLAS update available")
                            .setMessage("A GitHub preview APK is available. Android will require your approval to install it. Existing preview builds may have different signing certificates.")
                            .setPositiveButton("VIEW DOWNLOAD") { _,_ ->
                                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)))
                                prefs.edit().putString("last_seen_release",published).apply()
                            }.setNegativeButton("LATER",null).show()
                    } else if(manual) android.widget.Toast.makeText(this,"No newer release found",android.widget.Toast.LENGTH_LONG).show()
                }
            } catch(e:Exception) {
                if(manual)runOnUiThread { android.widget.Toast.makeText(this,"Could not check GitHub releases",android.widget.Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    inner class PirateMap : View(this@MainActivity) {
        var selected = -1
        private var zoom = 1f
        private var targetZoom = 1f
        private var panX = 0f
        private var panY = 0f
        private var downX = 0f
        private var downY = 0f
        private var moved = false
        private var lastDistance = 0f
        private val startedAt = android.os.SystemClock.uptimeMillis()
        private var frameTime = startedAt
        private val titlePaint = Paint(3)
        private val gold = 0xFFCEAE70.toInt()
        private val paint = Paint(3)
        private val islands = arrayOf(.25f to .29f, .74f to .30f, .28f to .69f, .73f to .71f)
        private val mapColors = intArrayOf(0xFFBDA878.toInt(), 0xFFC5AD7D.toInt(), 0xFFB2A278.toInt(), 0xFFB7A17A.toInt())
        private val names = arrayOf("TAVERN","SHIPYARD","VAULT","OBSERVATORY")
        private val handler = Handler(Looper.getMainLooper())
        private val tick = object : Runnable { override fun run() { invalidate(); handler.postDelayed(this, 15000) } }
        init { handler.postDelayed(tick, 15000) }
        private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = ink, bold: Boolean = true) {
            paint.color = color; paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(if (bold) "serif" else "serif", if (bold) Typeface.BOLD_ITALIC else Typeface.ITALIC)
            paint.textSize = size; paint.textAlign = Paint.Align.CENTER
            c.drawText(s, x, y, paint)
        }
        private fun line(c: Canvas, x1:Float,y1:Float,x2:Float,y2:Float,color:Int=ink,width:Float=2f) {
            paint.color=color; paint.strokeWidth=width; paint.style=Paint.Style.STROKE
            c.drawLine(x1,y1,x2,y2,paint); paint.style=Paint.Style.FILL
        }
        private fun island(c:Canvas,cx:Float,cy:Float,r:Float,seed:Int) {
            val p=Path()
            for(i in 0..48) {
                val a=i*PI*2/48
                val wobble=1f + .055f*sin(i*.78+seed*3.0).toFloat()+.038f*cos(i*1.47+seed).toFloat()
                val x=cx+cos(a).toFloat()*r*wobble
                val y=cy+sin(a).toFloat()*r*.76f*wobble
                if(i==0)p.moveTo(x,y) else p.lineTo(x,y)
            }
            p.close()
            paint.color=0xFFE4C994.toInt(); paint.style=Paint.Style.STROKE;paint.strokeWidth=13f
            c.drawPath(p,paint)
            paint.color=mapColors[seed];paint.style=Paint.Style.FILL;c.drawPath(p,paint)
            paint.color=0x33412B1B;paint.style=Paint.Style.STROKE;paint.strokeWidth=1.4f
            c.drawCircle(cx,cy,r*.57f,paint)
            c.drawCircle(cx,cy,r*.37f,paint)
            paint.style=Paint.Style.FILL
            paint.color=ink;paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;c.drawPath(p,paint);paint.style=Paint.Style.FILL
            val random=java.util.Random(seed.toLong()+18)
            repeat(16) {
                val x=cx+(random.nextFloat()-.5f)*r*1.2f
                val y=cy+(random.nextFloat()-.5f)*r*.9f
                line(c,x-5,y+4,x,y-7,0xFF566B4E.toInt(),2f)
                line(c,x,y-7,x+5,y+4,0xFF566B4E.toInt(),2f)
            }
        }
        private fun building(c:Canvas,x:Float,y:Float,scale:Float,index:Int) {
            paint.color=0xFF66503B.toInt()
            c.drawRect(x-9*scale,y-4*scale,x+9*scale,y+12*scale,paint)
            val roof=Path().apply { moveTo(x-14*scale,y-4*scale);lineTo(x,y-17*scale);lineTo(x+14*scale,y-4*scale);close() }
            paint.color=0xFF493529.toInt();c.drawPath(roof,paint)
            paint.color=0xFFD5B77D.toInt();c.drawRect(x-2*scale,y+3*scale,x+3*scale,y+12*scale,paint)
            if(index%3==0) { line(c,x+9*scale,y-6*scale,x+9*scale,y-23*scale);line(c,x+9*scale,y-23*scale,x+18*scale,y-20*scale) }
        }
        override fun onDraw(c:Canvas) {
            super.onDraw(c)
            val w=width.toFloat();val h=height.toFloat()
            c.drawColor(parchment)
            val header=96f*resources.displayMetrics.density
            val bottom=94f*resources.displayMetrics.density
            val mapTop=header;val mapH=h-header-bottom
            paint.color=sea;c.drawRect(0f,mapTop,w,h-bottom,paint)
            // Layered water contours and gentle wave movement
            val seconds=(android.os.SystemClock.uptimeMillis()-startedAt)/1000f
            paint.style=Paint.Style.STROKE;paint.strokeWidth=1.4f
            for(j in 0..25) {
                val yy=mapTop+j*mapH/24f
                val wave=Path()
                for(k in 0..30) {
                    val xx=k*w/30f
                    val yWave=yy+6f*sin(xx*.025f+seconds*.25f+j*.4f)
                    if(k==0)wave.moveTo(xx,yWave) else wave.lineTo(xx,yWave)
                }
                paint.color=if(j%2==0)0x337DE0D4 else 0x2243A6A9
                c.drawPath(wave,paint)
            }
            paint.style=Paint.Style.FILL
            c.save();c.clipRect(0f,mapTop,w,h-bottom)
            val centerX=w/2;val centerY=mapTop+mapH/2
            zoom+=(targetZoom-zoom)*.13f
            if(abs(targetZoom-zoom)>.003f) postInvalidateDelayed(16)
            c.translate(centerX+panX,centerY+panY)
            c.scale(zoom,zoom)
            c.translate(-centerX,-centerY)
            val radius=min(w,mapH)*.19f
            for(i in 0..3) {
                val x=w*islands[i].first;val y=mapTop+mapH*islands[i].second
                island(c,x,y,radius,i)
                building(c,x,y-16f,1.8f,i)
                // Landmarks: lookout, market and docks
                building(c,x-radius*.46f,y+radius*.12f,.65f,i+2)
                building(c,x+radius*.46f,y-radius*.15f,.8f,i+3)
                line(c,x-radius*.68f,y+radius*.46f,x+radius*.63f,y+radius*.41f,0xFF725639.toInt(),2.5f)
                text(c,names[i],x,y+radius*.53f, min(w/20f,21f),ink)
            }
            // Dashed sailing routes
            paint.color=0xFF375B5C.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=2f
            paint.pathEffect=DashPathEffect(floatArrayOf(9f,11f),0f)
            val route=Path().apply { moveTo(w*.25f,mapTop+mapH*.29f);quadTo(w*.5f,mapTop+mapH*.08f,w*.74f,mapTop+mapH*.30f);quadTo(w*.95f,mapTop+mapH*.5f,w*.73f,mapTop+mapH*.71f);quadTo(w*.5f,mapTop+mapH*.92f,w*.28f,mapTop+mapH*.69f) }
            c.drawPath(route,paint);paint.pathEffect=null;paint.style=Paint.Style.FILL
            if(selected>=0) {
                val x=w*islands[selected].first;val y=mapTop+mapH*islands[selected].second
                val assigned=assigned(selected)
                text(c,"BUILDINGS",x,y-radius*.63f,16f)
                assigned.take(18).forEachIndexed { j,app ->
                    val cols=3
                    val row=j/cols;val col=j%cols
                    val bx=x+(col-1)*radius*.68f
                    val by=y-radius*.34f+row*radius*.29f
                    building(c,bx,by, .55f,j)
                    val name=app.loadLabel(packageManager).toString().take(12)
                    text(c,name,bx,by+22f,8f,ink,false)
                }
            }
            c.restore()
            // Frame and map legend
            paint.color=ink;paint.style=Paint.Style.STROKE;paint.strokeWidth=3f
            c.drawRect(7f,mapTop+7f,w-7f,h-bottom-7f,paint);paint.style=Paint.Style.FILL
            text(c,"A T L A S",w/2,header*.39f,40f)
            text(c,"✦  THE CARTOGRAPHER’S REALM  ✦",w/2,header*.60f,11f)
            line(c,w*.13f,header*.66f,w*.87f,header*.66f,gold,2f)
            text(c,if(selected<0)"THE LOST ARCHIPELAGO" else descriptions[selected],w/2,header*.83f,14f)
            text(c,if(selected<0)"TAP AN ISLAND  •  PINCH TO EXPLORE" else "TAP A BUILDING TO OPEN AN APP",w/2,h-bottom+24f,12f)
            val battery=getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
            val pct=battery.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val now=java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(java.util.Date())
            text(c,"CLOCK TOWER  $now     LIGHTHOUSE  $pct%",w/2,h-bottom+49f,14f)
            text(c,"✦  NAVIGATE THE SEVEN SEAS  ✦",w/2,h-bottom+69f,11f)
            text(c,if(prefs.getBoolean("music",false))"♫ MUSIC ON" else "♫ MUSIC OFF",w*.2f,h-20f,14f)
            text(c,if(selected>=0)"✎ EDIT BUILDINGS" else "☷ ALL APPS",w*.72f,h-20f,14f)
            if(selected>=0)text(c,"‹ BACK",w*.13f,header*.83f,14f)
            // Compass rose overlay
            val compassX=w-45f;val compassY=mapTop+53f
            paint.color=0xAAEED9A6.toInt();c.drawCircle(compassX,compassY,33f,paint)
            paint.color=ink;paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;c.drawCircle(compassX,compassY,33f,paint);paint.style=Paint.Style.FILL
            line(c,compassX,compassY-25f,compassX,compassY+25f)
            line(c,compassX-25f,compassY,compassX+25f,compassY)
            text(c,"N",compassX,compassY-11f,12f)
            if(android.os.SystemClock.uptimeMillis()-frameTime>45L){frameTime=android.os.SystemClock.uptimeMillis();postInvalidateDelayed(80)}
        }
        private fun select(i:Int) {
            selected=i
            if(i<0){targetZoom=1f;panX=0f;panY=0f}
            else {
                targetZoom=2.55f
                panX=(width/2-width*islands[i].first)*targetZoom
                val header=96f*resources.displayMetrics.density
                val mapH=height-header-94f*resources.displayMetrics.density
                panY=(header+mapH/2-(header+mapH*islands[i].second))*targetZoom
            }
            invalidate()
        }
        fun goBack(){show3D()}
        fun openDistrict(i:Int){select(i)}
        fun showAppDrawer(){showAllApps()}
        override fun onTouchEvent(e:MotionEvent):Boolean {
            val x=e.x;val y=e.y;val h=height.toFloat();val w=width.toFloat()
            when(e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {downX=x;downY=y;moved=false;return true}
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if(e.pointerCount>=2)lastDistance=hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1))
                    moved=true;return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if(e.pointerCount>=2) {
                        val d=hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1))
                        if(lastDistance>0) {targetZoom=(targetZoom*d/lastDistance).coerceIn(1f,4f);zoom=targetZoom}
                        lastDistance=d
                    } else if(abs(x-downX)+abs(y-downY)>12f) {
                        moved=true
                        panX+=(x-downX);panY+=(y-downY)
                        downX=x;downY=y
                    }
                    invalidate();return true
                }
                MotionEvent.ACTION_UP -> {
                    if(!moved) {
                        if(y>h-68f*resources.displayMetrics.density) {
                            if(x<w*.43f)toggleMusic() else if(selected>=0)chooseApps(selected) else showAllApps()
                        } else if(selected>=0 && y<96f*resources.displayMetrics.density && x<w*.3f)goBack()
                        else if(y>96f*resources.displayMetrics.density && y<h-94f*resources.displayMetrics.density) {
                            val header=96f*resources.displayMetrics.density
                            val mapH=h-header-94f*resources.displayMetrics.density
                            val cx=w/2;val cy=header+mapH/2
                            val ux=(x-cx-panX)/zoom+cx
                            val uy=(y-cy-panY)/zoom+cy
                            if(selected<0) {
                                val index=islands.indices.minByOrNull { hypot(ux-w*islands[it].first,uy-(header+mapH*islands[it].second)) } ?: 0
                                val dist=hypot(ux-w*islands[index].first,uy-(header+mapH*islands[index].second))
                                if(dist<min(w,mapH)*.25f)select(index)
                            } else {
                                val ix=selected;val centerIslandX=w*islands[ix].first;val centerIslandY=header+mapH*islands[ix].second
                                val radius=min(w,mapH)*.19f
                                val col=round((ux-centerIslandX)/(radius*.68f)).toInt()+1
                                val row=round((uy-(centerIslandY-radius*.34f))/(radius*.29f)).toInt()
                                if(col in 0..2 && row in 0..5) {
                                    val index=row*3+col
                                    assigned(ix).getOrNull(index)?.let { launch(it) }
                                }
                            }
                        }
                    }
                    invalidate();return true
                }
            }
            return true
        }
        private fun showAllApps() {
            val labels=apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
            android.app.AlertDialog.Builder(this@MainActivity).setTitle("ALL APPS")
                .setItems(labels) { _,i -> launch(apps[i]) }.setNegativeButton("CLOSE",null).show()
        }
        override fun onDetachedFromWindow(){handler.removeCallbacks(tick);super.onDetachedFromWindow()}
    }
    private class SeaAudio {
        @Volatile private var running=false
        private var thread:Thread?=null
        fun start() {
            if(running)return
            running=true
            thread=Thread {
                val sampleRate=22050
                val min=AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)
                if(min<=0)return@Thread
                val track=AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(max(min,4096)).setTransferMode(AudioTrack.MODE_STREAM).build()
                val notes=doubleArrayOf(196.0,246.94,293.66,261.63,220.0,196.0,174.61,196.0)
                val data=ShortArray(2048)
                var t=0L
                try {
                    track.play()
                    while(running) {
                        for(i in data.indices) {
                            val time=t.toDouble()/sampleRate
                            val note=notes[((time/2.2).toInt())%notes.size]
                            val wave=sin(2*PI*note*time)*.45+sin(2*PI*note*.5*time)*.2+sin(2*PI*note*1.5*time)*.12
                            val fade=min(1.0,(time%2.2)*2)*min(1.0,(2.2-time%2.2)*2)
                            data[i]=(wave*fade*1900).toInt().coerceIn(-32768,32767).toShort()
                            t++
                        }
                        track.write(data,0,data.size)
                    }
                } catch(_:Exception){} finally {try{track.stop();track.release()}catch(_:Exception){}}
            }.also{it.start()}
        }
        fun stop(){running=false;thread?.interrupt();thread=null}
    }
}
