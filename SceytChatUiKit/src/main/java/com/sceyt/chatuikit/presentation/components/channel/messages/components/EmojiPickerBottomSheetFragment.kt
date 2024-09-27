package com.sceyt.chatuikit.presentation.components.channel.messages.components

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytFragmentBottomSheetEmojiPickerBinding
import com.sceyt.chatuikit.extensions.dismissSafety
import com.sceyt.chatuikit.extensions.getCompatColor
import com.vanniktech.emoji.EmojiTheming
import com.vanniktech.emoji.search.NoSearchEmoji

class EmojiPickerBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var binding: SceytFragmentBottomSheetEmojiPickerBinding
    private var emojiListener: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SceytAppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SceytFragmentBottomSheetEmojiPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initEmojis()
    }

    private fun initEmojis() {
        binding.emojiView.setUp(
            rootView = binding.root,
            onEmojiClickListener = {
                binding.emojiView.tearDown()
                emojiListener?.invoke(it.unicode)
                dismissSafety()
            },
            onEmojiBackspaceClickListener = null,
            editText = null,
            theming = EmojiTheming(backgroundColor = Color.TRANSPARENT,
                primaryColor = requireContext().getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor),
                secondaryColor = requireContext().getCompatColor(SceytChatUIKit.theme.colors.accentColor),
                dividerColor = requireContext().getCompatColor(R.color.sceyt_color_border),
                Color.BLACK, Color.BLACK),
            searchEmoji = NoSearchEmoji
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).isDraggable = false
            }
        }
    }

    fun setEmojiListener(listener: (String) -> Unit) {
        emojiListener = listener
    }
}

