package com.cjztest.glMyLightModel;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.example.learnopengl.R;

public class LightModelActivity extends Activity {
    private FrameLayout mFlContainer;
    private CheckBox mCBmoveModeSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_model);
        LightControlSurfaceView surfaceView = new LightControlSurfaceView(this);
        mFlContainer = findViewById(R.id.fl_light_test_gl_container);
        mFlContainer.addView(surfaceView);
        mCBmoveModeSwitch = findViewById(R.id.cb_light_test_move_light_dot);
        mCBmoveModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                surfaceView.setMode(LightControlSurfaceView.TouchMode.ONLY_LIGHT);
            } else {
                surfaceView.setMode(LightControlSurfaceView.TouchMode.SCENE);
            }
        });

    }
}
