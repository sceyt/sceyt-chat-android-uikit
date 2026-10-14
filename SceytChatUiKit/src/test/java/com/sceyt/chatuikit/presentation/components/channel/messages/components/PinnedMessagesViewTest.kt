package com.sceyt.chatuikit.presentation.components.channel.messages.components

import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytMessage
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytPinnedMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/**
 * The slide is driven by seeking the banner's animator rather than by idling the looper:
 * Robolectric's frame clock can jump past the whole 180 ms slide in a single step.
 */
@RunWith(RobolectricTestRunner::class)
class PinnedMessagesViewTest {

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var view: PinnedMessagesView
    private val jumps = mutableListOf<SceytPinnedMessage>()
    private var nextTapTime = SystemClock.uptimeMillis()

    private val body get() = view.findViewById<TextView>(R.id.body)

    private val contentAnimator
        get() = PinnedMessagesView::class.java.getDeclaredField("contentAnimator")
            .apply { isAccessible = true }
            .get(view) as ValueAnimator?

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = activityController.get()
        // The banner's layout uses AppCompat theme attributes, as every screen hosting it does.
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
        view = PinnedMessagesView(activity).apply {
            onAction = { if (it is PinnedMessagesView.Action.Jump) jumps += it.pinnedMessage }
        }
        activity.setContentView(FrameLayout(activity).apply {
            addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200))
        })
    }

    @After
    fun tearDown() {
        activityController.pause().stop().destroy()
    }

    @Test
    fun `shows the newest pin at once`() {
        view.setPinnedMessages(pins("one", "two", "three"))

        assertShows("three")
        assertThat(contentAnimator).isNull()
    }

    @Test
    fun `a tap slides the old pin down and out, then the older pin in from above`() {
        view.setPinnedMessages(pins("one", "two", "three"))

        tap()

        seek(50)
        assertThat(body.text.toString()).isEqualTo("three")
        assertThat(body.alpha).isLessThan(1f)
        assertThat(body.translationY).isGreaterThan(0f)

        seek(140)
        assertThat(body.text.toString()).isEqualTo("two")
        assertThat(body.alpha).isLessThan(1f)
        assertThat(body.translationY).isLessThan(0f)

        finishSlide()
        assertShows("two")
        assertThat(jumps.map { it.message.body }).containsExactly("two")
    }

    @Test
    fun `a new pin arriving while showing the newest slides up to it`() {
        val pins = pins("one", "two")
        view.setPinnedMessages(pins)

        view.setPinnedMessages(pins + pin(3, "three"))

        seek(50)
        assertThat(body.text.toString()).isEqualTo("two")
        assertThat(body.translationY).isLessThan(0f)

        finishSlide()
        assertShows("three")
    }

    @Test
    fun `the same pins arriving mid-slide do not cut the slide short`() {
        val pins = pins("one", "two", "three")
        view.setPinnedMessages(pins)

        tap()
        seek(50)
        // Jumping loads the pin's messages, which re-emits the same pins.
        view.setPinnedMessages(pins.map { it.copy() })

        assertThat(contentAnimator?.isRunning).isTrue()
        assertThat(body.alpha).isLessThan(1f)

        finishSlide()
        assertShows("two")
    }

    @Test
    fun `a refresh of the shown pin with no slide running updates it at once`() {
        view.setPinnedMessages(pins("one", "two"))

        view.setPinnedMessages(listOf(pin(1, "one"), pin(2, "two edited")))

        assertShows("two edited")
    }

    @Test
    fun `a tap during a slide ends on the newest selection fully visible`() {
        view.setPinnedMessages(pins("one", "two", "three"))

        tap()
        seek(50)
        tap()

        finishSlide()
        assertShows("one")
        assertThat(jumps.map { it.message.body }).containsExactly("two", "one").inOrder()
    }

    @Test
    fun `leaving the screen mid-slide leaves the current pin fully visible`() {
        view.setPinnedMessages(pins("one", "two", "three"))

        tap()
        seek(50)
        (view.parent as ViewGroup).removeView(view)

        assertShows("two")
    }

    private fun assertShows(text: String) {
        assertThat(body.text.toString()).isEqualTo(text)
        assertThat(body.alpha).isEqualTo(1f)
        assertThat(body.translationY).isZero()
    }

    /** A single tap, spaced past the double-tap window so each one counts on its own. */
    private fun tap() {
        val down = nextTapTime
        nextTapTime += ViewConfiguration.getDoubleTapTimeout() + 100L
        val downEvent = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        val upEvent = MotionEvent.obtain(down, down + 10, MotionEvent.ACTION_UP, 10f, 10f, 0)
        view.onTouchEvent(downEvent)
        view.onTouchEvent(upEvent)
        downEvent.recycle()
        upEvent.recycle()
    }

    private fun seek(millis: Long) {
        contentAnimator!!.currentPlayTime = millis
    }

    private fun finishSlide() {
        contentAnimator?.end()
    }

    private fun pins(vararg bodies: String) =
        bodies.mapIndexed { index, text -> pin(index + 1L, text) }

    private fun pin(id: Long, text: String) = sceytPinnedMessage(
        id = id,
        message = sceytMessage(id = id, tid = id).copy(body = text),
    )
}
