#version 300 es
precision highp float;
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
in vec4 fragObjectColor;
in vec3 objPos;
in vec3 lightPos;
out vec4 fragColor;//输出到的片元颜色\n
uniform int funcChoice; //光照方式选择
uniform mat3 lightTransMat; //todo 光照变换矩阵

void main() {
    vec4 color;
//    vec4 color = vec4(fragObjectColor.rgb * sqrt(0.5 / distance((lightPos), normalize(objPos))), fragObjectColor.a);
//    vec4 color = vec4(fragObjectColor.rgb * (1.0 / pow(distance((lightPos), (objPos)) , 2.0) * 20.0), fragObjectColor.a);
//    vec4 color = vec4(fragObjectColor.rgb * (1.0 / sqrt(distance((lightPos), (objPos)))), fragObjectColor.a);
//    vec4 color = vec4(fragObjectColor.rgb * (1.0 / (distance((lightPos), (objPos)))) * 5.0, fragObjectColor.a);
    //todo 添加功能，实现点积光，光向量方向修改的功能 可以使用两个光点之间的向量作为方向实现
    vec3 lightVec = lightPos;
    //通过dot product得到光线向量和顶点向量之间的相似性，再把相似性系数作为颜色深度系数
    switch (funcChoice) {
        default:
        case 0: //使用dot product方法
            //怎样才可以表达一条不是由原点出发的光线？
            /*nice1 假设顶点向量包含了光源向量，那么顶点向量 - 光源向量 = 光源为源头到顶点的向量，也就是去除了原点到光源的距离，
            其具体含义就是把光线设定为原点到光圈的方向，通过点乘计算当前顶点与前述向量的相关性，以这个相关性作为颜色的浓淡系数*/
            color = vec4(fragObjectColor.rgb * dot(normalize(lightVec), normalize(objPos - lightVec)), fragObjectColor.a);
            color = color * (5.0 / distance(lightVec, objPos)) * 5.0; //叠加一下与光强度与光源距离成反比的关系式
            break;
        case 1: //使用距离光:
            color = vec4(fragObjectColor.rgb * (1.0 / distance(lightPos, objPos)) * 5.0, fragObjectColor.a);
            break;
    }
    //todo 把缝隙的感觉搞出来
    fragColor = color;
}