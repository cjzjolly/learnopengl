#version 300 es
precision highp float;
uniform sampler2D sTexture;//纹理输入
uniform float frame;//第几帧
uniform vec2 resolution;//分辨率
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色\n

#define PI 3.1415926535

float rand(vec2 p){
    return fract(sin(dot(p.xy,vec2(12.9898,78.233)))*43758.5453);
}

mat2 rot(float t) {
    return mat2(cos(t),-sin(t),
    sin(t), cos(t));
}

// point to flower color, with alpha
vec4 flower(vec2 p, float n, float seed) {
    if (p.y > 1.0 || p.y < 0.0) return vec4(0.0);
    vec2 c = vec2(floor(p.x)+0.5, 0.5);
    c.x += rand(vec2(c.x+seed*100.0, c.y*20.134))*.32-.16;
    c.y += rand(vec2(c.x+seed*200.0, c.y*30.412))*.32-.16;
    vec2 v = p-c;

    float rot = rand(vec2(c.x+seed, c.y+10.512))*1.4-0.7;
    float t = atan(v.y, v.x)+(rand(c)*2.0-1.0)*2.0*PI+frame / 100.0*rot;
    float r = sin(t*n)*0.05+.25;
    float rr = cos(t*n)*0.005+.08;
    float l = length(v);

    // petal
    float petal = smoothstep(0.02, 0.0, l-r);
    // pistil
    float pistil = smoothstep(0.05, -0.0, l-rr);
    // border
    float border = smoothstep(-0.09, 0.0, l-r)*0.3;

    return vec4(vec3(0.5+pistil*0.5)-border, petal);
}

// beat value, 0->1, 1->0
float beat(float x) {
    float temp = x-1.0;
    temp *= temp;
    temp *= temp;
    return temp*(cos(30.0*x)*.5+.5);
}

// point to background color value
vec3 backgroundColor(vec2 p) {
    vec3 color = vec3(.3, .05, .2);

    // add a star
    float t = atan(p.y, p.x) / PI;
    t *= 5.0;
    t += frame / 100.0*0.5;
    t = abs(fract(t)*2.0-1.0);
    float star = smoothstep(0.5, 0.6, t);
    color = mix(color, vec3(0.5, 0.2, 0.4), star);

    // add some flowers
    p.y+=3.3;
    p *= 0.2;
    for (float i = 0.0 ; i < 5.5 ; i++) {
        vec2 pp = p;
        pp *= rot(.05*sin(2.0*frame / 100.0+2.0*PI*rand(vec2(i,1.0))));
        pp.x += frame / 100.0*(rand(vec2(i,2.0))*2.0-1.0)*(i+3.0)*.12;
        pp.y += sin(frame / 100.0+2.0*PI*rand(vec2(i,3.0)))*.1;
        vec4 flowerValue = flower(pp, 5.0+floor(i*.5), i);
        p.y += 0.02;
        p /= 0.9;

        vec3 flowerColor = vec3(rand(vec2(i, 19.0))*1.0,
        rand(vec2(i, 18.0))*0.2,
        rand(vec2(i, 16.0))*0.8);

        flowerValue.rgb = flowerColor*flowerValue.rgb*3.0+flowerValue.rgb;
        color = mix(color, flowerValue.rgb, flowerValue.a);
    }

    return color;
}

// point to heart value
float heartFormula(vec2 p, bool time) {
    // heartbeat
    if (time) {
        float beatValue = beat(fract(0.824*frame / 100.0))*0.1;
        p.x *= 1.0 + beatValue * 2.0;
        p.y *= 1.0 - beatValue * 1.5;
    }
    // center the heart around the axis
    p.y -= 1.6;
    // see http://mathworld.wolfram.com/HeartCurve.html
    float t = atan(p.y, p.x);
    float si = sin(t);
    float r = 2.0-2.0*si+si*(sqrt(abs(cos(t)))/(si+1.4));
    return length(p)-r;
}

// heart value to heart color with alpha
vec4 heartColor(vec2 p) {
    float v = heartFormula(p, true);
    vec3 color = vec3(1.0, 0.5, 0.8);
    color -= smoothstep(-0.8, +0.5, v)*vec3(0.6);
    color += smoothstep(-0.0, -1.6, v)*vec3(.4);
    color -= smoothstep(-0.2, +0.2, v)*vec3(0.1);
    return vec4(color, smoothstep(0.2, 0.1, v));
}

// opening
float opening(vec2 p) {
    float mult = max(0.0, 5.0-frame / 100.0*1.5);
    p *= 3.0*mult*mult*mult;
    p *= rot(sin(frame / 100.0*6.0)*.2);
    float v = heartFormula(p, false);
    return smoothstep(-0.5, 0.5, v);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
//    vec2 uv = fragCoord.xy / resolution.xy * 2.0 - 1.0;
    vec2 uv = fragCoord * 2.0 - 1.0;
    uv.x *= resolution.x / resolution.y;
    float mult = 3.0+4.0*beat(min(1.0, 0.09*frame / 100.0));
    uv *= mult;

    // get background
    vec3 color = backgroundColor(uv);
    // heart formula and color
    vec4 hcolor = heartColor(uv*mult*.32);
    // and blend with heart color
    color = mix(color, hcolor.rgb, hcolor.a);
    color = clamp(color, 0.0, 1.0);
    // set opening
    color -= opening(uv);

    fragColor.rgb = color;
    fragColor.a = 1.0;
}

void main() {
    mainImage(fragColor, fragVTexCoord);
    //    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
}