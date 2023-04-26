package com.cjztest.glMyLightModelAdvance;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LightControlSurfaceView  extends GLSurfaceView {

    /**操作对象选择**/
    public enum TouchMode {
        SCENE,
        ONLY_LIGHT,
        LIGHT_END_VEC,
    }
    private TouchMode mTouchMode = TouchMode.SCENE;

    private final SceneRenderer mRenderer;

    /**光点**/
    private LightDot mLightDot;

    /**光线方向箭头**/
    private LightArrow mLightArrow;

    /**塑料盒子**/
    private RoomBox mRoomBox;


    private float mPreviousX;
    private float mPreviousY;
    private double mPreviousLength;

    public LightControlSurfaceView(Context context) {
        super(context);
        this.setEGLContextClientVersion(3); //设置使用OPENGL ES3.0
        mRenderer = new SceneRenderer();	//创建场景渲染器
        setRenderer(mRenderer);				//设置渲染器
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//设置渲染模式为主动渲染
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //把触摸事件转换为光源的顶点，单指移动，多点则缩放
        if (e.getPointerCount() > 1) {
            double distance = Math.sqrt(Math.pow(e.getX(0) - e.getX(1), 2)
                    + Math.pow(e.getY(0)- e.getY(1), 2));
            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    switch (mTouchMode) {
                        case SCENE:
                            MatrixState.scale((float) (distance / mPreviousLength));
        //                    MatrixState.translate(0,0,-(float) (distance / mPreviousLength));
                            break;
                        case ONLY_LIGHT:
                            mLightDot.translate(new float[] {0, 0, 1f - (float) (distance / mPreviousLength)});
                            break;
                    }
                    break;
            }
            mPreviousLength = distance;
        } else {
            float y = e.getY();//获取此次触控的y坐标
            float x = e.getX();//获取此次触控的x坐标
            float dy = y - mPreviousY;//计算触控位置的Y位移
            float dx = x - mPreviousX;//计算触控位置的X位移
            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE://若为移动动作
                    switch (mTouchMode) {
                        case SCENE:
                            MatrixState.rotate(dx / getWidth() * 360, 0, 1, 0);
                            MatrixState.rotate(dy / getHeight() * 360, 1, 0, 0);
                            break;
                        case ONLY_LIGHT:
                            //单独移动光点位置
                            if (null != mLightDot) {
                                mLightDot.translate(new float[] {dx / getWidth(), -dy / getHeight(), 0});
                            }
                            break;
                        case LIGHT_END_VEC: //todo 移动光点的终点坐标
                            if (null != mLightDot && null != mLightArrow) {
                                mLightArrow.setEndVec(new float[] {dx / getWidth(), -dy / getHeight(), 0});
                            }
                            break;
                    }
                    break;
            }
            mPreviousY = y;//记录触控笔y坐标
            mPreviousX = x;//记录触控笔x坐标
        }
        requestRender();
        return true;
    }

    private class SceneRenderer implements Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            //设置屏幕背景色RGBA
            GLES30.glClearColor(0.0f,0.0f,0.0f,1.0f);
            //打开深度检测
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glDepthFunc(GLES30.GL_LEQUAL); //还可以
            //混合模式等
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
            GLES30.glDisable(GLES30.GL_DITHER);
            //打开背面剪裁(demo 不要开)
//            GLES30.glEnable(GLES30.GL_CULL_FACE);
            //初始化变换矩阵
            MatrixState.setInitStack();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            //设置视窗大小及位置
            GLES30.glViewport(0, 0, width, height);
            //计算GLSurfaceView的宽高比
            Constant.ratio = (float) width / height;
            // 调用此方法计算产生透视投影矩阵
            MatrixState.setProjectFrustum(-Constant.ratio, Constant.ratio, -1, 1, 20, 100);
            // 调用此方法产生摄像机9参数位置矩阵
            MatrixState.setCamera(0, 0f, 30, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            mRoomBox = new RoomBox(getResources(), Constant.ratio);
            mLightDot = new LightDot(getResources(), Constant.ratio);
            mLightArrow = new LightArrow(getResources(), Constant.ratio);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            //清除深度缓冲与颜色缓冲
            GLES30.glClear( GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
            //设置屏幕背景色RGBA
            GLES30.glClearColor(0f,0f,0f, 1.0f);
            if (null != mLightDot) {
                mRoomBox.setLightPosition(mLightDot.getLightDotPos());
            }
            mRoomBox.draw();
            if (null != mLightDot && null != mLightArrow) {
                mLightArrow.setStartVec(mLightDot.getLightDotPos());
            }
            mLightArrow.draw();
            mLightDot.draw();
        }
    }

    /**触摸场景切换**/
    public void setMode(TouchMode mode) {
        mTouchMode = mode;
        requestRender();
    }

    /**光照方式切换**/
    public void setLightMode(RoomBox.LightMode lightMode) {
        if (null == mRoomBox) {
            return;
        }
        mRoomBox.setLightMode(lightMode);
        requestRender();
    }


}
