#include <SoftwareSerial.h>

SoftwareSerial bluetooth(10, 11);

String command;

void setup() {
  bluetooth.begin(9600);

  pinMode(LED_BUILTIN, OUTPUT);
}

void loop() {
  command = "";
  if(bluetooth.available()) {
    while(bluetooth.available()) {
    char character = bluetooth.read();

    command += character;
    delay(10);
    }  
    if(command.indexOf("builtin_led") >= 0){
      digitalWrite(LED_BUILTIN, !digitalRead(LED_BUILTIN));
    }
    bluetooth.println("{");

    if(digitalRead(LED_BUILTIN)) {
      bluetooth.println("ledon");
    }
    else {
      bluetooth.println("ledoff");
    }

    bluetooth.println("}");
  }

}
