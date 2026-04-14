package com.sceyt.chatuikit.presentation.components.global_search.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytItemSearchSuggestionUserBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.common.recyclerview.AsyncListDiffer
import com.sceyt.chatuikit.presentation.common.recyclerview.UserDiffUtilItemCallBack
import com.sceyt.chatuikit.styles.search.GlobalSearchSuggestionsStyle
import kotlinx.coroutines.CoroutineScope

open class GlobalSearchSuggestionsAdapter(
    private val scope: CoroutineScope,
    private val style: GlobalSearchSuggestionsStyle,
    private val onClick: (SceytUser) -> Unit,
) : RecyclerView.Adapter<GlobalSearchSuggestionsAdapter.SuggestionViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val duffUtil = AsyncListDiffer(
        adapter = this, diffCallback = UserDiffUtilItemCallBack(), scope = scope
    )

    open fun submit(items: List<SceytUser>) {
        duffUtil.submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = SceytItemSearchSuggestionUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SuggestionViewHolder(binding, style)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(currentList[position], onClick)
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemId(position: Int): Long {
        return currentList[position].id.hashCode().toLong()
    }

    private val currentList: List<SceytUser>
        get() = duffUtil.currentList

    open class SuggestionViewHolder(
        private val binding: SceytItemSearchSuggestionUserBinding,
        private val style: GlobalSearchSuggestionsStyle,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context

        init {
            binding.applyStyle()
        }

        open fun bind(user: SceytUser, onClick: (SceytUser) -> Unit) = with(binding) {
            name.text = style.userNameFormatter.format(context, user)
            style.avatarRenderer.render(
                context = context,
                from = user,
                style = style.avatarStyle,
                avatarView = avatar
            )

            binding.root.setOnClickListener { onClick(user) }
        }

        private fun SceytItemSearchSuggestionUserBinding.applyStyle() {
            style.suggestionTextStyle.apply(name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                root.outlineSpotShadowColor = context.getCompatColor(R.color.sceyt_color_shadow)
            }
        }
    }
}
