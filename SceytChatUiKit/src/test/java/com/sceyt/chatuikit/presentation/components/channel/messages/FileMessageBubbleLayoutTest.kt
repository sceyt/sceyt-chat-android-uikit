package com.sceyt.chatuikit.presentation.components.channel.messages

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.common.truth.Truth.assertWithMessage
import com.sceyt.chatuikit.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FileMessageBubbleLayoutTest {
    private val layouts = listOf(R.layout.sceyt_item_inc_file_message, R.layout.sceyt_item_out_file_message)

    @Test
    fun `caption wider than the filename expands the bubble`() {
        for (layout in layouts) {
            val fileOnly = row(layout, caption = "")
            val withCaption = row(layout, caption = "Please review the attached file")

            assertWithMessage("layout $layout must size the bubble using the caption")
                .that(withCaption.bubble.width).isGreaterThan(fileOnly.bubble.width)
        }
    }

    @Test
    fun `long captions wrap within the maximum bubble width`() {
        for (layout in layouts) {
            val row = row(layout, caption = "Please review the attached file and send your comments. ".repeat(8))

            assertWithMessage("layout $layout must use the available width for long captions")
                .that(row.bubble.width).isEqualTo(MAX_BUBBLE_WIDTH)
            assertWithMessage("layout $layout must wrap long captions")
                .that(row.body.lineCount).isGreaterThan(1)
            assertWithMessage("layout $layout caption must fit inside the bubble")
                .that(row.body.right).isAtMost(row.bubble.width)
        }
    }

    @Test
    fun `a short caption does not shrink a wider file row`() {
        for (layout in layouts) {
            val fileOnly = row(layout, caption = "", filename = "Quarterly_report.pdf")
            val withCaption = row(layout, caption = "Hi", filename = "Quarterly_report.pdf")

            assertWithMessage("layout $layout must still fit the file details")
                .that(withCaption.bubble.width).isEqualTo(fileOnly.bubble.width)
        }
    }

    private fun row(layout: Int, caption: String, filename: String = "a.txt"): Row {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), androidx.appcompat.R.style.Theme_AppCompat)
        val root = LayoutInflater.from(context).inflate(layout, null)
        val bubble = root.findViewById<ConstraintLayout>(R.id.layoutDetails)
        (bubble.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxWidth = MAX_BUBBLE_WIDTH
        val body = root.findViewById<TextView>(R.id.messageBody).apply {
            text = caption
            isVisible = caption.isNotBlank()
        }
        root.findViewById<TextView>(R.id.tvFileName).text = filename
        root.findViewById<TextView>(R.id.tvFileSize).text = "12 KB"
        root.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        return Row(bubble, body)
    }

    private data class Row(val bubble: ConstraintLayout, val body: TextView)

    companion object {
        private const val MAX_BUBBLE_WIDTH = 300
    }
}
