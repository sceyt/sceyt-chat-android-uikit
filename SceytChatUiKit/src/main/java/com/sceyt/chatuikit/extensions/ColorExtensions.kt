package com.sceyt.chatuikit.extensions

import android.graphics.Color


fun darkenColor(color: Int, factor: Float): Int {/*
    val a = Color.alpha(color)
    val r = (Color.red(color) * factor).toInt()
    val g = (Color.green(color) * factor).toInt()
    val b = (Color.blue(color) * factor).toInt()
    return Color.argb(a, r.coerceAtLeast(0), g.coerceAtLeast(0), b.coerceAtLeast(0))*/
  /*  val a = Color.alpha(color)
    val r = Math.round(Color.red(color) * factor)
    val g = Math.round(Color.green(color) * factor)
    val b = Math.round(Color.blue(color) * factor)
    return Color.argb(a,
        min(r, 255),
        min(g, 255),
        min(b, 255));*/


     fun crimp( c:Int): Int {
        return Math.min(Math.max(c, 0), 255);
    }
    val factor = 0.5
    return (color and -0x1000000) or
            (crimp((((color shr 16) and 0xFF) * factor).toInt()) shl 16) or
            (crimp((((color shr 8) and 0xFF) * factor).toInt()) shl 8) or
            (crimp((((color) and 0xFF) * factor).toInt()))

   /*return Color.HSVToColor(FloatArray(3).apply {
        Color.colorToHSV(color, this)
        this[2] *= 0.2f
    })*/
}