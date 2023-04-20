package com.cjztest.glMyLightModel;

import android.content.res.Resources;
import android.opengl.GLES30;

import com.book2.Sample6_3.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**模型房间**/
public class RoomBox {
    private final Resources mRsc;
    private FloatBuffer mVertexBuffer;// 顶点坐标数据缓冲
    private FloatBuffer mUVBuffer;// 顶点坐标数据缓冲
    private FloatBuffer mColorBuffer;// 颜色坐标数据缓冲
    private String mVertexShader;// 顶点着色器代码脚本
    private String mFragmentShader;// 片元着色器代码脚本
    private int mProgram;
    private int muMVPMatrixHandle;
    private int muMVPMatrixHandleWithoutRotate;
    private int mObjMatrixHandle;
    private int maPositionPointer;
    private int mVTexCoordPointer;
    private int mLightPosPointer;
    /**光照模式指针**/
    private int mFuncChoicePointer;
    /**光照模式本地记录**/
    private int mLightMode;
    private int mVertxColorPointer;
    private float mLightPos[];

    /**光照模式表，与fragShaderRoom可以提供的模式一一对应**/
    public enum LightMode {
        BY_DOT_PRODUCT,
        BY_DISTANCE,
    }


    private float vertx[] = {
            //底面:
            2f, -2f, -2,
            -2f, -2f, -2,
            2f, 2f, -2,
            -2f, 2f, -2,
    }; //其他面通过旋转这个面来创造

    public RoomBox(Resources resources, float ratio) {
        mRsc = resources;
        initShader();
        initVertx();
    }

    /**
     * 加载shader
     **/
    public void initShader() {
        mVertexShader = ShaderUtil.loadFromAssetsFile("cjztest/lightmodel/vertShaderRoom.shader", mRsc);
        mFragmentShader = ShaderUtil.loadFromAssetsFile("cjztest/lightmodel/fragShaderRoom.shader",
                mRsc);
        mProgram = ShaderUtil.createProgram(mVertexShader, mFragmentShader);
        //变换矩阵索引
        muMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        muMVPMatrixHandleWithoutRotate = GLES30.glGetUniformLocation(mProgram, "uMVPMatrixWithoutRotate");
        mObjMatrixHandle = GLES30.glGetUniformLocation(mProgram, "objMatrix");
        // 获取程序中顶点位置属性引用
        maPositionPointer = GLES30.glGetAttribLocation(mProgram, "objectPosition");
        mVertxColorPointer = GLES30.glGetAttribLocation(mProgram, "objectColor");
        //纹理采样坐标
        mVTexCoordPointer = GLES30.glGetAttribLocation(mProgram, "vTexCoord");
        mLightPosPointer = GLES30.glGetUniformLocation(mProgram, "lightDotPos");
        mFuncChoicePointer = GLES30.glGetUniformLocation(mProgram, "funcChoice");
    }

    private void initVertx() {
        float texCoor[] = new float[]   //纹理内采样坐标,类似于canvas坐标
                {
                        1.0f, 0.0f,
                        0.0f, 0.0f,
                        1.0f, 1.0f,
                        0.0f, 1.0f
                };

        float colors[] = {
                1f, 0f, 0f, 1f,
                0f, 1f, 0f, 1f,
                0f, 0f, 1f, 1f,
                0f, 1f, 1f, 1f,
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertx.length * 4);
        vbb.order(ByteOrder.nativeOrder());// 设置字节顺序
        mVertexBuffer = vbb.asFloatBuffer();// 转换为float型缓冲
        mVertexBuffer.put(vertx);// 向缓冲区中放入顶点坐标数据
        mVertexBuffer.position(0);// 设置缓冲区起始位置

        ByteBuffer fragBB = ByteBuffer.allocateDirect(texCoor.length * 4);
        fragBB.order(ByteOrder.nativeOrder());// 设置字节顺序
        mUVBuffer = fragBB.asFloatBuffer();// 转换为float型缓冲
        mUVBuffer.put(texCoor);// 向缓冲区中放入顶点坐标数据
        mUVBuffer.position(0);// 设置缓冲区起始位置

        ByteBuffer colorBB = ByteBuffer.allocateDirect(colors.length * 4);
        colorBB.order(ByteOrder.nativeOrder());// 设置字节顺序
        mColorBuffer = colorBB.asFloatBuffer();// 转换为float型缓冲
        mColorBuffer.put(colors);// 向缓冲区中放入顶点坐标数据
        mColorBuffer.position(0);// 设置缓冲区起始位置

    }

    /**传入全方向射发射光线的坐标**/
    public void setLightPosition(float lightPosBuf[]) {
        mLightPos = lightPosBuf;
    }

    /**关照模式设定**/
    public void setLightMode(LightMode lightMode) {
        mLightMode = lightMode.ordinal();
    }

    /**进行绘制**/
    public void draw() {
        GLES30.glUseProgram(mProgram);
        /**光照模式设定**/
        GLES30.glUniform1i(mFuncChoicePointer, mLightMode);

        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false,
                MatrixState.getFinalMatrix(), 0);
        //光斑不跟随房间旋转，只应用和旋转无关的变换（不然光斑不动，但光线效果跟着房间走了）
        MatrixState.pushMatrix();
        MatrixState.reverseTotalRotate();
        GLES30.glUniformMatrix4fv(muMVPMatrixHandleWithoutRotate, 1, false,
                MatrixState.getFinalMatrix(), 0);
        MatrixState.popMatrix();
        GLES30.glUniformMatrix4fv(mObjMatrixHandle, 1, false, MatrixState.getMMatrix(), 0);
        GLES30.glVertexAttribPointer(maPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mVertexBuffer);
        GLES30.glUniform3fv(mLightPosPointer, 1, mLightPos, 0);
        GLES30.glVertexAttribPointer(mVTexCoordPointer, 2, GLES30.GL_FLOAT, false, 0, mUVBuffer);  //二维向量，size为2
        GLES30.glVertexAttribPointer(mVertxColorPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuffer);

        GLES30.glEnableVertexAttribArray(maPositionPointer); //启用顶点属性
        GLES30.glEnableVertexAttribArray(mVTexCoordPointer); //启用顶点属性
        GLES30.glEnableVertexAttribArray(mVertxColorPointer); //启用顶点属性


        MatrixState.pushMatrix();

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertx.length / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）

        MatrixState.rotate(90, 0, 1, 0);
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false,
                MatrixState.getFinalMatrix(), 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertx.length / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）

        MatrixState.rotate(-180, 0, 1, 0);
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false,
                MatrixState.getFinalMatrix(), 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertx.length / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）

        MatrixState.popMatrix();

        MatrixState.pushMatrix();

        MatrixState.rotate(90, 1, 0, 0);
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false,
                MatrixState.getFinalMatrix(), 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertx.length / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）

        MatrixState.rotate(-180, 1, 1, 0);
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false,
                MatrixState.getFinalMatrix(), 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertx.length / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）

        MatrixState.popMatrix();


        GLES30.glDisableVertexAttribArray(maPositionPointer); //启用顶点属性
        GLES30.glDisableVertexAttribArray(mVTexCoordPointer); //启用顶点属性
        GLES30.glDisableVertexAttribArray(mVertxColorPointer); //启用顶点属性
    }
}