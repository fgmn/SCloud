#include "sys.h"
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "pwm.h"




int main(void)
{ 
	u16 pwmval=185; //pwmval��195~���0��  190~���45�� 185~���90�� 180~���135�� 175~���180��  
	u8 dir=1;
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);//����ϵͳ�ж����ȼ�����2
	delay_init(168);  //��ʼ����ʱ����
	uart_init(115200);//��ʼ�����ڲ�����Ϊ115200
 	TIM14_PWM_Init(200-1,8400-1);	//84M/8400=0.01Mhz�ļ���Ƶ��,��װ��ֵ200������PWMƵ��Ϊ 0.01M/200=50hz.     
   while(1) //ʵ�ֱȽ�ֵ��0-300��������300���300-0�ݼ���ѭ��
	{

 
		TIM_SetCompare1(TIM14,pwmval);	//�޸ıȽ�ֵ���޸�ռ�ձ�
	}
}
