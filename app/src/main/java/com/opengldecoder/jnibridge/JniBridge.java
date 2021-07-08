package com.opengldecoder.jnibridge;

import android.graphics.Bitmap;
import android.view.Surface;

public class JniBridge {

    static {
        System.loadLibrary("opengl_decoder");
    }

    public static native void nativeGLInit(int viewPortWidth, int viewPortHeight);

    public static native void drawRGBABitmap(Bitmap bmp, int bmpW, int bmpH);

    public static native void drawToSurface(Surface surface, int color);

    public static native void drawBuffer();
}
