#extension GL_OES_EGL_image_external : require
precision mediump float;

varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;

// 1. 定义最大支持的人脸数量 (可根据需求调整，通常 5-10 足够)
#define MAX_FACES 5

// 2. 外部传入的归一化矩形数组 (x=minU, y=minV, z=maxU, w=maxV)
uniform vec4 uFaceRects[MAX_FACES];
uniform int uFaceCount; // 实际检测到的人脸数量

// 3. 归一化的马赛克块大小 (例如 vec2(0.02, 0.03))
uniform vec2 uMosaicBlockSize;

void main() {
    vec2 uv = vTextureCoord;
    bool applyMosaic = false;
    float alpha = 1.0; // 颜色强度 (0.0-1.0)

    // 4. 遍历所有人脸矩形，判断当前像素是否在其中
    // 注意：GLSL ES 2.0 要求 for 循环的边界必须是常量，所以必须循环 MAX_FACES 次
    for (int i = 0; i < MAX_FACES; i++) {
        if (i < uFaceCount) {
            vec4 rect = uFaceRects[i];
            // 判断 UV 坐标是否在矩形内
            if (uv.x >= rect.x && uv.x <= rect.z && uv.y >= rect.y && uv.y <= rect.w) {
                applyMosaic = true;
                break; // 只要在任意一个矩形内，就标记并跳出循环
            }
        }
    }

    // 5. 核心：高性能马赛克算法 (坐标离散化)
    if (applyMosaic) {
        // 将连续坐标除以块大小 -> 向下取整对齐到网格 -> 加 0.5 采样网格中心 -> 乘回块大小
        vec2 grid = floor(uv / uMosaicBlockSize);
        uv = (grid + 0.5) * uMosaicBlockSize;
        alpha = 0.5; // 可选：降低颜色强度，增强马赛克效果
    }
    // 6. 最终采样 (无论是否马赛克，都只采样 1 次，性能拉满)
    gl_FragColor = vec4(texture2D(sTexture, uv).rgb * alpha, 1.0);
}
