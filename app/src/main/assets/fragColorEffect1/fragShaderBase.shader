#version 300 es
precision highp float;
uniform sampler2D sTexture;//纹理输入
uniform int funChoice;
uniform float frame;//第几帧
uniform vec2 resolution;//分辨率
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色

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
    }
}