package com.sceyt.chatuikit.extensions

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Regression tests for the video controls bar growing taller every time it was rebound.
 *
 * `MediaVideoViewHolder.initVideoController()` applies system window insets to the controls
 * container on every `bind()`, and a RecyclerView rebinds the same recycled view while swiping
 * between videos. Each call used to re-snapshot the padding that already contained the insets, so
 * the bar grew by a navigation bar height per rebind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewInsetsExtensionsTest {

    private val navigationBarHeight = 100
    private val originalPadding = 10

    private fun createView(): View {
        val parent = FrameLayout(RuntimeEnvironment.getApplication())
        val view = View(RuntimeEnvironment.getApplication()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(originalPadding, originalPadding, originalPadding, originalPadding)
        }
        parent.addView(view)
        return view
    }

    private fun View.dispatchSystemBarInsets() {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(0, 0, 0, navigationBarHeight)
            )
            .setInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(0, 0, 0, navigationBarHeight)
            )
            .build()
            .toWindowInsets()
        requireNotNull(insets) { "Could not build platform WindowInsets" }
        dispatchApplyWindowInsets(insets)
    }

    @Test
    fun `insets are added to the original padding`() {
        val view = createView()

        view.applySystemWindowInsetsPadding(applyBottom = true)
        view.dispatchSystemBarInsets()

        assertThat(view.paddingBottom).isEqualTo(originalPadding + navigationBarHeight)
        assertThat(view.paddingTop).isEqualTo(originalPadding)
    }

    @Test
    fun `applying insets again on the same view does not grow the padding`() {
        val view = createView()

        // First bind.
        view.applySystemWindowInsetsPadding(applyBottom = true)
        view.dispatchSystemBarInsets()
        val paddingAfterFirstBind = view.paddingBottom

        // The item is rebound while swiping, so the same view is set up again.
        view.applySystemWindowInsetsPadding(applyBottom = true)
        view.dispatchSystemBarInsets()

        assertThat(view.paddingBottom).isEqualTo(paddingAfterFirstBind)
        assertThat(view.paddingBottom).isEqualTo(originalPadding + navigationBarHeight)
    }

    @Test
    fun `padding stays stable across many rebinds`() {
        val view = createView()

        repeat(10) {
            view.applySystemWindowInsetsPadding(applyBottom = true)
            view.dispatchSystemBarInsets()
        }

        assertThat(view.paddingBottom).isEqualTo(originalPadding + navigationBarHeight)
    }

    @Test
    fun `redelivering insets to an already configured view does not grow the padding`() {
        val view = createView()

        view.applySystemWindowInsetsPadding(applyBottom = true)
        repeat(5) { view.dispatchSystemBarInsets() }

        assertThat(view.paddingBottom).isEqualTo(originalPadding + navigationBarHeight)
    }

    @Test
    fun `sides that are not requested keep the original padding`() {
        val view = createView()

        view.applySystemWindowInsetsPadding(applyBottom = true, applyLeft = true)
        view.dispatchSystemBarInsets()

        assertThat(view.paddingLeft).isEqualTo(originalPadding)
        assertThat(view.paddingRight).isEqualTo(originalPadding)
        assertThat(view.paddingTop).isEqualTo(originalPadding)
        assertThat(view.paddingBottom).isEqualTo(originalPadding + navigationBarHeight)
    }
}
