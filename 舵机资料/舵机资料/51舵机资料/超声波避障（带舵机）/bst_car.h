#ifndef __BSTCAR_H__
#define __BSTCAR_H__
/************BST-V51智能小车头文件*************/
//定义小车驱动模块输入IO口 
sbit IN1=P1^2;
sbit IN2=P1^3;
sbit IN3=P1^6;
sbit IN4=P1^7;
sbit EN1=P1^4;
sbit EN2=P1^5;

//定义按键
sbit K4=P3^7;
sbit K3=P3^6;

//蜂鸣器驱动口定义
sbit FM=P2^3;

//循迹LED指示灯定义
sbit Left_1_led=P3^3;	 //左循迹红外传感器       
sbit Right_1_led=P3^2;	 //右循迹红外传感器  

//避障LED指示灯定义  
sbit Left_2_led=P3^4;	 //左避障红外传感器  
sbit Right_2_led=P3^5;	 //右避障红外传感器  

sbit TX=P2^1;	   //超声波模块Trig	控制端
sbit RX=P2^0;	   //超声波模块Echo	接收端  //暂时改到中断	  P2^0


#define Left_moto_go      {IN1=0,IN2=1;}   //左电机向前走
#define Left_moto_back    {IN1=1,IN2=0;}   //左边电机向后转
#define Left_moto_Stop    {EN1=0;}         //左边电机停转                     
#define Right_moto_go     {IN3=1,IN4=0;}   //右边电机向前走
#define Right_moto_back   {IN3=0,IN4=1;}   //右边电机向后走
#define Right_moto_Stop   {EN2=0;}	       //右边电机停转  

bit Right_PWM_ON=1;	           //右电机PWM开关
bit Left_PWM_ON =1;			   //左电机PWM开关

#endif