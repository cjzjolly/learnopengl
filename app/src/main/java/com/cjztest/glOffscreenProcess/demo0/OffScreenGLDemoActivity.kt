package com.cjztest.glOffscreenProcess.demo0

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout

class OffScreenGLDemoActivity : Activity() {

    private lateinit var mEGL: EGLMaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = LinearLayout(this)
        rootView.orientation = LinearLayout.VERTICAL
        rootView.setBackgroundColor(Color.GRAY)

        val imageView = ImageView(this)
        imageView.layoutParams = ViewGroup.LayoutParams(300, 300)
        imageView.setBackgroundColor(Color.GREEN)

        val renderer = Renderer()
        mEGL = EGLMaker(this, 300, 300)
        mEGL.setRenderer(renderer)
        mEGL.requestRender()

        imageView.setImageBitmap(renderer.getBitmap())

        rootView.addView(imageView)

        setContentView(rootView)
    }
}