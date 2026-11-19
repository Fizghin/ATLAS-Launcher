package com.atlas.launcher

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/** Real GPU-rendered low-poly archipelago. All models are original procedural meshes. */
class Atlas3DView(context: Context, private val onIsland: (Int) -> Unit) : GLSurfaceView(context) {
    private val scene = WorldRenderer()
    private var x0 = 0f
    private var y0 = 0f
    private var lastDistance = 0f
    init {
        setEGLContextClientVersion(2)
        setRenderer(scene)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { x0=e.x; y0=e.y; return true }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (e.pointerCount>1) lastDistance=hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if(e.pointerCount>1) {
                    val d=hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1))
                    if(lastDistance>0) scene.distance=(scene.distance*lastDistance/d).coerceIn(12f,34f)
                    lastDistance=d
                } else {
                    scene.yaw+=(e.x-x0)*.004f
                    x0=e.x; y0=e.y
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if(abs(e.x-x0)+abs(e.y-y0)<35f) {
                    val index=(if(e.x<width/2) 0 else 1)+(if(e.y<height/2) 0 else 2)
                    onIsland(index)
                }
                return true
            }
        }
        return true
    }

    private class WorldRenderer : Renderer {
        @Volatile var distance=23f
        @Volatile var yaw=.15f
        private var program=0
        private var position=0
        private var matrixUniform=0
        private var colorUniform=0
        private val projection=FloatArray(16)
        private val view=FloatArray(16)
        private val model=FloatArray(16)
        private val vp=FloatArray(16)
        private val mvp=FloatArray(16)
        private var time=0f
        private val cube=floatArrayOf(
            -1f,0f,-1f, 1f,0f,-1f, 1f,1f,-1f, -1f,0f,-1f, 1f,1f,-1f, -1f,1f,-1f,
            -1f,0f,1f, 1f,0f,1f, 1f,1f,1f, -1f,0f,1f, 1f,1f,1f, -1f,1f,1f,
            -1f,0f,-1f,-1f,0f,1f,-1f,1f,1f, -1f,0f,-1f,-1f,1f,1f,-1f,1f,-1f,
            1f,0f,-1f,1f,0f,1f,1f,1f,1f, 1f,0f,-1f,1f,1f,1f,1f,1f,-1f,
            -1f,1f,-1f,1f,1f,-1f,1f,1f,1f,-1f,1f,-1f,1f,1f,1f,-1f,1f,1f
        )
        private val vertices=ByteBuffer.allocateDirect(cube.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(cube);position(0) }
        private val centers=arrayOf(floatArrayOf(-5f,-5f),floatArrayOf(5f,-5f),floatArrayOf(-5f,5f),floatArrayOf(5f,5f))
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(.12f,.37f,.44f,1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            val vert="uniform mat4 uMVP; attribute vec3 aPos; varying float vShade; void main(){ gl_Position=uMVP*vec4(aPos,1.0); vShade=0.80+0.20*aPos.y; }"
            val frag="precision mediump float; uniform vec4 uColor; varying float vShade; void main(){gl_FragColor=vec4(uColor.rgb*vShade,uColor.a);}"
            val v=shader(GLES20.GL_VERTEX_SHADER,vert);val f=shader(GLES20.GL_FRAGMENT_SHADER,frag)
            program=GLES20.glCreateProgram()
            GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f)
            GLES20.glLinkProgram(program)
            position=GLES20.glGetAttribLocation(program,"aPos")
            matrixUniform=GLES20.glGetUniformLocation(program,"uMVP")
            colorUniform=GLES20.glGetUniformLocation(program,"uColor")
        }
        private fun shader(kind:Int,src:String):Int {
            val id=GLES20.glCreateShader(kind)
            GLES20.glShaderSource(id,src);GLES20.glCompileShader(id)
            return id
        }
        override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int) {
            GLES20.glViewport(0,0,w,h)
            Matrix.perspectiveM(projection,0,45f,w.toFloat()/h.coerceAtLeast(1),.1f,100f)
        }
        private fun box(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float) {
            Matrix.setIdentityM(model,0)
            Matrix.translateM(model,0,x,y,z)
            Matrix.scaleM(model,0,sx,sy,sz)
            Matrix.multiplyMM(mvp,0,vp,0,model,0)
            GLES20.glUniformMatrix4fv(matrixUniform,1,false,mvp,0)
            GLES20.glUniform4f(colorUniform,r,g,b,1f)
            vertices.position(0)
            GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT,false,0,vertices)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,cube.size/3)
        }
        override fun onDrawFrame(gl:GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            GLES20.glUseProgram(program)
            time+=.009f
            val eyeX=sin(yaw)*distance
            val eyeZ=cos(yaw)*distance
            Matrix.setLookAtM(view,0,eyeX,distance*.73f,eyeZ,0f,0f,0f,0f,1f,0f)
            Matrix.multiplyMM(vp,0,projection,0,view,0)
            // Deep sea surface with moving wave stripes
            box(0f,-1.15f,0f,18f,.05f,18f,.15f,.46f,.54f)
            for(i in -8..8)for(j in -8..8) {
                if((i+j)%3==0)box(i*2f,-1.08f+sin(time+i+j)*.025f,j*2f,.48f,.01f,.035f,.25f,.58f,.61f)
            }
            for((n,p) in centers.withIndex()) {
                val x=p[0];val z=p[1]
                // layered beaches, grassy slopes and village platform
                box(x,-.98f,z,3.55f,.35f,3.35f,.76f,.65f,.40f)
                box(x,-.62f,z,3.18f,.30f,3.0f,.34f,.52f,.33f)
                box(x,-.28f,z,2.6f,.18f,2.45f,.45f,.56f,.35f)
                // village centerpiece with roofs and chimneys
                val height=if(n==3)2.7f else if(n==2)1.8f else 1.25f
                box(x,.0f,z, .75f,height,.8f,.58f,.39f,.24f)
                box(x,height,z,.92f,.35f,.94f,.37f,.19f,.14f)
                box(x,height+.35f,z,.55f,.25f,.55f,.30f,.16f,.12f)
                if(n==3)box(x,height+.65f,z,.22f,.8f,.22f,.71f,.60f,.39f)
                // distinct app buildings, each with door and contrasting roof
                for(i in 0..7) {
                    val a=i*PI*2/8
                    val bx=x+cos(a).toFloat()*1.65f
                    val bz=z+sin(a).toFloat()*1.5f
                    box(bx,-.12f,bz,.34f,.46f,.34f,.67f,.47f,.28f)
                    box(bx,.34f,bz,.43f,.19f,.43f,.41f,.22f,.15f)
                    box(bx,-.10f,bz+.36f,.12f,.28f,.04f,.27f,.16f,.12f)
                }
                // small pines and palms
                for(i in 0..5) {
                    val a=i*PI*2/6+.4
                    val tx=x+cos(a).toFloat()*2.25f
                    val tz=z+sin(a).toFloat()*2.12f
                    box(tx,-.12f,tz,.08f,.45f,.08f,.37f,.25f,.15f)
                    box(tx,.34f,tz,.27f,.46f,.27f,.18f,.39f,.25f)
                    box(tx,.78f,tz,.17f,.32f,.17f,.16f,.34f,.22f)
                }
                // wood pier reaching into water
                box(x+3.4f,-.79f,z,1.5f,.09f,.43f,.47f,.29f,.16f)
                for(j in 0..3)box(x+2.7f+j*.5f,-1f,z+.33f,.07f,.44f,.07f,.39f,.25f,.15f)
            }
        }
    }
}
