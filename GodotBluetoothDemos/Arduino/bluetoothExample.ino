#include <SoftwareSerial.h>

SoftwareSerial bluetooth(10, 11);

#define led1 5
#define led2 4
#define led3 3

String command;

void setup() {
  bluetooth.begin(9600);

  pinMode(led1, OUTPUT);
  pinMode(led2, OUTPUT);
  pinMode(led3, OUTPUT);
}

void loop() {
  command = "";
  if(bluetooth.available()) {
    while(bluetooth.available()) {
    char character = bluetooth.read();

    command += character;
    delay(10);
    }  
    if(command.indexOf("led1") >= 0){
      digitalWrite(led1, !digitalRead(led1));
    }
    if(command.indexOf("led2") >= 0){
      digitalWrite(led2, !digitalRead(led2));
    }
    if(command.indexOf("led3") >= 0){
      digitalWrite(led3, !digitalRead(led3));
    }

    bluetooth.println("{");

    if(digitalRead(led1)) {
      bluetooth.println("l1on");
    }
    else {
      bluetooth.println("l1off");
    }

    if(digitalRead(led2)) {
      bluetooth.println("l2on");
    }
    else {
      bluetooth.println("l2off");
    }

    if(digitalRead(led3)) {
      bluetooth.println("l3on");
    }
    else {
      bluetooth.println("l3off");
    }
    bluetooth.println("}");
  }

}
