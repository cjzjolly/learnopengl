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
    float t = frame / 100.0;
    float z = sin(fragVTexCoord.s * resolution.x / 2000.0 + t * 1.0) + cos(fragVTexCoord.t * 1.2 * resolution.y / 300.0 + t) + 2.0 + 0.3;
    vec3 color = vec3(0.5, 0.7, 0.0) * z * 0.3;
    fragColor = vec4(color, 1.0);
}