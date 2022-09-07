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
        switch (v.getId()) {
            case R.id.btn_scale:
                mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_BE_BIGGER);
                break;
            case R.id.btn_scale_small:
                mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_BE_SMALLER);
                break;
            case R.id.btn_twirl:
                mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_TWIRL_CW);
                break;
            case R.id.btn_twirl_ccw:
                mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_TWIRL_CCW);
                break;
            case R.id.btn_squash:
                mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.PS_SQUASH);
                break;
            case R.id.btn_save:
                mPSEffectLayer.setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton.SAVE);
                v.postDelayed(() -> {
                    ((ImageView) findViewById(R.id.iv_save_bmp)).setRotation(180);
                    ((ImageView) findViewById(R.id.iv_save_bmp)).setRotationY(180);
                    ((ImageView) findViewById(R.id.iv_save_bmp)).setImageBitmap(mPSEffectLayer.getFBEDC().getSaveBmp());
                }, 100);
                break;
        }
    }
}