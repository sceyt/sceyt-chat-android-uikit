package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.GoogleEmojiData
import com.sceyt.sceytchatuikit.databinding.ItemEmojiBinding
import com.sceyt.sceytchatuikit.databinding.SceytFragmentBottomSheetEmojisBinding
import com.sceyt.sceytchatuikit.extensions.dismissSafety

class BottomSheetEmojisFragment(private val emojiListener: (String) -> Unit) : BottomSheetDialogFragment() {
    private lateinit var mBinding: SceytFragmentBottomSheetEmojisBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SceytAppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = SceytFragmentBottomSheetEmojisBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initEmojis()
    }

    private fun initEmojis() {
        mBinding.rvEmojes.layoutManager = GridLayoutManager(requireContext(), 7)
        mBinding.rvEmojes.adapter = EmojiAdapter()
    }

    inner class EmojiAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val data :List<String> = mutableListOf<String>().apply {
            GoogleEmojiData.data.forEach {
                addAll(it)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding = ItemEmojiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return EmojiViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as EmojiViewHolder).bind(data[position])
        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class EmojiViewHolder(val binding: ItemEmojiBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(emoji: String) {
                binding.tvReaction.setSmileText(emoji)

                binding.root.setOnClickListener {
                    emojiListener(emoji)
                    dismissSafety()
                }
            }
        }
    }
}

