package com.cjztest.glOffscreenProcess.demo0

import android.graphics.Bitmap
import android.opengl.GLES30
import java.nio.ByteBuffer

class Renderer : EGLMaker.IRenderer {

    private var mContentBmp: Bitmap? = null
    private var mWidth : Int? = null
    private var mHeight : Int? = null
    override fun onSurfaceCreated() {

    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        mWidth = width
        mHeight = height
    }

    override fun onDrawFrame() {
        //todo 可能要先创建并绑定FBO
        GLES30.glClearColor(1f, 0f, 0f, 1f) //设定清理颜色
        // 将颜色缓存区设置为预设的颜色
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        //可以使用glMap....等来实现，代替低速度的readPixel

        //试试读出里面的像素，如果有颜色就代表这一阶段成功了
        if (mWidth != null && mHeight != null) {
            val byteBuffer = ByteBuffer.allocate(mWidth!! * mHeight!! * 4)
            GLES30.glReadPixels(0, 0, mWidth!!, mHeight!!, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, byteBuffer)
            if (mContentBmp == null || mContentBmp?.width != mWidth!! || mContentBmp?.height != mWidth!!) {
                mContentBmp = Bitmap.createBitmap(mWidth!!, mHeight!!, Bitmap.Config.ARGB_8888)
            }
            mContentBmp?.copyPixelsFromBuffer(byteBuffer)
        }
    }

    fun getBitmap() : Bitmap? {
        return mContentBmp
    }
}