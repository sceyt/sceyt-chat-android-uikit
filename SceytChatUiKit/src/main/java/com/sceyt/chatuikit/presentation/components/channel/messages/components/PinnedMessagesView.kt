package com.sceyt.chatuikit.presentation.components.channel.messages.components

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.databinding.SceytPinnedMessagesViewBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.formatters.attributes.PinnedMessageBodyFormatterAttributes
import com.sceyt.chatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.chatuikit.persistence.mappers.getVisualMediaType
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

    private var contentAnimator: ValueAnimator? = null
    private val contentShift = dpToPx(10f).toFloat()
    private val contentOutInterpolator = AccelerateInterpolator()
    private val contentInInterpolator = DecelerateInterpolator()

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
        val previousIndex = pinnedMessages.indexOfFirst { it.messageTid == previousTid }
        items = pinnedMessages
        isVisible = pinnedMessages.isNotEmpty()
        if (pinnedMessages.isEmpty()) return

        selectedIndex = if (followsNewest) pinnedMessages.lastIndex
        else previousIndex.takeIf { it >= 0 } ?: pinnedMessages.lastIndex

        val newPinArrived = previousIndex >= 0 && selectedIndex == previousIndex + 1
        val samePinShown = currentItem()?.messageTid == previousTid
        when {
            newPinArrived -> render(slideTowardsNewer = true)
            samePinShown && contentAnimator?.isRunning == true ->
                binding.segmentIndicator.setSegments(items.size, selectedIndex)

            else -> render()
        }
    }

    fun currentItem(): SceytPinnedMessage? = items.getOrNull(selectedIndex)

    private fun step(forward: Boolean): Boolean {
        if (handledGesture || items.isEmpty()) return false
        handledGesture = true
        followsNewest = false
        select(if (forward) selectedIndex + 1 else selectedIndex - 1, slideTowardsNewer = forward)
        val current = currentItem() ?: return false
        onAction?.invoke(Action.Jump(current))
        return true
    }

    private fun select(index: Int, slideTowardsNewer: Boolean) {
        if (items.isEmpty()) return
        selectedIndex = ((index % items.size) + items.size) % items.size
        render(slideTowardsNewer)
    }

    private fun render(slideTowardsNewer: Boolean? = null) {
        val item = currentItem() ?: return
        binding.segmentIndicator.setSegments(items.size, selectedIndex)
        if (slideTowardsNewer == null) {
            contentAnimator?.cancel()
            renderContent(item)
        } else animateContent(slideTowardsNewer)
    }

    private fun renderContent(item: SceytPinnedMessage) {
        binding.body.text = style.bodyFormatter.format(
            context,
            PinnedMessageBodyFormatterAttributes(
                message = item.message,
                mentionTextStyle = style.mentionTextStyle,
                deletedStateText = style.deletedStateText,
            )
        )
        renderThumbnail(item)
        renderBodyIcon(item)
    }

    private fun animateContent(towardsNewer: Boolean) {
        contentAnimator?.cancel()
        val shift = if (towardsNewer) -contentShift else contentShift
        var swapped = false
        contentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CONTENT_ANIM_DURATION_MS
            addUpdateListener {
                val fraction = it.animatedFraction
                if (fraction < 0.5f) {
                    val out = contentOutInterpolator.getInterpolation(fraction * 2)
                    setContentOffset(shift * out, 1f - out)
                } else {
                    if (!swapped) {
                        swapped = true
                        currentItem()?.let(::renderContent)
                    }
                    val enter = contentInInterpolator.getInterpolation((fraction - 0.5f) * 2)
                    setContentOffset(-shift * (1f - enter), enter)
                }
            }
            doOnEnd {
                if (!swapped) currentItem()?.let(::renderContent)
                setContentOffset(0f, 1f)
            }
            start()
        }
    }

    private fun setContentOffset(translationY: Float, alpha: Float) = with(binding) {
        listOf(body, bodyIcon, thumbnailContainer).forEach {
            it.translationY = translationY
            it.alpha = alpha
        }
    }

    override fun onDetachedFromWindow() {
        contentAnimator?.cancel()
        contentAnimator = null
        super.onDetachedFromWindow()
    }

    private fun renderBodyIcon(item: SceytPinnedMessage) = with(binding) {
        val icon = style.iconProvider.provide(context, item.message)
        bodyIcon.isVisible = icon != null
        bodyIcon.setImageDrawable(icon)
    }


    private fun renderThumbnail(item: SceytPinnedMessage) = with(binding) {
        val attachment = item.message.attachments
            ?.firstOrNull { it.type != AttachmentTypeEnum.Link.value }
            ?.takeIf { it.getVisualMediaType() != null }

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
        bodyIcon.isVisible = false
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
        const val CONTENT_ANIM_DURATION_MS = 180L
    }
}