package com.cjztest.glOffscreenProcess.demo0

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.DisplayMetrics
import android.view.WindowManager

/**以指定的大小产生EGLContext、EGLDisplay对象，并把OpenGL ES渲染管线的内容输出到EGLDisplay中*/
class EGLMaker : Object {
    protected var mEGLDisplay: EGLDisplay? = null
    protected var mEGLConfig: EGLConfig? = null
    protected var mEGLContext: EGLContext? = null
    protected var mEGLSurface: EGLSurface? = null
    protected var mEglStatus: EglStatus = EglStatus.INVALID
    protected var mContext: Context? = null
    private var mRenderer: IRenderer? = null
    protected var mWidth: Int = 0
    protected var mHeight: Int = 0
    var mIsCreated = false

    // EGLConfig参数
    private val mEGLConfigAttrs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE,
            EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
    )

    enum class EglStatus {
        INVALID, INITIALIZED, CREATED, CHANGED, DRAW;
        val isValid: Boolean
            get() = this != INVALID
    }

    // 渲染器接口
    interface IRenderer {
        fun onSurfaceCreated()
        fun onSurfaceChanged(width: Int, height: Int)
        fun onDrawFrame()
    }

    fun setRenderer(renderer: IRenderer) {
        mRenderer = renderer
    }

    // EGLContext参数
    private val mEGLContextAttrs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)

    /**以当前window宽高创建EGLDisplay**/
    constructor(context: Context) {
        mContext = context
        val mWindowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getRealMetrics(displayMetrics)
        mWidth = displayMetrics.widthPixels
        mHeight = displayMetrics.heightPixels
        createEGLEnv()
    }

    /**
     * 以指定宽高创建EGLDisplay**/
    constructor(context: Context, width: Int, height: Int) {
        mContext = context
        mWidth = width
        mHeight = height
        createEGLEnv()
    }

    // 创建EGL环境
    fun createEGLEnv() : Boolean {
        if (mIsCreated) {
            return true
        }
        /**/
        //1.创建EGLDisplay
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val versions = IntArray(2)
        EGL14.eglInitialize(mEGLDisplay, versions, 0, versions, 1)
        // 2.创建EGLConfig
        val configs: Array<EGLConfig?> = arrayOfNulls(1)
        val configNum = IntArray(1)
        EGL14.eglChooseConfig(mEGLDisplay, mEGLConfigAttrs, 0, configs, 0, 1, configNum, 0) //获取1个EGL配置，因为这个例子只需要一个
        if (configNum[0] > 0) {
            mEGLConfig = configs[0]
        } else {
            return false
        }
        // 3.创建EGLContext
        if (mEGLConfig != null) {
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, EGL14.EGL_NO_CONTEXT, mEGLContextAttrs, 0)
        }

        // 4.创建EGLSurface
        if (mEGLContext != null && mEGLContext != EGL14.EGL_NO_CONTEXT) {
            val eglSurfaceAttrs = intArrayOf(EGL14.EGL_WIDTH, mWidth, EGL14.EGL_HEIGHT, mHeight, EGL14.EGL_NONE) //以传入的宽高作为eglSurface
            mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, eglSurfaceAttrs, 0)
        }
        // 5.绑定EGLSurface和EGLContext到显示设备（EGLDisplay），但这个EGLDisplay的内容没有和View绑定，所以并不会直接显示
        if (mEGLSurface != null && mEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
            mEglStatus = EglStatus.INITIALIZED
        }
        mIsCreated = true
        return true
    }

    // 销毁EGL环境
    fun destroyEGLEnv() {
        if (!mIsCreated) {
            return
        }
        // 与显示设备解绑
        EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        // 销毁 EGLSurface
        EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
        // 销毁EGLContext
        EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
        // 销毁EGLDisplay(显示设备)
        EGL14.eglTerminate(mEGLDisplay)
        mEGLContext = null
        mEGLSurface = null
        mEGLDisplay = null
        mIsCreated = false
    }

    override fun finalize() {
        super.finalize()
        destroyEGLEnv()
    }

    // 请求渲染
    fun requestRender() {
        if (!mEglStatus.isValid) {
            return
        }
        if (mEglStatus == EglStatus.INITIALIZED) {
            mRenderer?.onSurfaceCreated()
            mEglStatus = EglStatus.CREATED
        }
        if (mEglStatus == EglStatus.CREATED) {
            mRenderer?.onSurfaceChanged(mWidth, mHeight)
            mEglStatus = EglStatus.CHANGED
        }
        if (mEglStatus == EglStatus.CHANGED || mEglStatus == EglStatus.DRAW) {
            mRenderer?.onDrawFrame()
            mEglStatus = EglStatus.DRAW
        }
    }

    fun getWidth() : Int {
        return mWidth
    }

    fun getHeight() : Int {
        return mHeight
    }
    
}