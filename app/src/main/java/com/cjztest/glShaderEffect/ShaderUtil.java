package com.cjztest.glShaderEffect;

import android.content.res.Resources;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ShaderUtil {
    //从sh脚本中加载shader内容的方法
    public static String loadFromAssetsFile(String fname, Resources r)
    {
        String result = null;
        try {
            InputStream in = r.getAssets().open(fname);
            int ch = 0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((ch = in.read()) != -1) {
                baos.write(ch);
            }
            byte[] buff = baos.toByteArray();
            baos.close();
            in.close();
            result = new String(buff, "UTF-8");
            result = result.replaceAll("\\r\\n", "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
