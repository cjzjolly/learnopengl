package com.book2.Sample8_1;
import static com.book2.Sample8_1.ShaderUtil.createProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES30;

//圆柱侧面骨架类
public class CylinderSideL
{
    int mProgram;//自定义渲染管线着色器程序id
    int muMVPMatrixHandle;//总变换矩阵引用
    int maPositionHandle; //顶点位置属性引用
    int maColorHandle; //顶点颜色属性引用

    int muMMatrixHandle;//位置、旋转、缩放变换矩阵
    int maCameraHandle; //摄像机位置属性引用
    int maNormalHandle; //顶点法向量属性引用
    int maLightLocationHandle;//光源位置属性引用


    String mVertexShader;//顶点着色器代码脚本
    String mFragmentShader;//片元着色器代码脚本

    FloatBuffer   mVertexBuffer;//顶点坐标数据缓冲
    FloatBuffer   mColorBuffer;	//顶点颜色数据缓冲
    FloatBuffer   mNormalBuffer;//顶点法向量数据缓冲
    int vCount=0;
    float xAngle=0;//绕x轴旋转的角度
    float yAngle=0;//绕y轴旋转的角度
    float zAngle=0;//绕z轴旋转的角度

    public CylinderSideL(MySurfaceView mv,float scale,float r,float h,int n)
    {
        //调用初始化顶点数据的initVertexData方法
        initVertexData(scale,r,h,n);
        //调用初始化着色器的intShader方法
        initShader(mv);
    }

    //自定义初始化顶点坐标数据的方法
    public void initVertexData(
            float scale,	//大小
            float r,		//半径
            float h,		//高度
            int n			//切分的份数
    )
    {
        r=scale*r;
        h=scale*h;

        float angdegSpan=360.0f/n;
        vCount=3*n*4;//顶点个数，共有3*n*4个三角形，每个三角形都有三个顶点
        //坐标数据初始化
        float[] vertices=new float[vCount*3];
        float[] colors=new float[vCount*4];//顶点颜色值数组
        //坐标数据初始化
        int count=0;
        int colorCount=0;
        for(float angdeg=0;Math.ceil(angdeg)<360;angdeg+=angdegSpan)//侧面
        {
            double angrad=Math.toRadians(angdeg);//当前弧度
            double angradNext=Math.toRadians(angdeg+angdegSpan);//下一弧度
            //底圆当前点---0
            vertices[count++]=(float) (-r*Math.sin(angrad));
            vertices[count++]=0;
            vertices[count++]=(float) (-r*Math.cos(angrad));

            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            //顶圆下一点---3
            vertices[count++]=(float) (-r*Math.sin(angradNext));
            vertices[count++]=h;
            vertices[count++]=(float) (-r*Math.cos(angradNext));

            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            //顶圆当前点---2
            vertices[count++]=(float) (-r*Math.sin(angrad));
            vertices[count++]=h;
            vertices[count++]=(float) (-r*Math.cos(angrad));

            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;

            //底圆当前点---0
            vertices[count++]=(float) (-r*Math.sin(angrad));
            vertices[count++]=0;
            vertices[count++]=(float) (-r*Math.cos(angrad));

            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            //底圆下一点---1
            vertices[count++]=(float) (-r*Math.sin(angradNext));
            vertices[count++]=0;
            vertices[count++]=(float) (-r*Math.cos(angradNext));

            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            //顶圆下一点---3
            vertices[count++]=(float) (-r*Math.sin(angradNext));
            vertices[count++]=h;
            vertices[count++]=(float) (-r*Math.cos(angradNext));

            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
            colors[colorCount++]=1;
        }
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);//创建顶点坐标数据缓冲
        vbb.order(ByteOrder.nativeOrder());//设置字节顺序为本地操作系统顺序
        mVertexBuffer = vbb.asFloatBuffer();//转换为float型缓冲
        mVertexBuffer.put(vertices);//向缓冲区中放入顶点坐标数据
        mVertexBuffer.position(0);//设置缓冲区起始位置
        //法向量数据初始化
        for(int i=0;i<vertices.length;i++){
            if(i%3==1){
                vertices[i]=0;
            }
        }
        ByteBuffer nbb = ByteBuffer.allocateDirect(vertices.length*4);//创建顶点法向量数据缓冲
        nbb.order(ByteOrder.nativeOrder());//设置字节顺序为本地操作系统顺序
        mNormalBuffer = nbb.asFloatBuffer();//转换为float型缓冲
        mNormalBuffer.put(vertices);//向缓冲区中放入顶点法向量数据
        mNormalBuffer.position(0);//设置缓冲区起始位置

        //创建顶点着色数据缓冲
        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
        cbb.order(ByteOrder.nativeOrder());//设置字节顺序为本地操作系统顺序
        mColorBuffer = cbb.asFloatBuffer();//转换为Float型缓冲
        mColorBuffer.put(colors);//向缓冲区中放入顶点着色数据
        mColorBuffer.position(0);//设置缓冲区起始位置
    }

    //初始化着色器
    public void initShader(MySurfaceView mv)
    {
        //加载顶点着色器的脚本内容
        mVertexShader=ShaderUtil.loadFromAssetsFile("sample8_1/vertex_color_light.shader", mv.getResources());
        //加载片元着色器的脚本内容
        mFragmentShader=ShaderUtil.loadFromAssetsFile("sample8_1/frag_color_light.shader", mv.getResources());
        //基于顶点着色器与片元着色器创建程序
        mProgram = createProgram(mVertexShader, mFragmentShader);
        //获取程序中顶点位置属性引用id
        maPositionHandle = GLES30.glGetAttribLocation(mProgram, "aPosition");
        //获取程序中顶点颜色属性引用id
        maColorHandle= GLES30.glGetAttribLocation(mProgram, "aColor");
        //获取程序中总变换矩阵引用id
        muMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");

        //获取程序中顶点法向量属性引用id
        maNormalHandle= GLES30.glGetAttribLocation(mProgram, "aNormal");
        //获取程序中摄像机位置引用id
        maCameraHandle=GLES30.glGetUniformLocation(mProgram, "uCamera");
        //获取程序中光源位置引用id
        maLightLocationHandle=GLES30.glGetUniformLocation(mProgram, "uLightLocation");
        //获取位置、旋转变换矩阵引用id
        muMMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMMatrix");


    }

    public void drawSelf()
    {
        //制定使用某套shader程序
        GLES30.glUseProgram(mProgram);


        //将最终变换矩阵传入shader程序
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, MatrixState.getFinalMatrix(), 0);


        //将位置、旋转变换矩阵传入shader程序
        GLES30.glUniformMatrix4fv(muMMatrixHandle, 1, false, MatrixState.getMMatrix(), 0);
        //将摄像机位置传入shader程序
        GLES30.glUniform3fv(maCameraHandle, 1, MatrixState.cameraFB);
        //将光源位置传入shader程序
        GLES30.glUniform3fv(maLightLocationHandle, 1, MatrixState.lightPositionFB);


        //传送顶点位置数据
        GLES30.glVertexAttribPointer
                (
                        maPositionHandle,
                        3,
                        GLES30.GL_FLOAT,
                        false,
                        3*4,
                        mVertexBuffer
                );
        //传送顶点坐标数据
        GLES30.glVertexAttribPointer
                (
                        maColorHandle,
                        4,
                        GLES30.GL_FLOAT,
                        false,
                        4*4,
                        mColorBuffer
                );
        //传送顶点法向量数据
        GLES30.glVertexAttribPointer
                (
                        maNormalHandle,
                        4,
                        GLES30.GL_FLOAT,
                        false,
                        3*4,
                        mNormalBuffer
                );

        //启用顶点位置数据
        GLES30.glEnableVertexAttribArray(maPositionHandle);
        //启用顶点颜色数据
        GLES30.glEnableVertexAttribArray(maColorHandle);
        //启用顶点法向量数据
        GLES30.glEnableVertexAttribArray(maNormalHandle);
        //绘制线条的粗细
        GLES30.glLineWidth(2);
        //绘制
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, vCount);
    }
}
