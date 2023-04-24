#version 300 es
precision highp float;
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
in vec3 objPos;
out vec4 fragColor;//输出到的片元颜色\n

void main() {
    fragColor = vec4(0.0, 1.0, 0.0, 1.0);
}