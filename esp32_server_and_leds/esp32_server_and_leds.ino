#include <WiFi.h>
#include <AsyncUDP.h>
#include <FastLED.h>
#include <climits>

#define NUM_LEDS 300
#define LED_PIN 2
#define LED_TYPE NEOPIXEL

CRGB led[NUM_LEDS];
//CRGBArray<NUM_LEDS> leds_gradient;

AsyncUDP udp;

enum class LedAlgorithm : uint8_t {
  OFF = 0,
  AMPLITUDE = 1,
  SOLID_COLOR = 2,
  COLOR_BREATH = 3,
  GRADIENT = 4
};

struct PacketData {
  int amplitude{};
  uint8_t red{}, green{}, blue{};
  LedAlgorithm algorithm = LedAlgorithm::OFF;
} data;

template<typename T>
T swap_endian(const T &u) {
  union {
    T u;
    unsigned char u8[sizeof(T)];
  } source, dest;

  source.u = u;

  for (size_t k = 0; k < sizeof(T); k++)
    dest.u8[k] = source.u8[sizeof(T) - k - 1];

  return dest.u;
}

void setup() {
  const char *ssid = "DeborahBoard";
  const char *password = "dela1la8";
  FastLED.addLeds<WS2812, LED_PIN, GRB>(led, NUM_LEDS);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH);

  Serial.begin(115200);
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);

  if (udp.listen(1234)) {
    Serial.print("UDP Listening on IP: ");
    Serial.println(WiFi.localIP());
    udp.onPacket([](AsyncUDPPacket &packet) {
      // Parse and convert the received bytes:
      memcpy(&data, packet.data(), packet.length());
      data.amplitude = swap_endian(data.amplitude);

      Serial.printf("A: %u R: %u G: %u B: %u Alg: %u\n", data.amplitude, data.red, data.green, data.blue, data.algorithm);
      packet.printf("Got %u bytes of data", packet.length());
    });
  }
}

void loop() {
  PacketData data_cpy = data;
  const float amplitude_percetage = float(data_cpy.amplitude) / SHRT_MAX;
  const int num_leds = NUM_LEDS * amplitude_percetage;

  if (data_cpy.algorithm == LedAlgorithm::OFF) {
    for (int i = 0; i < NUM_LEDS; i++)
      led[i] = CRGB();
    FastLED.show();
  } else if (data_cpy.algorithm == LedAlgorithm::AMPLITUDE) {
    for (int i = 0; i < num_leds; i++) {
      led[i] = CRGB(data_cpy.red, data_cpy.green, data_cpy.blue);
    }
    for (int i = num_leds; i < NUM_LEDS; i++) {
      led[i] = CRGB();
    }
    FastLED.show();
  } else if (data_cpy.algorithm == LedAlgorithm::SOLID_COLOR) {
    for (int i = 0; i < NUM_LEDS; i++)
      led[i] = CRGB(data_cpy.red, data_cpy.green, data_cpy.blue);
    FastLED.show();
  } else if (data_cpy.algorithm == LedAlgorithm::GRADIENT) {
      static uint8_t hue = 0; 
      for (int i = 0; i < NUM_LEDS; i++)
        led[i]=CHSV(hue++, 255, 255);  
      FastLED.delay(70);
  }

  delay(100);
}