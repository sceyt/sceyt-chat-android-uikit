package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.*
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytRecyclerReplayContainerBinding
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.presentation.common.setMessageDateAndStatusIcon
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytToReplayLineView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.getAttachmentUrl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.getShowBody
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.AvatarStyle
import com.sceyt.sceytchatuikit.shared.helpers.RecyclerItemOffsetDecoration
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil.getDateTimeString
import kotlin.math.min

abstract class BaseMsgViewHolder(view: View,
                                 private val messageListeners: MessageClickListenersImpl? = null,
                                 private val displayedListener: ((SceytMessage) -> Unit)? = null,
                                 private val senderNameBuilder: ((User) -> String)? = null)
    : RecyclerView.ViewHolder(view) {

    protected val context: Context by lazy { view.context }
    private var replayMessageContainerBinding: SceytRecyclerReplayContainerBinding? = null
    private var recyclerViewReactions: RecyclerView? = null
    protected lateinit var messageListItem: MessageListItem

    @CallSuper
    open fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        messageListItem = item

        if (messageListItem is MessageListItem.MessageItem) {
            val message = (messageListItem as MessageListItem.MessageItem).message
            if (message.incoming && message.deliveryStatus != DeliveryStatus.Read)
                displayedListener?.invoke(message)
        }
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
    }

    @CallSuper
    open fun onViewAttachedToWindow() {

    }

    private var reactionsAdapter: ReactionsAdapter? = null

    @SuppressLint("SetTextI18n")
    protected fun setReplayCount(tvReplayCount: TextView, toReplayLine: SceytToReplayLineView, item: MessageListItem.MessageItem) {
        val replayCount = item.message.replyCount
        if (replayCount > 0) {
            tvReplayCount.text = "$replayCount ${itemView.context.getString(R.string.sceyt_replays)}"
            tvReplayCount.isVisible = true
            toReplayLine.isVisible = true

            tvReplayCount.setOnClickListener { messageListeners?.onReplayCountClick(it, item) }
        } else {
            tvReplayCount.isVisible = false
            toReplayLine.isVisible = false
        }
    }

    protected fun setMessageStatusAndDateText(message: SceytMessage, messageDate: SceytDateStatusView) {
        val isEdited = message.state == MessageState.Edited
        val dateText = getDateTimeString(message.createdAt)
        message.setMessageDateAndStatusIcon(messageDate, dateText, isEdited)
    }

    protected fun setReplayedMessageContainer(message: SceytMessage, viewStub: ViewStub) {
        if (message.parent == null || message.replyInThread || message.parent?.id == 0L) {
            viewStub.isVisible = false
            return
        }
        if (viewStub.parent != null)
            SceytRecyclerReplayContainerBinding.bind(viewStub.inflate()).also {
                replayMessageContainerBinding = it
            }
        with(replayMessageContainerBinding ?: return) {
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
                } else imageAttachment.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.sceyt_ic_file_with_bg))
                true
            }
            root.isVisible = true
        }
    }

    protected fun setMessageUserAvatarAndName(avatarView: SceytAvatarView, tvName: TextView, message: SceytMessage) {
        if (!message.isGroup) return

        if (message.canShowAvatarAndName) {
            val user = message.from
            val displayName = getSenderName(user)
            avatarView.setNameAndImageUrl(displayName, user?.avatarURL, AvatarStyle.userDefaultAvatar)
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
}