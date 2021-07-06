//
// Created by jiezhuchen on 2021/6/21.
//

#ifndef SHADER_UTIL_HEADER
#define SHADER_UTIL_HEADER
#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

struct gLslHandle {
    GLuint vertexShader;  //顶点shader
    GLuint fragmentShader; //片元shader
    GLuint programHandle;  //编译好的program句柄
};

typedef struct gLslHandle GLslHandle;

GLslHandle createProgram(char* vertexShaderSource,
                         char* fragmentShaderSource);
GLuint loadShader(char* source, GLenum shaderType);
void destroyProgram(GLslHandle programHandle);

#endif //SHADER_UTIL_HEADER
