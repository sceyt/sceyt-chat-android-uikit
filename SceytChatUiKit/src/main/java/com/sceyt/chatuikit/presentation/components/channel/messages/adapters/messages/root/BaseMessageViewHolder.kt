package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewStub
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.toSpannable
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytRecyclerReplyContainerBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.extractLinks
import com.sceyt.chatuikit.extensions.getStaticLayout
import com.sceyt.chatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.isRtl
import com.sceyt.chatuikit.extensions.isValidEmail
import com.sceyt.chatuikit.extensions.marginHorizontal
import com.sceyt.chatuikit.extensions.screenPortraitWidthPx
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionsAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.holders.ReactionViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.custom_views.ClickableTextView
import com.sceyt.chatuikit.presentation.custom_views.DecoratedTextView
import com.sceyt.chatuikit.presentation.custom_views.ToReplyLineView
import com.sceyt.chatuikit.presentation.extensions.setChatMessageDateAndStatusIcon
import com.sceyt.chatuikit.shared.helpers.RecyclerItemOffsetDecoration
import com.sceyt.chatuikit.shared.utils.ViewUtil
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import java.util.Date
import kotlin.math.min

abstract class BaseMessageViewHolder(
        private val view: View,
        private val itemStyle: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners? = null,
        private val displayedListener: ((MessageListItem) -> Unit)? = null
) : RecyclerView.ViewHolder(view) {
    protected val context: Context by lazy { view.context }
    protected val bubbleMaxWidth by lazy { calculateBubbleMaxWidth(context) }
    private var replyMessageContainerBinding: SceytRecyclerReplyContainerBinding? = null
    protected var recyclerViewReactions: RecyclerView? = null
    protected lateinit var messageListItem: MessageListItem
    val isMessageListItemInitialized get() = this::messageListItem.isInitialized
    private var highlightAnim: ValueAnimator? = null
    private val selectableAnimHelper by lazy { MessageSelectableAnimHelper(this) }

    @CallSuper
    open fun bind(item: MessageListItem, diff: MessageDiff) {
        messageListItem = item
        setMaxWidth()
        if (diff.selectionChanged || diff.statusChanged)
            selectableAnimHelper.doOnBind(selectMessageView, item)

        if (messageListItem.highligh)
            highlight()
    }

    @CallSuper
    open fun itemUpdated(item: MessageListItem) {
        if (item.getItemId() == messageListItem.getItemId())
            messageListItem = item
    }

    protected abstract val incoming: Boolean
    protected abstract val selectMessageView: View?
    open val enableReply = true
    protected val requireMessageItem get() = messageListItem as MessageListItem.MessageItem
    protected val requireMessage get() = (messageListItem as MessageListItem.MessageItem).message

    /** The Pair's param ViewGroup is layout bubble, the param Boolean when true, that mean the
     *  layout bubble with will resize depend reactions. */
    open val layoutBubbleConfig: Pair<ViewGroup, Boolean>? = null

    protected val layoutBubble get() = layoutBubbleConfig?.first

    protected open fun setMaxWidth() {
        (layoutBubble?.layoutParams as? ConstraintLayout.LayoutParams)?.matchConstraintMaxWidth = bubbleMaxWidth
    }

    fun rebind(diff: MessageDiff = MessageDiff.DEFAULT): Boolean {
        return if (::messageListItem.isInitialized) {
            bind(messageListItem, diff)
            true
        } else false
    }

    @CallSuper
    open fun onViewDetachedFromWindow() {
        reactionsAdapter = null
        if (messageListItem is MessageListItem.MessageItem) {
            highlightAnim?.cancel()
            view.setBackgroundColor(Color.TRANSPARENT)
            messageListItem.highligh = false
        }
    }

    @CallSuper
    open fun onViewAttachedToWindow() {
        if (::messageListItem.isInitialized) {
            displayedListener?.invoke(messageListItem)
            selectableAnimHelper.doOnAttach(selectMessageView, messageListItem)
        }
    }

    protected open var reactionsAdapter: ReactionsAdapter? = null

    protected open fun setMessageBody(
            messageBody: TextView,
            message: SceytMessage,
            checkLinks: Boolean = true,
            isLinkViewHolder: Boolean = false
    ) {
        val body = itemStyle.messageBodyFormatter.format(context, MessageBodyFormatterAttributes(
            message = message,
            mentionTextStyle = itemStyle.mentionTextStyle,
            mentionClickListener = {
                messageListeners?.onMentionClick(messageBody, it)
            }
        ))
        setTextAutoLinkMasks(messageBody, body, checkLinks, isLinkViewHolder)
        messageBody.setText(body, TextView.BufferType.SPANNABLE)
    }

    protected open fun setTextAutoLinkMasks(
            messageBody: TextView,
            bodyText: CharSequence,
            checkLinks: Boolean,
            isLinkViewHolder: Boolean
    ) {
        if (isLinkViewHolder || (checkLinks && bodyText.extractLinks().isNotEmpty())) {
            messageBody.autoLinkMask = Linkify.WEB_URLS
            return
        }
        if (bodyText.isValidEmail()) {
            messageBody.autoLinkMask = Linkify.EMAIL_ADDRESSES
            return
        }
        messageBody.autoLinkMask = 0
    }

    @SuppressLint("SetTextI18n")
    protected open fun setReplyCount(
            tvReplyCount: TextView,
            toReplyLine: ToReplyLineView,
            item: MessageListItem.MessageItem
    ) {
        val replyCount = item.message.replyCount
        if (replyCount > 0) {
            tvReplyCount.text = "$replyCount ${itemView.context.getString(R.string.sceyt_replies)}"
            tvReplyCount.isVisible = true
            toReplyLine.isVisible = true

            tvReplyCount.setOnClickListener { messageListeners?.onReplyCountClick(it, item) }
        } else {
            tvReplyCount.isVisible = false
            toReplyLine.isVisible = false
        }
    }

    protected open fun setMessageStatusAndDateText(message: SceytMessage, messageDate: DecoratedTextView) {
        val isEdited = message.state == MessageState.Edited
        val dateText = itemStyle.messageDateFormatter.format(context, Date(message.createdAt))
        message.setChatMessageDateAndStatusIcon(messageDate, itemStyle, dateText, isEdited)
    }

    // Invoke this method after invoking setOrUpdateReactions, to calculate the final width of the layout bubble
    protected open fun setReplyMessageContainer(message: SceytMessage, viewStub: ViewStub, calculateWith: Boolean = true) {
        val parent = message.parentMessage
        if (!message.isReplied || parent == null) {
            viewStub.isVisible = false
            return
        }
        if (viewStub.parent != null)
            SceytRecyclerReplyContainerBinding.bind(viewStub.inflate()).also {
                replyMessageContainerBinding = it
                it.viewReply.setBackgroundTint(if (message.incoming)
                    itemStyle.incomingReplyBackgroundColor else itemStyle.outgoingReplyBackgroundColor)
                it.applyStyle()
            }
        with(replyMessageContainerBinding ?: return) {
            val replyStyle = itemStyle.replyMessageStyle
            tvName.text = parent.user?.let {
                replyStyle.senderNameFormatter.format(context, it)
            } ?: ""
            if (parent.state == MessageState.Deleted) {
                val deletedText = itemStyle.deletedStateText.toSpannable()
                replyStyle.deletedMessageTextStyle.apply(context, deletedText)
                tvMessageBody.setText(deletedText, TextView.BufferType.SPANNABLE)
            } else {
                tvMessageBody.text = replyStyle.messageBodyFormatter.format(context, MessageBodyFormatterAttributes(
                    message = parent,
                    mentionTextStyle = replyStyle.mentionTextStyle
                ))
            }

            if (parent.attachments.isNullOrEmpty()) {
                imageAttachment.isVisible = false
                icFile.isVisible = false
            } else {
                val attachment = parent.attachments.getOrNull(0)
                val icon = attachment?.let { itemStyle.replyMessageStyle.attachmentIconProvider.provide(context, it) }
                when {
                    attachment?.type.isEqualsVideoOrImage() -> {
                        loadReplyMessageImageOrObserveToDownload(attachment, imageAttachment)
                        imageAttachment.isVisible = true
                        icFile.isVisible = false
                    }

                    attachment?.type == AttachmentTypeEnum.Link.value -> {
                        loadLinkImage(attachment, imageAttachment, icon)
                        imageAttachment.isVisible = true
                        icFile.isVisible = false
                    }

                    else -> {
                        imageAttachment.isVisible = false
                        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
                        icFile.setImageDrawable(icon)
                        icFile.isVisible = true
                    }
                }
            }
            with(root) {
                if (calculateWith) {
                    (layoutParams as? ConstraintLayout.LayoutParams)?.let { layoutParams ->
                        layoutParams.matchConstraintMaxWidth = bubbleMaxWidth - marginHorizontal
                        // Calculate the width of the layout bubble before measuring replyMessageContainer
                        layoutBubble?.measure(View.MeasureSpec.UNSPECIFIED, 0)
                        // Set the width LayoutParams.WRAP_CONTENT to layoutParas to measure the real width of replyMessageContainer
                        layoutParams.width = LayoutParams.WRAP_CONTENT
                        measure(View.MeasureSpec.UNSPECIFIED, 0)
                        val bubbleMeasuredWidth = min(bubbleMaxWidth, layoutBubble?.measuredWidth
                                ?: 0)
                        val minBoundOfWidth = bubbleMeasuredWidth - marginHorizontal
                        // If the width of replyMessageContainer is bigger than the width of layout bubble,
                        // set the width of layout bubble, else set the default width of replyMessageContainer
                        if (measuredWidth >= minBoundOfWidth)
                            layoutParams.matchConstraintMinWidth = minBoundOfWidth
                        else {
                            layoutParams.matchConstraintMinWidth = 0
                            layoutParams.width = 0
                        }
                    }
                }
                isVisible = true

                setOnClickListener {
                    (messageListItem as? MessageListItem.MessageItem)?.let { item ->
                        messageListeners?.onReplyMessageContainerClick(it, item)
                    }
                }

                setOnLongClickListener {
                    (messageListItem as? MessageListItem.MessageItem)?.let { item ->
                        messageListeners?.onMessageLongClick(it, item)
                    }
                    return@setOnLongClickListener true
                }
            }
        }
    }

    protected fun SceytRecyclerReplyContainerBinding.applyStyle() {
        itemStyle.replyMessageStyle.titleTextStyle.apply(tvName)
        itemStyle.replyMessageStyle.subtitleTextStyle.apply(tvMessageBody)
        view.setBackgroundColor(itemStyle.replyMessageStyle.borderColor)
    }

    protected open fun loadReplyMessageImageOrObserveToDownload(
            attachment: SceytAttachment?,
            imageAttachment: ImageView
    ) {
        attachment ?: return
        val path = attachment.filePath
        val placeHolder = getThumbFromMetadata(attachment.metadata)?.toDrawable(context.resources)?.mutate()

        fun loadImage(filePath: String?) {
            Glide.with(itemView.context)
                .load(filePath)
                .placeholder(placeHolder)
                .error(placeHolder)
                .override(imageAttachment.width, imageAttachment.height)
                .into(imageAttachment)
        }

        if (path.isNullOrBlank()) {
            imageAttachment.setImageDrawable(placeHolder)
        } else loadImage(path)
    }

    protected open fun loadLinkImage(
            attachment: SceytAttachment?,
            imageAttachment: ImageView,
            icon: Drawable?
    ) {
        attachment ?: return
        val url = attachment.linkPreviewDetails?.imageUrl
        if (!url.isNullOrBlank()) {
            Glide.with(itemView.context)
                .load(url)
                .placeholder(icon)
                .error(icon)
                .override(imageAttachment.width, imageAttachment.height)
                .into(imageAttachment)
        } else imageAttachment.setImageDrawable(icon)
    }

    protected open fun setMessageUserAvatarAndName(avatarView: AvatarView, tvName: TextView, message: SceytMessage) {
        if (!message.isGroup || message.disabledShowAvatarAndName) return

        if (message.shouldShowAvatarAndName) {
            val user = message.user
            val displayName = user?.let { itemStyle.senderNameFormatter.format(context, it) } ?: ""
            user?.let {
                tvName.setTextColor(itemStyle.senderNameColorProvider.provide(context, it))
                itemStyle.userAvatarRenderer.render(context, user, itemStyle.avatarStyle, avatarView)
            }
            tvName.text = displayName
            tvName.isVisible = true
            avatarView.isVisible = true
        } else {
            avatarView.isInvisible = true
            tvName.isVisible = false
        }
    }

    protected open fun setOrUpdateReactions(
            item: MessageListItem.MessageItem,
            rvReactionsViewStub: ViewStub,
            viewPool: RecyclerView.RecycledViewPool
    ) {
        val reactions: List<ReactionItem.Reaction>? = item.message.messageReactions?.take(19)
        val resizeWithDependReactions = layoutBubbleConfig?.second ?: false
        val layoutDetails = if (resizeWithDependReactions) layoutBubble else null

        if (reactions.isNullOrEmpty()) {
            reactionsAdapter = null
            rvReactionsViewStub.isVisible = false
            layoutDetails?.layoutParams?.width = LayoutParams.WRAP_CONTENT
            return
        }

        reactionsAdapter = ReactionsAdapter(
            message = item.message,
            viewHolderFactory = ReactionViewHolderFactory(itemView.context, messageListeners)).also {
            it.submitList(reactions)
        }

        if (rvReactionsViewStub.parent != null)
            rvReactionsViewStub.inflate().also {
                recyclerViewReactions = it as RecyclerView
                it.setBackgroundTint(itemStyle.reactionsContainerBackgroundColor)
            }

        with(recyclerViewReactions ?: return) {
            setRecycledViewPool(viewPool)
            itemAnimator = DefaultItemAnimator().also {
                it.moveDuration = 0
                it.removeDuration = 0
                it.addDuration = 200
            }

            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                alignItems = AlignItems.FLEX_START
                justifyContent = JustifyContent.FLEX_START
            }
            adapter = reactionsAdapter
        }
        recyclerViewReactions?.isVisible = true
        initWidthsDependReactions(recyclerViewReactions, layoutDetails)
    }

    protected open fun initWidthsDependReactions(rvReactions: ViewGroup?, layoutDetails: ViewGroup?) {
        if (layoutDetails == null || rvReactions == null) return

        rvReactions.measure(View.MeasureSpec.UNSPECIFIED, 0)
        layoutDetails.measure(View.MeasureSpec.UNSPECIFIED, 0)
        val margin = dpToPx(8f)

        when {
            rvReactions.measuredWidth + margin > layoutDetails.measuredWidth -> {
                val newWidth = min((rvReactions.measuredWidth + margin), bubbleMaxWidth)
                layoutDetails.layoutParams.width = newWidth
                rvReactions.layoutParams.width = newWidth - margin
            }

            rvReactions.measuredWidth < layoutDetails.measuredWidth -> {
                rvReactions.layoutParams.width = LayoutParams.WRAP_CONTENT
                layoutDetails.layoutParams.width = LayoutParams.WRAP_CONTENT
            }
        }
    }

    protected open fun setMessageDateDependAttachments(messageDate: DecoratedTextView, attachments: List<FileListItem>?) {
        messageDate.apply {
            val lastAttachment = attachments?.lastOrNull()
            val needHighlight = lastAttachment?.type == AttachmentTypeEnum.Image || lastAttachment?.type == AttachmentTypeEnum.Video
            setHighlighted(needHighlight)
            val marginEndBottom = if (needHighlight) Pair(25, 25) else {
                //Set the value which is set in xml
                Pair(dpToPx(5f), dpToPx(5f))
            }
            (layoutParams as ViewGroup.MarginLayoutParams).apply {
                val isRtl = context.isRtl()
                val left = if (isRtl) marginEndBottom.first else 0
                val right = if (isRtl) 0 else marginEndBottom.first
                setMargins(left, marginTop, right, marginEndBottom.second)
                marginEnd = marginEndBottom.first
            }
        }
    }

    protected open fun initFilesRecyclerView(message: SceytMessage, rvFiles: RecyclerView) {
        with(rvFiles) {
            if (itemDecorationCount == 0) {
                val offset = ViewUtil.dpToPx(2f)
                addItemDecoration(RecyclerItemOffsetDecoration(left = offset, top = offset, right = offset))
            }

            message.attachments?.firstOrNull()?.let {
                if (!it.type.isEqualsVideoOrImage()) {
                    setPadding(ViewUtil.dpToPx(8f))
                } else {
                    if (message.isForwarded || message.isReplied || message.shouldShowAvatarAndName || message.body.isNotNullOrBlank())
                        setPadding(0, ViewUtil.dpToPx(4f), 0, 0)
                    else setPadding(0)
                }
            }
        }
    }

    protected open fun setBodyTextPosition(bodyTextView: TextView, dateView: View, parentLayout: ConstraintLayout) {
        bodyTextView.minWidth = 0
        bodyTextView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        val currentViewWidth = bodyTextView.measuredWidth
        dateView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        val nextViewWidth = dateView.measuredWidth
        val px12 = dpToPx(12f) // bodyTextView end margins
        val px8 = dpToPx(8f) // bodyTextView bottom margins
        val px5 = dpToPx(5f) // bodyTextView top margins
        val body = bodyTextView.text.toString()
        val constraintSet = ConstraintSet()
        constraintSet.clone(parentLayout)

        constraintSet.clear(bodyTextView.id, ConstraintSet.END)
        constraintSet.clear(bodyTextView.id, ConstraintSet.BOTTOM)

        // Calculate maxWidth when dateView and bodyTextView are in the same line
        val maxWidthWithDate = bubbleMaxWidth - bodyTextView.marginHorizontal

        // If messageBody + dateView + px12 (margins) > maxWidthWithDate, then set messageBody to endOf parentLayout,
        // else set messageBody to endOf dateView
        if (currentViewWidth + nextViewWidth + px12 > maxWidthWithDate) {
            constraintSet.connect(bodyTextView.id, ConstraintSet.END, parentLayout.id, ConstraintSet.END, px12)
            // Calculate lines like bodyTextView end connected to parentLayout end
            val maxWidthWithoutDate = bubbleMaxWidth - (bodyTextView.marginStart + px12)
            bodyTextView.paint.getStaticLayout(body, bodyTextView.includeFontPadding, maxWidthWithoutDate).apply {
                if (lineCount > 1) {
                    val bodyIsRtl = body.isRtl()
                    val appIsRtl = context.isRtl()
                    val reqMinWidth = getLineMax(lineCount - 1) + nextViewWidth + px12
                    if (reqMinWidth < maxWidthWithDate && ((!bodyIsRtl && !appIsRtl) || (bodyIsRtl && appIsRtl))) {
                        bodyTextView.minWidth = reqMinWidth.toInt()
                        constraintSet.connect(bodyTextView.id, ConstraintSet.BOTTOM, parentLayout.id, ConstraintSet.BOTTOM, px8)
                    } else
                        constraintSet.connect(bodyTextView.id, ConstraintSet.BOTTOM, dateView.id, ConstraintSet.TOP, px5)
                } else
                    constraintSet.connect(bodyTextView.id, ConstraintSet.BOTTOM, dateView.id, ConstraintSet.TOP, px5)
            }
        } else {
            constraintSet.connect(bodyTextView.id, ConstraintSet.END, dateView.id, ConstraintSet.START, px12)
            constraintSet.connect(bodyTextView.id, ConstraintSet.BOTTOM, parentLayout.id, ConstraintSet.BOTTOM, px8)
        }
        constraintSet.applyTo(parentLayout)
    }

    protected open fun getReactionSpanCount(reactionsSize: Int, incoming: Boolean): Int {
        if (incoming) return 5
        return min(5, reactionsSize)
    }

    protected open fun calculateBubbleMaxWidth(context: Context): Int {
        return (context.screenPortraitWidthPx() * 0.75f).toInt()
    }

    fun getMessageItem(): MessageListItem? {
        return if (::messageListItem.isInitialized) messageListItem else null
    }

    open fun setSelectableState() {
        selectableAnimHelper.setSelectableState(selectMessageView, messageListItem)
    }

    open fun cancelSelectableState(onAnimEnd: (() -> Unit)? = null) {
        selectableAnimHelper.cancelSelectableState(selectMessageView, onAnimEnd)
    }

    open fun highlight() {
        highlightAnim?.cancel()
        val colorFrom = itemStyle.highlightedMessageColor
        view.setBackgroundColor(colorFrom)
        val colorTo: Int = Color.TRANSPARENT
        highlightAnim = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        highlightAnim?.duration = 2000
        highlightAnim?.addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
        highlightAnim?.start()
        messageListItem.highligh = false
    }

    protected open fun applyCommonStyle(
            layoutDetails: View,
            tvForwarded: AppCompatTextView,
            messageBody: ClickableTextView,
            tvThreadReplyCount: AppCompatTextView,
            toReplyLine: ToReplyLineView,
            tvSenderName: AppCompatTextView? = null,
            avatarView: AvatarView? = null,
    ) {
        layoutDetails.setBackgroundTint(if (incoming)
            itemStyle.incomingBubbleColor else itemStyle.outgoingBubbleColor)

        applyForwardedStyle(tvForwarded)
        messageBody.applyStyle(itemStyle)
        itemStyle.threadReplyCountTextStyle.apply(tvThreadReplyCount)
        tvSenderName?.let {
            itemStyle.senderNameTextStyle.apply(it)
        }
        avatarView?.let {
            itemStyle.avatarStyle.apply(it)
        }
        (selectMessageView as? CheckBox)?.let {
            itemStyle.selectionCheckboxStyle.apply(it)
        }
    }

    protected open fun applyForwardedStyle(tvForwarded: AppCompatTextView) {
        itemStyle.forwardTitleTextStyle.apply(tvForwarded)
        tvForwarded.setDrawableStart(itemStyle.forwardedIcon)
        tvForwarded.text = itemStyle.forwardedText
    }
}