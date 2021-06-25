package com.ffmpeg;

public class FFMpegUtil {
    static{
        System.loadLibrary("ffmpeg_caller");
    }

    public static native void version();

}
