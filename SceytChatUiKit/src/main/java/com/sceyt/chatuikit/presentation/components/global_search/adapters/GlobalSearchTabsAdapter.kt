package com.sceyt.chatuikit.presentation.components.global_search.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchChipBinding
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import com.sceyt.chatuikit.styles.search.GlobalSearchTabBarStyle

open class GlobalSearchTabsAdapter(
    private val style: GlobalSearchTabBarStyle,
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
        private val style: GlobalSearchTabBarStyle,
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
            val bgStyle = if (isSelected) style.selectedTabBackgroundStyle else style.unselectedTabBackgroundStyle
            val textStyle = if (isSelected) style.selectedTabTextStyle else style.unselectedTabTextStyle
            bgStyle.apply(binding.root)
            textStyle.apply(binding.root)
        }
    }
}
