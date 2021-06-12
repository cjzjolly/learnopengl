#version 300 es
precision highp float;
uniform sampler2D sTexture;//纹理输入
uniform int funChoice;
uniform float frame;//第几帧
uniform vec2 resolution;//分辨率
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色

void main() {
    float target = (sin(frame / 10.0 + fragVTexCoord.s * resolution.x / 200.0) + 1.0) / 8.0;
    if (fragVTexCoord.t < target) {
        fragColor = vec4(0.5, 0.5, 1.0, 1.0 - 2.0 * distance(fragVTexCoord, vec2(fragVTexCoord.s, target)));
    }
    target = (sin(frame / 10.0 + fragVTexCoord.s * resolution.x / 100.0) + 1.0) / 16.0;
    if (fragVTexCoord.t < target) {
        vec4 newColor = vec4(0.8, 0.8, 1.0, 1.0 - 2.0 * distance(fragVTexCoord, vec2(fragVTexCoord.s, target)));
        fragColor = fragColor * newColor;
    }
}