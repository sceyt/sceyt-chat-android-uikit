package com.sceyt.chatuikit.presentation.custom_views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DecoratedTextViewTest {

    @Test
    fun `turning highlight off restores the view own padding`() {
        val view = createView()
        view.setPadding(10, 0, 10, 0)
        val initialSize = view.measureSize()

        view.setHighlighted(true)
        val highlightedSize = view.measureSize()
        view.setHighlighted(false)

        assertThat(highlightedSize).isNotEqualTo(initialSize)
        assertThat(view.measureSize()).isEqualTo(initialSize)
    }

    @Test
    fun `overlay background uses appearance overlay color and is dropped when highlight is off`() {
        val view = createView()
        view.appearanceBuilder()
            .setOverlayColor(Color.RED)
            .build()
            .apply()

        view.setHighlighted(true)
        assertThat(view.drawnBackgroundColors()).containsExactly(Color.RED)

        view.setHighlighted(false)
        assertThat(view.drawnBackgroundColors()).isEmpty()
    }

    @Test
    fun `isHighlighted reflects the last setHighlighted call`() {
        val view = createView()

        view.setHighlighted(true)
        assertThat(view.isHighlighted).isTrue()

        view.setHighlighted(false)
        assertThat(view.isHighlighted).isFalse()
    }

    private fun createView(): DecoratedTextView {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sceytUiDecoratedTextTitle, "12:00")
            .build()
        return DecoratedTextView(RuntimeEnvironment.getApplication(), attrs)
    }

    private fun DecoratedTextView.measureSize(): Pair<Int, Int> {
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measure(spec, spec)
        return measuredWidth to measuredHeight
    }

    private fun DecoratedTextView.drawnBackgroundColors(): List<Int> {
        val (width, height) = measureSize()
        layout(0, 0, width, height)
        val canvas = RecordingCanvas()
        draw(canvas)
        return canvas.roundRectColors
    }

    private class RecordingCanvas : Canvas() {
        val roundRectColors = mutableListOf<Int>()

        override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
            roundRectColors += paint.color
        }
    }
}
