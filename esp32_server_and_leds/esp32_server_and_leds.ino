#include <WiFi.h>
#include <AsyncUDP.h>
#include <FastLED.h>
#include <climits>

#define NUM_LEDS 300
#define LED_PIN 13

CRGB led[NUM_LEDS];

AsyncUDP udp;

enum class LedAlgorithm : uint8_t {
  OFF = 0,
  AMPLITUDE = 1,
  SOLID_COLOR = 2,
  COLOR_BREATH = 3,
  GRADIENT = 4,
  WAVE = 5,
  DOUBLE_WAVE = 6,
  SPARKLE = 7
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

static uint8_t bri = 255;

void loop() {
  PacketData data_cpy = data;
  const float amplitude_percetage = float(data_cpy.amplitude) / SHRT_MAX;
  const int num_leds = NUM_LEDS * amplitude_percetage;

  const CRGB current_color(data_cpy.red, data_cpy.green, data_cpy.blue);

  if (data_cpy.algorithm == LedAlgorithm::OFF) {
    for (int i = 0; i < NUM_LEDS; i++)
      led[i] = CRGB();
    FastLED.show();
  } else if (data_cpy.algorithm == LedAlgorithm::AMPLITUDE) {
    for (int i = 0; i < num_leds; i++) {
      led[i] = current_color;
    }
    for (int i = num_leds; i < NUM_LEDS; i++) {
      led[i] = CRGB();
    }
    FastLED.show();
  } else if (data_cpy.algorithm == LedAlgorithm::SOLID_COLOR) {
    for (int i = 0; i < NUM_LEDS; i++)
      led[i] = current_color;
    FastLED.show();
  } else if (data_cpy.algorithm == LedAlgorithm::COLOR_BREATH) {
    static float t = 0.0f;
    static bool going_up = true;

    t += 0.02 * (-1.0f * going_up);

    if (t >= 1.0f) {
      going_up = false;
      t = 0.0f;
    } else if (t <= 0.0f) {
      going_up = true;
      t = 1.0f;
    }

    CRGB cb(
      static_cast<uint8_t>(current_color.r * t),
      static_cast<uint8_t>(current_color.g * t),
      static_cast<uint8_t>(current_color.b * t));

    for (int i = 0; i < NUM_LEDS; i++)
      led[i] = cb;
    FastLED.show();
  } else if (data_cpy.algorithm == LedAlgorithm::GRADIENT) {
    static uint8_t hue = 0;
    for (int i = 0; i < NUM_LEDS; i++)
      led[i] = CHSV(hue++, 255, 255);
    FastLED.delay(75);
  } else if (data_cpy.algorithm == LedAlgorithm::WAVE) {
    int ct = 0;
    for (int i = 0; i < NUM_LEDS; i++) {
      if (i > 120) {
        led[ct++] = CRGB();
      }
      led[i] = current_color;
      FastLED.show();
      delay(5);
    }
    for (int i = ct; i < NUM_LEDS; i++) {
      led[i] = CRGB();
      FastLED.show();
      delay(5);
    }
  } else if (data_cpy.algorithm == LedAlgorithm::DOUBLE_WAVE) {
    for (int i = 0; i <= NUM_LEDS / 2; i++) {
      led[i] = current_color;
      led[NUM_LEDS - i] = current_color;
      delay(7);
      FastLED.show();
    }
    delay(5);
    for (int i = 150; i < NUM_LEDS; i++) {
      led[i] = CRGB();
      led[NUM_LEDS - i] = CRGB();
      delay(7);
      FastLED.show();
    }
  } else if (data_cpy.algorithm == LedAlgorithm::SPARKLE) {
    uint8_t num = random8(30);
    for (int i = num; i < NUM_LEDS; i += 30)
      led[i] = current_color;
    FastLED.show();
    delay(8);
    FastLED.clear();
  }
  delay(20);
}