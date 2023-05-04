package com.deborah.dress

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class LedAlgorithm {
    OFF,
    AMPLITUDE,
    COLOR_BREATH;

    fun toByte(): Byte {
        return when (this) {
            OFF -> 0
            AMPLITUDE -> 1
            COLOR_BREATH -> 2
        }
    }

    companion object {
        fun fromByte(byte: Byte): LedAlgorithm {
            return when (byte) {
                0.toByte() -> OFF
                1.toByte() -> AMPLITUDE
                2.toByte() -> COLOR_BREATH
                else -> throw IllegalArgumentException("No such algorithm")
            }
        }
    }
}