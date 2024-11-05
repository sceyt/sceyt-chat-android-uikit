package com.sceyt.chatuikit.presentation.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.config.IntervalOption
import com.sceyt.chatuikit.databinding.SceytItemOptionBinding
import com.sceyt.chatuikit.styles.common.ButtonStyle

class IntervalOptionsAdapter(
        private val data: List<IntervalOption>,
        private val buttonStyle: ButtonStyle,
        private val clickListener: (IntervalOption) -> Unit,
) : RecyclerView.Adapter<IntervalOptionsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SceytItemOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(
            private val binding: SceytItemOptionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.applyStyle()
        }

        fun bind(option: IntervalOption) {
            binding.btnOption.text = option.title

            itemView.setOnClickListener {
                clickListener(option)
            }
        }

        private fun SceytItemOptionBinding.applyStyle() {
            buttonStyle.apply(btnOption)
        }
    }
}