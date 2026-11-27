package com.sceyt.chatuikit.presentation.components.channel.messages

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.databinding.SceytItemIncPollMessageBinding
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IncomingPollMessageLayoutTest {
    @Test
    fun `finished poll with a reaction uses the available bubble width`() {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), androidx.appcompat.R.style.Theme_AppCompat)
        val binding = SceytItemIncPollMessageBinding.inflate(LayoutInflater.from(context))
        (binding.layoutDetails.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxWidth = 300
        binding.tvPollQuestion.text = "FuH"
        binding.tvPollType.text = "Poll finished"
        binding.tvViewResults.isVisible = false
        binding.divider.isVisible = false
        binding.messageDate.setText("12:41", enableLeadingText = false)
        val reactions = binding.rvReactions.inflate().apply { minimumWidth = 38 }

        val root = binding.root
        root.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        assertThat(binding.layoutDetails.width).isEqualTo(300)
        assertThat(binding.tvPollQuestion.lineCount).isEqualTo(1)
        assertThat(binding.tvPollType.lineCount).isEqualTo(1)
        assertThat(reactions.left).isEqualTo(binding.layoutDetails.left + 3)
    }
}
