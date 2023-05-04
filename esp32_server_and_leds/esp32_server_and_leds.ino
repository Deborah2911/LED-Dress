#include <WiFi.h>
#include <AsyncUDP.h>
#include <FastLED.h>
#include <climits>

#define NUM_LEDS 300
#define LED_PIN 2

CRGB led[NUM_LEDS];

AsyncUDP udp;

enum class LedAlgorithm : uint8_t {
  OFF = 0,
  AMPLITUDE = 1,
  COLOR_BREATH = 2
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
    udp.onPacket([](AsyncUDPPacket& packet) {
      // Parse and convert the received bytes:
      memcpy(&data, packet.data(), packet.length());
      data.amplitude = swap_endian(data.amplitude);

      Serial.printf("A: %u R: %u G: %u B: %u\n", data.amplitude, data.red, data.green, data.blue);
      packet.printf("Got %u bytes of data", packet.length());

      const float amplitude_percetage = float(data.amplitude) / SHRT_MAX;
      const int num_leds = NUM_LEDS * amplitude_percetage;

      for (int i = 0; i < num_leds; i++) {
        led[i] = CRGB(data.red, data.green, data.blue);
      }
      for (int i = num_leds; i < NUM_LEDS; i++) {
        led[i] = CRGB();
      }
      FastLED.show();
    });
  }
}

void loop() {
  delay(100);
}