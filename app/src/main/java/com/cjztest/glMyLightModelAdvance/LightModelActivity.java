package com.cjztest.glMyLightModelAdvance;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.RadioButton;

import androidx.annotation.Nullable;

import com.example.learnopengl.R;

public class LightModelActivity extends Activity {
    private FrameLayout mFlContainer;
    private RadioButton mModeLightDotOp;
    private RadioButton mModeLightDoxVecOp;
    private RadioButton mModeBoxOp;
    private RadioButton mCBLightMode0;
    private RadioButton mCBLightMode1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_model_advance);
        LightControlSurfaceView surfaceView = new LightControlSurfaceView(this);
        mFlContainer = findViewById(R.id.fl_light_test_gl_container);
        mFlContainer.addView(surfaceView);
        mCBLightMode0 = findViewById(R.id.cb_light_mode_0);
        mCBLightMode1 = findViewById(R.id.cb_light_mode_1);
        mModeLightDotOp = findViewById(R.id.cb_light_test_move_light_dot);
        mModeBoxOp = findViewById(R.id.cb_light_test_move_and_scale_box);
        mModeLightDoxVecOp = findViewById(R.id.cb_light_test_vec);
        //只操作光点
        mModeLightDotOp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                surfaceView.setMode(LightControlSurfaceView.TouchMode.ONLY_LIGHT);
            }
        });
        //只操作盒子
        mModeBoxOp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                surfaceView.setMode(LightControlSurfaceView.TouchMode.SCENE);
            }
        });
        //只操作光点的终点位置:
        mModeLightDoxVecOp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                surfaceView.setMode(LightControlSurfaceView.TouchMode.LIGHT_END_VEC);
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
