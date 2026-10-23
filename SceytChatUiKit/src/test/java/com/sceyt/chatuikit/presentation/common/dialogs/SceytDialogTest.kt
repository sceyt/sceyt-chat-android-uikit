package com.sceyt.chatuikit.presentation.common.dialogs

import android.app.Activity
import android.os.Looper
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.extensions.dismissSafety
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SceytDialogTest {
    private val dialogs = mutableListOf<SceytDialog>()

    @After
    fun tearDown() {
        dialogs.forEach { it.dismissSafety() }
    }

    @Test
    fun dialogWhoseWindowWasRemovedWithoutDismissIsNotReused() {
        val first = activity()
        val stale = show(first)
        first.windowManager.removeViewImmediate(stale.window!!.decorView)
        shadowOf(Looper.getMainLooper()).idle()

        val next = show(activity())

        assertThat(next).isNotSameInstanceAs(stale)
        assertThat(next.isShowing).isTrue()
    }

    @Test
    fun showingDialogIsReusedWhenNotReplacingLastDialog() {
        val activity = activity()
        val shown = show(activity)

        assertThat(show(activity)).isSameInstanceAs(shown)
    }

    private fun activity(): Activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    private fun show(activity: Activity) = SceytDialog.showDialog(
        context = activity,
        title = "Title",
        description = "Description",
        positiveBtnTitle = "OK",
        replaceLastDialog = false,
    ).also {
        dialogs += it
        shadowOf(Looper.getMainLooper()).idle()
    }
}
