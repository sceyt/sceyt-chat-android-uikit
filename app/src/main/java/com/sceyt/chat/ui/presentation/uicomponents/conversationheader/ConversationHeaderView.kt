package com.sceyt.chat.ui.presentation.uicomponents.conversationheader

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.data.models.channels.SceytGroupChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.databinding.SceytConversationHeaderViewBinding
import com.sceyt.chat.ui.extensions.dpToPx
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.extensions.getString
import com.sceyt.chat.ui.extensions.shortToast
import com.sceyt.chat.ui.presentation.uicomponents.conversationheader.listeners.HeaderClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversationheader.listeners.HeaderClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.ConversationHeaderViewStyle
import com.sceyt.chat.ui.utils.DateTimeUtil.setLastActiveDateByTime
import com.sceyt.chat.ui.utils.binding.BindingUtil

class ConversationHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), HeaderClickListeners.ClickListeners {

    private val binding: SceytConversationHeaderViewBinding
    private var clickListeners = HeaderClickListenersImpl(this)

    init {
        binding = SceytConversationHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)

        if (!isInEditMode)
            BindingUtil.themedBackgroundColor(this, R.color.sceyt_color_bg)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ConversationHeaderView)
            ConversationHeaderViewStyle.updateWithAttributes(a)
            a.recycle()
        }
        init()
    }

    private fun init() {
        binding.setUpStyle()

        binding.icBack.setOnClickListener {
            clickListeners.onBackClick(it)
        }

        binding.avatar.setOnClickListener {
            clickListeners.onAvatarClick(it)
        }
    }

    private fun SceytConversationHeaderViewBinding.setUpStyle() {
        icBack.setImageResource(ConversationHeaderViewStyle.backIcon)
        title.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.titleColor))
        subTitle.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.subTitleColor))
    }

    private fun setChannelSubTitle(channel: SceytChannel) {
        post {
            val title = if (channel is SceytDirectChannel) {
                val member = channel.peer ?: return@post
                if (member.presence.state == PresenceState.Online) {
                    getString(R.string.online)
                } else {
                    if (member.presence.lastActiveAt != 0L)
                        setLastActiveDateByTime(member.presence.lastActiveAt)
                    else null
                }
            } else getString(R.string.members_count, (channel as SceytGroupChannel).memberCount)

            binding.subTitle.text = title
            binding.subTitle.isVisible = !title.isNullOrBlank()
        }
    }

    internal fun setChannel(channel: SceytChannel) {
        val subjAndSUrl = channel.getSubjectAndAvatarUrl()
        binding.avatar.setNameAndImageUrl(subjAndSUrl.first, subjAndSUrl.second)
        binding.title.text = subjAndSUrl.first

        setChannelSubTitle(channel)
    }

    internal fun setReplayMessage(message: SceytMessage?) {
        binding.avatar.isVisible = false
        with(binding.title) {
            text = getString(R.string.thread_replay)
            (layoutParams as MarginLayoutParams).setMargins(binding.avatar.marginLeft, marginTop, marginRight, marginBottom)
        }

        val fullName = message?.from?.fullName
        val subTitleText = String.format(getString(R.string.with), fullName)
        binding.subTitle.text = subTitleText
        binding.subTitle.isVisible = !fullName.isNullOrBlank()
    }

    fun setCustomClickListener(headerClickListenersImpl: HeaderClickListenersImpl) {
        clickListeners = headerClickListenersImpl
    }

    fun setClickListener(listeners: HeaderClickListeners) {
        clickListeners.setListener(listeners)
    }

    override fun onAvatarClick(view: View) {
        context.shortToast("todo")
    }

    override fun onBackClick(view: View) {
        (context as? AppCompatActivity)?.onBackPressed()
    }
}