package com.pboPicProcess;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.example.learnopengl.R;

/**基于PBO的图片打开与处理的Demo**/
public class PboPicProcess extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pbo_pic_process_main_page);
    }
}
