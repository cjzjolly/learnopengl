package com.cjztest.glShaderEffect;

import android.content.Context;

public class GLVideo extends GLObject {
    private final float mX;
    private final float mY;
    private final float mZ;
    private final float mWidth;
    private final float mHeight;
    private final float mWindowW;
    private final float mWindowH;

    public GLVideo(float x, float y, float z, float w, float h, int windowW, int windowH) {
        this.mX = x;
        this.mY = y;
        this.mZ = z;
        this.mWidth = w;
        this.mHeight = h;
        this.mWindowW = windowW;
        this.mWindowH = windowH;
    }



    @Override
    public void drawTo(int baseProgramID, int positionPointer, int vTexCoordPointer, int colorPointer, float[] cameraMatrix, float[] projMatrix, int muMVPMatrixPointer, int glFunChoicePointer) {

    }
}
