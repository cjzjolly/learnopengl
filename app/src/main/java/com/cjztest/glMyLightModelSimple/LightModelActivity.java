package com.cjztest.glMyLightModelSimple;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RadioButton;

import androidx.annotation.Nullable;

import com.example.learnopengl.R;

public class LightModelActivity extends Activity {
    private FrameLayout mFlContainer;
    private CheckBox mCBmoveModeSwitch;
    private RadioButton mCBLightMode0;
    private RadioButton mCBLightMode1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_model);
        LightControlSurfaceView surfaceView = new LightControlSurfaceView(this);
        mFlContainer = findViewById(R.id.fl_light_test_gl_container);
        mFlContainer.addView(surfaceView);
        mCBLightMode0 = findViewById(R.id.cb_light_mode_0);
        mCBLightMode1 = findViewById(R.id.cb_light_mode_1);
        mCBmoveModeSwitch = findViewById(R.id.cb_light_test_move_light_dot);
        mCBmoveModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                surfaceView.setMode(LightControlSurfaceView.TouchMode.ONLY_LIGHT);
            } else {
                surfaceView.setMode(LightControlSurfaceView.TouchMode.SCENE);
            }
        });
        mCBLightMode0.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                surfaceView.setLightMode(RoomBox.LightMode.BY_DISTANCE);
            }
        });
        mCBLightMode1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                surfaceView.setLightMode(RoomBox.LightMode.BY_DOT_PRODUCT);
            }
        });
    }
}
