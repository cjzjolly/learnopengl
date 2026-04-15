package com.cjztest.gldrawlinesByMultiVectors;

public class LineIntersection {

    /**
     * 判断线段1 (x1,y1)->(x2,y2) 与 线段2 (x3,y3)->(x4,y4) 是否相交
     */
    public static boolean doIntersect(float[] l1, float[] l2) {
        double x1 = l1[0], y1 = l1[1], x2 = l1[2], y2 = l1[3];
        double x3 = l2[0], y3 = l2[1], x4 = l2[2], y4 = l2[3];

        // 1. 找到四个方向的排列
        int o1 = orientation(x1, y1, x2, y2, x3, y3);
        int o2 = orientation(x1, y1, x2, y2, x4, y4);
        int o3 = orientation(x3, y3, x4, y4, x1, y1);
        int o4 = orientation(x3, y3, x4, y4, x2, y2);

        // 2. 通用情况：跨立实验成功
        if (o1 != o2 && o3 != o4) return true;

        // 3. 特殊情况：共线且点在线段上
//        if (o1 == 0 && onSegment(x1, y1, x3, y3, x2, y2)) return true;
//        if (o2 == 0 && onSegment(x1, y1, x4, y4, x2, y2)) return true;
//        if (o3 == 0 && onSegment(x3, y3, x1, y1, x4, y4)) return true;
//        if (o4 == 0 && onSegment(x3, y3, x2, y2, x4, y4)) return true;

        return false;
    }

    // 计算三点顺序：0=共线, 1=顺时针, 2=逆时针
    private static int orientation(double px, double py, double qx, double qy, double rx, double ry) {
        double val = (qy - py) * (rx - qx) - (qx - px) * (ry - qy);
        if (Math.abs(val) < 1e-9) return 0; // 共线
        return (val > 0) ? 1 : 2;
    }

    // 检查点 q 是否在线段 pr 上
    private static boolean onSegment(double px, double py, double qx, double qy, double rx, double ry) {
        return qx <= Math.max(px, rx) && qx >= Math.min(px, rx) &&
                qy <= Math.max(py, ry) && qy >= Math.min(py, ry);
    }
}
