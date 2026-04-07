package com.sceyt.chatuikit.presentation.components.global_search.adapters

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.appcompat.content.res.AppCompatResources
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchChipBinding
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import com.sceyt.chatuikit.presentation.components.global_search.displayName
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class GlobalSearchTabsAdapter(
    private val style: GlobalSearchStyle,
    private val tabs: List<GlobalSearchTab>,
    private val onClick: (GlobalSearchTab) -> Unit,
) : RecyclerView.Adapter<GlobalSearchTabsAdapter.TabViewHolder>() {
    private companion object {
        private const val PAYLOAD_SELECTION_CHANGED = "payload_selection_changed"
    }
    private var selectedTab: GlobalSearchTab = tabs.firstOrNull() ?: GlobalSearchTab.Chats

    init {
        setHasStableIds(true)
    }

    fun setSelectedTab(tab: GlobalSearchTab) {
        if (selectedTab == tab) return
        val previousIndex = tabs.indexOf(selectedTab)
        val newIndex = tabs.indexOf(tab)
        selectedTab = tab
        if (previousIndex >= 0) notifyItemChanged(previousIndex, PAYLOAD_SELECTION_CHANGED)
        if (newIndex >= 0) notifyItemChanged(newIndex, PAYLOAD_SELECTION_CHANGED)
    }

    override fun getItemId(position: Int): Long {
        return tabs[position].ordinal.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = SceytItemGlobalSearchChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TabViewHolder(binding, style)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.bind(tab, selectedTab == tab, onClick, animate = false)
    }

    override fun onBindViewHolder(
        holder: TabViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val tab = tabs[position]
        val animate = payloads.contains(PAYLOAD_SELECTION_CHANGED)
        holder.bind(tab, selectedTab == tab, onClick, animate = animate)
    }

    override fun getItemCount(): Int = tabs.size

    open class TabViewHolder(
        private val binding: SceytItemGlobalSearchChipBinding,
        private val style: GlobalSearchStyle,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var boundTab: GlobalSearchTab? = null
        private var lastIsSelected: Boolean? = null
        private var selectionAnimator: ValueAnimator? = null

        open fun bind(
            tab: GlobalSearchTab,
            isSelected: Boolean,
            onClick: (GlobalSearchTab) -> Unit,
            animate: Boolean,
        ) {
            binding.root.text = binding.root.context.getString(tab.titleRes)
            binding.root.chipIcon = null
            binding.root.isChipIconVisible = false
            val shouldAnimate = animate && boundTab == tab && lastIsSelected != null && lastIsSelected != isSelected
            if (shouldAnimate) {
                animateSelectionChange(isSelected)
            } else {
                applySelectedState(isSelected)
            }
            boundTab = tab
            lastIsSelected = isSelected
            binding.root.setOnClickListener { onClick(tab) }
        }

        protected open fun animateSelectionChange(isSelected: Boolean) {
            selectionAnimator?.cancel()

            val fromBackground = if (lastIsSelected == true) {
                style.tabSelectedBackgroundColor
            } else {
                style.tabUnselectedBackgroundColor
            }
            val toBackground = if (isSelected) {
                style.tabSelectedBackgroundColor
            } else {
                style.tabUnselectedBackgroundColor
            }
            val fromText = if (lastIsSelected == true) {
                style.tabSelectedTextColor
            } else {
                style.tabUnselectedTextColor
            }
            val toText = if (isSelected) {
                style.tabSelectedTextColor
            } else {
                style.tabUnselectedTextColor
            }
            val fromStroke = if (lastIsSelected == true) 0f else binding.root.resources.displayMetrics.density
            val toStroke = if (isSelected) 0f else binding.root.resources.displayMetrics.density

            selectionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 180L
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animator ->
                    val fraction = animator.animatedFraction
                    val backgroundColor = ArgbEvaluator().evaluate(fraction, fromBackground, toBackground) as Int
                    val textColor = ArgbEvaluator().evaluate(fraction, fromText, toText) as Int
                    binding.root.chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
                    binding.root.chipStrokeColor = ColorStateList.valueOf(style.tabStrokeColor)
                    binding.root.chipStrokeWidth = fromStroke + ((toStroke - fromStroke) * fraction)
                    binding.root.setTextColor(textColor)
                }
                doOnEnd {
                    applySelectedState(isSelected)
                }
                start()
            }
        }

        protected open fun applySelectedState(isSelected: Boolean) {
            selectionAnimator?.cancel()
            binding.root.chipBackgroundColor = ColorStateList.valueOf(
                if (isSelected) style.tabSelectedBackgroundColor else style.tabUnselectedBackgroundColor
            )
            binding.root.chipStrokeColor = ColorStateList.valueOf(style.tabStrokeColor)
            binding.root.chipStrokeWidth =
                if (isSelected) 0f else binding.root.resources.displayMetrics.density
            binding.root.setTextColor(
                if (isSelected) style.tabSelectedTextColor else style.tabUnselectedTextColor
            )
        }
    }
}

open class GlobalSearchSuggestionsAdapter(
    private val style: GlobalSearchStyle,
    private val onClick: (SceytUser) -> Unit,
) : RecyclerView.Adapter<GlobalSearchSuggestionsAdapter.SuggestionViewHolder>() {
    private var items: List<SceytUser> = emptyList()

    open fun submit(items: List<SceytUser>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = SceytItemGlobalSearchChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SuggestionViewHolder(binding, style)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    open class SuggestionViewHolder(
        private val binding: SceytItemGlobalSearchChipBinding,
        private val style: GlobalSearchStyle,
    ) : RecyclerView.ViewHolder(binding.root) {
        open fun bind(user: SceytUser, onClick: (SceytUser) -> Unit) {
            binding.root.text = user.displayName()
            binding.root.chipBackgroundColor = ColorStateList.valueOf(style.suggestionChipBackgroundColor)
            binding.root.chipStrokeWidth = 0f
            binding.root.setTextColor(style.suggestionChipTextColor)
            binding.root.chipIcon = AppCompatResources.getDrawable(
                binding.root.context,
                R.drawable.sceyt_ic_bottom_nav_profile
            )
            binding.root.isChipIconVisible = true
            binding.root.chipIconTint = ColorStateList.valueOf(style.suggestionChipIconColor)
            binding.root.chipIconSize = binding.root.resources.displayMetrics.density * 20
            binding.root.setOnClickListener { onClick(user) }
        }
    }
}
