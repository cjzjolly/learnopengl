package com.cjztest.glOffscreenProcess

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.cjztest.glOffscreenProcess.demo0.OffScreenGLDemoActivity
import com.cjztest.glOffscreenProcess.demo1.NativeModifySurfaceActivity

class MenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.VERTICAL

        var btnDemo = Button(this)
        btnDemo.text = "Demo0"
        btnDemo.setOnClickListener {
            startActivity(Intent(this, OffScreenGLDemoActivity::class.java))
        }
        ll.addView(btnDemo)

        btnDemo = Button(this)
        btnDemo.text = "Demo1"
        btnDemo.setOnClickListener {
            startActivity(Intent(this, NativeModifySurfaceActivity::class.java))
        }
        ll.addView(btnDemo)
        setContentView(ll)
    }
}