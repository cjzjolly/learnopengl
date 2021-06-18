#version 300 es
precision highp float;
uniform sampler2D sTexture;//纹理输入
uniform int funChoice; //功能代码块选择
uniform float effectR; //作用半径
uniform vec2 targetXY; //作用位置，使用纹理分辨率坐标
uniform vec2 resolution;//纹理分辨率
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色

mat2 rotate(float a) // 旋转矩阵
{
    float s = sin(a);
    float c = cos(a);
    return mat2(c,-s,s,c);
}

vec2 twirl(vec2 uv, vec2 center, float range, float angle) {
    float d = distance(uv, center);
    uv -=center;
    // d = clamp(-angle/range * d + angle,0.,angle); // 线性方程
    d = smoothstep(0., range, range-d) * angle;
    uv *= rotate(d);
    uv+=center;
    return uv;
}

void main() {
    vec2 targetXYToOne = targetXY / resolution; //归1化处理
    targetXYToOne.y = 1.0 - targetXYToOne.y;
    targetXYToOne = vec2(targetXYToOne.y, targetXYToOne.x);
    vec2 texCoord = vec2(fragVTexCoord.y, fragVTexCoord.x);
    switch (funChoice) {
        case 0: //采集纹理图片像素
            vec4 color = texture(sTexture, texCoord);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        //已经采集了纹理图片像素并留下残迹在Framebuffer了，可以进行图像处理了：
        case 1: //点击时framebuffer渲染流程切换到这里
            float d = distance(texCoord, targetXY);
            if (distance(targetXYToOne, texCoord) < effectR) {
                fragColor = vec4(1.0, 0.0, 0.0, 1.0);
            }
            break;
        case 2: //旋转形变， //todo 可是这里每次拿的都是纹理原样进行处理，如何才能做到将效果一直叠加呢
            vec2 newST = twirl(texCoord, targetXYToOne, 0.15, 3.0);
            if (distance(targetXYToOne, texCoord) < effectR) {
                color = texture(sTexture, newST);//采样纹理中对应坐标颜色，进行纹理渲染
                color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
                fragColor = color;
            }
            break;
        case -1: //鼠标抬起时要切换到这里，避免持续刷新
            break;
    }
}