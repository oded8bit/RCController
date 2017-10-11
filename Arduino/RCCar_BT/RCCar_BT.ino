#include <arduino.h>
#include <RBL_nRF8001.h>
#include <RBL_services.h>
#include <boards.h>
#include <SPI.h>
#include <EEPROM.h>

/***************************************************
 Car Firmware (Bluetooth) - Oded Cnaan 2016    
 This app allows a remote application (Android) to
 control the car over BLE.

 The car has one engine that controls the speed (FW/BW)
 and another engine for turning the wheels
 ****************************************************/
#define BLE_NAME              "RCCar_Oded"

/******************** Pins **************************/
#define PIN_CAR_WHEEL1        A2
#define PIN_CAR_WHEEL2        A3

#define PIN_CONNECTED_LED     A0
#define PIN_PROGRESS_LED      A1

#define PIN_CAR_FWD           A4
#define PIN_CAR_BWD           A5

#define PIN_CAR_SPEED         6

/****************************************************/
#define MAX_ANALOG_VALUE        1024
// The minimum value that causes the car to move.
// Needs to be adjusted per car type
#define SPEED_ANALOG_MIN_VALUE  760
#define SPEED_NUM_STEPS         10
#define SPEED_MULTIPLIER        (int)((MAX_ANALOG_VALUE - SPEED_ANALOG_MIN_VALUE) / SPEED_NUM_STEPS)

/******************** Commands *********************** 
Commands are string with the following format:
<command_char> <space> <value>

command_char may be 'S' (straight), 'L' (left)
'R' (right) and 'P' for power

value is a number between -10 and 10
*/


#define MAX_COMMAND           1
#define MAX_VALUE             5
#define BUFFER_SIZE           MAX_COMMAND + 1 + MAX_VALUE + 1

char mBuffer[BUFFER_SIZE+1];
char mCommand[MAX_COMMAND+1];
char mValue[MAX_VALUE+1];
int mBufindex = 0;

String mResponseStr = "OK";
char mResponse[BUFFER_SIZE];
int mResponseLength = 0;

/****************************************************/

/////////////////////// SETUP
void setup(void)
{  
  Serial.begin(115200);
  Serial.println(F("Initializing board..."));
  initPins();
  setProgressLed(true);
  showConnected(true);

  // Prepare response string
  memset(&mResponse, 0, sizeof(mResponse));
  mResponseStr.toCharArray(mResponse,BUFFER_SIZE);
  mResponseLength = mResponseStr.length();

  // Init BLE library
  Serial.println(F("Starting BLE shield and library..."));
  ble_begin();
  ble_set_name(BLE_NAME);
  
  testCar();
  setProgressLed(false);
  showConnected(true);
  Serial.println(F("Ready for connections");
}

/////////////////////// LOOP
void loop(void)
{
    if ( ble_available() ) {
        setProgressLed(true);

        // Clear the incoming data buffer and point to the beginning of it.
        mBufindex = 0;
        memset(&mBuffer, 0, sizeof(mBuffer));
        memset(&mCommand, 0, sizeof(mCommand));
        memset(&mValue, 0, sizeof(mValue));

        while ( ble_available() && (mBufindex < BUFFER_SIZE) ) {
            char ch = ble_read();
            if (ch != '\n' && ch != '\r')
              mBuffer[mBufindex++] = ch;
            else
              break;
        }
       
        // Send response
        sendResponse(mResponse, mResponseLength);
        
        if (parseCommand(mBuffer, mBufindex, mCommand, mValue)) {
          int value = atoi(mValue);
          Serial.print("Command: '");Serial.print(mCommand);
          Serial.print("' Value: '");Serial.print(value);Serial.println("'");
          handleCommand(mCommand, value);
        }
        else {
          Serial.println(F("ERR: Failed to parse command"));
        }
        setProgressLed(false);
     }
     ble_do_events();
}

void sendResponse(char *msg, int len) {
  ble_write_bytes((unsigned char *)msg,len);
}

/////////////////////// PARSING AND COMMANDS
void handleCommand(char *command, int value) {
  switch (command[0]) {
    case 'S':
      wheelStraight(value);
      break;
    case 'L':
      wheelLeft(value);
      break;
    case 'R':
      wheelRight(value);
      break;
    case 'P':
      power(value);
      break;
    default:
      break;
  }
}

bool parseCommand(char* buf,int bufSize, char* cmd, char *value) {
    if (bufSize < 2)
      return false;
    // Parse first word up to whitespace as action.
    char* space = strtok((char*)buf, " ");
    if (space != NULL) {
      strncpy(cmd, space, MAX_COMMAND);
      cmd[1] = NULL;
    }
    char* val = strtok(NULL, " ");
    if (val != NULL) {
      strncpy(value, val, MAX_VALUE);
      value[strlen(val)]=NULL;
    }
    return true;
}


/////////////////////// CAR CONTROL
void wheelStraight(int value) {
  analogWrite(PIN_CAR_WHEEL1, LOW);
  analogWrite(PIN_CAR_WHEEL2, LOW);        
}

void wheelLeft(int value) {
  analogWrite(PIN_CAR_WHEEL1, MAX_ANALOG_VALUE);
  analogWrite(PIN_CAR_WHEEL2, LOW);        
}

void wheelRight(int value) {
  analogWrite(PIN_CAR_WHEEL1, LOW);
  analogWrite(PIN_CAR_WHEEL2, MAX_ANALOG_VALUE);        
}

void power(int value) {
  if (value > 0) {  // forward
    analogWrite(PIN_CAR_FWD, MAX_ANALOG_VALUE);
    analogWrite(PIN_CAR_BWD, LOW);
  }
  else if (value < 0) { // reverse 
    analogWrite(PIN_CAR_FWD, LOW);
    analogWrite(PIN_CAR_BWD, MAX_ANALOG_VALUE);    
  }
  else { // stop
    analogWrite(PIN_CAR_FWD, LOW);
    analogWrite(PIN_CAR_BWD, LOW);        
  }
  int speed = getSpeed(value);
  analogWrite (PIN_CAR_SPEED, speed);
}

int getSpeed(int value) {
  if (value == 1)
    return 0;
  if (value < 0)
    value = -value;
  return SPEED_ANALOG_MIN_VALUE + (int)(value * SPEED_MULTIPLIER);
}

/////////////////////// PINS
void initPins() {
  pinMode(PIN_CONNECTED_LED, OUTPUT);
  analogWrite(PIN_CONNECTED_LED, LOW);
  pinMode(PIN_CAR_FWD, OUTPUT);
  analogWrite(PIN_CAR_FWD, LOW);
  pinMode(PIN_CAR_BWD, OUTPUT);
  analogWrite(PIN_CAR_BWD, LOW);
  pinMode(PIN_CAR_SPEED, OUTPUT);
  analogWrite(PIN_CAR_SPEED, LOW);
  pinMode(PIN_CAR_WHEEL1, OUTPUT);
  analogWrite(PIN_CAR_WHEEL1, LOW);
  pinMode(PIN_CAR_WHEEL2, OUTPUT);
  analogWrite(PIN_CAR_WHEEL2, LOW);
}

/////////////////////// LEDS
void showConnected(bool on) {
  if (on)
    analogWrite(PIN_CONNECTED_LED, MAX_ANALOG_VALUE);
  else
    analogWrite(PIN_CONNECTED_LED, LOW);
}

void showError() {
  digitalWrite(PIN_CONNECTED_LED, LOW);
  analogWrite(PIN_PROGRESS_LED, MAX_ANALOG_VALUE);
}

void setProgressLed(bool on) {
  if (on)
    analogWrite(PIN_PROGRESS_LED, MAX_ANALOG_VALUE);
  else
    analogWrite(PIN_PROGRESS_LED, LOW);
}

////////////////////////// Car Test
void testCar() {
  wheelRight(1);  
  delay(200);
  wheelLeft(1);
  delay(200);
  wheelStraight(1);
  delay(200);
  //power(5);
  //delay(200);
  power(0);
}


