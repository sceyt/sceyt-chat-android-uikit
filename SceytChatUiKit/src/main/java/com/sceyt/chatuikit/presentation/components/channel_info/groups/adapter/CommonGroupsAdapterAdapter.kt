package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemCommonGroupBinding
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.channel_info.common_groups.ChannelInfoCommonGroupsStyle

class ChannelsAdapter(
    private val groupsStyleProvider: () -> ChannelInfoCommonGroupsStyle,
    private val onChannelClick: (SceytChannel) -> Unit
) : RecyclerView.Adapter<ChannelsAdapter.ChannelViewHolder>() {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SceytChannel>() {
            override fun areItemsTheSame(oldItem: SceytChannel, newItem: SceytChannel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SceytChannel, newItem: SceytChannel): Boolean {
                return !oldItem.diff(newItem).hasDifference()
            }
        }
    }

    private val differ = AsyncListDiffer(this, DIFF_CALLBACK)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = SceytItemCommonGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount() = differ.currentList.size

    fun submitList(newChannels: List<SceytChannel>) {
        differ.submitList(newChannels)
    }

    inner class ChannelViewHolder(
        private val binding: SceytItemCommonGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onChannelClick(differ.currentList[bindingAdapterPosition])
                }
            }
        }

        fun bind(channel: SceytChannel) {
            val itemStyle = groupsStyleProvider().itemStyle

            with(binding) {
                root.setBackgroundColor(itemStyle.backgroundColor)

                val channelName = itemStyle.commonGroupTitleFormatter.format(
                    context = root.context,
                    from = channel
                )
                tvChannelName.text = channelName

                setAvatar(channel, avatar)
            }
        }

        private fun setAvatar(
            channel: SceytChannel,
            avatarView: AvatarView,
        ) {
            val itemStyle = groupsStyleProvider().itemStyle
            itemStyle.channelAvatarRenderer.render(
                context = binding.root.context,
                from = channel,
                style = itemStyle.avatarStyle,
                avatarView = avatarView
            )
        }
    }
}