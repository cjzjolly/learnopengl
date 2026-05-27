package com.facedetectandmosaic

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.util.Size
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
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

    // 在 MainActivity 类中定义两个变量，用于记录真实分辨率
    private var mRealVideoWidth = 1280
    private var mRealVideoHeight = 720

    private val mFrameRate = 10

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



    private fun startCameraX(surfaceTexture: android.graphics.SurfaceTexture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val strategy = ResolutionStrategy(Size(960, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
            val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(strategy).build()

//            val resolutionSelector = ResolutionSelector.Builder()
////                .setAllowedResolutionModes(ResolutionSelector.ALLOWED_RESOLUTION_MODES_SPECIFIC)
//                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
//                .build()

            // 1. 创建 Preview.Builder
            val previewBuilder = Preview.Builder()
                .setResolutionSelector(resolutionSelector)

            // 2. 使用 Camera2Interop 强制注入底层帧率限制 [30, 30]
            // 这样可以逼迫硬件无论是亮光还是暗光，都死死锁在 30 帧
            val ext = Camera2Interop.Extender(previewBuilder)
            ext.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(mFrameRate, mFrameRate) // 最小值 30，最大值 30
            )

            val preview = previewBuilder.build()

            preview.setSurfaceProvider { surfaceRequest ->
                mRealVideoWidth = surfaceRequest.resolution.width
                mRealVideoHeight = surfaceRequest.resolution.height
                Log.e("cjztest", "surfaceRequest: mRealVideoWidth: $mRealVideoWidth, mRealVideoHeight: $mRealVideoHeight")
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
            setVideoFrameRate(mFrameRate)
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