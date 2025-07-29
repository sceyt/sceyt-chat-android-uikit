package com.sceyt.chatuikit.presentation.components.channel.messages.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.databinding.SceytBottomShetReactionsInfoBinding
import com.sceyt.chatuikit.extensions.dismissSafety
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.FragmentReactedUsers
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.HeaderViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.ReactionHeaderItem
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.ReactionsHeaderAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.ViewPagerAdapterReactedUsers
import com.sceyt.chatuikit.presentation.style.StyleRegistry
import com.sceyt.chatuikit.styles.reactions_info.ReactionsInfoStyle
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.Integer.max

class ReactionsInfoBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var binding: SceytBottomShetReactionsInfoBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private var headerAdapter: ReactionsHeaderAdapter? = null
    private var pagerAdapter: ViewPagerAdapterReactedUsers? = null
    private lateinit var message: SceytMessage
    private var reactionClick: ((SceytReaction) -> Unit)? = null
    private var pagerCallback: ViewPager2.OnPageChangeCallback? = null
    private lateinit var style: ReactionsInfoStyle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = ReactionsInfoStyle.Builder(requireContext(), null).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SceytBottomShetReactionsInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        binding.applyStyle()

        initAdapters()
        observeToReactionsUpdate()
        (binding.viewPager.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxHeight = screenHeightPx() * 4 / 5
    }

    private fun getBundleArguments() {
        message = requireNotNull(arguments?.parcelable(MESSAGE_KEY))
    }

    private fun observeToReactionsUpdate() {
        MessageEventManager.onMessageReactionUpdatedFlow
            .filter { it.message.id == message.id }
            .onEach { eventData ->
                message = eventData.message
                if (message.reactionTotals.isNullOrEmpty()) {
                    dismissSafety()
                    return@onEach
                }
                val reactionTotal = eventData.message.reactionTotals?.find { it.key == eventData.reaction.key }
                        ?: ReactionTotal(eventData.reaction.key, 0, eventData.reaction.score.toLong())
                when (eventData.eventType) {
                    ReactionUpdateEventEnum.Add -> {
                        headerAdapter?.addOrUpdateItem(reactionTotal)
                        pagerAdapter?.addOrUpdateItem(createReactedUsersFragment(eventData.reaction.key, message.id), eventData.reaction)
                    }

                    ReactionUpdateEventEnum.Remove -> {
                        if (reactionTotal.score == 0L) {
                            headerAdapter?.removeItem(eventData.reaction)
                            pagerAdapter?.removeFragment(eventData.reaction.key)
                        } else {
                            headerAdapter?.addOrUpdateItem(reactionTotal)
                            pagerAdapter?.addOrUpdateItem(createReactedUsersFragment(eventData.reaction.key, message.id), eventData.reaction)
                        }
                    }
                }
                headerAdapter?.updateAppItem(eventData.message.reactionTotals?.sumOf { it.score }
                        ?: 0)
                pagerAdapter?.updateAllReactionsFragment()
                registerOnPageChangeCallback()
            }.launchIn(lifecycleScope)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            super.onCreateDialog(savedInstanceState).apply {
                setOnShowListener {
                    val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    style.backgroundStyle.apply(bottomSheet)
                    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                    bottomSheetBehavior.peekHeight = (screenHeightPx() / 2.5).toInt()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

    private fun initAdapters() {
        initHeaderAdapter(message.messageReactions ?: emptyList())
        initPager(message.messageReactions ?: emptyList())
    }

    private fun initHeaderAdapter(data: List<ReactionItem.Reaction>) {
        val allItem = ReactionHeaderItem.All(message.messageReactions?.sumOf {
            it.reaction.score
        }?.toLong() ?: 0)
        val reactions = listOf(allItem)
            .plus(data.map { ReactionHeaderItem.Reaction(it.reaction) })

        val factory = HeaderViewHolderFactory(style.headerItemStyle).also {
            it.setClickListener { _, position ->
                binding.viewPager.currentItem = position
            }
        }
        headerAdapter = ReactionsHeaderAdapter(reactions, factory)
        binding.rvReactions.adapter = headerAdapter
    }

    private fun initPager(data: List<ReactionItem.Reaction>) {
        val messageId = message.id
        val fragments = listOf(createReactedUsersFragment("", messageId))
            .plus(data.map {
                createReactedUsersFragment(it.reaction.key, messageId)
            })

        with(binding.viewPager) {
            adapter = ViewPagerAdapterReactedUsers(this@ReactionsInfoBottomSheetFragment, fragments).also {
                pagerAdapter = it
            }
            offscreenPageLimit = 1

            children.find { it is RecyclerView }?.let {
                (it as RecyclerView).isNestedScrollingEnabled = false
            }

            registerOnPageChangeCallback()

            addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                val oldHeight = (binding.viewPager.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMinHeight
                (binding.viewPager.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMinHeight = max(oldHeight, bottom - top)
            }
        }
    }

    private fun registerOnPageChangeCallback() {
        pagerCallback?.let {
            binding.viewPager.unregisterOnPageChangeCallback(it)
        }
        pagerCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                headerAdapter?.setSelected(position)
                binding.rvReactions.smoothScrollToPosition(position)
            }
        }
        binding.viewPager.registerOnPageChangeCallback(pagerCallback!!)
        headerAdapter?.setSelected(binding.viewPager.currentItem)
    }

    private fun createReactedUsersFragment(key: String, messageId: Long): FragmentReactedUsers {
        val style = style.reactedUserListStyle
        StyleRegistry.register(style)
        return FragmentReactedUsers.newInstance(messageId, key, style.styleId).apply {
            setClickListener {
                reactionClick?.invoke(it)
                dismissSafety()
            }
        }
    }

    fun setClickListener(listener: (SceytReaction) -> Unit) {
        reactionClick = listener
    }

    private fun SceytBottomShetReactionsInfoBinding.applyStyle() {
        style.backgroundStyle.apply(root)
        divider.dividerColor = style.dividerColor
    }

    override fun onDestroy() {
        super.onDestroy()
        StyleRegistry.unregister(style.reactedUserListStyle.styleId)
    }

    companion object {
        private const val MESSAGE_KEY = "messageKey"

        fun newInstance(message: SceytMessage): ReactionsInfoBottomSheetFragment {
            return ReactionsInfoBottomSheetFragment().setBundleArguments {
                putParcelable(MESSAGE_KEY, message)
            }
        }
    }
}