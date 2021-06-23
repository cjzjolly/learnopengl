package com.opengldecoder.jnibridge;

import android.view.Surface;

public class JniBridge {
    public static native void drawToSurface(Surface surface, int color);

    public static native void drawBuffer();

}
