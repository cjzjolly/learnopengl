package com.physicEffect.threeDWave

import android.app.Activity
import android.os.Bundle

class MainActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ThreeDimensionWaveGLAnimate(this))
    }
}