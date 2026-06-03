package com.facedetectandmosaic
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

class CameraGLRenderer(private val glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    var surfaceTexture: SurfaceTexture? = null
        private set
    var onSurfaceTextureReady: ((SurfaceTexture) -> Unit)? = null

    private var oesTextureId = -1
    private var fboId = -1
    private var fboTextureId = -1

    private var oesProgram = 0
    private var shader2DProgram = 0

    private val transformMatrix = FloatArray(16)
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var textureBuffer: FloatBuffer

    // 录制相关独立 EGL 环境
    @Volatile private var isRecording = false
    private var recordSurface: Surface? = null
    private var recordEglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var recordEglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var recordEglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var videoWidth = 1280
    private var videoHeight = 720
    private var screenWidth = 0
    private var screenHeight = 0

    /**马赛克人脸相关**/
    private var uFaceRectsHandle = 0
    private var uFaceCountHandle = 0
    private var uMosaicBlockSizeHandle = 0

    private val MAX_FACES = 5

    // 用于存储传递给 Shader 的 FloatArray (长度必须是 MAX_FACES * 4)
    private val faceRectsArray = FloatArray(MAX_FACES * 4)
    private var currentFaceCount = 0

    // OES 顶点着色器（带矩阵转换）
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

    // 滤镜片元着色器：留绿
    private val fragmentOesShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        
        // 1. 定义最大支持的人脸数量 (可根据需求调整，通常 5-10 足够)
        #define MAX_FACES 5 
        
        // 2. 外部传入的归一化矩形数组 (x=minU, y=minV, z=maxU, w=maxV)
        uniform vec4 uFaceRects[MAX_FACES]; 
        uniform int uFaceCount; // 实际检测到的人脸数量
        
        // 3. 归一化的马赛克块大小 (例如 vec2(0.02, 0.03))
        uniform vec2 uMosaicBlockSize; 
        
        void main() {
            vec2 uv = vTextureCoord;
            bool applyMosaic = false;
        
            // 4. 遍历所有人脸矩形，判断当前像素是否在其中
            // 注意：GLSL ES 2.0 要求 for 循环的边界必须是常量，所以必须循环 MAX_FACES 次
            for (int i = 0; i < MAX_FACES; i++) {
                if (i < uFaceCount) {
                    vec4 rect = uFaceRects[i];
                    // 判断 UV 坐标是否在矩形内
                    if (uv.x >= rect.x && uv.x <= rect.z && uv.y >= rect.y && uv.y <= rect.w) {
                        applyMosaic = true;
                        break; // 只要在任意一个矩形内，就标记并跳出循环
                    }
                }
            }
        
            // 5. 核心：高性能马赛克算法 (坐标离散化)
            if (applyMosaic) {
                // 将连续坐标除以块大小 -> 向下取整对齐到网格 -> 加 0.5 采样网格中心 -> 乘回块大小
                vec2 grid = floor(uv / uMosaicBlockSize);
                uv = (grid + 0.5) * uMosaicBlockSize;
            }
        
            // 6. 最终采样 (无论是否马赛克，都只采样 1 次，性能拉满)
            gl_FragColor = texture2D(sTexture, uv);
        }
    """.trimIndent()

    // 2D 顶点着色器（无矩阵转换，因为 FBO 出来的已经是标准正向正方形纹理）
    private val vertex2DShaderCode = """
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTextureCoord;
        void main() {
            gl_Position = aPosition;
            vTextureCoord = aTextureCoord.xy;
        }
    """.trimIndent()

    private val fragment2DShaderCode = """
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform sampler2D sTexture;
        void main() {
            gl_FragColor = texture2D(sTexture, vTextureCoord);
        }
    """.trimIndent()

    init {
        val cubeCoords = floatArrayOf(-1f, -1f, 0f, 1f, -1f, 0f, -1f, 1f, 0f, 1f, 1f, 0f)
        vertexBuffer = ByteBuffer.allocateDirect(cubeCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(cubeCoords).position(0) }

        // 渲染到 FBO 时需要上下翻转纹理坐标，校正相机镜像
        val textureCoords = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
        textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(textureCoords).position(0) }
    }

    override fun onSurfaceCreated(gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        // 编译两套 Program
        oesProgram = createProgram(vertexShaderCode, fragmentOesShaderCode)
        shader2DProgram = createProgram(vertex2DShaderCode, fragment2DShaderCode)

        // 创建 OES 纹理
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())

        surfaceTexture = SurfaceTexture(oesTextureId).apply { setOnFrameAvailableListener(this@CameraGLRenderer) }
        glSurfaceView.post { onSurfaceTextureReady?.invoke(surfaceTexture!!) }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.e("cjztest", "CameraGLRenderer, onSurfaceChanged, width:$width, height:$height")
        screenWidth = width
        screenHeight = height
        // 动态根据屏幕/相机尺寸初始化 FBO 缓冲区
        initFBO(width, height)
    }

    // 外部调用：更新人脸数据 (传入的是基于原始图像的归一化 UV 坐标)
    fun updateFaceRects(uvRects: List<RectF>) {
        currentFaceCount = minOf(uvRects.size, MAX_FACES)

        // 将 List<FloatArray> 展平为一维 FloatArray
        for (i in 0 until MAX_FACES) {
            if (i < currentFaceCount) {
                val rect = uvRects[i] // [minU, minV, maxU, maxV]
                faceRectsArray[i * 4 + 0] = rect.top / screenWidth.toFloat()
                faceRectsArray[i * 4 + 1] = rect.left / screenWidth.toFloat()
                faceRectsArray[i * 4 + 2] = rect.bottom / screenWidth.toFloat()
                faceRectsArray[i * 4 + 3] = rect.right / screenWidth.toFloat()
            } else {
                // 填充无效数据，防止脏数据干扰
                faceRectsArray[i * 4 + 0] = -1.0f
                faceRectsArray[i * 4 + 1] = -1.0f
                faceRectsArray[i * 4 + 2] = -1.0f
                faceRectsArray[i * 4 + 3] = -1.0f
            }
        }
    }

    private fun initFBO(w: Int, h: Int) {
        if (fboId != -1) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            GLES20.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
        }

        val fbos = IntArray(1)
        GLES20.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]

        val texs = IntArray(1)
        GLES20.glGenTextures(1, texs, 0)
        fboTextureId = texs[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTextureId, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        val surfaceTex = surfaceTexture ?: return
        synchronized(this) {
            surfaceTex.updateTexImage()
            surfaceTex.getTransformMatrix(transformMatrix)
        }

        if (fboId == -1) return

        // 步骤 1：全硬件留绿滤镜处理 -> 先渲染到离屏 FBO 中
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
        drawOesToFbo()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        // 步骤 2：将 FBO 处理好的绿色画面投递到普通手机屏幕
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
        drawFboToScreen()

        // 步骤 3：如果正在录制，利用专属独立 EGL 纯离屏渲染投递至 MediaRecorder 录制表面
        if (isRecording && recordEglSurface != EGL14.EGL_NO_SURFACE) {
            // 保存当前屏幕环境
            val oldDisplay = EGL14.eglGetCurrentDisplay()
            val oldDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
            val oldReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
            val oldContext = EGL14.eglGetCurrentContext()

            // 切换到录制环境
            EGL14.eglMakeCurrent(recordEglDisplay, recordEglSurface, recordEglSurface, recordEglContext)
            GLES20.glViewport(0, 0, videoWidth, videoHeight)
            drawFboToScreen() // 将绿色画面复刻一份塞入编码器
            EGL14.eglSwapBuffers(recordEglDisplay, recordEglSurface)

            // 还原主屏环境，防止 GLSurfaceView 崩溃
            EGL14.eglMakeCurrent(oldDisplay, oldDrawSurface, oldReadSurface, oldContext)
        }
    }

    private fun drawOesToFbo() {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(oesProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

        val posHandle = GLES20.glGetAttribLocation(oesProgram, "aPosition")
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
        GLES20.glEnableVertexAttribArray(posHandle)

        val texHandle = GLES20.glGetAttribLocation(oesProgram, "aTextureCoord")
        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)
        GLES20.glEnableVertexAttribArray(texHandle)

        val matrixHandle = GLES20.glGetUniformLocation(oesProgram, "uSTMatrix")
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, transformMatrix, 0)

        // 获取 打马赛克相关的Uniform 句柄
        uFaceRectsHandle = GLES20.glGetUniformLocation(oesProgram, "uFaceRects")
        uFaceCountHandle = GLES20.glGetUniformLocation(oesProgram, "uFaceCount")
        uMosaicBlockSizeHandle = GLES20.glGetUniformLocation(oesProgram, "uMosaicBlockSize")

        // 传递人脸矩形数组
        GLES20.glUniform4fv(uFaceRectsHandle, MAX_FACES, faceRectsArray, 0)
        GLES20.glUniform1i(uFaceCountHandle, currentFaceCount)

        // 传递马赛克块大小 (假设想要 20x20 像素的马赛克，图像是 1280x720)
        // 归一化大小 = 像素大小 / 图像宽高
        val blockW = 20.0f / screenWidth.toFloat()
        val blockH = 20.0f / screenHeight.toFloat()
        GLES20.glUniform2f(uMosaicBlockSizeHandle, blockW, blockH)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun drawFboToScreen() {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(shader2DProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)

        val posHandle = GLES20.glGetAttribLocation(shader2DProgram, "aPosition")
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
        GLES20.glEnableVertexAttribArray(posHandle)

        val texHandle = GLES20.glGetAttribLocation(shader2DProgram, "aTextureCoord")
        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)
        GLES20.glEnableVertexAttribArray(texHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        glSurfaceView.requestRender()
    }

    // 核心重构：为录制 Surface 创建独立隔离的全新 WindowSurface 环境
    fun startRecording(surface: Surface, width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        recordSurface = surface

        val sharedContext = EGL14.eglGetCurrentContext()
        recordEglDisplay = EGL14.eglGetCurrentDisplay()

        // 强行指定独立标志位，允许录制专用的 Buffer 标记
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            0x3142, 1, // 核心：显式通知系统这个底层是拿来录像的
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = intArrayOf(0)
        EGL14.eglChooseConfig(recordEglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        // 与主线程共享 Context，实现免内存拷贝复用 FBO 纹理
        recordEglContext = EGL14.eglCreateContext(recordEglDisplay, configs[0], sharedContext, ctxAttribs, 0)

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        recordEglSurface = EGL14.eglCreateWindowSurface(recordEglDisplay, configs[0], recordSurface, surfaceAttribs, 0)

        isRecording = true
    }

    fun stopRecording() {
        isRecording = false
        if (recordEglSurface != EGL14.EGL_NO_SURFACE) {
            GLES20.glFinish()
            EGL14.eglDestroySurface(recordEglDisplay, recordEglSurface)
            EGL14.eglDestroyContext(recordEglDisplay, recordEglContext)
            recordEglSurface = EGL14.EGL_NO_SURFACE
            recordEglContext = EGL14.EGL_NO_CONTEXT
            recordEglDisplay = EGL14.EGL_NO_DISPLAY
        }
        recordSurface = null
    }

    private fun createProgram(vertex: String, fragment: String): Int {
        val vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also { GLES20.glShaderSource(it, vertex); GLES20.glCompileShader(it) }
        val fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also { GLES20.glShaderSource(it, fragment); GLES20.glCompileShader(it) }
        return GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vShader)
            GLES20.glAttachShader(this, fShader)
            GLES20.glLinkProgram(this)
        }
    }
}