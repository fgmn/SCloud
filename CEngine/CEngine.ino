
int Pin1 = 2, Pin2 = 4;
// 1号舵机连接Pin1，控制朝向；
// 2号舵机连接Pin2，控制俯仰

int myangle, pulsewidth, val, Serv;
// 角度变量，脉宽变量

int Pos1, Pos2, Dir, Step, Destination;

// 限位
int IdealPos1 = 65, IdealPos2 = 60;
int Range1 = 50, Range2 = 20;
int RangeMin = 20;

// 接收命令
char Cmd[50];
String Type;
int Cnt;
bool valid;
float X, Y;
float Width = 1280, Height = 720;

// 命令解释器
String CJ = "CJ";
String CM = "CM";
String SM = "SM";
String ST = "ST";

float Pi = 3.1415926;

void setup() {
  // put your setup code here, to run once:
  pinMode(Pin1, OUTPUT);
  pinMode(Pin2, OUTPUT);

  Serial.begin(9600);
  Serial.println("Starting...");

  Initial();
}

void loop() {
  // put your main code here, to run repeatedly:
  // 串口控制
//  work();
//  work2();
  work3();
}

// myangle / (pulsewidth - .5ms) = 45 degree / .5ms
// 11.111... = 11
void servopulse(int Pin, int myangle) {
  pulsewidth = myangle * 11 + 500;  // 将角度转化为500~2480的脉宽值
  digitalWrite(Pin, HIGH);
  delayMicroseconds(pulsewidth);  // 延时脉宽值的微妙数
  digitalWrite(Pin, LOW);
  delay(20 - pulsewidth / 1000);
}

void work() {
  if (Serial.available()) {
    Serv = Serial.parseInt();
    val = Serial.parseInt();
    Serial.print("Servo ");
    Serial.print(Serv);
    Serial.print(" moving to ");
    Serial.println(val);

    // 给舵机足够的时间到达指定位置
    for (int i = 0; i <= 75; i++) {
      if (Serv == 1) servopulse(Pin1, val);
      else if (Serv == 2) servopulse(Pin2, val);
    }
  }
}

void work2() {
  if (Serial.available()) {
    char Op = Serial.read();

    switch(Op) {
      case 'a':
        Serv = Pin1, Dir = -1;
        break;
      case 'd':
        Serv = Pin1, Dir = 1;
        break;
      case 'w':
        Serv = Pin2, Dir = 1;
        break;
      case 's':
        Serv = Pin2, Dir = -1;
        break;
    }
    
    if (Serv == Pin1) {
      Pos1 = Pos1 + Step * Dir;
      Destination = Pos1;
    }
    else if (Serv == Pin2) {
      Pos2 = Pos2 + Step * Dir;
      Destination = Pos2;      
    }
    
    for (int i = 0; i < 5; i++) {
      servopulse(Serv, Destination);
    }
  }
}

void Initial() {
  Pos1 = IdealPos1;
  Pos2 = IdealPos2;
  Step = 1;
  
  Cnt = 0;
  valid = false;
  
  for (int i = 0; i <= 75; i++) {
    servopulse(Pin1, Pos1);
    servopulse(Pin2, Pos2);  
  }

}

void work3() {
  if (Serial.available()) {
    char c = Serial.read();
    if (c == ';') {
      Cmd[Cnt] = c;
      Cnt = 0;
      Serial.println(c);
      
      Type = strtok(Cmd, ",");
//      Serial.println(Type);
      if (Type == CJ) {
        X = atof(strtok(NULL, ","));
        Y = atof(strtok(NULL, ";"));
//        Serial.println(X);
//        Serial.println(Y);
        valid = true;
        Trans();
        if (valid) Exec();
      }
      else if (Type == CM) {
        // X: picth Y: roll
        X = atof(strtok(NULL, ","));
        Y = atof(strtok(NULL, ";"));
        valid = true;
        Trans2();
        if (valid) Exec();
      }
      else if (Type == SM) {
        // 同步视图宽高
        Width = atof(strtok(NULL, ","));
        Height = atof(strtok(NULL, ";"));
        Serial.println(Width);
        Serial.println(Height);
      }
      else if (Type == ST) {
        X = atof(strtok(NULL, ","));
        Y = atof(strtok(NULL, ";"));
        valid = true;
        Trans3();
        if (valid) Exec();
      }
      else {
        valid = false;
//        Serial.println("invalid");
      }
    }
    else {
      Cmd[Cnt++] = c;
      Serial.print(c);
    }
  }
}

void Trans() {
  float x = X, y = Y;
  X = x * cos(Pi / 4) - y * sin(Pi / 4);
  Y = y * cos(Pi / 4) + x * sin(Pi / 4);

  if (X > 0 && Y > 0) {   // 'd'
     Serv = Pin1, Dir = 1;
  }
  else if (X < 0 && Y > 0) {  // 'w'
    Serv = Pin2, Dir = 1;
  }
  else if (X < 0 && Y < 0) {  // 'a'
    Serv = Pin1, Dir = -1;
  }
  else if (X > 0 && Y < 0) { // 's'
    Serv = Pin2, Dir = -1;
  }
}

void Trans2() {
  if (max(fabs(X), fabs(Y)) > RangeMin) {
    if (fabs(X) >= fabs(Y)) {
      if (X > 0) Serv = Pin2, Dir = 1;
      else Serv = Pin2, Dir = -1;
    }
    else {
      if (Y > 0) Serv = Pin1, Dir = 1;
      else Serv = Pin1, Dir = -1;
    }
  }
  else valid = false;
}

void Trans3() {
  if (X > Width / 2 + Width / 10) {   //
     Serv = Pin1, Dir = -1;
  }
  else if (X < Width / 2 - Width / 10) {
    Serv = Pin1, Dir = 1;
  }
  else if (Y < Height / 2 - Height / 10) {
    Serv = Pin2, Dir = 1;
  }
  else if (Y > Height / 2 + Height / 10) {
    Serv = Pin2, Dir = -1;
  }
}

void Exec() {

  if (Serv == Pin1) {
    if (Pos1 + Step * Dir >= IdealPos1 - Range1 &&\
      Pos1 + Step * Dir <= IdealPos1 + Range1) {
        Pos1 = Pos1 + Step * Dir;
      }
    Destination = Pos1;
  }
  else if (Serv == Pin2) {
    if (Pos2 + Step * Dir >= IdealPos2 - Range2 &&\
      Pos2 + Step * Dir <= IdealPos2 + Range2) {
        Pos2 = Pos2 + Step * Dir;
      }
    Destination = Pos2;      
  }
  
  for (int i = 0; i < 5; i++) {
    servopulse(Serv, Destination);
//    Serial.print(Serv);
//    Serial.print(" at ");
//    Serial.println(Destination);
  }
}
