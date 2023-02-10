package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages

enum class PlaybackSpeed(val value: Float, val displayValue: String) {
    X1(1f, "1x"),
    X1_5(1.5f, "1.5x"),
    X2(2f, "2x");

    fun next(): PlaybackSpeed {
        return when (this) {
            X1 -> X1_5
            X1_5 -> X2
            X2 -> X1
        }
    }

    companion object {
        fun fromValue(value: Float): PlaybackSpeed {
            return when (value) {
                X1.value -> X1
                X1_5.value -> X1_5
                X2.value -> X2
                else -> X1
            }
        }
    }
}