#version 300 es
precision highp float;
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色\n

///**uv 传入当前遍历到的纹理ST(x,y)坐标
//   center 需要更改颜色的目标(x,y值)
//   intensity 扩散程度**/
//vec4 getSpotLightOne(vec2 uv, vec2 center, float intensity, vec3 color) {
//    float ratio = resolution.x / resolution.y;
////    float ratio = 1.0;
//    uv.x *= ratio;
//    center.x *= ratio;
//    //使用公式 1 / sqrt(当前遍历到的x,y 到 目标x,y 的距离)，实现二维平面范围内距离中心点越远颜色越非线性变淡的效果
//    float dist = intensity/sqrt(distance(uv, center));
//    return vec4(color * dist, dist); //传入颜色值乘以浓淡系数，得到距离目标中心越远越淡的效果（非线性）
//}
//
//void fireflyEffect(out vec4 fragColor, in vec2 fragCoord){
//    vec2 uv = fragCoord;
//    vec4 light1 = getSpotLightOne(uv, CENTER(objPos.x, objPos.y), objPos.z, vec4(1.0, 1.0, 1.0, 1.0));
//    fragColor = light1;
//}

void main() {
//    fireflyEffect(fragColor, fragVTexCoord);
//    fragColor = vec4(1.0, 1.0, 1.0, 1.0);
    fragColor = vec4(0.0, 1.0, 1.0, 1.0);
}