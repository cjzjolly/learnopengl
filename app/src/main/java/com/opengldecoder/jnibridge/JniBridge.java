package com.opengldecoder.jnibridge;

import android.graphics.Bitmap;
import android.view.Surface;

public class JniBridge {

    static {
        System.loadLibrary("opengl_decoder");
    }

    /**渲染器类型枚举器 todo java要调用，则也要抄一份**/
    public enum RENDER_PROGRAM_KIND {
        RENDER_OES_TEXTURE, //OES纹理渲染
        RENDER_YUV, //YUV数据或纹理渲染
        RENDER_CONVOLUTION, //添加卷积处理
    }

    public static native void nativeGLInit(int viewPortWidth, int viewPortHeight);

//    public static native void drawRGBABitmap(Bitmap bmp, int bmpW, int bmpH);

    public static native void drawToSurface(Surface surface, int color);

    public static native void drawBuffer();

    public static native long addFullContainerLayer(int texturePointer, int textureWidthAndHeight[], long dataPointer,
                                                    int dataWidthAndHeight[],
                                                    int dataPixelFormat);

    public static native void removeLayer(long layerPointer);

    /**为指定图层添加渲染器
     @param layerPointer 图层的内存地址**/
    public static native long addRenderForLayer(long layerPointer,
                                                int renderProgramKind);

    public static native void setRenderAlpha(long renderPointer, float alpha);

    /**渲染器亮度调整**/
    public static native void setBrightness(long renderPointer, float brightness);

    /**渲染器对比度调整**/
    public static native void setContrast(long renderPointer, float contrast);

    /**白平衡调整**/
    public static native void setWhiteBalance(long renderPointer, float redWeight, float greenWeight, float blueWeight);

    public static native void renderLayer(int fboPointer, int fboWidth, int fboHeight);

    public static native void layerScale(long layerPointer, float scaleX, float scaleY);

}
