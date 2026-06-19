package com.sceyt.chatuikit.presentation.components.global_search.voice.adapter

import android.view.View
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VoiceSearchListAdapterTest {

    @Test
    fun recyclerLifecycle_isForwardedToFileViewHolder() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val adapter = VoiceSearchListAdapter(scope, mock())
        val holder = TrackingFileViewHolder(
            View(RuntimeEnvironment.getApplication())
        )

        adapter.onViewAttachedToWindow(holder)
        adapter.onViewDetachedFromWindow(holder)

        assertThat(holder.attachCount).isEqualTo(1)
        assertThat(holder.detachCount).isEqualTo(1)
        scope.cancel()
    }

    private class TrackingFileViewHolder(view: View) : BaseFileViewHolder<Nothing>(view, {}) {
        var attachCount = 0
        var detachCount = 0

        override fun onViewAttachedToWindow() {
            super.onViewAttachedToWindow()
            attachCount++
        }

        override fun onViewDetachedFromWindow() {
            super.onViewDetachedFromWindow()
            detachCount++
        }
    }
}
