#version 300 es
precision highp float;
uniform sampler2D textureY;//YPanel纹理输入，glTexImage2d时要设置直字节格式为：GL_LUMINANCE/GL_ALPHA(单个字节), GL_UNSIGNED_BYTE
uniform sampler2D textureUV;//UV输入，glTexImage2d时要设置直字节格式为：GL_LUMINANCE_ALPHA(双字节), GL_UNSIGNED_BYTE
uniform sampler2D textureU;  //for yuv420sp，u和v分层，都用GL_LUMINANCE/GL_ALPHA(单个字节)
uniform sampler2D textureV;
uniform int funChoice;
uniform float frame;//第几帧
uniform vec2 resolution;//分辨率
in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
out vec4 fragColor;//输出到的片元颜色

//https://blog.csdn.net/byhook/article/details/84037338 有各种YUV的排列方式
/**yuvtorgb
        R = Y + 1.4075 *（V-128）
       G = Y – 0.3455 *（U –128） – 0.7169 *（V –128）
       B = Y + 1.779 *（U – 128）
**/

vec3 yuvToRGB(float y, float u, float v) {
    float r = y + 1.370705 * (v - 0.5);
    float g = y - 0.337633 * (u - 0.5) - 0.698001 * (v - 0.5);
    float b = y + 1.732446 * (u - 0.5);
//    r = clamp(r, 0.0, 1.0);
//    g = clamp(g, 0.0, 1.0);
//    b = clamp(b, 0.0, 1.0);
    return vec3(r, g, b);
}

void convertYUV420SP(bool reverse, in vec2 fragVTexCoord, out vec4 fragColor){
    float y = texture(textureY, fragVTexCoord)[0];
    //uvuvuvuv
    float u = texture(textureUV, fragVTexCoord)[3];
    float v = texture(textureUV, fragVTexCoord)[0];
    vec3 rgb;
    if (reverse) {
        rgb = yuvToRGB(y, v, u);  //NV21
    } else {
        rgb = yuvToRGB(y, u, v);  //NV12
    }
    fragColor = vec4(rgb, 1.0);
}

void convertYUV420P(bool reverse, in vec2 fragVTexCoord, out vec4 fragColor){
    float y = texture(textureY, fragVTexCoord)[0];
    /*
    uuu...
    vvv....
    */
    float u = texture(textureU, fragVTexCoord)[0];
    float v = texture(textureV, fragVTexCoord)[0];
    vec3 rgb;
    if (reverse) {
        rgb = yuvToRGB(y, v, u);  //YV12
    } else {
        rgb = yuvToRGB(y, u, v);  //YU12 / I420
    }
    fragColor = vec4(rgb, 1.0);
}


void main() {
    switch (funChoice) {
        default:
        case 0:
            convertYUV420SP(false, fragVTexCoord, fragColor);  //uvuv
            break;
        case 1:
            convertYUV420SP(true, fragVTexCoord, fragColor);   //vuvu
            break;
        case 2:
            convertYUV420P(false, fragVTexCoord, fragColor);  //uuuvvv
            break;
        case 3:
            convertYUV420P(true, fragVTexCoord, fragColor);   //vvvuuu
            break;
    }
}