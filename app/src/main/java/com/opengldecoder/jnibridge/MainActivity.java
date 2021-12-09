package com.opengldecoder.jnibridge;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.ffmpeg.FFMpegUtil;

public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //分配权限
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(permissions[1]) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(permissions);
        }
        setContentView(new NativeGLSurfaceView(this));
        FFMpegUtil.version();
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