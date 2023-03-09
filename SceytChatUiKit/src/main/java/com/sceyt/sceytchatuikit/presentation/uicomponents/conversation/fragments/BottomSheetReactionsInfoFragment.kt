package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.ReactionData
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytBottomShetReactionsInfoBinding
import com.sceyt.sceytchatuikit.extensions.dismissSafety
import com.sceyt.sceytchatuikit.extensions.screenHeightPx
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters.FragmentReactedUsers
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters.ReactionsHeaderAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters.ViewPagerAdapterReactedUsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BottomSheetReactionsInfoFragment : BottomSheetDialogFragment() {
    private lateinit var binding: SceytBottomShetReactionsInfoBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private var headerAdapter: ReactionsHeaderAdapter? = null
    private lateinit var message: SceytMessage
    private var reactionClick: ((Reaction) -> Unit)? = null

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
        initData()
    }

    private fun getBundleArguments() {
        message = requireNotNull(arguments?.getParcelable(MESSAGE_KEY))
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

    private fun initData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val reactions = /*SceytKitClient.getMessagesMiddleWare().getMessageReactionsDbByKey(message.id, "")*/message.selfReactions
                    ?: return@launch

            val groups = reactions.groupBy { it.key }
            val headerData = groups.map { it.value.firstOrNull() }.filterNotNull()

            withContext(Dispatchers.Main) {
                initPager(groups, reactions.toList())
                initHeaderAdapter(headerData)
            }
        }
    }

    private fun initHeaderAdapter(data: List<Reaction>) {
        val reactions: ArrayList<ReactionItem> = data.map { ReactionItem.Reaction(ReactionData(it.key, it.score.toLong()), message) }.toArrayList()
        reactions.add(0, ReactionItem.Other(message))
        headerAdapter = ReactionsHeaderAdapter(reactions, (message.reactionScores?.sumOf { it.score }
                ?: 0).toInt()) { _, position ->
            binding.viewPager.currentItem = position
        }
        binding.rvReactions.adapter = headerAdapter
    }

    private fun initPager(groups: Map<String, List<Reaction>>, allReactions: List<Reaction>) {
        val fragments: List<Fragment> = groups.map { data ->
            createReactedUsersFragment(data.value)
        }.toMutableList().apply {
            add(0, createReactedUsersFragment(allReactions))
        }

        with(binding.viewPager) {
            adapter = ViewPagerAdapterReactedUsers(this@BottomSheetReactionsInfoFragment, fragments)
            offscreenPageLimit = 2

            children.find { it is RecyclerView }?.let {
                (it as RecyclerView).isNestedScrollingEnabled = false
            }

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    headerAdapter?.setSelected(position)
                    binding.rvReactions.smoothScrollToPosition(position)
                }
            })
        }
    }

    private fun createReactedUsersFragment(reactions: List<Reaction>) = FragmentReactedUsers().apply {
        setReactions(reactions)
        setClickListener {
            reactionClick?.invoke(it)
            dismissSafety()
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