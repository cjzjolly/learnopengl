package com.cjztest.glOffscreenProcess

import android.app.Activity
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout

class OffScreenGLDemoActivity : Activity() {

    private lateinit var mEGL: EGLMaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = LinearLayout(this)
        rootView.orientation = LinearLayout.VERTICAL

        val imageView = ImageView(this)


        mEGL = EGLMaker(this, 300, 300)
        mEGL.setRenderer(Renderer())
    }
}