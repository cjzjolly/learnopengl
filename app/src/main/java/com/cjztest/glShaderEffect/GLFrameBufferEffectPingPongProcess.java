package com.cjztest.glShaderEffect;

import android.content.Context;

/**todo 一种使用双FrameBuffer轮换实现纯GPU一边渲染一边迭代运算，以小球刚性碰撞物理效果为例**/
public class GLFrameBufferEffectPingPongProcess extends GLLine {
    private void createBalls() {

    }

    /**把数据结构通过纹理的方式进行表达**/
    private void transBallToTextureObjs() {

    }

    public GLFrameBufferEffectPingPongProcess(int baseProgramPointer, float x, float y, float z, float w, float h, int windowW, int windowH, Context context) {
        super(baseProgramPointer);
    }

}
