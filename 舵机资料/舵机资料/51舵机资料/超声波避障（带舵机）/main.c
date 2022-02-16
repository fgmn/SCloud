
******************************************************************/
 //�ϵ����K4���������У�С�����г�����������
 //LCD1602Һ��������ʾ̽�⵽��ʵʱ����

/**************************************************************
//��Ҫ˵����û��LCD1602Һ����ʾ����ͬѧ����Ҫ�öŰ��߽�P0.7�ӵس��������������
****************************************************************/

//ע�����ֻ���ο�֮�ã�Ҫ�ﵽ������ı���Ч��������Ҫͬѧ��ϸ�ĵ��ԡ�

#include <reg52.h>	   //52оƬ�����ļ�
#include <intrins.h>   //����nop��ϵͳ����
#include "bst_car.h"   //����bst��v51����С��ͷ�ļ�

#define LCM_Data  P0   //����Һ�������ݿ�
#define Busy    0x80   //���ڼ��LCM״̬���е�Busy��ʶ

sbit DU = P2^6;		   //����ܶ�ѡ
sbit WE = P2^7;		   //�����λѡ


sbit pwm = P2^7;//���PWM�ź�
unsigned char count=0;
unsigned char SEH_count = 10;  //����ǰ��

int LeftDistance = 0, RightDistance = 0;
unsigned long ultrasonic[5] = {0};

unsigned char hongwai = 0; //�к�������ֹͣǰ����־λ

sbit LCM_RW=P1^1 ;     //����LCD����
sbit LCM_RS=P1^0 ;
sbit LCM_E=P2^5	 ;

unsigned char code Range[] ="==Range Finder==";//LCD1602��ʾ��ʽ
unsigned char code ASCII[13] = "0123456789.-M";
unsigned char code table[]="Distance:000.0cm";
unsigned char code table1[]="!!! Out of range";

unsigned char disbuff[4]={0,0,0,0};//���ڷֱ��ž����ֵ0.1mm��mm��cm��m��ֵ

unsigned int  time=0;//���ڴ�Ŷ�ʱ��ʱ��ֵ
unsigned long S = 0;//���ڴ�ž����ֵ
bit  flag =0; //���������־λ
char a=0;


unsigned char pwm_val_left  =0;//��������
unsigned char pwm_val_right =0;
unsigned char push_val_left =10;// ����ռ�ձ�N/20 //�ٶȵ��ڱ��� 0-20������0��С��20���
unsigned char push_val_right=10;// �ҵ��ռ�ձ�N/20 
unsigned char pwmcount = 0;
//=========================================================================================================================
void delay(unsigned int xms)				
{
    unsigned int i,j;
	for(i=xms;i>0;i--)		      //i=xms����ʱԼxms����
    for(j=112;j>0;j--);
}

void Delay10us(unsigned int i)    	//10us��ʱ���� ����������ģ��ʱʹ��
{ 
   	unsigned int j; 
	do
	{ 
  		j = 10; 
  		do
		{ 
   			_nop_(); 
   		}
		while(--j); 
	}
	while(--i); 
}  

void cmg88()//�������
{
    DU=1;  
    P0=0X00;
    DU=0;
}
/************************************LCD1602Һ������������************************************************/
//*******************��״̬*************************//
unsigned char ReadStatusLCM(void)
{
	LCM_Data = 0xFF; 
	LCM_RS = 0;
	Delay10us(1); 
	LCM_RW = 1;
	Delay10us(1); 
	do{
		LCM_E = 0;
		Delay10us(1); 
		LCM_E = 0;
		Delay10us(1); 
		LCM_E = 1;
		Delay10us(1); 
	}
	while (LCM_Data & Busy); //���æ�ź�
	return(LCM_Data);
}

/****************д����************************/
void WriteDataLCM(unsigned char WDLCM) 
{
	ReadStatusLCM(); //���æ
	LCM_Data = WDLCM;
	LCM_RS = 1;
	Delay10us(1); 
	LCM_RW = 0;
	Delay10us(1); 
	LCM_E = 0; //�������ٶ�̫�߿���������С����ʱ
	Delay10us(1); 
	LCM_E = 0; //��ʱ
	Delay10us(1); 
	LCM_E = 1;
	Delay10us(1); 
}

//****************дָ��*************************//
void WriteCommandLCM(unsigned char WCLCM,BuysC) //BuysCΪ0ʱ����æ���
{
	if (BuysC) ReadStatusLCM(); //������Ҫ���æ
	LCM_Data = WCLCM;
	LCM_RS = 0;
	Delay10us(1); 
	LCM_RW = 0;	
	Delay10us(1); 
	LCM_E = 0;
	Delay10us(1); 
	LCM_E = 0;
	Delay10us(1); 
	LCM_E = 1;
	Delay10us(1); 
}



//*******************LCM��ʼ��**********************//
void LCMInit(void) 
{
	LCM_Data = 0;
	WriteCommandLCM(0x38,0); //������ʾģʽ���ã������æ�ź�
	delay(5); 
	WriteCommandLCM(0x38,0);
	delay(5); 
	WriteCommandLCM(0x38,0);
	delay(5); 

	WriteCommandLCM(0x38,1); //��ʾģʽ����,��ʼҪ��ÿ�μ��æ�ź�
	WriteCommandLCM(0x08,1); //�ر���ʾ
	WriteCommandLCM(0x01,1); //��ʾ����
	WriteCommandLCM(0x06,1); // ��ʾ����ƶ�����
	WriteCommandLCM(0x0c,1); // ��ʾ�����������
}

//*********************��ָ��λ����ʾһ���ַ�***********************//
void DisplayOneChar(unsigned char X, unsigned char Y, unsigned char DData)
{
	Y &= 0x1;
	X &= 0xF; //����X���ܴ���15��Y���ܴ���1
	if (Y) X |= 0x40; //��Ҫ��ʾ�ڶ���ʱ��ַ��+0x40;
	X |= 0x80; //���ָ����
	WriteCommandLCM(X, 1); //��������
	WriteDataLCM(DData); //������
}

//**********************��ָ��λ����ʾһ���ַ�*************************//
void DisplayListChar(unsigned char X, unsigned char Y, unsigned char code *DData)
{
	unsigned char ListLength;

  	ListLength = 0;
	Y &= 0x1;
	X &= 0xF; //����X���ܴ���15��Y���ܴ���1
	while (DData[ListLength]>0x19) //�������ִ�β���˳�
	{
		if (X <= 0xF) //X����ӦС��0xF
		{
			DisplayOneChar(X, Y, DData[ListLength]); //��ʾ�����ַ�
			ListLength++;
			X++;
		}
	}
}


/***************************************************************************/


void  StartModule() 		         //����������ģ��
{
	  TX=1;			                     //����һ��ģ��
      Delay10us(2);
	  TX=0;
}

 void Forward(int ms)//ǰ��
{
	 IN2=1;
	 IN3=1;

	 IN1=0;
	 IN4=0;
	 delay(ms);
}

void Stop(int ms)	//ͣ��
{

	 IN1=0; 
	 IN2=0;
	 IN3=0;
	 IN4=0;
	 delay(ms);
}

void back(int ms)	//����
{

	 IN1=1; 
	 IN2=0;
	 IN3=0;
	 IN4=1;
	 delay(ms);
}    
void Turn_Right(int ms)	 //������ת
{
    
	IN2=1;
	IN3=0;

	IN1=0; 
	IN4=1;
	delay(ms);
}
void Turn_Left(int ms)	 //������ת
{
   	IN2=0;
	IN3=1;

	IN1=1;
	IN4=0;
	delay(ms);
}
//=========================================================================================================================
/********����������***************/
unsigned long conut1(void)
{
	unsigned long dis = 0;
	time=TH1*256+TL1;
	TH1=0;
	TL1=0;
	
   //��ʱtime��ʱ�䵥λ�����ھ����Ƶ�ʣ���Ӿ���Ϊ11.0592MHZ
   //��ô1us�������߶�Զ�ľ����أ�1s=1000ms=1000000us 
   // 340/1000000=0.00034��
   //0.00034��/1000=0.34����  Ҳ����1us����0.34����
   //���ǣ��������ڼ�����Ǵӳ��������䵽������յ�˫·�̣�
   //�������ǽ�����Ľ������2����ʵ�ʵ�·��
    dis=time*0.17+10;//��ʱ���㵽�Ľ��Ϊ���ף������Ǿ�ȷ�����׵ĺ���λ�ˣ�������С���� 
	return dis;
}

//ð������
void bubble(unsigned long *a, int n) /*�������������������׵�ַ�������С*/

{
	int i,j,temp;	
	for(i = 0;i < n-1; i++)	
	{	
		for(j = i + 1; j < n; j++) /*ע��ѭ����������*/
		{
			if(a[i] > a[j])
			{
				temp = a[i];		
				a[i] = a[j];		
				a[j] = temp;			
			}
		}
	}

}
unsigned long Distance(void)
{
	int num = 0;
	unsigned long Distance = 0;
	RX = 1;
	StartModule(); 				
    while(RX==0);    
	

	TR1=1;			    //��������
    while(RX);			//��RXΪ1�������ȴ�
    TR1=0;				//�رռ���
    
	conut1();

	while(num < 5) // ��5������
	{
		RX = 1;
		StartModule(); 				
	    while(RX==0);    
	
		TR1=1;			    //��������
	    while(RX);			//��RXΪ1�������ȴ�
	    TR1=0;				//�رռ���
	    
		S = conut1();
		ultrasonic[num] = S;
		num++;
	}
	num = 0;
	bubble(ultrasonic, 5);
	Distance = (ultrasonic[1]+ultrasonic[2]+ultrasonic[3])/3; //ȥ��������Сȡ�м�ƽ��ֵ   
	return Distance;
}

void DisplayLCD1602(unsigned long v_s)
{
  	if((v_s>=5000)||flag==1) //����������Χ
	{
	    flag=0;
        DisplayListChar(0, 1, table1);
	}
	else
	{
        disbuff[0]=v_s%10;
	    disbuff[1]=v_s/10%10;
	    disbuff[2]=v_s/100%10;
	    disbuff[3]=v_s/1000;
	    DisplayListChar(0, 1, table);
	    DisplayOneChar(9, 1, ASCII[disbuff[3]]);
	    DisplayOneChar(10, 1, ASCII[disbuff[2]]);	
	    DisplayOneChar(11, 1, ASCII[disbuff[1]]);
        DisplayOneChar(12, 1, ASCII[10]);
	    DisplayOneChar(13, 1, ASCII[disbuff[0]]);
	 }
}
void Deal(void)
{
	unsigned long Dis = 0;
	Dis = Distance();
	DisplayLCD1602(Dis);

    if(S <= 300 || hongwai == 1)//�������ϰ���ʱ
    {

		Stop(1);//ͣ���������
		
		SEH_count = 20;		//0.5mS + ��ʱ��ʱ��  SEH_COUNT: 0��0.5ms*0(0.1*0T)->0�㣨������Ϊ3�� ��10:0.5+1��0.1*10T��=1.5ms->90�㣬 20:0.5+2ms��0.1*20T��=2.5ms->180��
		delay(500); //�ȴ������λ
		Dis = Distance();
		DisplayLCD1602(Dis);		
		LeftDistance = Dis;
		delay(100);
	  
	 
		SEH_count = 3;		//0.5mS + ��ʱ��ʱ��  SEH_COUNT: 0��0.5ms*0(0.1*0T)->0�㣨������Ϊ3�� ��10:0.5+1��0.1*10T��=1.5ms->90�㣬 20:0.5+2ms��0.1*20T��=2.5ms->180��
		delay(500); //�ȴ������λ
		Dis = Distance();
		DisplayLCD1602(Dis);			
		RightDistance = Dis;
	 	delay(100);

		SEH_count = 10;		//0.5mS + ��ʱ��ʱ��  SEH_COUNT: 0��0.5ms*0(0.1*0T)->0�㣨������Ϊ3�� ��10:0.5+1��0.1*10T��=1.5ms->90�㣬 20:0.5+2ms��0.1*20T��=2.5ms->180��
		delay(500); //�ȴ������λ
		
		back(20);
		Stop(1);//ͣ���������
		if((LeftDistance < 220 ) &&( RightDistance < 220 ))//��������������ϰ��￿�ñȽϽ�
			Turn_Left(300);//��ת��ͷ
		else if(LeftDistance > RightDistance)//��߱��ұ߿տ�
		{      
			Turn_Left(150);//��ת
			Stop(50);//ɲ�����ȶ�����
		}
		else//�ұ߱���߿տ�
		{
			Turn_Right(150);//��ת
			Stop(50);//ɲ�����ȶ�����
		}
    }
    else if(S > 300 && hongwai == 0)//�������ϰ���ʱ
    {
		Forward(0); //���ϰ��ֱ��     
    }

	 
}

/********************************************************/
void zd0() interrupt 3 		 //T0�ж��������������,������෶Χ
{
    flag=1;			 //�ж������־
	RX=0;
}

/********************************************************/
void keyscan(void)              //����ɨ�躯��
{
    A:    if(K4==0)			//�ж��Ƿ��а����ź�
		{
		    delay(10);		  //��ʱ10ms
			if(K4==0)			//�ٴ��ж��Ƿ���
			 {
			    FM=0;               //��������  		
			    while(K4==0);	//�ж��Ƿ��ɿ�����
			    FM=1;               //������ֹͣ  
		 	 }
		    else
		     {
		       goto A;        //��ת��A���¼��
	              }
		}
		else
		{
		  goto A;             //��ת��A���¼��
		}
}
/*����push_val_left��ֵ�ı���ת��,ռ�ձ�*/
void pwm_out_left_moto(void)
{  
     if(Left_PWM_ON)
     {
          if(pwm_val_left<=push_val_left)
	      {
	           EN1=1; 
	      }
	      else 
	      {
	           EN1=0;
          }
          if(pwm_val_left>=20)
	      pwm_val_left=0;
     }
     else    
     {
          EN1=0;   //��δ����PWM��EN1=0 ���� ֹͣ
     }
}
/******************************************************************/
/*                    �ҵ������                                  */  
void pwm_out_right_moto(void)
{ 
    if(Right_PWM_ON)
    { 
        if(pwm_val_right<=push_val_right)	//20ms�ڵ�ƽ�ź� 111 111 0000 0000 0000 00
	    {
	        EN2=1; 							//ռ�ձ�6:20
        }
	    else 
	    {
	        EN2=0;
        }
	    if(pwm_val_right>=20)
	    pwm_val_right=0;
    }
    else    
    {
        EN2=0;	  //��δ����PWM��EN2=0 �ҵ�� ֹͣ
    }
} 
/********************************************************/ 
 /*�������*/
void InitPWMTimer(void)
{
	TMOD |= 0x01;//����ʱ��0
  	TH0 = 0xff;   //���ö�ʱ��0��ֵ�����ʱ��Ϊ0.1ms
    TL0 = 0xa3;
	
	EA=1;//���ܶ�
	TR0=1;//����ʱ��0
	ET0=1;
}

void timer0(void) interrupt 1//��ʱ��0�жϺ���
{
 	TR0 = 0;      //�رն�ʱ��0
    TH0 = 0xff;   //��װ��ֵ0.1ms
    TL0 = 0xa3;
    //���1
    if(count <= SEH_count) //����ռ�ձ�����
    {
        //���count�ļ���С�ڣ�5-25��Ҳ����0.5ms-2.5ms�����Сt���ڳ����ߵ�ƽ����������
        pwm = 1;
    }
    else
    {
        pwm = 0;
		//TR0 = 0; //�ض�ʱ��0
    }

	count++;
	pwmcount++;
    if (count >= 200) //T = 20ms��ʱ������������0
    {
        count = 0;
    }
	if(pwmcount >= 10)
	{
		pwm_val_left++;
		pwm_val_right++;
		pwm_out_left_moto();
		pwm_out_right_moto();
		pwmcount = 0;
	}

    TR0 = 1; //������ʱ��0
}

/*�������*/
/*************������********************/
void main(void)
{
	cmg88();//�������
	delay(400); //�����ȴ�����LCM���빤��״̬
	LCMInit(); //LCM��ʼ��
	delay(5);//��ʱƬ��

	DisplayListChar(0, 0, Range);
	DisplayListChar(0, 1, table);
    
	TMOD=TMOD|0x10;//��T1Ϊ��ʽ1��GATE=1��
    EA=1;					   //�������ж�
    TH1=0;
    TL1=0;          
    ET1=1;             //����T1�ж�
    
	SEH_count = 10;

	InitPWMTimer();	 //��ʼ�����PWM
	//=======================================================================================================================			
	keyscan() ;  //����ɨ��
	while(1)
	{
	        
	    //�жϺ�������Ƿ񴥷�
	    if(Left_2_led==0 || Right_2_led==0)
		{
			Stop(1);//ɲ�����ȶ�����
			hongwai = 1;	
		}
		else
		{
			hongwai = 0;
		}

		Deal();
    
   	}

} 



                            