package com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.inlinequery

import android.content.Context

/**
 * Encapsulate how to replace a query with a user selected result.
 */
sealed class InlineQueryReplacement(@get:JvmName("isKeywordSearch") val keywordSearch: Boolean = false) {
  abstract fun toCharSequence(context: Context): CharSequence
}
