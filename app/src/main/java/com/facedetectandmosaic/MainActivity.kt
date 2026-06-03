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
import androidx.camera.core.ImageAnalysis
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

    // 在 MainActivity 类中定义两个变量，用于记录真实分辨率
    private var mRealVideoWidth = 1280
    private var mRealVideoHeight = 720

    private val mFrameRate = Range(3, 30)

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
//                .setTargetRotation(android.view.Surface.ROTATION_90)

            // 2. 使用 Camera2Interop 强制注入底层帧率限制
            val ext = Camera2Interop.Extender(previewBuilder)
            ext.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                mFrameRate
            )

            val preview = previewBuilder.build()

            val imageAnalysis = ImageAnalysis.Builder()
//                .setTargetRotation(android.view.Surface.ROTATION_0) // 与 Preview 保持一致
                // 关键：只保留最新帧，丢弃处理不过来的旧帧，防止内存积压和延迟
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(720, 960))
                .build()


            preview.setSurfaceProvider { surfaceRequest ->
                mRealVideoWidth = surfaceRequest.resolution.width
//                mRealVideoHeight = surfaceRequest.resolution.height
//                if () { //todo 这里可以根据实际情况调整宽高的计算方式，确保它们符合你的预期和设备的能力。比如有些设备可能不支持某些特定的分辨率，或者你想要强制使用某个宽高比。
                    mRealVideoHeight = (surfaceRequest.resolution.height * 4f / 3f).toInt()  //cjztest  这个比例看起来才正确
//                }

                // 绑定分析器
                val isFront = false // 根据你用的 CameraSelector 判断
                val faceAnalyzer = FaceAnalyzer( Size(mRealVideoWidth, mRealVideoHeight), isFront)
                faceAnalyzer.setOnFacesDetectedListener(object : FaceAnalyzer.OnFacesDetectedListener {
                    override fun onFacesDetected(screenRects: List<android.graphics.RectF>) {
                        // 这里拿到的 screenRects 是已经转换成屏幕坐标系的矩形列表，可以直接用来更新 UI
//                    Log.d("FaceAnalyzer", "检测到 ${screenRects.size} 张人脸")
                        renderer.updateFaceRects(screenRects) // 将检测到的人脸坐标传递给 Renderer 进行绘制
                    }
                })
                imageAnalysis
                    .setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        faceAnalyzer
                    )


                // 3. 根据 CameraX 实际分辨率，调整 GLSurfaceView 的布局参数和 SurfaceTexture 的缓冲区大小
                val fixedParams = LinearLayout.LayoutParams(mRealVideoWidth, mRealVideoHeight) // 这里假设 GLSurfaceView 的宽高比固定为 4:3，实际项目中可能需要更灵活的适配方案
                glSurfaceView.layoutParams = fixedParams


                Log.e("cjztest", "surfaceRequest: mRealVideoWidth: $mRealVideoWidth, mRealVideoHeight: $mRealVideoHeight")
                surfaceTexture.setDefaultBufferSize(mRealVideoWidth, mRealVideoHeight)

                val surface = android.view.Surface(surfaceTexture)
                surfaceRequest.provideSurface(surface, ContextCompat.getMainExecutor(this)) {
                    surface.release()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRecordingInternal() {
        val storageDir = getExternalFilesDir("video")
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        val outputFile = File(storageDir, "green_clip_${System.currentTimeMillis()}.mp4")
        if (!outputFile.exists()) {
            outputFile.createNewFile()
        }
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
            setVideoFrameRate(mFrameRate.upper)
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