package com.androidEffect.snowFlow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Snow extends View {
    private int mWidth;
    private int mHeight;
    private List<SnowItem> mSnowList;
    Random random = new Random();

    private class SnowItem {
        public int x;
        public int y;
        public int targetX;
        public float step;
    }

    public Snow(Context context) {
        super(context);
    }

    public Snow(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Snow(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        mSnowList = new ArrayList<>();
        for(int i = 0; i < 20; i++) {
            SnowItem s = new SnowItem();
            s.x = mWidth / 20 * i;
            s.y = (int) (Math.random() * mHeight);
            s.targetX = s.x + random.nextInt(100) - 20;
            s.step = (float) (s.targetX - s.x) / (mHeight - s.y);
            Log.i("cjztest", "step:" + s.step);
            mSnowList.add(s);
        }
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(int i = 0; i < mSnowList.size(); i++) {
                    SnowItem s = mSnowList.get(i);
                    s.x += s.step * 100;
                    s.y += 10;
                    if (s.y >= mHeight) {
                        s.x = mWidth / mSnowList.size() * i;
                        s.y = 0;
                        s.targetX = s.x + random.nextInt(100) - 20;
                        s.step = (float) (s.targetX - s.x) / (mHeight - s.y);
                    }
                }
                invalidate();
            }
        }).start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (w != mWidth || h != mHeight) { //已经onMeasuer过一次，除非界面大小改动否则不重新初始化view
            mWidth = w;
            mHeight = h;
            init();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.RED);
        p.setStrokeWidth(20f);
        p.setAntiAlias(true);
        if (mSnowList != null) {
            for (SnowItem s : mSnowList) {
                canvas.drawCircle(s.x, s.y, 20, p);
            }
        }
    }
}
