package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.emojiview.emojiview.AXEmojiManager
import com.emojiview.emojiview.AXEmojiTheme
import com.emojiview.emojiview.emoji.Emoji
import com.emojiview.emojiview.listener.OnEmojiActions
import com.emojiview.emojiview.view.AXSingleEmojiView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytFragmentBottomSheetEmojisBinding
import com.sceyt.chat.ui.extensions.getCompatColor

class BottomSheetEmojisFragment(private val emojiListener: (Emoji) -> Unit) : BottomSheetDialogFragment() {
    private lateinit var mBinding: SceytFragmentBottomSheetEmojisBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = SceytFragmentBottomSheetEmojisBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initEmojis()
    }

    private fun initEmojis() {
        AXEmojiManager.setEmojiViewTheme(AXEmojiTheme().apply {
            backgroundColor = requireContext().getCompatColor(R.color.sceyt_color_dialog_bg_themed)
            variantPopupBackgroundColor = requireContext().getCompatColor(R.color.sceyt_color_dialog_bg_themed)
            categoryColor = requireContext().getCompatColor(R.color.sceyt_color_dialog_bg_themed)
            titleColor = requireContext().getCompatColor(R.color.sceyt_color_text_themed)
            dividerColor = requireContext().getCompatColor(R.color.sceyt_color_divider)
            variantDividerColor = requireContext().getCompatColor(R.color.sceyt_color_divider)
        })
        mBinding.emojiPopupLayout.initPopupView(AXSingleEmojiView(requireContext()).apply {
            onEmojiActionsListener = object : OnEmojiActions {
                override fun onClick(view: View?, emoji: Emoji, fromRecent: Boolean, fromVariant: Boolean) {
                    emojiListener(emoji)
                    this@BottomSheetEmojisFragment.dismiss()
                }

                override fun onLongClick(view: View?, emoji: Emoji, fromRecent: Boolean, fromVariant: Boolean): Boolean {
                    return true
                }
            }
            // From emoji variants
            editText = EditText(requireContext())
        })

        mBinding.emojiPopupLayout.show()
    }
}