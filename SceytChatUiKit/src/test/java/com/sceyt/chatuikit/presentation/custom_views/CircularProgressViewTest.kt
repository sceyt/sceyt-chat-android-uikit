package com.sceyt.chatuikit.presentation.custom_views

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.ViewGroup
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class CircularProgressViewTest {

    @Test
    fun `rebinding a running transfer keeps the rotation position`() {
        val view = attachedProgressView()
        view.setProgress(50f)
        idleFor(400)
        view.setRotationAngle(123f)

        view.release(50f)
        view.setProgress(50f)

        assertThat(view.rotationAngle).isWithin(0.5f).of(123f)
    }

    @Test
    fun `icon set after layout is drawn at full size`() {
        val view = attachedProgressView()
        val icon = GradientDrawable().apply { setSize(24, 24) }

        view.setIcon(icon)
        view.draw(Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)))

        assertThat(view.width).isEqualTo(100)
        assertThat(icon.bounds.width()).isGreaterThan(0)
        assertThat(icon.bounds.height()).isGreaterThan(0)
    }

    private fun attachedProgressView(): CircularProgressView {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = CircularProgressView(activity)
        activity.setContentView(view, ViewGroup.LayoutParams(100, 100))
        idleFor(16)
        return view
    }

    private fun CircularProgressView.setRotationAngle(angle: Float) {
        CircularProgressView::class.java.getDeclaredField("startAngle").apply {
            isAccessible = true
            setFloat(this@setRotationAngle, angle)
        }
    }

    private fun idleFor(millis: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
    }
}
