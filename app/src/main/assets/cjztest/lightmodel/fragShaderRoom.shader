#version 300 es
precision highp float;
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
in vec4 fragObjectColor;
in vec3 objPos;
out vec4 fragColor;//输出到的片元颜色\n

void main() {
    fragColor = fragObjectColor;
//    fragColor = vec4(objPos, 1.0);
//    fragColor = fragObjectColor * objPos.x;
}