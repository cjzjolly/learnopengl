#version 300 es
precision highp float;
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色\n

/**uv 传入当前遍历到的纹理ST(x,y)坐标
   center 需要更改颜色的目标(x,y值)
   intensity 扩散程度**/
vec4 getSpotLightOne(vec2 uv, vec2 center, float intensity, vec3 color) {
    //使用公式 1 / sqrt(当前遍历到的x,y 到 目标x,y 的距离)，实现二维平面范围内距离中心点越远颜色越非线性变淡的效果
    float dist = intensity / sqrt(distance(uv, center));
    return vec4(color * dist, dist * dist);
}

void fireflyEffect(out vec4 fragColor, in vec2 fragCoord) {
    fragColor = getSpotLightOne(fragCoord, vec2(0.5, 0.5), 0.2, vec3(1.0, 1.0, 1.0));
}

void main() {
    fireflyEffect(fragColor, fragVTexCoord);
}