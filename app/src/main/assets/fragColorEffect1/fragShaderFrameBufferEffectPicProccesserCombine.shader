#version 300 es
precision highp float;
uniform sampler2D sTexture;//纹理输入
uniform int frame;//第几帧
uniform int funChoice; //功能代码块选择
uniform float effectR; //作用半径
uniform vec2 targetXY; //作用位置，使用纹理分辨率坐标
uniform vec2 resolution;//纹理分辨率
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色

#define PINCH_VECTOR vec2( sin(10. * frame / 10.0), cos(20. * frame / 10.0)) * .03 // 挤压向量

mat2 rotate(float a) // 旋转矩阵
{
    float s = sin(a);
    float c = cos(a);
    return mat2(c,-s,s,c);
}

vec2 twirl(vec2 uv, vec2 center, float range, float angle, bool cw) {
    float d = distance(uv, center);
    uv -=center;
    // d = clamp(-angle/range * d + angle,0.,angle); // 线性方程
    d = smoothstep(0., range, range-d) * angle;
    if (cw) {
        uv *= rotate(d);
    } else {
        uv *= rotate(-d);
    }
    uv+=center;
    return uv;
}

vec2 inflate(vec2 uv, vec2 center, float range, float strength) {
    float dist = distance(uv , center);
    vec2 dir = normalize(uv - center);
    float scale = 1.-strength + strength * smoothstep(0., 1. ,dist / range);
    float newDist = dist * scale;
    return center + newDist * dir;
}

vec2 pinch(vec2 uv, vec2 targetPoint, vec2 vector, float range)
{
    vec2 center = targetPoint + vector;
    float dist = distance(uv, targetPoint);
    vec2 point = targetPoint +  smoothstep(0., 1., dist / range) * vector;
    return uv - center + point;
}

void main() {
    vec2 targetXYToOne = targetXY / resolution; //归1化处理
    targetXYToOne.y = 1.0 - targetXYToOne.y;
//    vec2 texCoord = vec2(fragVTexCoord.y, fragVTexCoord.x);
    vec2 texCoord = fragVTexCoord;
    switch (funChoice) {
        case 0: //采集纹理图片像素
            vec4 color = texture(sTexture, texCoord);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        //已经采集了纹理图片像素并留下残迹在Framebuffer了，可以进行图像处理了：
        case 1: //绘制点
            float d = distance(texCoord, targetXY);
            if (distance(targetXYToOne, texCoord) < effectR) {
                fragColor = vec4(1.0, 0.0, 0.0, 1.0);
            }
            break;
        case 2: //旋转形变1
            vec2 newST = twirl(texCoord, targetXYToOne, effectR, 0.05, true);
            color = texture(sTexture, newST);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        case 3: //旋转形变2
            newST = twirl(texCoord, targetXYToOne, effectR, 0.05, false);
            color = texture(sTexture, newST);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        case 4: //膨胀:
            newST = inflate(texCoord, targetXYToOne, effectR, 0.02);
            color = texture(sTexture, newST);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        case 5: //收缩:
            newST = inflate(texCoord, targetXYToOne, effectR, -0.02);
            color = texture(sTexture, newST);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        case 6: //挤压
            newST = pinch(texCoord, targetXYToOne, vec2(0.1, 0.2), effectR);
            color = texture(sTexture, newST);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        default:
        case -1: //鼠标抬起时要切换到这里，避免持续刷新
            break;
    }
}