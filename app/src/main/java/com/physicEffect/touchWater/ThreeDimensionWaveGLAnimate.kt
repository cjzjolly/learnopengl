package com.physicEffect.touchWater

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.hypot

class ThreeDimensionWaveGLAnimate(context: Context) : GLSurfaceView(context) {
    private val renderer: WaveMeshRenderer

    init {
        setEGLContextClientVersion(3) // GLES 3.x (we use 3.1)
        renderer = WaveMeshRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    // Track multi-touch with pointer ids -> last position, to compute drag velocity
    private val lastX = mutableMapOf<Int, Float>()
    private val lastY = mutableMapOf<Int, Float>()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pidIndex = event.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val id = event.getPointerId(pidIndex)
                val x = event.getX(pidIndex) / width.toFloat()
                val y = 1f - event.getY(pidIndex) / height.toFloat()
                lastX[id] = x
                lastY[id] = y
                queueEvent { renderer.setTouchActive(id, x, y, true, 0f) }
            }
            MotionEvent.ACTION_MOVE -> {
                // multiple pointers
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i) / width.toFloat()
                    val y = 1f - event.getY(i) / height.toFloat()
                    val lx = lastX[id] ?: x
                    val ly = lastY[id] ?: y
                    val dx = x - lx
                    val dy = y - ly
                    val speed = hypot(dx.toDouble(), dy.toDouble()).toFloat() * 60f // scaled per second
                    lastX[id] = x
                    lastY[id] = y
                    queueEvent { renderer.setTouchActive(id, x, y, true, speed) }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val id = event.getPointerId(pidIndex)
                lastX.remove(id); lastY.remove(id)
                queueEvent { renderer.setTouchActive(id, 0f, 0f, false, 0f) }
            }
        }
        return true
    }
}
