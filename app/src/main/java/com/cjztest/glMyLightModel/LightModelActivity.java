package com.cjztest.glMyLightModel;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.example.learnopengl.R;

public class LightModelActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new LightControlSurfaceView(this));
    }
}
