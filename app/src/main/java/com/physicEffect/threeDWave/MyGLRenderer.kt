package com.physicEffect.threeDWave

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

class MyGLRenderer : GLSurfaceView.Renderer {

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer
    private var program = 0

    // 100×100 网格
    private val widthSegments = 100
    private val heightSegments = 100

    // 矩阵
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    // 顶点和索引数量
    private var vertexCount = 0
    private var indexCount = 0
    private val startTimeStamp = System.currentTimeMillis()

    // 着色器
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        uniform float uTimeStamp;
        attribute vec4 vPosition;
        varying float vDepth;
        void main() {
            vec4 point = vec4(vPosition);
            float t = uTimeStamp / 400.0;
            float z0 = ( sin(radians(vPosition.x * 100.0) + t) + cos(radians(vPosition.y * 50.0) + t / 2.0) ) * 0.5;
            float z1 = ( sin(radians(vPosition.x * 50.0) + t / 4.0) + cos(radians(vPosition.y * 60.0) + t / 3.0) ) * 0.5;
            point.z = z0 + z1;
            gl_Position = uMVPMatrix * point;
            vDepth = point.z;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying float vDepth;
        void main() {
            float shade = 0.5 + 0.5 * (1.0 - abs(vDepth) * 0.1);
            gl_FragColor = vec4(shade, shade, shade, 1.0);
        }
    """.trimIndent()

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // 初始化平面顶点和索引
        val (vertices, indices) = createPlane(widthSegments, heightSegments)
        vertexCount = vertices.size / 3
        indexCount = indices.size

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(indices)
                position(0)
            }

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)

        // 相机位置 (eyeX, eyeY, eyeZ)，观察点 (centerX, centerY, centerZ)，上方向 (upX, upY, upZ)
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 5f, 8f,   // 相机位置
            0f, 0f, 0f,   // 看向原点
            0f, 0f, 0.2f)   // Y 向上
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val timestampHandle = GLES20.glGetUniformLocation(program, "uTimeStamp")

        // 模型矩阵：慢慢旋转
        Matrix.setIdentityM(modelMatrix, 0)
//        Matrix.rotateM(modelMatrix, 0, (System.currentTimeMillis() % 3600L) / 10f, 0f, 0f, 1f)

        // MVP = P * V * M
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(timestampHandle, (System.currentTimeMillis() - startTimeStamp).toFloat())

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, vertexBuffer)

        GLES20.glDrawElements(GLES20.GL_LINES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    /**
     * 生成一个 widthSegments × heightSegments 的平面
     * 范围：X, Z ∈ [-1, 1]
     */
    private fun createPlane(widthSegments: Int, heightSegments: Int): Pair<FloatArray, ShortArray> {
        val vertices = FloatArray((widthSegments + 1) * (heightSegments + 1) * 3)
        val indices = ShortArray(widthSegments * heightSegments * 6)

        var vertexOffset = 0
        for (y in 0..heightSegments) {
            val v = y.toFloat() / heightSegments
            for (x in 0..widthSegments) {
                val u = x.toFloat() / widthSegments
                val px = (u - 0.5f) * 10f  // 放大平面范围
                val py = (v - 0.5f) * 10f
                vertices[vertexOffset++] = px
                vertices[vertexOffset++] = py
                vertices[vertexOffset++] = 0f
            }
        }

        var indexOffset = 0
        for (y in 0 until heightSegments) {
            for (x in 0 until widthSegments) {
                val topLeft = (y * (widthSegments + 1) + x).toShort()
                val bottomLeft = ((y + 1) * (widthSegments + 1) + x).toShort()

                indices[indexOffset++] = topLeft
                indices[indexOffset++] = bottomLeft
                indices[indexOffset++] = (topLeft + 1).toShort()

                indices[indexOffset++] = (topLeft + 1).toShort()
                indices[indexOffset++] = bottomLeft
                indices[indexOffset++] = (bottomLeft + 1).toShort()
            }
        }

        return Pair(vertices, indices)
    }
}
