package com.sceyt.chatuikit.presentation.components.pinned_messages

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class PinnedMessagesActivityStyleTest {

    private lateinit var controller: ActivityController<StyleLifecycleActivity>
    private lateinit var suppliedStyle: MessagesListViewStyle

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        suppliedStyle = MessagesListViewStyle.Builder(context, null).build()
            .copy(sameSenderMessageDistance = 37)
        val intent = PinnedMessagesActivity.createIntent(
            context,
            createChannel(id = 7L, pinnedAt = 0L, createdAt = 0L),
            suppliedStyle,
        ).setClass(context, StyleLifecycleActivity::class.java)
        controller = Robolectric.buildActivity(StyleLifecycleActivity::class.java, intent).setup()
    }

    @After
    fun tearDown() {
        if (!controller.get().isDestroyed) {
            controller.get().finish()
            controller.pause().stop().destroy()
        }
        StyleRegistry.unregister(suppliedStyle.styleId)
    }

    @Test
    fun `recreation retains the supplied conversation style`() {
        val previous = controller.get()

        controller.recreate()

        assertThat(controller.get()).isNotSameInstanceAs(previous)
        assertThat(StyleRegistry.getTyped<MessagesListViewStyle>(suppliedStyle.styleId))
            .isSameInstanceAs(suppliedStyle)
    }

    @Test
    fun `finishing removes the supplied conversation style`() {
        controller.get().finish()

        controller.pause().stop().destroy()

        assertThat(StyleRegistry.getTyped<MessagesListViewStyle>(suppliedStyle.styleId)).isNull()
    }

    class StyleLifecycleActivity : PinnedMessagesActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.SceytAppTheme)
            super.onCreate(savedInstanceState)
        }

        // Keep the production lifecycle while avoiding unrelated message-loading dependencies.
        override fun initViews() = Unit
        override fun observeState() = Unit
    }
}
