package com.cjztest.gldrawlinesByMultiVectors;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.example.learnopengl.R;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_lines);
        ViewGroup container = findViewById(R.id.draw_lines_canvas_container);
        container.addView(new LinesCanvasSurface(this));
    }
}
