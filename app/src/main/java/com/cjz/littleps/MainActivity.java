package com.cjz.littleps;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.cjztest.glShaderEffect.GLFrameBufferEffectPingPongSave;
import com.cjztest.glShaderEffect.PanelView;
import com.example.learnopengl.R;

public class MainActivity extends Activity implements View.OnClickListener {

    private PanelView panelView;
    private PSEffectLayer mPSEffectLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ps_activity_main);
        panelView = findViewById(R.id.pv_shader_effect);
        findViewById(R.id.btn_twirl).setOnClickListener(this);
        findViewById(R.id.btn_twirl_ccw).setOnClickListener(this);
        findViewById(R.id.btn_scale).setOnClickListener(this);
        findViewById(R.id.btn_scale_small).setOnClickListener(this);
        findViewById(R.id.btn_squash).setOnClickListener(this);
        findViewById(R.id.btn_save).setOnClickListener(this);
        mPSEffectLayer = new PSEffectLayer();
        panelView.getRender().setOndrawListener(mPSEffectLayer);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_scale) {
            mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_BE_BIGGER);
        } else if (id == R.id.btn_scale_small) {
            mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_BE_SMALLER);
        } else if (id == R.id.btn_twirl) {
            mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_TWIRL_CW);
        } else if (id == R.id.btn_twirl_ccw) {
            mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_TWIRL_CCW);
        } else if (id == R.id.btn_squash) {
            mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_SQUASH);
        } else if (id == R.id.btn_save) {
            mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.SAVE);

            //注意：这里保留了原本的 postDelayed 逻辑
            v.postDelayed(() -> {
                ImageView ivSave = findViewById(R.id.iv_save_bmp);
                if (ivSave != null) {
                    ivSave.setRotation(180);
                    ivSave.setRotationY(180);
                    ivSave.setImageBitmap(mPSEffectLayer.getFBEDC().getSaveBmp());
                }
            }, 100);
        }
    }
}