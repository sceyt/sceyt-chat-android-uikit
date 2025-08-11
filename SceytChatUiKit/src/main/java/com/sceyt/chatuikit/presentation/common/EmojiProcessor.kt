package com.sceyt.chatuikit.presentation.common

import androidx.emoji2.text.EmojiCompat
import com.sceyt.chatuikit.extensions.processEmojiCompat

class EmojiProcessor {
    private var emojiInitCallback: EmojiCompat.InitCallback? = null

    fun processEmojiSafe(
            text: CharSequence,
            onEmojiReady: ((CharSequence) -> Unit)? = null,
    ): CharSequence {
        return when (EmojiCompat.get().loadState) {
            EmojiCompat.LOAD_STATE_SUCCEEDED -> text.processEmojiCompat() ?: text
            EmojiCompat.LOAD_STATE_LOADING -> {
                val emojiInitCallback = object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        onEmojiReady?.invoke(text.processEmojiCompat() ?: text)
                        unregister()
                    }

                    override fun onFailed(throwable: Throwable?) {
                        unregister()
                    }
                }
                this.emojiInitCallback = emojiInitCallback
                EmojiCompat.get().registerInitCallback(emojiInitCallback)
                text // fallback while loading
            }

            else -> text
        }
    }

    fun unregister() {
        emojiInitCallback?.let {
            EmojiCompat.get().unregisterInitCallback(it)
            emojiInitCallback = null
        }
    }
}
