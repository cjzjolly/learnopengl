package com.whiteboard;

public class JniBridge {
    static {
        System.loadLibrary("gl_whiteboard");
    }

    /**把触摸事件传到C层并渲染**/
    protected static native void touchAndDraw(float x, float y, int action);

    public static native void nativeGLInit(int viewPortWidth, int viewPortHeight);

}