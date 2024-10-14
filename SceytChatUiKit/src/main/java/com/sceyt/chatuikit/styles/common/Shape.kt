package com.sceyt.chatuikit.styles.common

sealed interface Shape {
    data object Circle : Shape
    data class RoundedRectangle(val radius: Float) : Shape
}