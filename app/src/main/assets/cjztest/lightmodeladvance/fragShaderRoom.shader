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
            color = vec4(fragObjectColor.rgb * max(0.0, dot(normalize(lightVec), normalize(objPos - lightVec))), fragObjectColor.a);
            color = color * (5.0 / distance(lightVec, objPos)) * 5.0; //叠加一下与光强度与光源距离成反比的关系式
            break;
        case 1: //使用距离光:
            color = vec4(fragObjectColor.rgb * (1.0 / distance(lightPos, objPos)) * 5.0, fragObjectColor.a);
            break;
    }
    //todo 把缝隙的感觉搞出来
//    float maxLenToTexCenter = sqrt(2.0 * pow(0.5, 2.0));
//    color = vec4(color.rgb * 1.0 - smoothstep(maxLenToTexCenter * 0.92, maxLenToTexCenter, distance(fragVTexCoord, vec2(0.5, 0.5))), color.a);

//    float edgeColorX = smoothstep(0.44, 0.45, abs(0.9 * (vec2(0.5, 0.5) - fragVTexCoord)));

//    vec2 uvInCenter = fragVTexCoord - vec2(0.5, 0.5); //外框
//    vec2 uvInCenterSmaller = fragVTexCoord * 0.9 - vec2(0.5, 0.5); //内框
////    float edgeColorX = smoothstep(0.0, 0.1, abs(distance(uvInCenterSmaller, uvInCenter)));
//    float edgeColorX = smoothstep(0.0, 0.2, abs(distance(uvInCenter, vec2(0.0, 0.0))));
//    color = vec4(color.rgb * edgeColorX, color.a);

//    vec2 uvInCenterAndHaveMaxLen = normalize(fragVTexCoord - vec2(0.5, 0.5)) * 0.5; //把向量们拉回原点为中心。然后求这个方向在圆形中所能到达的最大长度向量
    vec2 uvInCenterAndHaveMaxLen = normalize(fragVTexCoord - vec2(0.5, 0.5)); //把向量们拉回原点为中心。然后求这个方向在矩形中所能到达的最大长度向量
    uvInCenterAndHaveMaxLen = uvInCenterAndHaveMaxLen * 0.5
    * distance(vec2((fragVTexCoord[0] - 0.5) / (fragVTexCoord[0] - 0.5), (fragVTexCoord[1] - 0.5) / (fragVTexCoord[0] - 0.5)), vec2(0.0, 0.0));
    vec2 uvInCenter = fragVTexCoord - vec2(0.5, 0.5);
//    float edgeColorX = 1.0 - smoothstep(0.95, 1.0, distance(fragVTexCoord, vec2(0.5, 0.5)) / distance(uvInCenterAndHaveMaxLen, vec2(0.0, 0.0))); //圆
//    float edgeColorX = 1.0 - smoothstep(0.0, 0.1, distance(fragVTexCoord, vec2(0.5, 0.5)) - distance(uvInCenterAndHaveMaxLen, vec2(0.0, 0.0))); //还是圆
    float edgeColorX = 1.0 - smoothstep(0.95, 1.0, distance(uvInCenter, vec2(0.0, 0.0)) /  distance(uvInCenterAndHaveMaxLen, vec2(0.0, 0.0))); //还是圆
    color = vec4(color.rgb * edgeColorX, color.a);

    fragColor = color;
}