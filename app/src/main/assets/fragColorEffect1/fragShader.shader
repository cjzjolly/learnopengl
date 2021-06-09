#version 300 es
precision highp float;
uniform sampler2D sTexture;//纹理输入
uniform int funChoice;
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









/*
 * "Seascape" by Alexander Alekseev aka TDM - 2014
 * License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * Contact: tdmaav@gmail.com
 */

//const int NUM_STEPS = 8;
const int NUM_STEPS = 3; //减少细节提高速度
//const float PI	 	= 3.141592;
const float PI	 	= 3.14; //降低精度提高效率
const float EPSILON	= 1e-3;
//#define EPSILON_NRM (0.1 / iResolution.x)
#define EPSILON_NRM (0.1 / resolution.x)
#define AA

// sea
const int ITER_GEOMETRY = 3;
const int ITER_FRAGMENT = 5;
const float SEA_HEIGHT = 0.6;
const float SEA_CHOPPY = 4.0;
const float SEA_SPEED = 0.8;
const float SEA_FREQ = 0.16;
const vec3 SEA_BASE = vec3(0.0,0.09,0.18);
const vec3 SEA_WATER_COLOR = vec3(0.8,0.9,0.6)*0.6;
#define SEA_TIME (1.0 + frame / 50.0 * SEA_SPEED)
const mat2 octave_m = mat2(1.6,1.2,-1.2,1.6);

// math
mat3 fromEuler(vec3 ang) {
    vec2 a1 = vec2(sin(ang.x),cos(ang.x));
    vec2 a2 = vec2(sin(ang.y),cos(ang.y));
    vec2 a3 = vec2(sin(ang.z),cos(ang.z));
    mat3 m;
    m[0] = vec3(a1.y*a3.y+a1.x*a2.x*a3.x,a1.y*a2.x*a3.x+a3.y*a1.x,-a2.y*a3.x);
    m[1] = vec3(-a2.y*a1.x,a1.y*a2.y,a2.x);
    m[2] = vec3(a3.y*a1.x*a2.x+a1.y*a3.x,a1.x*a3.x-a1.y*a3.y*a2.x,a2.y*a3.y);
    return m;
}
float hash( vec2 p ) {
    float h = dot(p,vec2(127.1,311.7));
    return fract(sin(h)*43758.5453123);
}
float noise( in vec2 p ) {
    vec2 i = floor( p );
    vec2 f = fract( p );
    vec2 u = f*f*(3.0-2.0*f);
    return -1.0+2.0*mix( mix( hash( i + vec2(0.0,0.0) ),
    hash( i + vec2(1.0,0.0) ), u.x),
    mix( hash( i + vec2(0.0,1.0) ),
    hash( i + vec2(1.0,1.0) ), u.x), u.y);
}

// lighting
float diffuse(vec3 n,vec3 l,float p) {
    return pow(dot(n,l) * 0.4 + 0.6,p);
}
float specular(vec3 n,vec3 l,vec3 e,float s) {
    float nrm = (s + 8.0) / (PI * 8.0);
    return pow(max(dot(reflect(e,n),l),0.0),s) * nrm;
}

// sky
vec3 getSkyColor(vec3 e) {
    e.y = (max(e.y,0.0)*0.8+0.2)*0.8;
    return vec3(pow(1.0-e.y,2.0), 1.0-e.y, 0.6+(1.0-e.y)*0.4) * 1.1;
}

// sea
float sea_octave(vec2 uv, float choppy) {
    uv += noise(uv);
    vec2 wv = 1.0-abs(sin(uv));
    vec2 swv = abs(cos(uv));
    wv = mix(wv,swv,wv);
    return pow(1.0-pow(wv.x * wv.y,0.65),choppy);
}

float map(vec3 p) {
    float freq = SEA_FREQ;
    float amp = SEA_HEIGHT;
    float choppy = SEA_CHOPPY;
    vec2 uv = p.xz; uv.x *= 0.75;

    float d, h = 0.0;
    for(int i = 0; i < ITER_GEOMETRY; i++) {
        d = sea_octave((uv+SEA_TIME)*freq,choppy);
        d += sea_octave((uv-SEA_TIME)*freq,choppy);
        h += d * amp;
        uv *= octave_m; freq *= 1.9; amp *= 0.22;
        choppy = mix(choppy,1.0,0.2);
    }
    return p.y - h;
}

float map_detailed(vec3 p) {
    float freq = SEA_FREQ;
    float amp = SEA_HEIGHT;
    float choppy = SEA_CHOPPY;
    vec2 uv = p.xz; uv.x *= 0.75;

    float d, h = 0.0;
    for(int i = 0; i < ITER_FRAGMENT; i++) {
        d = sea_octave((uv+SEA_TIME)*freq,choppy);
        d += sea_octave((uv-SEA_TIME)*freq,choppy);
        h += d * amp;
        uv *= octave_m; freq *= 1.9; amp *= 0.22;
        choppy = mix(choppy,1.0,0.2);
    }
    return p.y - h;
}

vec3 getSeaColor(vec3 p, vec3 n, vec3 l, vec3 eye, vec3 dist) {
    float fresnel = clamp(1.0 - dot(n,-eye), 0.0, 1.0);
    fresnel = pow(fresnel,3.0) * 0.5;

    vec3 reflected = getSkyColor(reflect(eye,n));
    vec3 refracted = SEA_BASE + diffuse(n,l,80.0) * SEA_WATER_COLOR * 0.12;

    vec3 color = mix(refracted,reflected,fresnel);

    float atten = max(1.0 - dot(dist,dist) * 0.001, 0.0);
    color += SEA_WATER_COLOR * (p.y - SEA_HEIGHT) * 0.18 * atten;

    color += vec3(specular(n,l,eye,60.0));

    return color;
}

// tracing
vec3 getNormal(vec3 p, float eps) {
    vec3 n;
    n.y = map_detailed(p);
    n.x = map_detailed(vec3(p.x+eps,p.y,p.z)) - n.y;
    n.z = map_detailed(vec3(p.x,p.y,p.z+eps)) - n.y;
    n.y = eps;
    return normalize(n);
}

float heightMapTracing(vec3 ori, vec3 dir, out vec3 p) {
    float tm = 0.0;
    float tx = 1000.0;
    float hx = map(ori + dir * tx);
    if(hx > 0.0) return tx;
    float hm = map(ori + dir * tm);
    float tmid = 0.0;
    for(int i = 0; i < NUM_STEPS; i++) {
        tmid = mix(tm,tx, hm/(hm-hx));
        p = ori + dir * tmid;
        float hmid = map(p);
        if(hmid < 0.0) {
            tx = tmid;
            hx = hmid;
        } else {
            tm = tmid;
            hm = hmid;
        }
    }
    return tmid;
}

vec3 getPixel(in vec2 coord, float time) {
//    vec2 uv = coord / iResolution.xy; //shadertoy上的纹理坐标是实际屏幕大小，但这里本来就是0~1，所以不用转换
    vec2 uv = coord;
    uv = uv * 2.0 - 1.0;
    uv.x *= resolution.x / resolution.y;

    // ray
    vec3 ang = vec3(sin(time*3.0)*0.1,sin(time)*0.2+0.3,time);
    vec3 ori = vec3(0.0,3.5,time*5.0);
    vec3 dir = normalize(vec3(uv.xy,-2.0)); dir.z += length(uv) * 0.14;
    dir = normalize(dir) * fromEuler(ang);

    // tracing
    vec3 p;
    heightMapTracing(ori,dir,p);
    vec3 dist = p - ori;
    vec3 n = getNormal(p, dot(dist,dist) * EPSILON_NRM);
    vec3 light = normalize(vec3(0.0,1.0,0.8));

    // color
    return mix(
    getSkyColor(dir),
    getSeaColor(p,n,light,dir,dist),
    pow(smoothstep(0.0,-0.02,dir.y),0.2));
//    return vec3(0.0);
}

// main
void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
//    float time = iTime * 0.3 + iMouse.x*0.01;
    float time = frame / 50.0 * 0.3;

//    #ifdef AA
//    vec3 color = vec3(0.0);
//    for(int i = -1; i <= 1; i++) {
//        for(int j = -1; j <= 1; j++) {
//            vec2 uv = fragCoord+vec2(i,j)/3.0;
//            color += getPixel(uv, time);
//        }
//    }
//    color /= 9.0;
//    #else
    vec3 color = getPixel(fragCoord, time); //fragCoord，当前遍历到纹理的哪个坐标
//    #endif

    // post
    fragColor = vec4(pow(color,vec3(0.65)), 1.0);
}


void main() {
    switch (funChoice) {
        case 0://线条渲染
            fragColor = fragObjectColor;//给此片元颜色值
            break;
        case 1://纹理渲染
            vec4 color = texture(sTexture, fragVTexCoord);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        case 2:
            fireflyEffect(fragColor, fragVTexCoord);
            break;
        case 3:
            mainImage(fragColor, fragVTexCoord);
            break;
    }
}