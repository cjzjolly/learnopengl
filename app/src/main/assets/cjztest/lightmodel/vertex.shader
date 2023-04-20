#version 300 es

uniform mat4 uMVPMatrix; //旋转平移缩放 总变换矩阵。物体矩阵乘以它即可产生变换
uniform mat4 objMatrix;
in vec3 objectPosition; //物体位置向量，参与运算但不输出给片源
in vec2 vTexCoord; //纹理内坐标
out vec2 fragVTexCoord;//输出处理后的纹理内坐标给片元程序
out vec3 objPos;

void main() {
    vec4 pos = uMVPMatrix * vec4(objectPosition, 1.0);
    gl_Position = pos; //设置物体位置
    fragVTexCoord = vTexCoord; //默认无任何处理，直接输出物理内采样坐标
    objPos = pos.xyz;
}