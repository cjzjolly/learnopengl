#version 300 es
uniform mat4 uMVPMatrix; 						//总变换矩阵
uniform mat4 uMMatrix; 							//变换矩阵(包括平移、旋转、缩放)
uniform vec3 uLightLocation;						//光源位置
in vec3 aPosition;  						//顶点位置
in vec3 aNormal;    						//顶点法向量
out vec3 vPosition;							//用于传递给片元着色器的顶点位置
out vec4 vDiffuse;							//用于传递给片元着色器的散射光分量

void pointLight (								//散射光光照计算的方法
  in vec3 normal,								//法向量
  inout vec4 diffuse,								//散射光计算结果
  in vec3 lightLocation,							//光源位置
  in vec4 lightDiffuse							//散射光强度
){


  vec3 normalTarget=aPosition+normal;					//计算变换后的法向量  （把法向量的分量比例值附加到发向量自身，也就是相当于把法向量保持原有方向的基础上延长1个单位）
  vec3 newNormal=(uMMatrix*vec4(normalTarget,1)).xyz-(uMMatrix*vec4(aPosition,1)).xyz;    //（然后将法向量联通法向量的比例值的和一起进行变换，最后再减去法向量的长度）（如果不进行uMMatrix变换，则光源本身也会被变换）


//    vec3 newNormal=(uMMatrix*vec4(normal, 1)).xyz;  //不知道为什么，这么做是不等价的，似乎只有方向向量是不够的，长度一定要符合才行


//  newNormal=normalize(newNormal);					//对法向量规格化  （看起来是个多余步骤）
//计算从表面点到光源位置的向量vp
  vec3 vp = normalize(lightLocation-(uMMatrix*vec4(aPosition,1)).xyz);
  vp=normalize(vp);									//规格化vp
  float nDotViewPosition=max(0.0,dot(newNormal,vp)); 	//求法向量与vp向量的点积与0的最大值
  diffuse=lightDiffuse*nDotViewPosition;			//计算散射光的最终强度
}

void main(){
   gl_Position = uMVPMatrix * vec4(aPosition,1); 	//根据总变换矩阵计算此次绘制此顶点的位置
   vec4 diffuseTemp=vec4(0.0,0.0,0.0,0.0);
   pointLight(normalize(aNormal), diffuseTemp, uLightLocation, vec4(0.8,0.8,0.8,1.0));
   vDiffuse=diffuseTemp;					//将散射光最终强度传给片元着色器
   vPosition = aPosition; 					//将顶点的位置传给片元着色器
}

