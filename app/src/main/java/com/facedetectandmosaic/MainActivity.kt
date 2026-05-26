package com.facedetectandmosaic

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var btnRecord: Button
    private lateinit var renderer: CameraGLRenderer

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    private val videoWidth = 1280
    private val videoHeight = 720

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 简易动态布局
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        btnRecord = Button(this).apply { text = "开始录制" }
        glSurfaceView = GLSurfaceView(this)
        root.addView(btnRecord)
        root.addView(glSurfaceView)
        setContentView(root)

        // 1. 初始化配置 GLSurfaceView
        glSurfaceView.setEGLContextClientVersion(2)
        renderer = CameraGLRenderer(glSurfaceView)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        // 当 GL 线程在底层就绪并创建好外部纹理时，回调触发 CameraX 启动
        renderer.onSurfaceTextureReady = { surfaceTexture ->
            startCameraX(surfaceTexture)
        }

        btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecordingInternal()
                btnRecord.text = "开始录制"
            } else {
                if (checkPermissions()) {
                    startRecordingInternal()
                    btnRecord.text = "停止录制"
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 100)
                }
            }
        }

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 100)
        }
    }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    // 在 MainActivity 类中定义两个变量，用于记录真实分辨率
    private var mRealVideoWidth = 1280
    private var mRealVideoHeight = 720

    private fun startCameraX(surfaceTexture: android.graphics.SurfaceTexture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 允许相机寻找最接近 720p 的分辨率
            val strategy = ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
            val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(strategy).build()

            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()

            preview.setSurfaceProvider { surfaceRequest ->
                // 【核心安全举措】直接获取 CameraX 根据硬件算出来的、绝对合法的真实分辨率
                mRealVideoWidth = surfaceRequest.resolution.width
                mRealVideoHeight = surfaceRequest.resolution.height

                // 让外部纹理的缓存尺寸与相机严格 1:1 对齐
                surfaceTexture.setDefaultBufferSize(mRealVideoWidth, mRealVideoHeight)

                val surface = android.view.Surface(surfaceTexture)
                surfaceRequest.provideSurface(surface, ContextCompat.getMainExecutor(this)) {
                    surface.release()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRecordingInternal() {
        val outputFile = File(getExternalFilesDir(null), "green_clip_${System.currentTimeMillis()}.mp4")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE) // 必须声明使用 Surface 录制
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(mRealVideoWidth, mRealVideoHeight)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(4 * 1024 * 1024)
            prepare()
        }

        val inputSurface = mediaRecorder!!.surface

        // 1. 启动录制器
        mediaRecorder?.start()
        isRecording = true

        // 2. 硬件缓冲沉淀：给 Stagefright 底层框架 100 毫秒初始化队列，杜绝 -22
        try { Thread.sleep(100) } catch (e: Exception) {}

        // 3. 异步送入 OpenGL 初始化专属渲染管线
        glSurfaceView.queueEvent {
            renderer.startRecording(inputSurface, mRealVideoWidth, mRealVideoHeight)
        }

        Toast.makeText(this, "正在录制录像...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecordingInternal() {
        if (!isRecording) return
        isRecording = false

        val signalLock = java.lang.Object()

        // 1. 先让 GL 线程切断画面分发并销毁本地录制 EGLSurface
        synchronized(signalLock) {
            glSurfaceView.queueEvent {
                synchronized(signalLock) {
                    renderer.stopRecording()
                    signalLock.notifyAll() // 唤醒主线程
                }
            }
            try {
                signalLock.wait(1000) // 主线程最多等一秒，让 GL 线程先退场
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        // 2. 这时候 GL 线程绝对不会再往里面画画了，MediaRecorder 可以安全功成身退
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        }
        Toast.makeText(this, "视频已录制并成功封装！", Toast.LENGTH_SHORT).show()
    }
}