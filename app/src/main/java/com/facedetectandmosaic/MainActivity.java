package com.facedetectandmosaic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.learnopengl.R;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CameraDemo";
    private static final int REQUEST_PERMISSIONS = 10;

    public GLSurfaceView glSurfaceView;
    private CameraPreviewRenderer renderer;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String outputFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_face_detect);

        glSurfaceView = findViewById(R.id.gl_surface_view);
        Button recordButton = findViewById(R.id.record_button);

        // 设置GLSurfaceView
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new CameraPreviewRenderer(this);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                recordButton.setText("开始录制");
            } else {
                startRecording();
                recordButton.setText("停止录制");
            }
        });

        requestPermissions();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
                return;
            }
        }

        // 权限已获取，启动相机
        renderer.startCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
//            boolean allGranted = true;
//            for (int result : grantResults) {
//                if (result != PackageManager.PERMISSION_GRANTED) {
//                    Log.e("cjztest", "失败权限:" + result);
//                    allGranted = false;
//                    break;
//                }
//            }
//
//            if (allGranted) {
                renderer.startCamera();
//            } else {
//                Toast.makeText(this, "需要所有权限才能运行应用", Toast.LENGTH_SHORT).show();
//                finish();
//            }
        }
    }

    private void startRecording() {
        try {
            outputFile = getExternalFilesDir(null) + "/recording_" +
                    System.currentTimeMillis() + ".mp4";

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(outputFile);
            mediaRecorder.setVideoEncodingBitRate(1000000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(renderer.getPreviewWidth(), renderer.getPreviewHeight());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            mediaRecorder.prepare();
            renderer.setMediaRecorder(mediaRecorder);
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "开始录制: " + outputFile);
        } catch (IOException e) {
            Log.e(TAG, "录制失败", e);
            Toast.makeText(this, "录制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                renderer.setMediaRecorder(null);
                isRecording = false;
                Log.d(TAG, "录制完成: " + outputFile);
                Toast.makeText(this, "录制完成: " + outputFile, Toast.LENGTH_LONG).show();
            } catch (RuntimeException e) {
                Log.e(TAG, "停止录制失败", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        if (isRecording) {
            stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) {
            stopRecording();
        }
        renderer.release();
    }
}