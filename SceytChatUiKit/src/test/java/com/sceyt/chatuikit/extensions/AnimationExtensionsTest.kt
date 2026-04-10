package com.sceyt.chatuikit.extensions

import android.os.Looper
import android.view.View
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AnimationExtensionsTest {

    @Test
    fun `visibleGoneWithBottomSlideAnim shows view and restores final properties`() {
        val view = View(RuntimeEnvironment.getApplication()).apply {
            visibility = View.GONE
        }

        view.visibleInvisibleWithBottomSlideAnim(
            visible = true,
            offsetY = 24f,
            enterDuration = 0L
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(view.visibility).isEqualTo(View.VISIBLE)
        assertThat(view.alpha).isEqualTo(1f)
        assertThat(view.translationY).isEqualTo(0f)
    }

    @Test
    fun `visibleGoneWithBottomSlideAnim hides view and resets animated properties`() {
        val view = View(RuntimeEnvironment.getApplication()).apply {
            visibility = View.VISIBLE
        }

        view.visibleInvisibleWithBottomSlideAnim(
            visible = false,
            offsetY = 24f,
            exitDuration = 0L
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(view.visibility).isEqualTo(View.INVISIBLE)
        assertThat(view.alpha).isEqualTo(1f)
        assertThat(view.translationY).isEqualTo(0f)
    }
}
