package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.GradientDrawable
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.Shapeable

sealed interface Shape {
    data object Circle : Shape

    data class RoundedCornerShape(
            val topLeft: Float = 0f,
            val topRight: Float = 0f,
            val bottomRight: Float = 0f,
            val bottomLeft: Float = 0f
    ) : Shape {
        constructor(radius: Float) : this(radius, radius, radius, radius)
    }

    data object UnsetShape : Shape
}

fun Shape.applyTo(view: GradientDrawable) {
    when (this) {
        is Shape.Circle -> view.shape = GradientDrawable.OVAL
        is Shape.RoundedCornerShape -> view.cornerRadii = floatArrayOf(
            topLeft, topLeft,
            topRight, topRight,
            bottomRight, bottomRight,
            bottomLeft, bottomLeft
        )

        Shape.UnsetShape -> return
    }
}

fun Shape.applyTo(view: Shapeable) {
    view.shapeAppearanceModel = when (this) {
        is Shape.Circle -> {
            ShapeAppearanceModel()
                .toBuilder()
                .setAllCornerSizes(RelativeCornerSize(0.5f))
                .build()
        }

        is Shape.RoundedCornerShape -> {
            ShapeAppearanceModel()
                .toBuilder()
                .setTopLeftCornerSize(topLeft)
                .setTopRightCornerSize(topRight)
                .setBottomRightCornerSize(bottomRight)
                .setBottomLeftCornerSize(bottomLeft)
                .build()
        }

        Shape.UnsetShape -> return
    }
}