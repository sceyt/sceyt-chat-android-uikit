package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.*
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserActivityStatus
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytRecyclerReplyContainerBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.persistence.constants.SceytConstants
import com.sceyt.sceytchatuikit.persistence.mappers.getInfoFromMetadataByKey
import com.sceyt.sceytchatuikit.presentation.common.getShowBody
import com.sceyt.sceytchatuikit.presentation.common.setConversationMessageDateAndStatusIcon
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytToReplyLineView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle
import com.sceyt.sceytchatuikit.shared.helpers.RecyclerItemOffsetDecoration
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil.getDateTimeString
import com.sceyt.sceytchatuikit.shared.utils.ViewUtil
import kotlin.math.min


abstract class BaseMsgViewHolder(private val view: View,
                                 private val messageListeners: MessageClickListeners.ClickListeners? = null,
                                 private val displayedListener: ((MessageListItem) -> Unit)? = null,
                                 private val senderNameBuilder: ((User) -> String)? = null)
    : RecyclerView.ViewHolder(view) {

    protected val context: Context by lazy { view.context }
    protected val bubbleMaxWidth by lazy { getBubbleMaxWidth(context) }
    private var replyMessageContainerBinding: SceytRecyclerReplyContainerBinding? = null
    protected var recyclerViewReactions: RecyclerView? = null
    protected lateinit var messageListItem: MessageListItem
    val isMessageListItemInitialized get() = this::messageListItem.isInitialized
    private var highlightAnim: ValueAnimator? = null

    @CallSuper
    open fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        messageListItem = item
        setMaxWidth()
        if (messageListItem.highlighted)
            highlight()
    }

    /** The Pair's param ViewGroup is layout bubble, the param Boolean when true, that mean the
     *  layout bubble with will resize depend reactions. */
    open val layoutBubbleConfig: Pair<ViewGroup, Boolean>? = null

    protected val layoutBubble get() = layoutBubbleConfig?.first

    protected open fun setMaxWidth() {
        (layoutBubble?.layoutParams as? ConstraintLayout.LayoutParams)?.matchConstraintMaxWidth = bubbleMaxWidth
    }

    fun rebind(diff: MessageItemPayloadDiff = MessageItemPayloadDiff.DEFAULT): Boolean {
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
            messageListItem.highlighted = false
        }
    }

    @CallSuper
    open fun onViewAttachedToWindow() {
        if (::messageListItem.isInitialized)
            displayedListener?.invoke(messageListItem)
    }

    private var reactionsAdapter: ReactionsAdapter? = null

    protected fun setMessageBody(messageBody: TextView, message: SceytMessage, isLink: Boolean = false) {
        val bodyText = message.body.trim()
        messageBody.autoLinkMask = 0
        if (!MentionUserHelper.containsMentionsUsers(message)) {
            setTextAutoLinkMasks(messageBody, bodyText, isLink)
            messageBody.text = bodyText
        } else {
            messageBody.text = MentionUserHelper.buildWithMentionedUsers(context, bodyText,
                message.metadata, message.mentionedUsers, enableClick = true)
        }
    }

    private fun setTextAutoLinkMasks(messageBody: TextView, bodyText: String, isLink: Boolean) {
        if (isLink) {
            messageBody.autoLinkMask = Linkify.WEB_URLS
            return
        }
        try {
            if (bodyText.isValidEmail()) {
                messageBody.autoLinkMask = Linkify.EMAIL_ADDRESSES
                return
            }
            val phone = PhoneNumberUtil.getInstance().parse(bodyText, "")
            val isValid = PhoneNumberUtil.getInstance().isValidNumber(phone)
            if (isValid) {
                messageBody.autoLinkMask = Linkify.PHONE_NUMBERS
            }
        } catch (_: Exception) {
        }
    }

    @SuppressLint("SetTextI18n")
    protected fun setReplyCount(tvReplyCount: TextView, toReplyLine: SceytToReplyLineView, item: MessageListItem.MessageItem) {
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

    protected fun setMessageStatusAndDateText(message: SceytMessage, messageDate: SceytDateStatusView) {
        val isEdited = message.state == MessageState.Edited
        val dateText = getDateTimeString(message.createdAt)
        message.setConversationMessageDateAndStatusIcon(messageDate, dateText, isEdited)
    }

    protected fun setReplyMessageContainer(message: SceytMessage, viewStub: ViewStub) {
        if (!message.isReplied) {
            viewStub.isVisible = false
            return
        }
        if (viewStub.parent != null)
            SceytRecyclerReplyContainerBinding.bind(viewStub.inflate()).also {
                replyMessageContainerBinding = it
                it.tvName.setTextColor(context.getCompatColor(MessagesStyle.senderNameTextColor))
                it.view.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(MessagesStyle.replyMessageLineColor))
            }
        with(replyMessageContainerBinding ?: return) {
            val parent = message.parent
            tvName.text = getSenderName(parent?.from)
            if (parent?.state == MessageState.Deleted) {
                tvMessageBody.setTypeface(tvMessageBody.typeface, Typeface.ITALIC)
                tvMessageBody.setTextColor(itemView.context.getCompatColor(R.color.sceyt_color_gray_400))
            } else {
                tvMessageBody.setTypeface(tvMessageBody.typeface, Typeface.NORMAL)
                tvMessageBody.setTextColor(itemView.context.getCompatColor(R.color.sceyt_color_black_themed))
            }

            tvMessageBody.text = parent?.getShowBody(itemView.context)
            imageAttachment.isVisible = if (parent?.attachments.isNullOrEmpty()) {
                false
            } else {
                val attachment = parent?.attachments?.getOrNull(0)
                icMsgBodyStartIcon.isVisible = attachment?.type == AttachmentTypeEnum.Voice.value()
                when {
                    attachment?.type.isEqualsVideoOrImage() -> {
                        val path = attachment?.filePath
                        val placeHolder = attachment?.metadata.getInfoFromMetadataByKey(SceytConstants.Thumb).toByteArraySafety()
                            ?.decodeByteArrayToBitmap()?.toDrawable(context.resources)?.mutate()
                        Glide.with(itemView.context)
                            .load(path)
                            .placeholder(placeHolder)
                            .error(placeHolder)
                            .override(imageAttachment.width, imageAttachment.height)
                            .into(imageAttachment)
                        true
                    }

                    attachment?.type == AttachmentTypeEnum.Voice.value() -> {
                        icMsgBodyStartIcon.setImageDrawable(context.getCompatDrawable(R.drawable.sceyt_ic_voice)?.apply {
                            if (message.incoming)
                                setTint("#818C99".toColorInt())
                            else setTint(context.getCompatColor(SceytKitConfig.sceytColorAccent))
                        })
                        false
                    }

                    attachment?.type == AttachmentTypeEnum.Link.value() -> false
                    else -> {
                        imageAttachment.setImageResource(MessagesStyle.fileAttachmentIcon)
                        true
                    }
                }
            }
            root.isVisible = true

            root.setOnClickListener {
                (messageListItem as? MessageListItem.MessageItem)?.let { item ->
                    messageListeners?.onReplyMessageContainerClick(it, item)
                }
            }

            root.post {
                val bubbleWidth = layoutBubble?.width ?: return@post
                if (root.width < bubbleWidth) {
                    root.layoutParams.width = bubbleWidth
                    root.requestLayout()
                }
            }
        }
    }

    protected fun setMessageUserAvatarAndName(avatarView: SceytAvatarView, tvName: TextView, message: SceytMessage) {
        if (!message.isGroup) return

        if (message.canShowAvatarAndName) {
            val user = message.from
            val displayName = getSenderName(user)
            if (isDeletedUser(user)) {
                avatarView.setImageUrl(null, UserStyle.deletedUserAvatar)
                tvName.setTextColor(context.getCompatColor(R.color.sceyt_color_red))
            } else {
                avatarView.setNameAndImageUrl(displayName, user?.avatarURL, UserStyle.userDefaultAvatar)
                tvName.setTextColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
            }
            tvName.text = displayName
            tvName.isVisible = true
            avatarView.isVisible = true
        } else {
            avatarView.isInvisible = true
            tvName.isVisible = false
        }
    }

    /** @param layoutDetails when not null, that mean layout details will resize with reactions. */
    protected fun setOrUpdateReactions(item: MessageListItem.MessageItem, rvReactionsViewStub: ViewStub,
                                       viewPool: RecyclerView.RecycledViewPool, layoutDetails: ViewGroup? = null) {
        val reactions: List<ReactionItem.Reaction>? = item.message.messageReactions?.take(19)

        if (reactions.isNullOrEmpty()) {
            reactionsAdapter = null
            rvReactionsViewStub.isVisible = false
            layoutDetails?.layoutParams?.width = ViewGroup.LayoutParams.WRAP_CONTENT
            return
        }

        reactionsAdapter = ReactionsAdapter(
            ReactionViewHolderFactory(itemView.context, messageListeners)).also {
            it.submitList(reactions)
        }

        if (rvReactionsViewStub.parent != null)
            rvReactionsViewStub.inflate().also {
                recyclerViewReactions = it as RecyclerView
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

    protected fun initWidthsDependReactions(rvReactions: ViewGroup?, layoutDetails: ViewGroup?) {
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
                rvReactions.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                layoutDetails.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    protected fun setMessageDateDependAttachments(messageDate: SceytDateStatusView, attachments: List<FileListItem>?) {
        messageDate.apply {
            val lastAttachment = attachments?.lastOrNull()
            val needHighlight = lastAttachment is FileListItem.Image || lastAttachment is FileListItem.Video
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

    protected fun initFilesRecyclerView(message: SceytMessage, rvFiles: RecyclerView) {
        with(rvFiles) {
            if (itemDecorationCount == 0) {
                val offset = ViewUtil.dpToPx(2f)
                addItemDecoration(RecyclerItemOffsetDecoration(left = offset, top = offset, right = offset))
            }

            message.attachments?.firstOrNull()?.let {
                if (it.type == AttachmentTypeEnum.File.value()) {
                    setPadding(ViewUtil.dpToPx(8f))
                } else {
                    if (message.isForwarded || message.isReplied || message.canShowAvatarAndName || message.body.isNotNullOrBlank())
                        setPadding(0, ViewUtil.dpToPx(4f), 0, 0)
                    else setPadding(0)
                }
            }
        }
    }

    protected fun setBodyTextPosition(currentView: TextView, nextView: View, parentLayout: ConstraintLayout) {
        val maxWidth = getBodyMaxAcceptableWidth(currentView)
        currentView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val currentViewWidth = currentView.measuredWidth
        nextView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val nextViewWidth = nextView.measuredWidth

        val body = currentView.text.toString()
        val constraintLayout: ConstraintLayout = parentLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintSet.clear(currentView.id, ConstraintSet.END)
        constraintSet.clear(currentView.id, ConstraintSet.BOTTOM)

        if (currentViewWidth + nextViewWidth > maxWidth) {
            constraintSet.connect(currentView.id, ConstraintSet.END, parentLayout.id, ConstraintSet.END, dpToPx(12f))

            currentView.paint.getStaticLayout(body, currentView.includeFontPadding, maxWidth).apply {
                if (lineCount > 1) {
                    val bodyIsRtl = body.isRtl()
                    val appIsRtl = context.isRtl()
                    if (getLineMax(lineCount - 1) + nextViewWidth < maxWidth && ((!bodyIsRtl && !appIsRtl) || (bodyIsRtl && appIsRtl))) {
                        constraintSet.connect(currentView.id, ConstraintSet.BOTTOM, parentLayout.id, ConstraintSet.BOTTOM, dpToPx(8f))
                    } else
                        constraintSet.connect(currentView.id, ConstraintSet.BOTTOM, nextView.id, ConstraintSet.TOP, dpToPx(5f))
                } else
                    constraintSet.connect(currentView.id, ConstraintSet.BOTTOM, nextView.id, ConstraintSet.TOP, dpToPx(5f))
            }
        } else {
            constraintSet.connect(currentView.id, ConstraintSet.END, nextView.id, ConstraintSet.START, dpToPx(12f))
            constraintSet.connect(currentView.id, ConstraintSet.BOTTOM, parentLayout.id, ConstraintSet.BOTTOM, dpToPx(8f))
        }
        constraintSet.applyTo(constraintLayout)
    }

    private fun getBodyMaxAcceptableWidth(textView: TextView): Int {
        return bubbleMaxWidth - (textView.marginStart + textView.marginEnd)
    }

    private fun getReactionSpanCount(reactionsSize: Int, incoming: Boolean): Int {
        if (incoming) return 5
        return min(5, reactionsSize)
    }

    private fun getSenderName(user: User?): String {
        user ?: return ""
        return senderNameBuilder?.invoke(user) ?: user.getPresentableNameCheckDeleted(context)
    }

    private fun isDeletedUser(user: User?): Boolean {
        return user?.activityState == UserActivityStatus.Deleted
    }

    private fun getBubbleMaxWidth(context: Context): Int {
        return (context.screenPortraitWidthPx() * 0.77f).toInt()
    }

    fun getMessageItem(): MessageListItem? {
        return if (::messageListItem.isInitialized) messageListItem else null
    }

    fun highlight() {
        highlightAnim?.cancel()
        val colorFrom = context.getCompatColor(SceytKitConfig.sceytColorAccent)
        view.setBackgroundColor(colorFrom)
        val colorFro = ColorUtils.setAlphaComponent(colorFrom, (0.7 * 255).toInt())
        val colorTo: Int = Color.TRANSPARENT
        highlightAnim = ValueAnimator.ofObject(ArgbEvaluator(), colorFro, colorTo)
        highlightAnim?.duration = 2000
        highlightAnim?.addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
        highlightAnim?.start()
        highlightAnim?.doOnEnd { messageListItem.highlighted = false }
    }
}