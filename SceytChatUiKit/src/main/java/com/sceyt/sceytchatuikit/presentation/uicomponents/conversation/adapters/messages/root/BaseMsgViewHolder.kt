package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.RelativeLayout
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
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.*
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
    private var replyMessageContainerBinding: SceytRecyclerReplyContainerBinding? = null
    protected var recyclerViewReactions: RecyclerView? = null
    protected var bodyMaxWidth = context.resources.getDimensionPixelSize(com.sceyt.sceytchatuikit.R.dimen.bodyMaxWidth)
    protected lateinit var messageListItem: MessageListItem
    val isMessageListItemInitialized get() = this::messageListItem.isInitialized
    private var highlightAnim: ValueAnimator? = null

    @CallSuper
    open fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        messageListItem = item
        if (messageListItem.highlighted)
            highlight()
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

    protected fun setMessageBody(messageBody: TextView, message: SceytMessage) {
        if (!MentionUserHelper.containsMentionsUsers(message))
            messageBody.text = message.body.trim()
        else {
            messageBody.movementMethod = LinkMovementMethod.getInstance()
            messageBody.text = MentionUserHelper.buildWithMentionedUsers(context, message.body.trim(),
                message.metadata, message.mentionedUsers, enableClick = true)
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
                                       viewPool: RecyclerView.RecycledViewPool, layoutDetails: ViewGroup? = null): RecyclerView? {
        val reactions: List<ReactionItem.Reaction>? = item.message.messageReactions?.take(19)

        if (reactions.isNullOrEmpty()) {
            reactionsAdapter = null
            rvReactionsViewStub.isVisible = false
            layoutDetails?.layoutParams?.width = ViewGroup.LayoutParams.WRAP_CONTENT
            return null
        }


        //if (reactionsAdapter == null) {
        reactionsAdapter = ReactionsAdapter(
            ReactionViewHolderFactory(itemView.context, messageListeners)).also {
            it.submitList(reactions)
        }

        if (rvReactionsViewStub.parent != null)
            rvReactionsViewStub.inflate().also {
                recyclerViewReactions = it as RecyclerView
            }

        with(recyclerViewReactions ?: return null) {
            setRecycledViewPool(viewPool)
            itemAnimator = DefaultItemAnimator().also {
                it.moveDuration = 0
                it.removeDuration = 0
                it.addDuration = 200
            }
            /* if (itemDecorationCount == 0)
                 addItemDecoration(RecyclerItemOffsetDecoration(0, 4, 8, 4))*/

            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                alignItems = AlignItems.FLEX_START
                justifyContent = JustifyContent.FLEX_START
            }

            adapter = reactionsAdapter

        }
        /*  } else {
              (recyclerViewReactions?.layoutManager as? FlexboxLayoutManager)?.justifyContentForReactions(
                  item.message.incoming, reactions.size.plus(1)
              )
              reactionsAdapter?.submitList(reactions)
          }*/
        recyclerViewReactions?.isVisible = true
        initWidthsDependReactions(recyclerViewReactions, layoutDetails, item.message)

        return recyclerViewReactions
    }

    protected fun initWidthsDependReactions(rvReactions: ViewGroup?, layoutDetails: ViewGroup?, message: SceytMessage) {
        if (layoutDetails == null || rvReactions == null) return

        rvReactions.measure(View.MeasureSpec.UNSPECIFIED, 0)
        layoutDetails.measure(View.MeasureSpec.UNSPECIFIED, 0)

        /*when {
            rvReactions.measuredWidth > layoutDetails.measuredWidth -> {
                layoutDetails.layoutParams.width = min((rvReactions.measuredWidth + dpToPx(8f)), bodyMaxWidth)
                if (message.incoming.not())
                    (rvReactions.layoutParams as RelativeLayout.LayoutParams).apply {
                        if (message.incoming) {
                            addRule(RelativeLayout.ALIGN_START, R.id.layoutDetails)
                            removeRule(RelativeLayout.ALIGN_PARENT_END)
                        } else {
                            removeRule(RelativeLayout.ALIGN_START)
                            addRule(RelativeLayout.ALIGN_PARENT_END)
                            addRule(RelativeLayout.ALIGN_START, R.id.layoutDetails)
                        }
                        marginEnd = dpToPx(8f)
                    }
            }
            rvReactions.measuredWidth < layoutDetails.measuredWidth -> {
                layoutDetails.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                (rvReactions.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.ALIGN_START, R.id.layoutDetails)
                    removeRule(RelativeLayout.ALIGN_PARENT_END)
                    marginEnd = 0
                }
            }
        }*/
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

    protected fun setBodyTextPosition(currentView: TextView, nextView: View, parentLayout: ConstraintLayout, maxWidth: Int) {
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