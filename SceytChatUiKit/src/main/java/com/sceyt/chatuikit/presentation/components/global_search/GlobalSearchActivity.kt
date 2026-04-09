package com.sceyt.chatuikit.presentation.components.global_search

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytActivityGlobalSearchBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.extensions.visibleInvisibleWithBottomSlideAnim
import com.sceyt.chatuikit.persistence.extensions.collectWithLifecycle
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchSuggestionsAdapter
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchTabsAdapter
import com.sceyt.chatuikit.presentation.components.global_search.chats.ChatsSearchFragment
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class GlobalSearchActivity : AppCompatActivity() {
    protected lateinit var binding: SceytActivityGlobalSearchBinding
        private set
    protected lateinit var style: GlobalSearchStyle
        private set
    protected open val headerViewModel: GlobalSearchHeaderViewModel by viewModels {
        createHeaderViewModelFactory()
    }

    private val providedTabs by lazy(LazyThreadSafetyMode.NONE) {
        provideTabs().distinct().ifEmpty { GlobalSearchTab.entries.toList() }
    }

    private val pagerAdapter by lazy(LazyThreadSafetyMode.NONE, ::createPagerAdapter)
    private val tabsAdapter by lazy(LazyThreadSafetyMode.NONE, ::createTabsAdapter)
    private val suggestionsAdapter by lazy(LazyThreadSafetyMode.NONE, ::createSuggestionsAdapter)
    private var suggestionsVisible = false

    private val pagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            headerViewModel.onTabSelected(pagerAdapter.tabAt(position))
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        if (launchedWithSharedTransition()) {
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        style = buildStyle()
        StyleRegistry.register(style)
        setActivityContentView()
        if (launchedWithSharedTransition()) {
            postponeEnterTransition()
            setupSharedElementTransition()
            binding.searchInputView.doOnPreDraw { startPostponedEnterTransition() }
        }

        applyInsetsAndWindowColor(binding.root)
        statusBarIconsColorWithBackground()
        applyStyle()
        initViews()
        observeState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeWithBackAnimation()
            }
        })
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(pagerCallback)
        StyleRegistry.unregister(style.styleId)
        super.onDestroy()
    }

    protected open fun setActivityContentView() {
        binding = SceytActivityGlobalSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    protected open fun buildStyle(): GlobalSearchStyle {
        return GlobalSearchStyle.Builder(this).build()
    }

    protected open fun provideTabs(): List<GlobalSearchTab> {
        return GlobalSearchTab.entries.toList()
    }

    protected open fun createFragment(tab: GlobalSearchTab): Fragment {
        return when (tab) {
            GlobalSearchTab.Chats -> ChatsSearchFragment.newInstance(
                styleId = style.styleId,
                sessionId = headerViewModel.sessionId
            )

            GlobalSearchTab.Channels -> ChannelsSearchFragment.newInstance(
                styleId = style.styleId,
                sessionId = headerViewModel.sessionId
            )

            GlobalSearchTab.Media -> MediaSearchFragment.newInstance(
                styleId = style.styleId,
                sessionId = headerViewModel.sessionId
            )

            GlobalSearchTab.Files -> FilesSearchFragment.newInstance(
                styleId = style.styleId,
                sessionId = headerViewModel.sessionId
            )

            GlobalSearchTab.Voice -> VoiceSearchFragment.newInstance(
                styleId = style.styleId,
                sessionId = headerViewModel.sessionId
            )

            GlobalSearchTab.Links -> LinksSearchFragment.newInstance(
                styleId = style.styleId,
                sessionId = headerViewModel.sessionId
            )
        }
    }

    protected open fun createPagerAdapter(): GlobalSearchPagerAdapter {
        return GlobalSearchPagerAdapter(this, providedTabs, ::createFragment)
    }

    protected open fun createTabsAdapter(): GlobalSearchTabsAdapter {
        return GlobalSearchTabsAdapter(style.tabBarStyle, providedTabs, headerViewModel::onTabSelected)
    }

    protected open fun createSuggestionsAdapter(): GlobalSearchSuggestionsAdapter {
        return GlobalSearchSuggestionsAdapter(
            scope = lifecycleScope,
            style = style.suggestionsStyle,
            onClick = headerViewModel::onMemberSelected
        )
    }

    protected open fun createHeaderViewModelFactory(): ViewModelProvider.Factory {
        return GlobalSearchHeaderViewModelFactory(
            initialTab = providedTabs.first(),
            memberSuggestionsProvider = provideMemberSuggestionsProvider(),
            memberSuggestionsLimit = getMemberSuggestionsLimit(),
            memberSuggestionsDebounceMs = getMemberSuggestionsDebounceMs()
        )
    }

    protected open fun provideMemberSuggestionsProvider(): GlobalSearchMemberSuggestionsProvider {
        return GlobalSearchLocalInteractor()
    }

    protected open fun getMemberSuggestionsLimit(): Int = DEFAULT_MEMBER_SUGGESTIONS_LIMIT

    protected open fun getMemberSuggestionsDebounceMs(): Long =
        DEFAULT_MEMBER_SUGGESTIONS_DEBOUNCE_MS

    protected open fun initViews() {
        binding.tabsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GlobalSearchActivity, HORIZONTAL, false)
            itemAnimator = DefaultItemAnimator().apply {
                changeDuration = 150L
            }
            adapter = tabsAdapter
        }
        binding.suggestionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GlobalSearchActivity, HORIZONTAL, false)
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 120L
                removeDuration = 120L
                moveDuration = 120L
                changeDuration = 0L
            }
            adapter = suggestionsAdapter
        }

        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.registerOnPageChangeCallback(pagerCallback)

        binding.btnBack.setOnClickListener { closeWithBackAnimation() }
        binding.searchInputView.setOnQueryChangedListener(headerViewModel::onQueryChanged)
        binding.searchInputView.setOnClearClickListener(headerViewModel::onClearRequested)
        binding.searchInputView.setOnEmptyDeleteListener(headerViewModel::onEmptyQueryDeleteRequested)

        requestInputFocus()
    }

    protected open fun observeState() {
        headerViewModel.headerState.collectWithLifecycle(owner = this, collector = ::renderHeader)
    }

    protected open fun renderHeader(state: GlobalSearchHeaderState) {
        val selectedIndex = pagerAdapter.positionOf(state.activeTab)
        val shouldShowSuggestions = state.showSuggestions
        tabsAdapter.setSelectedTab(state.activeTab)

        updateSuggestionsVisibility(shouldShowSuggestions) {
            if (!shouldShowSuggestions) {
                suggestionsAdapter.submit(emptyList())
            }
        }
        if (shouldShowSuggestions)
            suggestionsAdapter.submit(state.memberSuggestions)

        binding.searchInputView.setQuery(state.query)
        binding.searchInputView.setSelectedMember(
            member = state.selectedMember,
            isPendingRemoval = state.isSelectedMemberRemovalPending
        )

        if (selectedIndex >= 0 && binding.viewPager.currentItem != selectedIndex) {
            binding.viewPager.setCurrentItem(selectedIndex, true)
        }
        if (selectedIndex >= 0) {
            binding.tabsRecyclerView.smoothScrollToPosition(selectedIndex)
        }
    }

    protected open fun applyStyle() {
        binding.root.setBackgroundColor(style.backgroundColor)
        binding.divider.setBackgroundColor(style.dividerColor)
        binding.btnBack.setColorFilter(style.navigationIconColor)
        binding.suggestionsContainer.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.setAlphaComponent(style.backgroundColor, 0),
                style.backgroundColor
            )
        )
        binding.searchInputView.applyStyle(style.inputStyle)
    }

    protected open fun requestInputFocus() {
        if (!launchedWithSharedTransition()) {
            binding.searchInputView.post { binding.searchInputView.focusInput() }
            return
        }

        val transition = window.sharedElementEnterTransition
        if (transition == null) {
            binding.searchInputView.post { binding.searchInputView.focusInput() }
            return
        }

        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) = Unit
            override fun onTransitionCancel(transition: Transition) = onTransitionEnd(transition)
            override fun onTransitionPause(transition: Transition) = Unit
            override fun onTransitionResume(transition: Transition) = Unit
            override fun onTransitionEnd(transition: Transition) {
                transition.removeListener(this)
                binding.searchInputView.post { binding.searchInputView.focusInput() }
            }
        })
    }

    protected open fun closeWithBackAnimation() {
        if (launchedWithSharedTransition()) {
            finishAfterTransition()
        } else {
            super.finish()
            overrideTransitions(
                R.anim.sceyt_anim_slide_hold,
                R.anim.sceyt_anim_slide_out_right,
                false
            )
        }
    }

    open fun onChannelClicked(channel: SceytChannel) {
        ChannelActivity.launch(this, channel)
        finish()
    }

    open fun onMessageClicked(
        messageId: Long,
        channel: SceytChannel,
    ) {
        ChannelActivity.launch(this, channel, messageId)
        finish()
    }

    open fun onAttachmentClicked(result: GlobalSearchAttachmentResult) {
        onMessageClicked(result.message.id, result.channel)
    }

    private fun launchedWithSharedTransition(): Boolean {
        return intent.getBooleanExtra(EXTRA_SHARED_TRANSITION, false)
    }

    private fun setupSharedElementTransition() {
        window.sharedElementEnterTransition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeClipBounds())
            duration = 220L
        }
        window.sharedElementReturnTransition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeClipBounds())
            duration = 220L
        }
        window.enterTransition = Fade().apply {
            duration = 160L
            startDelay = 40L
        }
        window.returnTransition = Fade().apply {
            duration = 140L
        }
    }

    private fun updateSuggestionsVisibility(visible: Boolean, doOnFinish: () -> Unit = { }) {
        if (suggestionsVisible == visible) return

        suggestionsVisible = visible
        binding.suggestionsRecyclerView.visibleInvisibleWithBottomSlideAnim(
            visible = visible,
            doOnFinish = doOnFinish
        )
    }

    companion object {
        const val SHARED_TRANSITION_NAME = "sceyt_global_search_bar"
        const val STYLE_ID_KEY = "GLOBAL_SEARCH_STYLE_ID_KEY"
        const val SESSION_ID_KEY = "GLOBAL_SEARCH_SESSION_ID_KEY"
        private const val EXTRA_SHARED_TRANSITION = "EXTRA_SHARED_TRANSITION"

        fun launch(activity: Activity, sourceView: View? = null) {
            if (sourceView != null) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    sourceView,
                    SHARED_TRANSITION_NAME
                )
                activity.launchActivity<GlobalSearchActivity>(
                    options = options.toBundle() ?: Bundle()
                ) {
                    putExtra(EXTRA_SHARED_TRANSITION, true)
                }
            } else {
                activity.launchActivity<GlobalSearchActivity>(
                    R.anim.sceyt_anim_slide_in_right,
                    R.anim.sceyt_anim_slide_hold
                )
            }
        }
    }
}
