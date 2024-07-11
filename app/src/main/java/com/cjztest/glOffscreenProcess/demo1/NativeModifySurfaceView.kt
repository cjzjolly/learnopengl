package com.cjztest.glOffscreenProcess.demo1

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.opengldecoder.jnibridge.JniBridge
import kotlin.random.Random

class NativeModifySurfaceView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private lateinit var mSurfaceHolder: SurfaceHolder

    inner class TestThread : Thread() {
        override fun run() {
            while(this@NativeModifySurfaceView.isAttachedToWindow) {
                JniBridge.drawToSurface(holder.surface
                        , (0xFF000000.toInt()
                        or (Random.nextFloat() * 255f).toInt()
                        or ((Random.nextFloat() * 255f).toInt() shl  8)
                        or ((Random.nextFloat() * 255f).toInt() shl 16)))
                sleep(16)
            }
        }
    }

    init {
        mSurfaceHolder = holder
        mSurfaceHolder.addCallback(this)
        mSurfaceHolder.setFormat(PixelFormat.RGBA_8888)
        isFocusable = true
        setFocusableInTouchMode(true)
    }

    private var mTestThread: TestThread ?= null

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (mTestThread == null) {
            mTestThread = TestThread()
        }
        mTestThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

}