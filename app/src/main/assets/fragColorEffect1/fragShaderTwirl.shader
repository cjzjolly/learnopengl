#version 300 es
precision highp float;
uniform sampler2D sTexture;//纹理输入
uniform int funChoice;
uniform float frame;//第几帧
uniform vec2 resolution;//分辨率
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色

vec2 deform(vec2 uv, vec2 center, float range, float strength) {
    float dist = distance(uv, center);
    vec2 direction = normalize(uv - center);
    dist = transform(dist, range, strength); // 改变采样圈半径
    center = transform(center, dist, range, strength); // 改变采样圈中心位置
    return center + dist * direction;
}

void main() {
    switch (funChoice) {
        case 1://纹理渲染
            vec2 newST = deform(fragVTexCoord, vec2(0.5, 0.5), 0.5, 0.5);
            vec4 color = texture(sTexture, newST);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
    }
}