package com.opengldecoder.jnibridge;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.example.learnopengl.R;
import com.ffmpeg.FFMpegUtil;

public class NativeOpenGLOESRenderActivity extends Activity implements View.OnClickListener {

    private NativeGLSurfaceView mNativeGLSurfaceView = null;
    private SeekBar mSeekBarBrightness = null;
    private SeekBar mSeekBarContrast = null;
    private SeekBar mSeekBarChannelRed = null;
    private SeekBar mSeekBarChannelGreen = null;
    private SeekBar mSeekBarChannelBlue = null;
    private CheckBox mCheckBox;

    private float rgb[] = {1f, 1f, 1f};
    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            switch (seekBar.getId()) {
                case R.id.seekBar_brightness:
                    mNativeGLSurfaceView.setRenderBrightness((float) progress / 100f - 1f);
                    break;

                case R.id.seekBar_channel_red:
                    rgb[0] = (float) progress / 100f;
                    mNativeGLSurfaceView.setRenderWhiteBalance(rgb[0], rgb[1], rgb[2]);
                    break;
                case R.id.seekBar_channel_green:
                    rgb[1] = (float) progress / 100f;
                    mNativeGLSurfaceView.setRenderWhiteBalance(rgb[0], rgb[1], rgb[2]);
                    break;
                case R.id.seekBar_channel_blue:
                    rgb[2] = (float) progress / 100f;
                    mNativeGLSurfaceView.setRenderWhiteBalance(rgb[0], rgb[1], rgb[2]);
                    break;
                case R.id.seekBar_contrast:
                    mNativeGLSurfaceView.setRenderContrast((float) progress / 100f);
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("cjztest", "oncreate");
        super.onCreate(savedInstanceState);
        //分配权限
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(permissions[1]) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(permissions);
        }
        setContentView(R.layout.native_oes_render_test);
        mNativeGLSurfaceView = findViewById(R.id.ngls);
        mSeekBarBrightness = findViewById(R.id.seekBar_brightness);
        mSeekBarContrast = findViewById(R.id.seekBar_contrast);
        mSeekBarChannelRed = findViewById(R.id.seekBar_channel_red);
        mSeekBarChannelGreen = findViewById(R.id.seekBar_channel_green);
        mSeekBarChannelBlue = findViewById(R.id.seekBar_channel_blue);
        mCheckBox = findViewById(R.id.cb_denoise);
        mSeekBarChannelRed.setMax(100);
        mSeekBarChannelRed.setProgress(100);
        mSeekBarChannelGreen.setMax(100);
        mSeekBarChannelGreen.setProgress(100);
        mSeekBarChannelBlue.setMax(100);
        mSeekBarChannelBlue.setProgress(100);
        mSeekBarBrightness.setMax(200);
        mSeekBarBrightness.setProgress(100);
        mSeekBarContrast.setMax(100);
        mSeekBarContrast.setProgress(0);
        mSeekBarBrightness.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarContrast.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarChannelRed.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarChannelGreen.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarChannelBlue.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mNativeGLSurfaceView.setRenderNoiseReductionOnOff(b);
            }
        });
    }

    protected void requestPermission(String permissions[]) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 1234);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        }
    }
}