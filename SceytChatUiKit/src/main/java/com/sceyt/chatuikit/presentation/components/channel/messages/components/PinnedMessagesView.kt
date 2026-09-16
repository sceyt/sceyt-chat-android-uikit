package com.sceyt.chatuikit.presentation.components.channel.messages.components

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.chatuikit.persistence.mappers.getVisualMediaType
import com.sceyt.chatuikit.databinding.SceytPinnedMessagesViewBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.formatters.attributes.PinnedMessageBodyFormatterAttributes
import com.sceyt.chatuikit.styles.messages_list.PinnedMessagesViewStyle
import kotlin.math.abs

/**
 * The banner under the toolbar. Shows one pin at a time and pages through the channel's pins.
 */
class PinnedMessagesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: SceytPinnedMessagesViewBinding =
        SceytPinnedMessagesViewBinding.inflate(LayoutInflater.from(context), this)
    val style: PinnedMessagesViewStyle = PinnedMessagesViewStyle.Builder(context, attrs).build()

    private var items: List<SceytPinnedMessage> = emptyList()
    private var selectedIndex: Int = 0

    var onAction: ((Action) -> Unit)? = null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                handledGesture = false
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return step(forward = false)
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                if (abs(velocityY) < FLING_VELOCITY_THRESHOLD) return false
                return step(forward = velocityY < 0)
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                val totalDy = (e1 ?: return false).y - e2.y
                if (abs(totalDy) < panThreshold) return false
                return step(forward = totalDy > 0)
            }
        }
    )

    private var handledGesture = false

    private var followsNewest = true

    private val panThreshold = dpToPx(12f).toFloat()

    init {
        applyStyle()

        binding.pinListButton.setOnClickListener {
            onAction?.invoke(Action.ShowList)
        }
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    fun setPinnedMessages(pinnedMessages: List<SceytPinnedMessage>) {
        val previousTid = currentItem()?.messageTid
        items = pinnedMessages
        isVisible = pinnedMessages.isNotEmpty()
        if (pinnedMessages.isEmpty()) return

        selectedIndex = if (followsNewest) pinnedMessages.lastIndex
        else pinnedMessages
            .indexOfFirst { it.messageTid == previousTid }
            .takeIf { it >= 0 } ?: pinnedMessages.lastIndex

        render()
    }

    fun currentItem(): SceytPinnedMessage? = items.getOrNull(selectedIndex)

    private fun step(forward: Boolean): Boolean {
        if (handledGesture || items.isEmpty()) return false
        handledGesture = true
        followsNewest = false
        select(if (forward) selectedIndex + 1 else selectedIndex - 1)
        val current = currentItem() ?: return false
        onAction?.invoke(Action.Jump(current))
        return true
    }

    private fun select(index: Int) {
        if (items.isEmpty()) return
        selectedIndex = ((index % items.size) + items.size) % items.size
        render()
    }

    private fun render() {
        val item = currentItem() ?: return
        binding.body.text = style.bodyFormatter.format(
            context,
            PinnedMessageBodyFormatterAttributes(
                message = item.message,
                mentionTextStyle = style.mentionTextStyle,
                deletedStateText = style.deletedStateText,
            )
        )
        binding.segmentIndicator.setSegments(items.size, selectedIndex)
        renderThumbnail(item)
    }


    private fun renderThumbnail(item: SceytPinnedMessage) = with(binding) {
        val attachment = item.message.attachments
            ?.firstOrNull { it.type != AttachmentTypeEnum.Link.value }
            ?.takeIf { it.getVisualMediaType() != null }

        // A view-once pin must never leak a preview of what it is hiding.
        if (attachment == null || item.message.viewOnce) {
            thumbnailContainer.isVisible = false
            return@with
        }

        thumbnailContainer.isVisible = true
        val placeholder = getThumbFromMetadata(attachment.metadata)
            ?.toDrawable(context.resources)?.mutate()
        Glide.with(context)
            .load(attachment.filePath ?: attachment.url)
            .placeholder(placeholder)
            .override(THUMBNAIL_PX)
            .centerCrop()
            .into(thumbnail)
    }

    private fun applyStyle() = with(binding) {
        setBackgroundColor(style.backgroundColor)
        style.titleTextStyle.apply(title)
        style.bodyTextStyle.apply(body)
        separator.setBackgroundColor(style.separatorColor)
        pinListButton.setImageDrawable(style.pinIcon)
        segmentIndicator.setColors(style.indicatorActiveColor, style.indicatorInactiveColor)
        thumbnailContainer.isVisible = false
    }

    sealed interface Action {
        /** Scroll the conversation to this pin. */
        data class Jump(val pinnedMessage: SceytPinnedMessage) : Action

        /** Open the full pinned-messages list. */
        data object ShowList : Action
    }

    private companion object {
        const val FLING_VELOCITY_THRESHOLD = 300f
        const val THUMBNAIL_PX = 100
    }
}
