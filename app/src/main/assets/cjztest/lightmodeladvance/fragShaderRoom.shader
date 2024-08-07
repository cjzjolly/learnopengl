#version 300 es
precision highp float;
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
in vec4 fragObjectColor;
in vec3 objPos;
in vec3 lightPos;
out vec4 fragColor;//输出到的片元颜色\n
uniform int funcChoice; //光照方式选择

void main() {
    vec4 color;
    vec3 lightVec = lightPos;
    //通过dot product得到光线向量和顶点向量之间的相似性，再把相似性系数作为颜色深度系数
    switch (funcChoice) {
        default:
        case 0: //使用dot product方法
            //怎样才可以表达一条不是由原点出发的光线？
            /*nice1 假设顶点向量包含了光源向量，那么顶点向量 - 光源向量 = 光源为源头到顶点的向量，也就是去除了原点到光源的距离，
            其具体含义就是把光线设定为原点到光圈的方向，通过点乘计算当前顶点与前述向量的相关性，以这个相关性作为颜色的浓淡系数*/
            //todo 完全相反的面也被点亮了，为啥?????
            color = vec4(fragObjectColor.rgb * dot(normalize(lightVec), normalize(objPos - lightVec)), fragObjectColor.a);

//            color = color * (5.0 / distance(lightVec, objPos)) * 5.0;//叠加一下与光强度与光源距离成反比的关系式
            break;
        case 1: //使用距离光:
            color = vec4(fragObjectColor.rgb * (1.0 / distance(lightPos, objPos)) * 5.0, fragObjectColor.a);
            break;
    }
    //把缝隙的感觉搞出来
    vec2 fragVTexCoordInCenter = fragVTexCoord - vec2(0.5, 0.5); //采样点变换为以点(0,0)为原点
    vec2 fragVTexCoordInCenterNor = normalize(fragVTexCoordInCenter); //当前采样变量的方向特征向量
    //了解当前采样向量的单位长度
    vec2 times = abs(1.0 / fragVTexCoordInCenterNor); //求出如果让x要为单位1.0，要让fragVTexCoordInCenterNor.x乘以多少系数
    vec2 uvInCenterMaxLen = min(times[0], times[1]) * fragVTexCoordInCenterNor * 0.5;
    vec2 uvInCenter = fragVTexCoord - vec2(0.5, 0.5);
    float edgeColorX = 1.0 - smoothstep(0.9, 1.0, distance(uvInCenter, vec2(0.0, 0.0)) /  distance(uvInCenterMaxLen, vec2(0.0, 0.0)));
    color = vec4(color.rgb * edgeColorX, color.a);

    fragColor = color;
}