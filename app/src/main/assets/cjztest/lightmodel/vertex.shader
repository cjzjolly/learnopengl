#version 300 es

uniform mat4 uMVPMatrix; //旋转平移缩放 总变换矩阵。物体矩阵乘以它即可产生变换
in vec3 objectPosition; //物体位置向量，参与运算但不输出给片源
in vec2 vTexCoord; //纹理内坐标
out vec2 fragVTexCoord;//输出处理后的纹理内坐标给片元程序

void main() {
    gl_Position = uMVPMatrix * vec4(objectPosition, 1.0); //设置物体位置 todo 变换矩阵有问题，一乘就什么都显示不出来
    fragVTexCoord = vTexCoord; //默认无任何处理，直接输出物理内采样坐标
}