package com.cjztest.glShaderEffect;

import android.content.res.Resources;
import android.opengl.GLES30;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ShaderUtil {
    //从sh脚本中加载shader内容的方法
    public static String loadFromAssetsFile(String fname, Resources r)
    {
        String result = null;
        try {
            InputStream in = r.getAssets().open(fname);
            int ch = 0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((ch = in.read()) != -1) {
                baos.write(ch);
            }
            byte[] buff = baos.toByteArray();
            baos.close();
            in.close();
            result = new String(buff, "UTF-8");
            result = result.replaceAll("\\r\\n", "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        if (shader != 0) { //若创建shader脚本的"指针"成功
            GLES30.glShaderSource(shader, shaderCode);
            GLES30.glCompileShader(shader);
            //存放编译成功shader数量的数组
            int[] compiled = new int[1];
            //获取Shader的编译情况
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {//若编译失败则显示错误日志并删除此shader
                Log.e("ES30_ERROR", "Could not compile shader " + type + ":");
                Log.e("ES30_ERROR", GLES30.glGetShaderInfoLog(shader));
                GLES30.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    //检查每一步操作是否有错误的方法
    public static void checkGlError(String op) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e("ES30_ERROR", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public static void destroyShader(int programPointer, int vertShaderPointer, int fragShaderPoint) {
        GLES30.glDetachShader(programPointer, vertShaderPointer);
        GLES30.glDetachShader(programPointer, fragShaderPoint);
        GLES30.glDeleteShader(vertShaderPointer);
        GLES30.glDeleteShader(fragShaderPoint);
        GLES30.glDeleteProgram(programPointer);
    }
}
