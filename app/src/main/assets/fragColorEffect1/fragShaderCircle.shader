#version 300 es
precision highp float;
uniform vec2 resolution;//分辨率
uniform float r;//半径
uniform vec2 targetST;//纹理目标位置
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色

void main() {
    float ratio = resolution.x / resolution.y;
    vec2 stVal = vec2(fragVTexCoord.s * ratio, fragVTexCoord.t);
    vec2 tVal = vec2(targetST.s * ratio, targetST.t);
    if (distance(stVal, tVal) < r) {
//        fragColor = fragObjectColor; //实心圆
        fragColor = fragObjectColor * distance(stVal, tVal) / r; //中空渐变圆
    }
    //圆边框
//    if (distance(stVal, tVal) > r - 0.01f && distance(stVal, tVal) < r) {
//        fragColor = fragObjectColor;
//    }
}