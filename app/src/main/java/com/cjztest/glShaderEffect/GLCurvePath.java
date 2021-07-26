package com.cjztest.glShaderEffect;

import android.graphics.Point;
import android.graphics.PointF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**曲线实现**/
public class GLCurvePath extends GLObject {
    //线条顶点集
    private List<PointF> mVertx = new ArrayList<>();
    //线条最小宽度
    private float mMinCurveWidth = 0.01f;
    private PointF mPrevTouchPoint = null;


    public void onTouch(MotionEvent event) {
       switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPrevTouchPoint = new PointF();
                mPrevTouchPoint.x = event.getX();
                mPrevTouchPoint.y = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                mPrevTouchPoint.x = event.getX();
                mPrevTouchPoint.y = event.getY();
                /*todo 现在和过去两个点连成线段，然后取其垂直线段作为线条结点条带的两个端点：
                *  1、先确定两点连线的旋转角度a
                *  2、把指定宽度作为长度的线条旋转a + 90度
                *  3、step2的线条的中点移动到step1线段端点处*/
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
    }


    private void drawCurve() {

    }

    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {

    }
}
