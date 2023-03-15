package com.cjztest.glShaderEffect;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.learnopengl.R;

public class MainActivity extends Activity implements View.OnClickListener {

    private PanelView panelView;
    private EffectLayerBlendTest mEffectLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shader_effect_choice);
        LinearLayout btns = findViewById(R.id.ll_shader_effect_btn_container);
        for (int i = 0; i < btns.getChildCount(); i++) {
            btns.getChildAt(i).setOnClickListener(this);
        }
//        findViewById(R.id.btn_light_mode).setOnClickListener(this);
        panelView = findViewById(R.id.pv_shader_effect);
        mEffectLayer = new EffectLayerBlendTest();
        panelView.getRender().setOndrawListener(mEffectLayer);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_light_mode:
                mEffectLayer.selectMode(EffectLayerBlendTest.RENDERER_EFFECT.LIGHT_POTS);
                break;
            case R.id.btn_sea_mode:
                mEffectLayer.selectMode(EffectLayerBlendTest.RENDERER_EFFECT.SEA);
                break;
        }
    }
}