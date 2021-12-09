package com.cjztest.glShaderEffect;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {

    private PanelView panelView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        panelView = new PanelView(this);
        setContentView(panelView);
        panelView.getRender().setOndrawListener(new EffectLayerBlendTest());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }
}