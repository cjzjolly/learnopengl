#version 300 es

uniform mat4 uMVPMatrix; //旋转平移缩放 总变换矩阵。物体矩阵乘以它即可产生变换
in vec3 objectPosition; //物体位置向量，参与运算但不输出给片源

void main() {
    vec4 pos = uMVPMatrix * vec4(objectPosition, 1.0);
    gl_Position = pos; //设置物体位置
}