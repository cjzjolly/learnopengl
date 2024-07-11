package com.cjztest.glOffscreenProcess.demo1

import android.app.Activity
import android.os.Bundle

class NativeModifySurfaceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(NativeModifySurfaceView(this))
    }
}