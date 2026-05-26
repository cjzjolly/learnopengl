package com.facedetectandmosaic

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

class CameraGLRenderer(private val glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    var surfaceTexture: SurfaceTexture? = null
        private set

    var onSurfaceTextureReady: ((SurfaceTexture) -> Unit)? = null

    private var textureId = -1
    private val transformMatrix = FloatArray(16)

    private var program = 0
    private var maPositionHandle = 0
    private var maTextureHandle = 0
    private var muSTMatrixHandle = 0

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var textureBuffer: FloatBuffer

    // 录制相关控制量
    @Volatile private var isRecording = false
    private var recordEGLSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var videoWidth = 1280
    private var videoHeight = 720
    private var screenWidth = 0
    private var screenHeight = 0

    private val vertexShaderCode = """
        uniform mat4 uSTMatrix;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTextureCoord;
        void main() {
            gl_Position = aPosition;
            vTextureCoord = (uSTMatrix * aTextureCoord).xy;
        }
    """.trimIndent()

    // 关键点：Fragment Shader 中只保留绿色通道 (color.g)
    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        void main() {
            vec4 color = texture2D(sTexture, vTextureCoord);
            gl_FragColor = vec4(0.0, color.g, 0.0, color.a);
        }
    """.trimIndent()

    init {
        val cubeCoords = floatArrayOf(
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
            1.0f,  1.0f, 0.0f
        )
        vertexBuffer = ByteBuffer.allocateDirect(cubeCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(cubeCoords).position(0) }

        val textureCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
        textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(textureCoords).position(0) }
    }

    override fun onSurfaceCreated(gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vertexShader)
            GLES20.glAttachShader(this, fragmentShader)
            GLES20.glLinkProgram(this)
        }

        maPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        maTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        muSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")

        // 生成外部 OES 纹理绑定到 Camera
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())

        surfaceTexture = SurfaceTexture(textureId).apply {
            setOnFrameAvailableListener(this@CameraGLRenderer)
        }

        Handler(Looper.getMainLooper()).post {
            onSurfaceTextureReady?.invoke(surfaceTexture!!)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        val surfaceTex = surfaceTexture ?: return
        synchronized(this) {
            surfaceTex.updateTexImage()
            surfaceTex.getTransformMatrix(transformMatrix)
        }

        // 拦截 GLSurfaceView 自动创建的当前 EGL 环境与显示表面
        val display = EGL14.eglGetCurrentDisplay()
        val context = EGL14.eglGetCurrentContext()
        val screenSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)

        // 1. 渲染到预览屏幕
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
        drawScene()

        // 2. 低功耗双输出：如果开启录制，直接无缝切换 EGL 表面画第二遍，直接塞入编码器
        if (isRecording && recordEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(display, recordEGLSurface, recordEGLSurface, context)
            GLES20.glViewport(0, 0, videoWidth, videoHeight)
            drawScene()
            EGL14.eglSwapBuffers(display, recordEGLSurface)

            // 必须切换回原来的屏幕表面，让 GLSurfaceView 内部自己去做主屏的 SwapBuffers
            EGL14.eglMakeCurrent(display, screenSurface, screenSurface, context)
        }
    }

    private fun drawScene() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
        GLES20.glEnableVertexAttribArray(maPositionHandle)

        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)
        GLES20.glEnableVertexAttribArray(maTextureHandle)

        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, transformMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        glSurfaceView.requestRender() // 仅在有新硬件帧时刷新，保证低功耗
    }

    fun startRecording(surface: Surface, width: Int, height: Int) {
        videoWidth = width
        videoHeight = height

        val display = EGL14.eglGetCurrentDisplay()
        val context = EGL14.eglGetCurrentContext()

        // 绝招：动态查询当前 GLSurfaceView 的 Config 保证完美匹配，防止部分机型 EGL_BAD_MATCH 闪退
        val configId = intArrayOf(0)
        EGL14.eglQueryContext(display, context, EGL14.EGL_CONFIG_ID, configId, 0)

        val attribList = intArrayOf(EGL14.EGL_CONFIG_ID, configId[0], EGL14.EGL_NONE)
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = intArrayOf(0)
        EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, numConfigs, 0)

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        recordEGLSurface = EGL14.eglCreateWindowSurface(display, configs[0], surface, surfaceAttribs, 0)
        isRecording = true
    }

    fun stopRecording() {
        isRecording = false
        if (recordEGLSurface != EGL14.EGL_NO_SURFACE) {
            val display = EGL14.eglGetCurrentDisplay()
            EGL14.eglDestroySurface(display, recordEGLSurface)
            recordEGLSurface = EGL14.EGL_NO_SURFACE
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}