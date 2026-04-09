package com.sceyt.chatuikit.presentation.components.global_search.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchChipBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class GlobalSearchTabsAdapter(
    private val style: GlobalSearchStyle,
    private val tabs: List<GlobalSearchTab>,
    private val onClick: (GlobalSearchTab) -> Unit,
) : RecyclerView.Adapter<GlobalSearchTabsAdapter.TabViewHolder>() {

    private var selectedTab: GlobalSearchTab = tabs.firstOrNull() ?: GlobalSearchTab.Chats

    init {
        setHasStableIds(true)
    }

    fun setSelectedTab(tab: GlobalSearchTab) {
        if (selectedTab == tab) return
        val previousIndex = tabs.indexOf(selectedTab)
        val newIndex = tabs.indexOf(tab)
        selectedTab = tab
        if (previousIndex >= 0)
            notifyItemChanged(previousIndex)
        if (newIndex >= 0)
            notifyItemChanged(newIndex)
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
        holder.bind(tab, selectedTab == tab, onClick)
    }

    override fun onBindViewHolder(
        holder: TabViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val tab = tabs[position]
        holder.bind(tab, selectedTab == tab, onClick)
    }

    override fun getItemCount(): Int = tabs.size

    open class TabViewHolder(
        private val binding: SceytItemGlobalSearchChipBinding,
        private val style: GlobalSearchStyle,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var boundTab: GlobalSearchTab? = null
        private var lastIsSelected: Boolean? = null

        open fun bind(
            tab: GlobalSearchTab,
            isSelected: Boolean,
            onClick: (GlobalSearchTab) -> Unit,
        ) {
            binding.root.text = binding.root.context.getString(tab.titleRes)
            applySelectedState(isSelected)
            boundTab = tab
            lastIsSelected = isSelected
            binding.root.setOnClickListener { onClick(tab) }
        }

        protected open fun applySelectedState(isSelected: Boolean) {
            val bgColor = if (isSelected) {
                style.tabSelectedBackgroundColor
            } else {
                style.tabUnselectedBackgroundColor
            }

            val textColor = if (isSelected) {
                style.tabSelectedTextColor
            } else {
                style.tabUnselectedTextColor
            }

            val strokeWidth = if (isSelected) 0 else dpToPx(1f)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(30f).toFloat()
                setColor(bgColor)
                setStroke(strokeWidth, style.tabStrokeColor)
            }

            binding.root.background = drawable
            binding.root.setTextColor(textColor)
        }
    }
}
