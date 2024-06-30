package com.cjztest.glOffscreenProcess

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.cjztest.glOffscreenProcess.demo0.OffScreenGLDemoActivity

class MenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.VERTICAL

        val btnDemo0 = Button(this)
        btnDemo0.text = "Demo0"
        btnDemo0.setOnClickListener {
            startActivity(Intent(this, OffScreenGLDemoActivity::class.java))
        }

        ll.addView(btnDemo0)
        setContentView(ll)
    }
}