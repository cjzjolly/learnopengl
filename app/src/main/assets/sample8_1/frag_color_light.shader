#version 300 es
precision mediump float;
in  vec4 vaaColor; //���մӶ�����ɫ�������Ĳ���
in vec4 vambient;
in vec4 vdiffuse;
in vec4 vspecular;
out vec4 fragColor;//�������ƬԪ��ɫ
void main()                         
{
   //����ɫ����ƬԪ
	vec4 finalColor = vaaColor;
   //����ƬԪ��ɫֵ 
   fragColor = finalColor*vambient+finalColor*vspecular+finalColor*vdiffuse;//����ƬԪ��ɫֵ
}              