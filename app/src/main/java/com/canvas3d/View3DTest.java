package com.canvas3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class View3DTest extends View {

    public View3DTest(Context context) {
        super(context);
    }

    public View3DTest(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public View3DTest(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.RED);
    }
}
