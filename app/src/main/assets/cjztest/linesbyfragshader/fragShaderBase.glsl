#version 300 es
    precision highp float;

    uniform vec2 u_resolution;
    uniform int u_pointCount;
    uniform vec2 u_points[32];
    uniform float u_strokeWidth;

    out vec4 fragColor;

    // 计算点 p 到线段 ab 的最近距离
    float distanceToSegment(vec2 p, vec2 a, vec2 b) {
        vec2 pa = p - a;
        vec2 ba = b - a;
        float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
        return length(pa - ba * h);
    }

    void main() {
        vec2 p = gl_FragCoord.xy / u_resolution.xy;

        float halfStrokeWidth = u_strokeWidth * 0.5;
        float minDist = halfStrokeWidth;
        float dist = 1e6;

        // 遍历所有连续线段，获得线段中和采样点最近的线段的最短距离。
        for (int i = 1; i < 32; i++) {
            vec2 a = u_points[i - 1];
            vec2 b = u_points[i];
            // if (distance(a, b) < 1e-5) continue;
            dist = min(dist, distanceToSegment(p, a, b));
        }

        // // 当P点距离小于r的一半时才设为不透明
        // float alpha = 0.0;
        // if (dist < minDist) {
        //   alpha = 1.0;
        // }

        /*值越超过halfStrokeWidth + aa，就越是0。
        否则越接近halfStrokeWidth - aa, 就越接近1。从而保证大于
        PQ的长度大于线条粗度时颜色为全0，只有小部分小于线条粗度的采样条件时
        可以显示线条颜色*/
        float aa = fwidth(dist) * 2.0;
        float alpha = smoothstep(halfStrokeWidth + aa, halfStrokeWidth - aa, dist);

        vec3 color = vec3(0.5, 0.5, 0.5);
        fragColor = vec4(color * alpha, 0.5);
    }

    // // 判断点 p 是否在由 a 和 b 定义的矩形内（包含边界）
    // bool isPointInRect(vec2 p, vec2 a, vec2 b) {
    //     float halfStrokeWidth = u_strokeWidth;
    //     // 计算矩形的边界
    //     vec2 minCorner = min(a, b) - halfStrokeWidth;
    //     vec2 maxCorner = max(a, b) + halfStrokeWidth;

    //     // 检查点是否在矩形范围内
    //     return (p.x >= minCorner.x && p.x <= maxCorner.x &&
    //             p.y >= minCorner.y && p.y <= maxCorner.y);
    // }

    // vec2 calcPointQ(vec2 p, vec2 a, vec2 b) {
    //   vec2 minCorner = min(a, b);
    //   vec2 maxCorner = max(a, b);
    //   float q_x = (p.y - minCorner.y) / (maxCorner.y - minCorner.y) * (maxCorner.x - minCorner.x) + minCorner.x;
    //   return vec2(q_x, p.y);
    // }

    // void main() {
    //   vec2 p = gl_FragCoord.xy / u_resolution.xy;
    //   float halfStrokeWidth = u_strokeWidth * 0.5;
    //   for (int i = 1; i < 32; i++) {
    //       vec2 a = u_points[i - 1];
    //       vec2 b = u_points[i];
    //       if (isPointInRect(p, a, b)) {
    //           vec2 pointInLine = calcPointQ(p, a, b);
    //           if (distance(p, pointInLine) < halfStrokeWidth) {
    //             fragColor = vec4(0.0, 1.0, 0.0, 1.0);
    //             // break;
    //           }
    //       }


    //       // if (distance(a, p) < halfStrokeWidth) {
    //       //   fragColor = vec4(0.0, 1.0, 0.0, 1.0);
    //       // }
    //   }
    // }