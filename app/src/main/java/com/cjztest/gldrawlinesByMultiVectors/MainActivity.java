package com.cjztest.gldrawlinesByMultiVectors;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;

import com.example.learnopengl.R;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_lines);
        ViewGroup container = findViewById(R.id.draw_lines_canvas_container);
        LinesCanvasSurface linesCanvasSurface =  new LinesCanvasSurface(this);
        container.addView(linesCanvasSurface);
        RadioGroup radioGroup = findViewById(R.id.draw_lines_style_choice);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.draw_lines_style_normal:
                        linesCanvasSurface.setPenStyle(GLLineWithBezier.PenStyle.NORMAL);
                        break;
                    case R.id.draw_lines_style_pen:
                        linesCanvasSurface.setPenStyle(GLLineWithBezier.PenStyle.BY_ACC);
                        break;
                    case R.id.draw_lines_style_pen_of_device:
                        linesCanvasSurface.setPenStyle(GLLineWithBezier.PenStyle.BY_DEV_PRESSURE);
                        break;
                }
            }
        });
    }
}
