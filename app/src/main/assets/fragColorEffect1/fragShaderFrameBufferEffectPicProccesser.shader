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

void main() {
    vec4 color = vec4(0.0);
    switch (funChoice) {
        case 0: //采集纹理图片像素
            vec4 color = texture(sTexture, fragVTexCoord);//采样纹理中对应坐标颜色，进行纹理渲染
            color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
            fragColor = color;
            break;
        //已经采集了纹理图片像素并留下残迹在Framebuffer了，可以进行图像处理了：
        case 1: //旋转形变，点击时framebuffer渲染流程切换到这里
            float d = distance(fragVTexCoord, targetXY);
            vec2 targetXYToOne = targetXY / resolution; //归1化处理
            break;
        default: //鼠标抬起时要切换到这里，避免持续刷新
            break;
    }
}