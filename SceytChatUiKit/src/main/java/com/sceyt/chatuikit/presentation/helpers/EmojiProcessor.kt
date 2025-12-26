package com.sceyt.chatuikit.presentation.helpers

import androidx.emoji2.text.EmojiCompat
import com.sceyt.chatuikit.extensions.processEmojiCompat

class EmojiProcessor {
    private var emojiInitCallback: EmojiCompat.InitCallback? = null

    /**
     * Process emoji with improved performance for avatar use cases.
     * Returns text immediately without blocking, and optionally calls callback when EmojiCompat is ready.
     */
    fun processEmojiSafe(
        text: CharSequence,
        onEmojiReady: ((CharSequence) -> Unit)? = null,
    ): CharSequence {
        return when (getEmojiCompatLoadState()) {
            EmojiCompat.LOAD_STATE_SUCCEEDED -> text.processEmojiCompat() ?: text
            EmojiCompat.LOAD_STATE_LOADING -> {
                // Register callback only if one is provided and we don't already have one
                if (onEmojiReady != null && emojiInitCallback == null) {
                    val emojiInitCallback = object : EmojiCompat.InitCallback() {
                        override fun onInitialized() {
                            onEmojiReady.invoke(text.processEmojiCompat() ?: text)
                            unregister()
                        }

                        override fun onFailed(throwable: Throwable?) {
                            unregister()
                        }
                    }
                    this.emojiInitCallback = emojiInitCallback
                    EmojiCompat.get().registerInitCallback(emojiInitCallback)
                }
                text // Return immediately without blocking
            }

            else -> text
        }
    }

    /**
     * Fast emoji processing that doesn't wait for EmojiCompat initialization.
     * Use this for avatar initials where we prefer speed over perfect emoji rendering.
     */
    fun processEmojiNonBlocking(text: CharSequence): CharSequence {
        return if (getEmojiCompatLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED) {
            text.processEmojiCompat() ?: text
        } else {
            text // Return immediately without waiting
        }
    }

    private fun getEmojiCompatLoadState(): Int {
        return try {
            EmojiCompat.get().loadState
        } catch (_: Exception) {
            EmojiCompat.LOAD_STATE_DEFAULT
        }
    }

    fun unregister() {
        emojiInitCallback?.let {
            try {
                EmojiCompat.get().unregisterInitCallback(it)
            } catch (_: Exception) {
                // EmojiCompat might not be available
            }
            emojiInitCallback = null
        }
    }
}