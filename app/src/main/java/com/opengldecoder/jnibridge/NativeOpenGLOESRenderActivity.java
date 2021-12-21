package com.opengldecoder.jnibridge;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;

import com.example.learnopengl.R;

public class NativeOpenGLOESRenderActivity extends Activity implements View.OnClickListener {

    private NativeGLSurfaceView mNativeGLSurfaceView = null;
    private SeekBar mSeekBarBrightness = null;
    private SeekBar mSeekBarContrast = null;
    private SeekBar mSeekBarChannelRed = null;
    private SeekBar mSeekBarChannelGreen = null;
    private SeekBar mSeekBarChannelBlue = null;
    private SeekBar mSeekBarScaleX = null;
    private SeekBar mSeekBarScaleY = null;
    private SeekBar mSeekBarRotate = null;
    private CheckBox mCheckBoxDeNoise;
    private ListView mLutList;

    private float rgb[] = {1f, 1f, 1f};
    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        float scaleX = 1f;
        float scaleY = 1f;

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
                case R.id.seekBar_scale_x:
                    scaleX = (float) progress / 100f;
                    mNativeGLSurfaceView.setScale(scaleX, scaleY);
                    break;
                case R.id.seekBar_scale_y:
                    scaleY = (float) progress / 100f;
                    mNativeGLSurfaceView.setScale(scaleX, scaleY);
                    break;
                case R.id.seekBar_rotate:
                    mNativeGLSurfaceView.setRotate(progress);
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
    private CheckBox mLutCheckBox;
    private CheckBox mCheckBoxDeBackGround;

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
        mSeekBarScaleX = findViewById(R.id.seekBar_scale_x);
        mSeekBarScaleY = findViewById(R.id.seekBar_scale_y);
        mSeekBarRotate = findViewById(R.id.seekBar_rotate);
        mCheckBoxDeNoise = findViewById(R.id.cb_denoise);
        mCheckBoxDeBackGround = findViewById(R.id.cb_deBackground);
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
        mSeekBarScaleX.setMax(200);
        mSeekBarScaleX.setProgress(100);
        mSeekBarScaleY.setMax(200);
        mSeekBarScaleY.setProgress(100);
        mSeekBarRotate.setMax(360);
        mSeekBarRotate.setProgress(0);
        mSeekBarBrightness.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarContrast.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarChannelRed.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarChannelGreen.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarChannelBlue.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarScaleX.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarScaleY.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBarRotate.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mCheckBoxDeNoise.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mNativeGLSurfaceView.setRenderNoiseReductionOnOff(b);
            }
        });
        mLutCheckBox = findViewById(R.id.cb_lut);
        mLutList = findViewById(R.id.lv_lut_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[] {
                "黑色", "反差暖", "朦胧", "暖色", "鲜明"
        });
        mLutCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mNativeGLSurfaceView.setRenderLutOnOff(isChecked);
            }
        });
        mLutList.setAdapter(adapter);
        mLutList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mNativeGLSurfaceView.setLut(BitmapFactory.decodeResource(getResources(), R.mipmap.lut_hei_se));
                        break;
                    case 1:
                        mNativeGLSurfaceView.setLut(BitmapFactory.decodeResource(getResources(), R.mipmap.lut_fan_cha_nuan));
                        break;
                    case 2:
                        mNativeGLSurfaceView.setLut(BitmapFactory.decodeResource(getResources(), R.mipmap.lut_menglongf));
                        break;
                    case 3:
                        mNativeGLSurfaceView.setLut(BitmapFactory.decodeResource(getResources(), R.mipmap.lut_nuanse));
                        break;
                    case 4:
                        mNativeGLSurfaceView.setLut(BitmapFactory.decodeResource(getResources(), R.mipmap.lut_xianming));
                        break;
                }
            }
        });
        mCheckBoxDeBackGround.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mNativeGLSurfaceView.setRenderDeBackgroundOnOff(isChecked);
            }
        });

//        new Handler().postDelayed(() -> {
//            mNativeGLSurfaceView.setLut(BitmapFactory.decodeResource(getResources(), R.mipmap.lut_menglongf));
//        }, 800);
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