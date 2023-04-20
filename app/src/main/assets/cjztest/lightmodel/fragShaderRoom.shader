#version 300 es
precision highp float;
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
in vec4 fragObjectColor;
in vec3 objPos;
in vec3 lightPos;
out vec4 fragColor;//输出到的片元颜色\n
uniform int funcChoice;

void main() {
    vec4 color;
//    vec4 color = vec4(fragObjectColor.rgb * sqrt(0.5 / distance((lightPos), normalize(objPos))), fragObjectColor.a);
//    vec4 color = vec4(fragObjectColor.rgb * (1.0 / pow(distance((lightPos), (objPos)) , 2.0) * 20.0), fragObjectColor.a);
//    vec4 color = vec4(fragObjectColor.rgb * (1.0 / sqrt(distance((lightPos), (objPos)))), fragObjectColor.a);
//    vec4 color = vec4(fragObjectColor.rgb * (1.0 / (distance((lightPos), (objPos)))) * 5.0, fragObjectColor.a);
    //通过dot product得到光线向量和顶点向量之间的相似性，再把相似性系数作为颜色深度系数
    switch (funcChoice) {
        default:
        case 0: //使用dot product方法
            color = vec4(fragObjectColor.rgb * dot(normalize(lightPos), normalize(objPos)), fragObjectColor.a);
            break;
        case 1: //使用距离光:
            color = vec4(fragObjectColor.rgb * (1.0 / distance(lightPos, objPos)) * 5.0, fragObjectColor.a);
            break;
    }
    fragColor = color;
}