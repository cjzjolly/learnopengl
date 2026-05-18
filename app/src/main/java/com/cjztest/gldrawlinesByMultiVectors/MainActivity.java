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
        RadioGroup radioGroupDisplay = findViewById(R.id.display_lines_style_choice);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.draw_lines_style_normal) {
                linesCanvasSurface.setPenStyle(GLLineWithBezier.PenStyle.NORMAL);
            } else if (checkedId == R.id.draw_lines_style_pen) {
                linesCanvasSurface.setPenStyle(GLLineWithBezier.PenStyle.BY_ACC);
            } else if (checkedId == R.id.draw_lines_style_pen_of_device) {
                linesCanvasSurface.setPenStyle(GLLineWithBezier.PenStyle.BY_DEV_PRESSURE);
            }
        });

        radioGroupDisplay.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.display_style_tri) {
                linesCanvasSurface.setDisplayStyle(GLLineWithBezier.DisplayStyle.TRIANGLE_STRIPS);
            } else if (checkedId == R.id.display_style_line) {
                linesCanvasSurface.setDisplayStyle(GLLineWithBezier.DisplayStyle.LINE);
            }
        });

    }
}
