package com.physicEffect.threeDWave

import android.content.Context
import android.opengl.GLSurfaceView

class ThreeDimensionWaveGLAnimate(context: Context) : GLSurfaceView(context) {
    private val renderer: MyGLRenderer

    init {
        // 指定 OpenGL ES 2.0
        setEGLContextClientVersion(3)

        renderer = MyGLRenderer()
        setRenderer(renderer)

        renderMode = RENDERMODE_CONTINUOUSLY
    }


}