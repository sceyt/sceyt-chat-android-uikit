package com.sceyt.chat.ui

import android.graphics.Color
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
import com.sceyt.chat.ui.databinding.FragmentBottomSheetEmojisBinding

class BottomSheetEmojisFragment(private val emojiListener: (Emoji) -> Unit) : BottomSheetDialogFragment() {
    private lateinit var mBinding: FragmentBottomSheetEmojisBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = FragmentBottomSheetEmojisBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initEmojis()
    }

    private fun initEmojis() {
        AXEmojiManager.setEmojiViewTheme(AXEmojiTheme().apply {
            backgroundColor = Color.WHITE
            categoryColor = Color.WHITE
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