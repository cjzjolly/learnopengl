#version 300 es
precision highp float;
uniform sampler2D sTexture;//纹理输入
uniform float frame;//第几帧
uniform vec2 resolution;//分辨率
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色\n

//通过三角函数+帧数实现数值循环
#define CENTER(w, h)     vec2((cos(frame / 150.0 * w) + 1.) * 0.5, (sin(frame / 150.0 * h) + 1.)*0.5)
#define F_TIME(a, b)     a * fract(frame / 150.0 * b)
#define F_TIME_RE(a, b)    a - F_TIME(a, b)
#define C_TIME(a, b)        (cos(frame / 150.0 * a) + 1.) * b
#define S_TIME(a, b)        (sin(frame / 150.0 * a) + 1.) * b

//https://www.shadertoy.com/view/Wd23DG 漂亮的粒子效果
vec4 sum(vec4 a, vec4 b){
    return vec4(min(1., a.r + b.r), min(1., a.g + b.g), min(1., a.b + b.b), min(1., a.a + b.a));
}

/**uv 传入当前遍历到的纹理ST(x,y)坐标
   center 需要更改颜色的目标(x,y值)
   intensity 扩散程度**/
vec4 getSpotLightOne(vec2 uv, vec2 center, float intensity, vec3 color) {
    float ratio = resolution.x / resolution.y;
//    float ratio = 1.0;
    uv.x *= ratio;
    center.x *= ratio;
    //使用公式 1 / sqrt(当前遍历到的x,y 到 目标x,y 的距离)，实现二维平面范围内距离中心点越远颜色越非线性变淡的效果
    float dist = intensity/sqrt(distance(uv, center));
    return vec4(color * dist, dist); //传入颜色值乘以浓淡系数，得到距离目标中心越远越淡的效果（非线性）
}

void fireflyEffect(out vec4 fragColor, in vec2 fragCoord){
    vec2 uv = fragCoord;
    vec4 light1 = getSpotLightOne(uv, CENTER(0.5, 3.0), F_TIME(0.2, 0.3), vec3(0., 1., 0.));
    vec4 light2 = getSpotLightOne(uv, CENTER(2., 0.5), F_TIME_RE(0.15, .5), vec3(1., 1., 0.));
    vec4 light3 = getSpotLightOne(uv, CENTER(1.5, 0.3), S_TIME(3., .1), vec3(1., 0., 0.));
    vec4 light4 = getSpotLightOne(uv, CENTER(1., 1.3), C_TIME(3., .1), vec3(0., 1., 1.));
    vec4 light5 = getSpotLightOne(uv, CENTER(3., 2.3), F_TIME(.4, 1.1), vec3(0.5, 0.5, .5));
    //4个荧光点颜色叠加
    vec4 light = sum(light1, light2);
    light = sum(light, light3);
    light = sum(light, light4);
    light = sum(light, light5);
    fragColor = mix(vec4(0.), light, light.a);
}

void main() {
    fireflyEffect(fragColor, fragVTexCoord);
//    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
}