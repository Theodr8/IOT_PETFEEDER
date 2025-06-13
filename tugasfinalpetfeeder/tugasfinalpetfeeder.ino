#include <Wire.h>
#include <BluetoothSerial.h>
#include <ESP32Servo.h>

BluetoothSerial SerialBT; // Inisialisasi Bluetooth Serial
Servo Servo;
long duration;
float distance;
#define RELAY_PIN 2        // Pin relay terhubung ke pin 2 ESP32
#define TRIG_PIN 18
#define ECHO_PIN 26
const int servoPin = 17;  

// Inisialisasi LCD I2C 20x4 dengan alamat 0x27

bool bluetoothConnected = false; // Status koneksi Bluetooth

void setup() {
  pinMode(RELAY_PIN, OUTPUT);
  pinMode(TRIG_PIN, OUTPUT); //sensor
  pinMode(ECHO_PIN, INPUT); //sensor
  digitalWrite(RELAY_PIN, OUTPUT); 
  Serial.begin(115200);
  SerialBT.begin("ESP32"); // Nama Bluetooth ESP32
  Serial.println("Bluetooth ESP32 siap");

  // Inisialisasi LCD
    Servo.attach(servoPin);
  Servo.write(0); // Posisi awal

}

void loop() {
  // Periksa apakah Bluetooth terhubung
  if (SerialBT.hasClient() && !bluetoothConnected) {
    bluetoothConnected = true;
    Serial.println("Bluetooth Terhubung");
  } else if (!SerialBT.hasClient() && bluetoothConnected) {
    bluetoothConnected = false;
    Serial.println("Bluetooth Terputus");
  }

  // Cek data yang diterima dari Bluetooth
  if (SerialBT.available()) {
    char receivedChar = SerialBT.read(); // Membaca data dari Bluetooth
    String command = SerialBT.readStringUntil('\n'); // Read the entire command until newline
    command.trim(); // Remove leading/trailing whitespace

    Serial.print("Received BT Command: [");
    Serial.print(command);
    Serial.println("]");

    if (receivedChar == '1') {
      digitalWrite(RELAY_PIN, HIGH); // Nyalakan relay
      Servo.write(110);
      Serial.println("Relay diaktifkan");
      

      delay (2000);

      Servo.write(0);
    } else if (receivedChar == '0') {
      digitalWrite(RELAY_PIN, LOW); 
      Servo.write(0);
      Serial.println("Relay dimatikan");
    }
    else if (command == "ead"){
    Serial.println("masuk");
    command.trim();
      cekisi();
    }
    // else if (receivedChar == '2'){

    // }
  }
}

void cekisi() {
  // if (SerialBT.available()) {

    // if (command == "read") {
      float duration, distance, isi;

      digitalWrite(TRIG_PIN, LOW);
      delayMicroseconds(2);
      digitalWrite(TRIG_PIN, HIGH);
      delayMicroseconds(10);
      digitalWrite(TRIG_PIN, LOW);

      duration = pulseIn(ECHO_PIN, HIGH, 30000); // timeout 30ms
      distance = duration * 0.034 / 2;
      isi = 1 - distance/24 ;
        Serial.println(distance);
        Serial.println(isi);
      if (isi > 1 || isi < 0) {
        SerialBT.println("Tidak terdeteksi");
        Serial.println("Tidak terdeteksi");
      } else {
        SerialBT.println(isi*100);
        Serial.println(isi);
      }
    } 
//     }
//   }


