package com.opengldecoder.jnibridge;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.learnopengl.R;
import com.ffmpeg.FFMpegUtil;

public class NativeOpenGLOESRenderActivity extends Activity implements View.OnClickListener {

    private NativeGLSurfaceView mNativeGLSurfaceView = null;

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