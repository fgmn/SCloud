#include "sys.h"
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "pwm.h"




int main(void)
{ 
	u16 pwmval=185; //pwmval：195~舵机0°  190~舵机45° 185~舵机90° 180~舵机135° 175~舵机180°  
	u8 dir=1;
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);//设置系统中断优先级分组2
	delay_init(168);  //初始化延时函数
	uart_init(115200);//初始化串口波特率为115200
 	TIM14_PWM_Init(200-1,8400-1);	//84M/8400=0.01Mhz的计数频率,重装载值200，所以PWM频率为 0.01M/200=50hz.     
   while(1) //实现比较值从0-300递增，到300后从300-0递减，循环
	{

 
		TIM_SetCompare1(TIM14,pwmval);	//修改比较值，修改占空比
	}
}
