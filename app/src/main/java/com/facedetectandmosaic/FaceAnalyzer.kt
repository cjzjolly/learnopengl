package com.facedetectandmosaic

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.max

class FaceAnalyzer(
    private val previewSize: Size, // GLSurfaceView 的预览尺寸 (宽高)
    private val isFrontCamera: Boolean = false // 是否前置摄像头
) : ImageAnalysis.Analyzer {

    interface OnFacesDetectedListener {
        fun onFacesDetected(screenRects: List<RectF>)
    }

    private var onFacesDetectedListener: OnFacesDetectedListener? = null
    
    // 设置人脸检测回调监听器（可为 null 以移除监听）
    fun setOnFacesDetectedListener(listener: OnFacesDetectedListener?) {
        this.onFacesDetectedListener = listener
    }

    
    
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // 优先速度
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .build()
    private val faceDetector = FaceDetection.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        // 1. 将 ImageProxy 转为 ML Kit 需要的 InputImage
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        // 获取图像旋转后的逻辑宽高 (用于后续坐标转换)
        val rotation = imageProxy.imageInfo.rotationDegrees
        val imgW = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val imgH = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                val screenRects = faces.map { face ->
                    // 2. 核心：坐标系转换 (图像坐标 -> 屏幕坐标)
                    transformRect(face.boundingBox, previewSize, imgW, imgH, isFrontCamera)
                }
                //todo 在这里将 screenRects 传递给 GLSurfaceView 的 Renderer，进行绘制
                Log.e("cjztest", "Detected ${faces.size} faces, screen rects: $screenRects")
                onFacesDetectedListener?.onFacesDetected(screenRects)
            }
            .addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Detection failed", e)
            }
            .addOnCompleteListener {
                // 必须关闭 imageProxy，否则不会收到下一帧
                imageProxy.close()
            }
    }

    /**
     * 将 ML Kit 返回的图像坐标，转换为 OverlayView 的屏幕坐标
     * 假设你的 GLSurfaceView 采用的是 Center Crop (居中裁剪填满屏幕) 策略
     */
    private fun transformRect(rect: Rect, previewSize: Size, imgW: Int, imgH: Int, isFront: Boolean): RectF {
        val viewW = previewSize.width.toFloat()
        val viewH = previewSize.height.toFloat()
        if (viewW == 0f || viewH == 0f) return RectF(rect)

        // 计算缩放比例 (Center Crop 逻辑：取较大的缩放比以填满屏幕)
        val scale = max(viewW / imgW, viewH / imgH)

        Log.e("cjztest", "transformRect: viewW: $viewW, viewH: $viewH, imgW: $imgW, imgH: $imgH, scale: $scale")

        // 计算裁剪导致的偏移量
        val offsetX = (viewW - imgW * scale) / 2f
        val offsetY = (viewH - imgH * scale) / 2f

        var left = rect.left * scale + offsetX
        var top = rect.top * scale + offsetY
        var right = rect.right * scale + offsetX
        var bottom = rect.bottom * scale + offsetY

        // 如果是前置摄像头，画面通常会被水平镜像 (Mirror)，框也需要镜像翻转
        if (isFront) {
            val tempLeft = viewW - right
            val tempRight = viewW - left
            left = tempLeft
            right = tempRight
        }

        return RectF(left, top, right, bottom)
    }
}