package com.sceyt.chatuikit.presentation.components.create_poll.adapters

import android.text.Editable
import android.text.TextWatcher
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemPollOptionBinding
import com.sceyt.chatuikit.presentation.components.create_poll.PollOption
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle

class PollOptionViewHolder(
        private val binding: SceytItemPollOptionBinding,
        private val style: CreatePollStyle,
        private val onTextChanged: (PollOption, String) -> Unit,
        private val onRemoveClick: (PollOption) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private var currentOption: PollOption? = null
    private var textWatcher: TextWatcher? = null

    init {
        applyStyle()
    }

    private fun applyStyle() = with(binding) {
        style.optionItemBackgroundStyle.apply(root)
        style.optionInputTextStyle.apply(etOption)
        icRemove.setImageDrawable(style.removeOptionIcon)
        icDrag.setImageDrawable(style.dragIcon)
    }

    fun bind(option: PollOption, canRemove: Boolean) {
        currentOption = option

        // Remove previous text watcher if exists
        textWatcher?.let { binding.etOption.removeTextChangedListener(it) }

        // Set the text
        if (binding.etOption.text.toString() != option.text) {
            binding.etOption.setText(option.text)
        }

        // Show/hide remove button
        binding.icRemove.isVisible = canRemove

        // Add new text watcher
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(option, s?.toString() ?: "")
            }
        }
        binding.etOption.addTextChangedListener(textWatcher)

        binding.icRemove.setOnClickListener {
            onRemoveClick(option)
        }

        binding.icDrag.setOnTouchListener { _, _ ->
            // Handle drag functionality if needed
            false
        }
    }

    fun unbind() {
        textWatcher?.let { binding.etOption.removeTextChangedListener(it) }
        textWatcher = null
        currentOption = null
    }
}

