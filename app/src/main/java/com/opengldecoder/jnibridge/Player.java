package com.opengldecoder.jnibridge;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.Surface;

import java.io.IOException;

public class Player {

    private MediaPlayer mMediaPlayer;

    public Player(Context context, Surface surface, MediaPlayer.OnVideoSizeChangedListener sizeChangedListener) {
        initMediaPlayer(context, surface, sizeChangedListener);
    }

    private void initMediaPlayer(Context context, Surface surface, MediaPlayer.OnVideoSizeChangedListener sizeChangedListener) {
        mMediaPlayer = new MediaPlayer();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("cross.mp4");
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
//            String path = "http://192.168.1.254:8192";
//            mediaPlayer.setDataSource(path);
//            mediaPlayer.setDataSource(TextureViewMediaActivity.videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.setOnVideoSizeChangedListener(sizeChangedListener);
        mMediaPlayer.setSurface(surface);
        mMediaPlayer.prepareAsync();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
    }

}
