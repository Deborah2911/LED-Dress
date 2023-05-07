package com.deborah.dress

enum class LedAlgorithm {
    OFF,
    AMPLITUDE,
    SOLID_COLOR,
    COLOR_BREATH;

    fun toByte(): Byte {
        return when (this) {
            OFF -> 0
            AMPLITUDE -> 1
            SOLID_COLOR -> 2
            COLOR_BREATH -> 3
        }
    }

    override fun toString(): String {
        return when (this) {
            OFF -> "Off"
            AMPLITUDE -> "Amplitude"
            SOLID_COLOR -> "Solid Color"
            COLOR_BREATH -> "Color Breath"
        }
    }

    companion object {
        fun fromByte(byte: Byte): LedAlgorithm {
            return when (byte) {
                0.toByte() -> OFF
                1.toByte() -> AMPLITUDE
                2.toByte() -> SOLID_COLOR
                3.toByte() -> COLOR_BREATH
                else -> throw IllegalArgumentException("No such algorithm")
            }
        }
    }
}