package com.cjztest.glDCTDenoise

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle

class MainActivity : Activity() {
    val s = android.opengl.GLSurfaceView(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val render = DCTRGBRenderer(this).apply {
//            setInputBitmap(Bitma.)
        }
        s.setEGLContextClientVersion(3)
        s.setRenderer(render)
        s.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY;
    }

    override fun onResume() {
        super.onResume()
        s.requestRender()
    }

}