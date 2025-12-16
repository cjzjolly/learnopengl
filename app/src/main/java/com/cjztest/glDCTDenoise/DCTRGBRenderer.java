package com.cjztest.glDCTDenoise;

// DCTRGBRenderer.java
// GLES 3.0, 4-pass DCT/IDCT RGB demo with precomputed cos basis and JPEG quant table.
// Minimal but complete core. Add error handling and resource cleanup in production.

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import android.opengl.GLSurfaceView;

public class DCTRGBRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "DCTRGBRenderer";
    private final Context ctx;

    private int width = 0, height = 0;
    private int quadVao;

    // Programs
    private int progRowDCT, progColDCT, progColIDCT, progRowIDCT, progShow;

    // Textures
    private int texInput;
    private int texPass1, texCoeff, texPass3, texOut;

    private int fbo;

    // DCT precomputed arrays
    private final float[] cosBasis = new float[64];
    private final float[] quantTable = new float[64];

    // low-pass cutoff
    private int uMax = 3, vMax = 3;

    public DCTRGBRenderer(Context ctx) {
        this.ctx = ctx;
        precomputeCosBasis();
        initQuantTable();
    }

    private void precomputeCosBasis() {
        final double PI = Math.PI;
        for (int u = 0; u < 8; u++) {
            for (int x = 0; x < 8; x++) {
                cosBasis[u*8 + x] = (float)Math.cos(( (2.0 * x + 1.0) * u * PI) / 16.0);
            }
        }
    }

    private void initQuantTable() {
        // Standard JPEG luminance quant table (can use same for R/G/B or supply separate)
        float[] q = {
                16,11,10,16,24,40,51,61,
                12,12,14,19,26,58,60,55,
                14,13,16,24,40,57,69,56,
                14,17,22,29,51,87,80,62,
                18,22,37,56,68,109,103,77,
                24,35,55,64,81,104,113,92,
                49,64,78,87,103,121,120,101,
                72,92,95,98,112,100,103,99
        };
        System.arraycopy(q, 0, quantTable, 0, 64);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        quadVao = createFullScreenQuad();

        progRowDCT = createProgram(VERT_SRC, FRAG_ROW_DCT);
        progColDCT = createProgram(VERT_SRC, FRAG_COL_DCT);
        progColIDCT = createProgram(VERT_SRC, FRAG_COL_IDCT);
        progRowIDCT = createProgram(VERT_SRC, FRAG_ROW_IDCT);
        progShow = createProgram(VERT_SRC, FRAG_SHOW);

        // create textures placeholders
        texInput = createEmptyTexture(GLES30.GL_RGBA8, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);
        texPass1 = createFloatTexture(4,4);
        texCoeff = createFloatTexture(4,4);
        texPass3 = createFloatTexture(4,4);
        texOut = createByteTexture(4,4);

        int[] fb = new int[1];
        GLES30.glGenFramebuffers(1, fb, 0);
        fbo = fb[0];

        GLES30.glClearColor(0f,0f,0f,1f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES30.glViewport(0,0,w,h);
    }

    // Call to set input image bitmap (RGBA)
    public void setInputBitmap(Bitmap bmp) {
        width = bmp.getWidth();
        height = bmp.getHeight();

        // upload input
        if (texInput == 0) texInput = createEmptyTexture(GLES30.GL_RGBA8, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texInput);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);

        // recreate intermediate textures to image size
        texPass1 = createFloatTexture(width, height);
        texCoeff = createFloatTexture(width, height);
        texPass3 = createFloatTexture(width, height);
        texOut = createByteTexture(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (width <= 0 || height <= 0) return;

        // Upload uniforms common: cosBasis & quantTable to each program before use
        // (we can also set them once when program linked)
        // PASS 1: Row DCT -> texPass1
        bindFBOTexture(texPass1);
        GLES30.glViewport(0,0,width,height);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(progRowDCT);
        setCommonUniforms(progRowDCT);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texInput);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(progRowDCT, "uInput"), 0);
        GLES30.glUniform2i(GLES30.glGetUniformLocation(progRowDCT, "uSize"), width, height);
        drawQuad();

        // PASS 2: Col DCT + quant -> texCoeff
        bindFBOTexture(texCoeff);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(progColDCT);
        setCommonUniforms(progColDCT);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texPass1);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(progColDCT, "uInput"), 0);
        GLES30.glUniform2i(GLES30.glGetUniformLocation(progColDCT, "uSize"), width, height);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(progColDCT, "uUMax"), uMax);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(progColDCT, "uVMax"), vMax);
        drawQuad();

        // PASS 3: Col IDCT (dequant) -> texPass3
        bindFBOTexture(texPass3);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(progColIDCT);
        setCommonUniforms(progColIDCT);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texCoeff);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(progColIDCT, "uInput"), 0);
        GLES30.glUniform2i(GLES30.glGetUniformLocation(progColIDCT, "uSize"), width, height);
        drawQuad();

        // PASS 4: Row IDCT -> texOut
        bindFBOTexture(texOut);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(progRowIDCT);
        setCommonUniforms(progRowIDCT);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texPass3);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(progRowIDCT, "uInput"), 0);
        GLES30.glUniform2i(GLES30.glGetUniformLocation(progRowIDCT, "uSize"), width, height);
        drawQuad();

        // Show final result
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glViewport(0,0, Math.max(1, width), Math.max(1, height));
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(progShow);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texOut);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(progShow, "uTex"), 0);
        drawQuad();
    }

    private void bindFBOTexture(int tex) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, tex, 0);
    }

    private void setCommonUniforms(int program) {
        int locCos = GLES30.glGetUniformLocation(program, "uCos");
        if (locCos >= 0) GLES30.glUniform1fv(locCos, 64, FloatBuffer.wrap(cosBasis));
        int locQ = GLES30.glGetUniformLocation(program, "uQuant");
        if (locQ >= 0) GLES30.glUniform1fv(locQ, 64, FloatBuffer.wrap(quantTable));
    }

    // ---- GL util helpers (same as previous example) ----

    private int createFullScreenQuad() {
        float[] verts = {
                -1f, -1f, 0f, 0f, 0f,
                1f, -1f, 0f, 1f, 0f,
                -1f,  1f, 0f, 0f, 1f,
                1f,  1f, 0f, 1f, 1f
        };
        FloatBuffer fb = ByteBuffer.allocateDirect(verts.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(verts).position(0);

        int[] va = new int[1];
        GLES30.glGenVertexArrays(1, va, 0);
        int vao = va[0];
        GLES30.glBindVertexArray(vao);

        int[] vbo = new int[1];
        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.length * 4, fb, GLES30.GL_STATIC_DRAW);

        int posLoc = 0;
        int texLoc = 1;
        GLES30.glEnableVertexAttribArray(posLoc);
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, 5*4, 0);
        GLES30.glEnableVertexAttribArray(texLoc);
        GLES30.glVertexAttribPointer(texLoc, 2, GLES30.GL_FLOAT, false, 5*4, 3*4);

        GLES30.glBindVertexArray(0);
        return vao;
    }

    private void drawQuad() {
        GLES30.glBindVertexArray(quadVao);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);
    }

    private int createProgram(String vs, String fs) {
        int vsId = compileShader(GLES30.GL_VERTEX_SHADER, vs);
        int fsId = compileShader(GLES30.GL_FRAGMENT_SHADER, fs);
        int p = GLES30.glCreateProgram();
        GLES30.glAttachShader(p, vsId);
        GLES30.glAttachShader(p, fsId);
        GLES30.glBindAttribLocation(p, 0, "aPosition");
        GLES30.glBindAttribLocation(p, 1, "aTexCoord");
        GLES30.glLinkProgram(p);
        int[] link = new int[1];
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, link, 0);
        if (link[0] == 0) {
            Log.e(TAG, "Link error: " + GLES30.glGetProgramInfoLog(p));
        }
        return p;
    }

    private int compileShader(int type, String src) {
        int s = GLES30.glCreateShader(type);
        GLES30.glShaderSource(s, src);
        GLES30.glCompileShader(s);
        int[] ok = new int[1];
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            Log.e(TAG, "Compile error: " + GLES30.glGetShaderInfoLog(s));
        }
        return s;
    }

    private int createEmptyTexture(int internalFormat, int format, int type) {
        int[] t = new int[1];
        GLES30.glGenTextures(1,t,0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, t[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, internalFormat, 4, 4, 0, format, type, null);
        return t[0];
    }

    private int createFloatTexture(int w, int h) {
        int[] t = new int[1];
        GLES30.glGenTextures(1,t,0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, t[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, w, h, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null);
        return t[0];
    }

    private int createByteTexture(int w,int h) {
        int[] t = new int[1];
        GLES30.glGenTextures(1,t,0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, t[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, w, h, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        return t[0];
    }

    // ---- shaders (RGB-aware, use uCos and uQuant arrays) ----

    private static final String VERT_SRC =
            "#version 300 es\n" +
                    "layout(location=0) in vec3 aPosition;\n" +
                    "layout(location=1) in vec2 aTexCoord;\n" +
                    "out vec2 vTex;\n" +
                    "void main(){ vTex = aTexCoord; gl_Position = vec4(aPosition, 1.0); }";

    // Pass1: Row DCT -> write F(u,y) at (tileX*8 + u, tileY*8 + y)
    private static final String FRAG_ROW_DCT =
            "#version 300 es\n" +
                    "precision highp float;\n" +
                    "uniform sampler2D uInput;\n" +
                    "uniform float uCos[64];\n" +
                    "uniform float uQuant[64];\n" +
                    "uniform ivec2 uSize;\n" +
                    "out vec4 fragColor;\n" +
                    "float alpha(int k){ return (k==0) ? 0.70710678 : 1.0; }\n" +
                    "void main(){\n" +
                    "  ivec2 coord = ivec2(gl_FragCoord.xy);\n" +
                    "  int tileX = coord.x / 8;\n" +
                    "  int tileY = coord.y / 8;\n" +
                    "  int u = coord.x % 8;\n" +
                    "  int y = coord.y % 8;\n" +
                    "  vec3 sum = vec3(0.0);\n" +
                    "  for(int x=0;x<8;x++){\n" +
                    "    int sx = clamp(tileX*8 + x, 0, uSize.x-1);\n" +
                    "    int sy = clamp(tileY*8 + y, 0, uSize.y-1);\n" +
                    "    vec3 f = texelFetch(uInput, ivec2(sx,sy), 0).rgb;\n" +
                    "    float c = uCos[u*8 + x];\n" +
                    "    sum += f * c;\n" +
                    "  }\n" +
                    "  // apply DCT row scaling\n" +
                    "  vec3 Fuy = 0.5 * alpha(u) * sum;\n" +
                    "  // don't quantize per-row; store raw Fuy for next pass. But to reduce range we could (optional) divide by Q\n" +
                    "  fragColor = vec4(Fuy, 1.0);\n" +
                    "}";

    // Pass2: Column DCT -> produce C(u,v) at (tileX*8 + u, tileY*8 + v). Apply quantization here.
    private static final String FRAG_COL_DCT =
            "#version 300 es\n" +
                    "precision highp float;\n" +
                    "uniform sampler2D uInput; // pass1\n" +
                    "uniform float uCos[64];\n" +
                    "uniform float uQuant[64];\n" +
                    "uniform ivec2 uSize;\n" +
                    "uniform int uUMax; uniform int uVMax;\n" +
                    "out vec4 fragColor;\n" +
                    "float alpha(int k){ return (k==0) ? 0.70710678 : 1.0; }\n" +
                    "void main(){\n" +
                    "  ivec2 coord = ivec2(gl_FragCoord.xy);\n" +
                    "  int tileX = coord.x / 8;\n" +
                    "  int tileY = coord.y / 8;\n" +
                    "  int u = coord.x % 8;\n" +
                    "  int v = coord.y % 8;\n" +
                    "  vec3 sum = vec3(0.0);\n" +
                    "  for(int y=0;y<8;y++){\n" +
                    "    int sx = clamp(tileX*8 + u, 0, uSize.x-1); // F(u,y) stored at x = tileX*8 + u\n" +
                    "    int sy = clamp(tileY*8 + y, 0, uSize.y-1);\n" +
                    "    vec3 Fuy = texelFetch(uInput, ivec2(sx,sy), 0).rgb;\n" +
                    "    float c = uCos[v*8 + y];\n" +
                    "    sum += Fuy * c;\n" +
                    "  }\n" +
                    "  vec3 Cuv = 0.5 * alpha(v) * sum;\n" +
                    "  // low-pass: zero out high freq\n" +
                    "  if (u > uUMax || v > uVMax) Cuv = vec3(0.0);\n" +
                    "  // quantize: divide by Q (simulating lossy quantization). Using same Q for R/G/B.\n" +
                    "  float Q = uQuant[u*8 + v];\n" +
                    "  Cuv = Cuv / Q;\n" +
                    "  fragColor = vec4(Cuv, 1.0);\n" +
                    "}";

    // Pass3: Column IDCT (dequant + column IDCT) -> produce G(u,y) stored at (tileX*8 + u, tileY*8 + y)
    private static final String FRAG_COL_IDCT =
            "#version 300 es\n" +
                    "precision highp float;\n" +
                    "uniform sampler2D uInput; // coeff\n" +
                    "uniform float uCos[64];\n" +
                    "uniform float uQuant[64];\n" +
                    "uniform ivec2 uSize;\n" +
                    "out vec4 fragColor;\n" +
                    "float alpha(int k){ return (k==0) ? 0.70710678 : 1.0; }\n" +
                    "void main(){\n" +
                    "  ivec2 coord = ivec2(gl_FragCoord.xy);\n" +
                    "  int tileX = coord.x / 8;\n" +
                    "  int tileY = coord.y / 8;\n" +
                    "  int x = coord.x % 8; // space x\n" +
                    "  int v = coord.y % 8; // freq v\n" +
                    "  vec3 sum = vec3(0.0);\n" +
                    "  for(int k=0;k<8;k++){\n" +
                    "    int sx = clamp(tileX*8 + k, 0, uSize.x-1); // coeff index u=k\n" +
                    "    int sy = clamp(tileY*8 + v, 0, uSize.y-1); // coeff index v\n" +
                    "    vec3 C = texelFetch(uInput, ivec2(sx,sy), 0).rgb;\n" +
                    "    // dequantize\n" +
                    "    float Q = uQuant[k*8 + v]; // note ordering: k is u index, v is v index\n" +
                    "    vec3 Cde = C * Q;\n" +
                    "    float c = uCos[k*8 + x]; // cos((2x+1)*k*pi/16)\n" +
                    "    sum += (alpha(k) * Cde) * c;\n" +
                    "  }\n" +
                    "  vec3 G = 0.5 * sum; // G(u,x) or G(u,y) layout: stored at (tileX*8 + u, tileY*8 + x)\n        // We choose to store G(u,y) at (tileX*8 + u, tileY*8 + x) where x is the local space coordinate\n" +
                    "  fragColor = vec4(G, 1.0);\n" +
                    "}";

    // Pass4: Row IDCT -> reconstruct pixel f'(x,y), reading G(u,y) stored at (tileX*8 + u, tileY*8 + x)
    private static final String FRAG_ROW_IDCT =
            "#version 300 es\n" +
                    "precision highp float;\n" +
                    "uniform sampler2D uInput; // pass3\n" +
                    "uniform float uCos[64];\n" +
                    "uniform float uQuant[64];\n" +
                    "uniform ivec2 uSize;\n" +
                    "out vec4 fragColor;\n" +
                    "float alpha(int k){ return (k==0) ? 0.70710678 : 1.0; }\n" +
                    "void main(){\n" +
                    "  ivec2 coord = ivec2(gl_FragCoord.xy);\n        // coord corresponds to overall image pixel. local tile coords:\n" +
                    "  int tileX = coord.x / 8;\n  int tileY = coord.y / 8;\n  " +
                    "  int x = coord.x % 8;\n" +
                    "  int y = coord.y % 8;\n" +
                    "  vec3 sum = vec3(0.0);\n" +
                    "  for(int u=0; u<8; u++){\n" +
                    "        int sx = clamp(tileX*8 + u, 0, uSize.x-1); // G stored at (tileX*8 + u, tileY*8 + x)\n" +
                    "        int sy = clamp(tileY*8 + x, 0, uSize.y-1);\n" +
                    "        vec3 G = texelFetch(uInput, ivec2(sx, sy), 0).rgb;\n" +
                    "        float c = uCos[u*8 + x];\n" +
                    "        sum += (alpha(u) * G) * c;\n" +
                    "  }\n" +
                    "  vec3 val = 0.5 * sum;\n" +
                    "  // clamp and output\n" +
                    "  val = clamp(val, 0.0, 1.0);\n" +
                    "  fragColor = vec4(val, 1.0);\n" +
                    "}";

    // Show pass: display final RGBA8 texture
    private static final String FRAG_SHOW =
            "#version 300 es\n" +
                    "precision mediump float;\n" +
                    "uniform sampler2D uTex;\n" +
                    "in vec2 vTex;\n" +
                    "out vec4 fragColor;\n" +
                    "void main(){ fragColor = texture(uTex, vTex); }";
}

