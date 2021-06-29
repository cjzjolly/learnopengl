package com.cjztest.glCameraPreview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;

import com.cjztest.glShaderEffect.PanelView;

import java.util.concurrent.ExecutorService;

public class MainActivity extends Activity implements View.OnClickListener, Camera2Listener {

    private CameraEffectLayer mCameraEffectLayer;
    private byte[] mYuvBytes;
    private PanelView mPanelView;
    private FrameLayout mFrameLayout;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
        mPanelView = new PanelView(this);
        mCameraEffectLayer = new CameraEffectLayer(1920, 1080);
        mPanelView.getRender().setOndrawListener(mCameraEffectLayer);
        mFrameLayout = new FrameLayout(this);
        mFrameLayout.addView(mPanelView);
        setContentView(mFrameLayout);
        initCamera();
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

    private static final String TAG = "MainActivity";
    private Camera2Helper camera2Helper;
    // 默认打开的CAMERA
    private static final String CAMERA_ID = Camera2Helper.CAMERA_ID_BACK;
    // 图像帧数据，全局变量避免反复创建，降低gc频率
    private byte[] nv21;
    // 显示的旋转角度
    private int displayOrientation;
    // 是否手动镜像预览
    private boolean isMirrorPreview;
    // 实际打开的cameraId
    private String openedCameraId;
    // 当前获取的帧数
    private int currentIndex = 0;
    // 处理的间隔帧
    private static final int PROCESS_INTERVAL = 30;
    // 线程池
    private ExecutorService imageProcessExecutor;


    void initCamera() {
        TextureView textureView = new TextureView(this); //仅用作把预览打开的作用，但不显示出来
        camera2Helper = new Camera2Helper.Builder()
                .cameraListener(this)
                .maxPreviewSize(new Point(1920, 1080))
                .minPreviewSize(new Point(1280, 720))
                .specificCameraId(CAMERA_ID)
                .context(getApplicationContext())
                .previewOn(textureView)
                .previewViewSize(new Point(1920, 1080))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .isMirror(false)
                .build();
        camera2Helper.start();
        mFrameLayout.addView(textureView);
        textureView.setAlpha(0);
    }

    @Override
    protected void onPause() {
        if (camera2Helper != null) {
            camera2Helper.stop();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera2Helper != null) {
            camera2Helper.start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCameraOpened(CameraDevice cameraDevice, String cameraId, Size previewSize, int displayOrientation, boolean isMirror) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride) {
        Log.i("cjztest", "camera");
        byte yuv[] = new byte[y.length + u.length + v.length];
        System.arraycopy(y, 0, yuv, 0, y.length);
        System.arraycopy(u, 0, yuv, y.length, u.length);
        System.arraycopy(v, 0, yuv, y.length + u.length, v.length);
        mCameraEffectLayer.refreshYUV(yuv);
    }

    @Override
    public void onCameraClosed() {
        Log.i(TAG, "onCameraClosed: ");
    }

    @Override
    public void onCameraError(Exception e) {
        e.printStackTrace();
    }

    @Override
    protected void onDestroy() {
        if (imageProcessExecutor != null) {
            imageProcessExecutor.shutdown();
            imageProcessExecutor = null;
        }
        if (camera2Helper != null) {
            camera2Helper.release();
        }
        super.onDestroy();
    }

    public void switchCamera(View view) {
        if (camera2Helper != null) {
            camera2Helper.switchCamera();
        }
    }

}