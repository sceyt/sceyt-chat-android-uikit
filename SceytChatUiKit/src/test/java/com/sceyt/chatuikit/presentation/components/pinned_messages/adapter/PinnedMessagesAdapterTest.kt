package com.sceyt.chatuikit.presentation.components.pinned_messages.adapter

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.databinding.SceytItemPinnedMessageBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.styles.pinned_messages.PinnedMessagesStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PinnedMessagesAdapterTest {

    @Test
    fun `wrapper forwards attach and detach to the message holder`() {
        val context = RuntimeEnvironment.getApplication()
        val binding = SceytItemPinnedMessageBinding.inflate(LayoutInflater.from(context))
        val callbacks = mutableListOf<String>()
        val messageHolder = object : BaseMessageViewHolder(FrameLayout(context), mock()) {
            override val incoming = true
            override val selectMessageView: View? = null

            override fun onViewAttachedToWindow() {
                callbacks.add("attached")
            }

            override fun onViewDetachedFromWindow() {
                callbacks.add("detached")
            }
        }
        binding.bubbleContainer.addView(messageHolder.itemView)
        val adapter = PinnedMessagesAdapter(
            style = PinnedMessagesStyle.Builder(context, null).build(),
            viewHolderFactory = mock(),
            onNavigateClick = {}
        )
        val holder = adapter.ViewHolder(binding, messageHolder)

        adapter.onViewAttachedToWindow(holder)
        adapter.onViewDetachedFromWindow(holder)
        adapter.onViewAttachedToWindow(holder)

        assertThat(callbacks).containsExactly("attached", "detached", "attached").inOrder()
    }
}
