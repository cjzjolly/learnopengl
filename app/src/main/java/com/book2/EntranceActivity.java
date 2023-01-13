package com.book2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.book2.Sample1_1.BufferObjectExampleActivity;
import com.book2.Sample1_5.FrameBufferObjectExampleActivity;
import com.book2.Sample8_1.MyActivity;
import com.book2.Sample5_2.Sample5_2_Activity;
import com.example.learnopengl.R;

public class EntranceActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_samples_menu);
        LinearLayout menusll = findViewById(R.id.books_samples_menu_ll);

        Button button = new Button(this);
        button.setText("圆柱体体验");
        button.setOnClickListener(v -> {
            startActivity(new Intent(this, MyActivity.class));
        });
        menusll.addView(button);

        button = new Button(this);
        button.setText("BufferObj体验");
        button.setOnClickListener(v -> {
            startActivity(new Intent(this, BufferObjectExampleActivity.class));
        });
        menusll.addView(button);

        button = new Button(this);
        button.setText("FBO体验");
        button.setOnClickListener(v -> {
            startActivity(new Intent(this, FrameBufferObjectExampleActivity.class));
        });
        menusll.addView(button);

        button = new Button(this);
        button.setText("透视投影体验");
        button.setOnClickListener(v -> {
            startActivity(new Intent(this, Sample5_2_Activity.class));
        });
        menusll.addView(button);
    }
}
