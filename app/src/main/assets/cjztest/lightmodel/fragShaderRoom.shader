#version 300 es
precision highp float;
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
in vec4 fragObjectColor;
in vec3 objPos;
in vec3 lightPos;
out vec4 fragColor;//输出到的片元颜色\n

void main() {
    vec4 color = vec4(fragObjectColor.rgb * 1.0 / distance(normalize(lightPos), normalize(objPos)), fragObjectColor.a);
    fragColor = color;
}