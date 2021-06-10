#version 300 es
#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES sTexture; //内存纹理输入，不克隆到显存，每次渲染的时候到内存地址中拿
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序

void main() {
    gl_FragColor=texture2D(sTexture, fragVTexCoord);
}