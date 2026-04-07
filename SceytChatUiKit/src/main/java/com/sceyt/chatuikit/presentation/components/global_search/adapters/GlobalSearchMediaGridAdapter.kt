package com.sceyt.chatuikit.presentation.components.global_search.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMediaGridBinding
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchMediaGridItem
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

open class GlobalSearchMediaGridAdapter(
    private val onClick: (GlobalSearchMediaGridItem) -> Unit,
) : RecyclerView.Adapter<GlobalSearchMediaGridAdapter.MediaGridViewHolder>() {
    private var items: List<GlobalSearchMediaGridItem> = emptyList()

    open fun submit(items: List<GlobalSearchMediaGridItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaGridViewHolder {
        return MediaGridViewHolder(
            SceytItemGlobalSearchMediaGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MediaGridViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    open class MediaGridViewHolder(
        private val binding: SceytItemGlobalSearchMediaGridBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        open fun bind(item: GlobalSearchMediaGridItem, onClick: (GlobalSearchMediaGridItem) -> Unit) {
            val attachment = item.result.attachment
            val metadata = attachment.getInfoFromMetadata()
            val model = attachment.filePath ?: attachment.url

            if (model == null) {
                binding.previewImage.setImageResource(R.drawable.sceyt_ic_empty_medias)
            } else {
                Glide.with(binding.previewImage.context.applicationContext)
                    .load(model)
                    .placeholder(R.drawable.sceyt_ic_empty_medias)
                    .centerCrop()
                    .into(binding.previewImage)
            }

            binding.ivPlay.visibility =
                if (attachment.type == "video") android.view.View.VISIBLE else android.view.View.GONE
            binding.tvDuration.visibility =
                if ((metadata.duration ?: 0L) > 0) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvDuration.text = DateTimeUtil.convertMillisToString(metadata.duration ?: 0L)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
