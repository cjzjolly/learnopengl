package com.mainpage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.book2.Sample5_2.Sample5_2_Activity;
import com.book2.EntranceActivity;
import com.cjztest.glOffscreenProcess.MenuActivity;
import com.opengldecoder.jnibridge.NativeOpenGLOESRenderActivity;

public class MainPage extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(linearLayout);
        Button button = new Button(this);
        button.setText("透视投影矩阵Demo");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, Sample5_2_Activity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);

        button = new Button(this);
        button.setText("书本例子");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntranceActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);

        button = new Button(this);
        button.setText("线条绘制demo1");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.cjztest.gldrawline.MainActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);

        button = new Button(this);
        button.setText("线条绘制demo2");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.cjztest.gldrawlinesByMultiVectors.MainActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);

        button = new Button(this);
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

        button = new Button(this);
        button.setText("EGL、离屏渲染使用相关例子");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);

        button = new Button(this);
        button.setText("实时YUV转RGB并输出到view");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.cjztest.glCameraPreview.MainActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);

        button = new Button(this);
        button.setText("光照实验");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.cjztest.glMyLightModelSimple.LightModelActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);

        button = new Button(this);
        button.setText("光照实验——进阶");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.cjztest.glMyLightModelAdvance.LightModelActivity.class);
            startActivity(intent);
        });
        linearLayout.addView(button);
    }
}
