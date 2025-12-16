package com.physicEffect.touchWater

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.*
import java.util.concurrent.ConcurrentHashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min

class WaveMeshRenderer(private val ctx: Context) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "WaveMeshRenderer"

        // mesh resolution (vertices = GRID_W * GRID_H)
        private const val GRID_W = 100
        private const val GRID_H = 100

        // heightfield texture resolution (we'll align to grid)
        private const val TEX_W = GRID_W
        private const val TEX_H = GRID_H

        // max simultaneous touches tracked
        private const val MAX_TOUCHES = 8

        // wave parameters
        private const val WAVE_SPEED = 1.0f
        private const val DAMPING = 0.997f
    }

    // programs
    private var computeProg = 0
    private var renderProg = 0

    // ping-pong height textures (R32F)
    private val heightTex = IntArray(2)
    private var prevIdx = 0
    private var currIdx = 1 // we'll write next into other index and rotate

    // mesh buffers
    private val meshVbo = IntArray(1)
    private val meshIbo = IntArray(1)
    private val meshVao = IntArray(1)
    private var indexCount = 0

    // fullscreen quad VBO/VAO for debug/fullscreen (not used for mesh)
    private val quadVao = IntArray(1)
    private var quadVbo = 0

    // time
    private var lastNs = System.nanoTime()

    // matrix
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val mvp = FloatArray(16)

    // touch state: map pointerId -> (x,y,active,speed)
    private val touches = ConcurrentHashMap<Int, TouchState>()

    private data class TouchState(var x: Float = 0f, var y: Float = 0f, var active: Boolean = false, var speed: Float = 0f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // compile
        computeProg = createComputeProgram(COMPUTE_CS)
        renderProg = createRenderProgram(RENDER_VS, RENDER_FS)

        // textures
        setupHeightTextures(TEX_W, TEX_H)

        // mesh
        setupMesh(GRID_W, GRID_H)

        // camera
        Matrix.setIdentityM(view, 0)
        Matrix.setLookAtM(view, 0, 0f, 2.0f, 4.5f, 0f, 0f, 0f, 0f, 1f, 0f)

        lastNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.perspectiveM(proj, 0, 45f, ratio, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        var dt = (now - lastNs).toFloat() / 1e9f
        lastNs = now
        if (dt <= 0f) dt = 1f / 60f
        dt = min(dt, 0.033f)

        // 1) compute step: prev/curr -> next (write into other texture)
        GLES31.glUseProgram(computeProg)

        // bind images: binding 0 = prev, 1 = curr, 2 = next (we'll choose indices)
        val nextIdx = 1 - currIdx // write into the one that's not curr (and not prev)
        GLES31.glBindImageTexture(0, heightTex[prevIdx], 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_R32F)
        GLES31.glBindImageTexture(1, heightTex[currIdx], 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_R32F)
        GLES31.glBindImageTexture(2, heightTex[nextIdx], 0, false, 0, GLES31.GL_WRITE_ONLY, GLES31.GL_R32F)

        // collect touches into array of vec3 [MAX_TOUCHES] where z = strength (derived from speed)
        val touchArray = FloatArray(MAX_TOUCHES * 3) // x,y,str
        val activeList = ArrayList<TouchState>()
        for ((_, s) in touches) if (s.active) activeList.add(s)
        val count = min(activeList.size, MAX_TOUCHES)
        for (i in 0 until count) {
            val t = activeList[i]
            touchArray[i*3 + 0] = t.x
            touchArray[i*3 + 1] = t.y
            // strength = base + speed factor, clamp
            touchArray[i*3 + 2] = min(1.5f, 0.12f + t.speed * 0.02f)
        }
        // fill remaining with invalid marker (z <= 0)
        for (i in count until MAX_TOUCHES) {
            touchArray[i*3 + 0] = -1f
            touchArray[i*3 + 1] = -1f
            touchArray[i*3 + 2] = 0f
        }

        // set uniforms
        GLES31.glUniform1f(GLES31.glGetUniformLocation(computeProg, "uDt"), dt)
        GLES31.glUniform2f(GLES31.glGetUniformLocation(computeProg, "uSize"), TEX_W.toFloat(), TEX_H.toFloat())
        GLES31.glUniform1f(GLES31.glGetUniformLocation(computeProg, "uSpeed"), WAVE_SPEED)
        GLES31.glUniform1f(GLES31.glGetUniformLocation(computeProg, "uDamping"), DAMPING)
        GLES31.glUniform1i(GLES31.glGetUniformLocation(computeProg, "uTouchCount"), count)
        val locTouch = GLES31.glGetUniformLocation(computeProg, "uTouchPos")
        if (locTouch >= 0) {
            // upload as vec3 array
            GLES31.glUniform3fv(locTouch, MAX_TOUCHES, touchArray, 0)
        }

        // dispatch
        val groupX = (TEX_W + 15) / 16
        val groupY = (TEX_H + 15) / 16
        GLES31.glDispatchCompute(groupX, groupY, 1)
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)

        // rotate indices: prev <- curr, curr <- next
        prevIdx = currIdx
        currIdx = nextIdx

        // 2) render mesh: bind height texture currIdx and draw indexed mesh
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(renderProg)

        // compute MVP
        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        val angle = ((System.nanoTime() / 1e9f) * 10f) % 360f
        Matrix.rotateM(model, 0, angle, 0f, 1f, 0f)
        val mv = FloatArray(16)
        Matrix.multiplyMM(mv, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(renderProg, "uMVP"), 1, false, mvp, 0)

        // bind height texture as sampler2D at unit0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, heightTex[currIdx])
        GLES20.glUniform1i(GLES20.glGetUniformLocation(renderProg, "uHeightTex"), 0)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(renderProg, "uTexSize"), TEX_W.toFloat(), TEX_H.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(renderProg, "uHeightScale"), 0.6f)

        // draw
        GLES30.glBindVertexArray(meshVao[0])
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, meshIbo[0])
//        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glDrawElements(GLES30.GL_LINES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)
    }

    // called from UI thread via queueEvent: set touch active/inactive
    fun setTouchActive(pointerId: Int, x: Float, y: Float, active: Boolean, speed: Float) {
        val s = touches.getOrPut(pointerId) { TouchState() }
        s.x = clamp01(x); s.y = clamp01(y); s.active = active; s.speed = speed
        if (!active) touches.remove(pointerId)
    }

    private fun clamp01(v: Float) = max(0f, min(1f, v))

    // ---------- resources setup ----------
    private fun setupHeightTextures(w: Int, h: Int) {
        GLES31.glGenTextures(2, heightTex, 0)
        val zero = FloatArray(w * h) // zeros
        val buf = ByteBuffer.allocateDirect(zero.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(zero).position(0)
        for (i in 0..1) {
            GLES31.glBindTexture(GLES20.GL_TEXTURE_2D, heightTex[i])
            // allocate R32F
            GLES31.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES31.GL_R32F, w, h, 0, GLES31.GL_RED, GLES20.GL_FLOAT, null)
            GLES31.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES31.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES31.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            // initialize zeros
            GLES31.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, w, h, GLES31.GL_RED, GLES20.GL_FLOAT, buf)
            buf.position(0)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        prevIdx = 0; currIdx = 1
    }

    private fun setupMesh(gw: Int, gh: Int) {
        // vertex layout: uv.x, uv.y (we'll compute pos in vertex shader by sampling height)
        val vcount = gw * gh
        val verts = FloatArray(vcount * 2)
        var off = 0
        for (y in 0 until gh) {
            val v = y.toFloat() / (gh - 1)
            for (x in 0 until gw) {
                val u = x.toFloat() / (gw - 1)
                verts[off++] = u
                verts[off++] = v
            }
        }
        // indices (two triangles per cell)
        val indices = IntArray((gw - 1) * (gh - 1) * 6)
        var idx = 0
        for (y in 0 until gh - 1) {
            for (x in 0 until gw - 1) {
                val tl = y * gw + x
                val bl = (y + 1) * gw + x
                val tr = tl + 1
                val br = bl + 1
                indices[idx++] = tl; indices[idx++] = bl; indices[idx++] = tr
                indices[idx++] = tr; indices[idx++] = bl; indices[idx++] = br
            }
        }
        indexCount = indices.size

        // upload VBO
        GLES30.glGenBuffers(1, meshVbo, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, meshVbo[0])
        val vb = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(verts).position(0)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, vb, GLES30.GL_STATIC_DRAW)

        // upload IBO (unsigned int)
        GLES30.glGenBuffers(1, meshIbo, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, meshIbo[0])
        val ib = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        ib.put(indices).position(0)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, ib, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)

        // VAO
        GLES30.glGenVertexArrays(1, meshVao, 0)
        GLES30.glBindVertexArray(meshVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, meshVbo[0])
        val aUv = GLES20.glGetAttribLocation(renderProg, "aUV")
        GLES20.glEnableVertexAttribArray(aUv)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 2 * 4, 0)
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    // ---------- shader helpers ----------
    private fun createComputeProgram(source: String): Int {
        val prog = GLES31.glCreateProgram()
        val cs = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER)
        GLES31.glShaderSource(cs, source)
        GLES31.glCompileShader(cs)
        val compiled = IntArray(1)
        GLES31.glGetShaderiv(cs, GLES31.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES31.glGetShaderInfoLog(cs)
            Log.e(TAG, "Compute compile error: $log")
            throw RuntimeException("Compute shader compile error: $log")
        }
        GLES31.glAttachShader(prog, cs)
        GLES31.glLinkProgram(prog)
        return prog
    }

    private fun createRenderProgram(vsSrc: String, fsSrc: String): Int {
        val vs = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER)
        GLES31.glShaderSource(vs, vsSrc)
        GLES31.glCompileShader(vs)
        val compV = IntArray(1)
        GLES31.glGetShaderiv(vs, GLES31.GL_COMPILE_STATUS, compV, 0)
        if (compV[0] == 0) throw RuntimeException("VS compile: ${GLES31.glGetShaderInfoLog(vs)}")

        val fs = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER)
        GLES31.glShaderSource(fs, fsSrc)
        GLES31.glCompileShader(fs)
        val compF = IntArray(1)
        GLES31.glGetShaderiv(fs, GLES31.GL_COMPILE_STATUS, compF, 0)
        if (compF[0] == 0) throw RuntimeException("FS compile: ${GLES31.glGetShaderInfoLog(fs)}")

        val prog = GLES31.glCreateProgram()
        GLES31.glAttachShader(prog, vs)
        GLES31.glAttachShader(prog, fs)
        GLES31.glBindAttribLocation(prog, 0, "aPos")
        GLES31.glBindAttribLocation(prog, 1, "aUV")
        GLES31.glLinkProgram(prog)
        return prog
    }

    // ---------- shaders ----------
    // Compute shader: finite-difference wave; touches array vec3[uTouchPos]
    private val COMPUTE_CS = """
        #version 310 es
        layout (local_size_x = 16, local_size_y = 16) in;

        layout (binding = 0, r32f) readonly uniform highp image2D uPrev;
        layout (binding = 1, r32f) readonly uniform highp image2D uCurr;
        layout (binding = 2, r32f) writeonly uniform highp image2D uNext;

        uniform float uDt;
        uniform vec2 uSize;
        uniform float uSpeed;
        uniform float uDamping;

        // touch positions (x,y,strength) in uv space
        uniform vec3 uTouchPos[$MAX_TOUCHES];
        uniform int uTouchCount;

        void main() {
            ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
            if (gid.x >= int(uSize.x) || gid.y >= int(uSize.y)) return;

            ivec2 sizei = ivec2(uSize);
            ivec2 left = ivec2(max(gid.x-1,0), gid.y);
            ivec2 right= ivec2(min(gid.x+1, sizei.x-1), gid.y);
            ivec2 up   = ivec2(gid.x, max(gid.y-1,0));
            ivec2 down = ivec2(gid.x, min(gid.y+1, sizei.y-1));

            float hPrev = imageLoad(uPrev, gid).r;
            float hCurr = imageLoad(uCurr, gid).r;
            float hL = imageLoad(uCurr, left).r;
            float hR = imageLoad(uCurr, right).r;
            float hU = imageLoad(uCurr, up).r;
            float hD = imageLoad(uCurr, down).r;

            float lap = (hL + hR + hU + hD - 4.0 * hCurr);

            float c = uSpeed;
            float dt = uDt;
            float next = 2.0 * hCurr - hPrev + (c * c) * (dt * dt) * lap;
            next *= uDamping;

            // apply touches
            vec2 uv = (vec2(gid) + vec2(0.5)) / uSize;
            for (int i=0;i<$MAX_TOUCHES;i++) {
                if (i >= uTouchCount) break;
                vec3 t = uTouchPos[i];
                if (t.z <= 0.0) continue;
                vec2 d = uv - t.xy;
                float dist2 = dot(d,d);
                float radius = 0.03; // adjust
                float fall = exp(-dist2 / (radius*radius));
                next += t.z * fall;
            }

            // simple floor clamp
            if (next < -1.5) next = -1.5;
            imageStore(uNext, gid, vec4(next,0.0,0.0,1.0));
        }
    """.trimIndent()

    // Vertex shader: input aUV, sample height texture to get position, compute normal via sampling neighbors
    private val RENDER_VS = """
        #version 300 es
        precision highp float;
        layout(location=1) in vec2 aUV;
        uniform sampler2D uHeightTex;
        uniform vec2 uTexSize;
        uniform float uHeightScale;
        uniform mat4 uMVP;
        out vec3 vNormal;
        out vec3 vPos;

        void main() {
            // uv -> sample height
            float h = texture(uHeightTex, aUV).r * uHeightScale;

            // compute world position: map uv to [-1,1] plane XZ
            float x = (aUV.x - 0.5) * 2.0;
            float z = (aUV.y - 0.5) * 2.0;
            vec3 pos = vec3(x, h, z);
            vPos = pos;

            // compute normal by sampling height neighbors in texture space
            vec2 texel = 1.0 / uTexSize;
            float hx = texture(uHeightTex, aUV + vec2(texel.x, 0.0)).r * uHeightScale - texture(uHeightTex, aUV - vec2(texel.x, 0.0)).r * uHeightScale;
            float hz = texture(uHeightTex, aUV + vec2(0.0, texel.y)).r * uHeightScale - texture(uHeightTex, aUV - vec2(0.0, texel.y)).r * uHeightScale;
            vec3 n = normalize(vec3(-hx, 2.0, -hz));
            vNormal = n;

            gl_Position = uMVP * vec4(pos, 1.0);
        }
    """.trimIndent()

    private val RENDER_FS = """
        #version 300 es
        precision highp float;
        in vec3 vNormal;
        in vec3 vPos;
        out vec4 fragColor;
        void main() {
            vec3 lightDir = normalize(vec3(0.5,1.0,0.3));
            float diff = max(dot(normalize(vNormal), lightDir), 0.0);
            vec3 base = vec3(0.0, 0.35, 0.6);
            vec3 col = base + diff * vec3(0.5);
            // rim
            float rim = pow(1.0 - max(dot(normalize(vNormal), vec3(0.0,0.0,1.0)),0.0), 2.0);
            col += vec3(0.15) * rim;
            // height tint
            col += vPos.y * 0.5;
            fragColor = vec4(col, 1.0);
        }
    """.trimIndent()
}
