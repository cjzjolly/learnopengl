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

    private fun startCameraX(surfaceTexture: android.graphics.SurfaceTexture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 设定贴近录制分辨率的策略以优化性能
            val strategy = ResolutionStrategy(Size(videoWidth, videoHeight), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
            val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(strategy).build()

            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()

            preview.setSurfaceProvider { surfaceRequest ->
                surfaceTexture.setDefaultBufferSize(surfaceRequest.resolution.width, surfaceRequest.resolution.height)
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
            setAudioSource(MediaRecorder.AudioSource.MIC)        // 引入环境音
            setVideoSource(MediaRecorder.VideoSource.SURFACE)    // 关键点：接受来自 OpenGL 的 Surface 投递
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)     // H264 硬件硬编，功耗极低
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)      // 音频 AAC 编码
            setVideoSize(videoWidth, videoHeight)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(3 * 1024 * 1024)             // 3Mbps 码率均衡功耗与画质
            prepare()
        }

        // 必须在 prepare 之后，start 之前获取录制 Surface
        val inputSurface = mediaRecorder!!.surface

        // 向 GL 渲染线程排队插入异步任务：创建录制表面
        glSurfaceView.queueEvent {
            renderer.startRecording(inputSurface, videoWidth, videoHeight)
        }

        mediaRecorder?.start()
        isRecording = true
        Toast.makeText(this, "录制保存至: ${outputFile.name}", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecordingInternal() {
        if (!isRecording) return

        // 渲染线程先断开并销毁录制表面，防止 MediaRecorder 停止后还有帧挤入导致 crash
        glSurfaceView.queueEvent {
            renderer.stopRecording()
        }

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace() // 规避用户极快点击导致的未收到帧异常
        }
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
        Toast.makeText(this, "录制完成并成功保存", Toast.LENGTH_SHORT).show()
    }
}