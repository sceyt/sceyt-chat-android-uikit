package com.sceyt.chatuikit.presentation.components.channel.header

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.formatters.Formatter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class MessagesListHeaderViewConnectionStateTest {
    private lateinit var originalFormatter: Formatter<ConnectionState>
    private lateinit var activityController: ActivityController<AppCompatActivity>

    @Before
    fun setUp() {
        originalFormatter = SceytChatUIKit.formatters.connectionStateTitleFormatter
        activityController = Robolectric.buildActivity(AppCompatActivity::class.java).apply {
            get().setTheme(R.style.SceytAppTheme)
            setup()
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.formatters.connectionStateTitleFormatter = originalFormatter
        activityController.destroy()
    }

    @Test
    fun `blank connection state title hides subtitle`() {
        SceytChatUIKit.formatters.connectionStateTitleFormatter = Formatter { _, _ -> "" }
        val headerView = createHeaderView()
        val subtitle = headerView.findViewById<TextView>(R.id.subTitle).apply {
            text = "Last seen recently"
            isVisible = true
        }

        headerView.onConnectionStateChanged(ConnectionState.Disconnected, channel())

        assertThat(subtitle.isVisible).isFalse()
    }

    @Test
    fun `non blank connection state title shows subtitle`() {
        SceytChatUIKit.formatters.connectionStateTitleFormatter =
            Formatter { _, _ -> "Waiting for network…" }
        val headerView = createHeaderView()
        val subtitle = headerView.findViewById<TextView>(R.id.subTitle)

        headerView.onConnectionStateChanged(ConnectionState.Disconnected, channel())

        assertThat(subtitle.text.toString()).isEqualTo("Waiting for network…")
        assertThat(subtitle.isVisible).isTrue()
    }

    private fun createHeaderView(): MessagesListHeaderView {
        return MessagesListHeaderView(activityController.get())
    }

    private fun channel() = createChannel(1L, 0L, 0L).copy(type = "group")
}
