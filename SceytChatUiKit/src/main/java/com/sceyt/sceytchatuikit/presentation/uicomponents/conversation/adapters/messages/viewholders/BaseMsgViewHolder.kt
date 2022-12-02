package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.*
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytRecyclerReplyContainerBinding
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.presentation.common.getAttachmentUrl
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
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle
import com.sceyt.sceytchatuikit.shared.helpers.RecyclerItemOffsetDecoration
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil.getDateTimeString
import kotlin.math.min


abstract class BaseMsgViewHolder(private val view: View,
                                 private val messageListeners: MessageClickListenersImpl? = null,
                                 private val displayedListener: ((MessageListItem) -> Unit)? = null,
                                 private val senderNameBuilder: ((User) -> String)? = null)
    : RecyclerView.ViewHolder(view) {

    protected val context: Context by lazy { view.context }
    private var replyMessageContainerBinding: SceytRecyclerReplyContainerBinding? = null
    private var recyclerViewReactions: RecyclerView? = null
    protected lateinit var messageListItem: MessageListItem
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
        highlightAnim?.cancel()
        view.setBackgroundColor(Color.TRANSPARENT)
        messageListItem.highlighted = false
    }

    @CallSuper
    open fun onViewAttachedToWindow() {
        if (::messageListItem.isInitialized)
            displayedListener?.invoke(messageListItem)
    }

    private var reactionsAdapter: ReactionsAdapter? = null

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
        if (message.parent == null || message.replyInThread || message.parent?.id == 0L) {
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
                val url = parent?.getAttachmentUrl(itemView.context)
                if (!url.isNullOrBlank()) {
                    Glide.with(itemView.context)
                        .load(url)
                        .override(imageAttachment.width, imageAttachment.height)
                        .into(imageAttachment)
                } else
                    imageAttachment.setImageResource(MessagesStyle.fileAttachmentIcon)
                true
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
            avatarView.setNameAndImageUrl(displayName, user?.avatarURL, UserStyle.userDefaultAvatar)
            tvName.text = displayName
            tvName.isVisible = true
            avatarView.isVisible = true
        } else {
            avatarView.isInvisible = true
            tvName.isVisible = false
        }
    }

    protected fun setOrUpdateReactions(item: MessageListItem.MessageItem, rvReactionsViewStub: ViewStub,
                                       viewPool: RecyclerView.RecycledViewPool) {
        val reactions: List<ReactionItem>? = item.message.messageReactions

        if (reactions.isNullOrEmpty()) {
            rvReactionsViewStub.isVisible = false
            return
        }

        if (reactionsAdapter == null) {
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
                    it.moveDuration = 100
                    it.removeDuration = 100
                }
                if (itemDecorationCount == 0)
                    addItemDecoration(RecyclerItemOffsetDecoration(0, 4, 8, 4))

                layoutManager = FlexboxLayoutManager(context).apply {
                    flexDirection = FlexDirection.ROW
                    flexWrap = FlexWrap.WRAP
                    alignItems = AlignItems.FLEX_START
                    justifyContentForReactions(item.message.incoming, reactions.size.plus(1))
                }
                adapter = reactionsAdapter
            }
        } else {
            (recyclerViewReactions?.layoutManager as? FlexboxLayoutManager)?.justifyContentForReactions(
                item.message.incoming, reactions.size.plus(1)
            )
            reactionsAdapter?.submitList(reactions)
        }
        recyclerViewReactions?.isVisible = true
    }

    private fun FlexboxLayoutManager.justifyContentForReactions(incoming: Boolean, reactionsSize: Int) {
        justifyContent = if (incoming) {
            JustifyContent.FLEX_START
        } else if (reactionsSize > 5) JustifyContent.FLEX_START else JustifyContent.FLEX_END
    }

    protected fun setMessageDateDependAttachments(messageDate: SceytDateStatusView, attachments: List<FileListItem>?) {
        messageDate.apply {
            val lastAttachment = attachments?.lastOrNull()
            val needHighlight = lastAttachment is FileListItem.Image || lastAttachment is FileListItem.Video
            setHighlighted(needHighlight)
            val marginEndBottom = if (needHighlight) Pair(25, 25) else {
                //Set the value which is set in xml
                Pair(dpToPx(5f), dpToPx(2f))
            }
            (layoutParams as ViewGroup.MarginLayoutParams).apply {
                setMargins(0, marginTop, marginEndBottom.first, marginEndBottom.second)
                marginEnd = marginEndBottom.first
            }
        }
    }

    private fun getReactionSpanCount(reactionsSize: Int, incoming: Boolean): Int {
        if (incoming) return 5
        return min(5, reactionsSize)
    }

    private fun getSenderName(user: User?): String {
        user ?: return ""
        return senderNameBuilder?.invoke(user) ?: user.getPresentableName()
    }

    private fun highlight() {
        val colorFrom = context.getCompatColor(SceytKitConfig.sceytColorAccent)
        view.setBackgroundColor(colorFrom)
        val colorFro = ColorUtils.setAlphaComponent(colorFrom, (0.8 * 255).toInt())
        val colorTo: Int = Color.TRANSPARENT
        highlightAnim = ValueAnimator.ofObject(ArgbEvaluator(), colorFro, colorTo)
        highlightAnim?.duration = 2000
        highlightAnim?.addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
        highlightAnim?.start()
        highlightAnim?.doOnEnd { messageListItem.highlighted = false }
    }
}