package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments

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
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytBottomShetReactionsInfoBinding
import com.sceyt.sceytchatuikit.extensions.dismissSafety
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.screenHeightPx
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters.FragmentReactedUsers
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters.ReactionHeaderItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters.ReactionsHeaderAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters.ViewPagerAdapterReactedUsers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.Integer.max


class BottomSheetReactionsInfoFragment : BottomSheetDialogFragment() {
    private lateinit var binding: SceytBottomShetReactionsInfoBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private var headerAdapter: ReactionsHeaderAdapter? = null
    private var pagerAdapter: ViewPagerAdapterReactedUsers? = null
    private lateinit var message: SceytMessage
    private var reactionClick: ((Reaction) -> Unit)? = null
    private var pagerCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SceytAppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SceytBottomShetReactionsInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initAdapters()
        observeToReactionsUpdate()
        (binding.viewPager.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxHeight = screenHeightPx() * 4 / 5
    }

    private fun getBundleArguments() {
        message = requireNotNull(arguments?.parcelable(MESSAGE_KEY))
    }

    private fun observeToReactionsUpdate() {
        MessageEventsObserver.onMessageReactionUpdatedFlow
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
                    ReactionUpdateEventEnum.ADD -> {
                        headerAdapter?.addOrUpdateItem(reactionTotal)
                        pagerAdapter?.addOrUpdateItem(createReactedUsersFragment(eventData.reaction.key, message.id), eventData.reaction)
                    }

                    ReactionUpdateEventEnum.REMOVE -> {
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
                    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                    bottomSheetBehavior.peekHeight = (screenHeightPx() / 2.5).toInt()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

    private fun initAdapters() {
        initHeaderAdapter(message.reactionTotals ?: emptyArray())
        initPager(message.reactionTotals ?: emptyArray())
    }

    private fun initHeaderAdapter(data: Array<ReactionTotal>) {
        val reactions: ArrayList<ReactionHeaderItem> = data.map { ReactionHeaderItem.Reaction(it) }.toArrayList()
        reactions.add(0, ReactionHeaderItem.All(message.reactionTotals?.sumOf { it.score } ?: 0))
        headerAdapter = ReactionsHeaderAdapter(reactions) { _, position ->
            binding.viewPager.currentItem = position
        }
        binding.rvReactions.adapter = headerAdapter
    }

    private fun initPager(data: Array<ReactionTotal>) {
        val messageId = message.id
        val fragments: ArrayList<FragmentReactedUsers> = data.map {
            createReactedUsersFragment(it.key, messageId)
        }.toArrayList().apply {
            add(0, createReactedUsersFragment("", messageId))
        }

        with(binding.viewPager) {
            adapter = ViewPagerAdapterReactedUsers(this@BottomSheetReactionsInfoFragment, fragments).also {
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
        return FragmentReactedUsers.newInstance(messageId, key).apply {
            setClickListener {
                reactionClick?.invoke(it)
                dismissSafety()
            }
        }
    }

    fun setClickListener(listener: (Reaction) -> Unit) {
        reactionClick = listener
    }

    companion object {
        private const val MESSAGE_KEY = "messageKey"

        fun newInstance(message: SceytMessage): BottomSheetReactionsInfoFragment {
            val fragment = BottomSheetReactionsInfoFragment()
            fragment.setBundleArguments {
                putParcelable(MESSAGE_KEY, message)
            }
            return fragment
        }
    }
}