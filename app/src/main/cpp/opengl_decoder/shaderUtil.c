/*@author 陈杰柱
shader加载、销毁工具 */

#include <stdio.h>
#include <stdlib.h>
#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

typedef struct gLslHandle {
	GLuint vertexShader;  //顶点shader
	GLuint fragmentShader; //片元shader
	GLuint programHandle;  //编译好的program句柄
} GLslHandle;

GLslHandle createProgram(char* vertexShaderSource,
									char* fragmentShaderSource);
GLuint loadShader(char* source, GLenum shaderType);
void destroyProgram(GLslHandle programHandle);


GLslHandle createProgram(char* vertexShaderSource,
									char* fragmentShaderSource)
{
	GLuint vertexShader = loadShader(vertexShaderSource, GL_VERTEX_SHADER);	//加载顶点着色器
	GLuint fragmentShader = loadShader(fragmentShaderSource, GL_FRAGMENT_SHADER);	//加载片元着色器
	GLuint programHandle = glCreateProgram();//创建着色器程序
	glAttachShader(programHandle, vertexShader);//向着色器程序中加入顶点着色器
	glAttachShader(programHandle, fragmentShader);//向着色器程序中加入片元着色器
	glLinkProgram(programHandle);//链接着色器程序
	GLint linkSuccess;//声明链接是否成功标志变量
	glGetProgramiv(programHandle, GL_LINK_STATUS, &linkSuccess);
	if (linkSuccess == GL_FALSE) {//若连接失败获取获取错误信息
		// GLchar messages[256];
		// glGetProgramInfoLog(programHandle, sizeof(messages), 0, &messages[0]);
		// memset(messages, 0, 256);
		printf("Shader Link Error:");
        // printf("%s",(char*)messages);
	}
	GLslHandle g;
	g.vertexShader = vertexShader;
	g.fragmentShader = fragmentShader;
	g.programHandle = programHandle;
	return g;//返回结果
}

GLuint loadShader(char* source, GLenum shaderType)
{
	GLuint shaderHandle = glCreateShader(shaderType);
	glShaderSource(shaderHandle, 1, &source, 0);//加载着色器的脚本
	glCompileShader(shaderHandle);//编译着色器
	GLint compileSuccess;//声明编译是否成功标志变量
	glGetShaderiv(shaderHandle, GL_COMPILE_STATUS, &compileSuccess);
	if (compileSuccess == GL_FALSE) {//若编译失败则获取错误信息
		// GLchar messages[256];
		// glGetShaderInfoLog(shaderHandle, sizeof(messages), 0, &messages[0]);
		// memset(messages, 0, 256);
		printf("Shader Compile Error:");
		// printf("%s",(char*)messages);
	}
	return shaderHandle;//返回结果
}

/*SHADER用完一定要销毁不然会内存泄露、屏幕闪烁 */
void destroyProgram(GLslHandle g) {
	glDetachShader(g.programHandle, g.vertexShader);
	glDetachShader(g.programHandle, g.fragmentShader);
	glDeleteShader(g.vertexShader);
	glDeleteShader(g.fragmentShader);
	glDeleteProgram(g.programHandle);
}