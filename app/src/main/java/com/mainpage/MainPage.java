package com.mainpage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.opengldecoder.jnibridge.NativeOpenGLOESRenderActivity;

public class MainPage extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(linearLayout);
        Button button = new Button(this);
        button.setText("shader特效demo");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.cjztest.glShaderEffect.MainActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);
        button = new Button(this);
        button.setText("PS FBO例子");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.cjz.littleps.MainActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);
        button = new Button(this);
        button.setText("Native OpenGL视频播放处理");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, NativeOpenGLOESRenderActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);
    }
}
