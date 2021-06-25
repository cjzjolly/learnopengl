package com.opengldecoder.jnibridge;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.cjztest.glShaderEffect.EffectLayerTest;
import com.cjztest.glShaderEffect.PanelView;
import com.ffmpeg.FFMpegUtil;

public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new NativeGLSurfaceView(this));
        FFMpegUtil.version();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }
}