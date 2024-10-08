package com.sceyt.chatuikit.presentation.components.channel.input.mention.query

import android.content.Context

/**
 * Encapsulate how to replace a query with a user selected result.
 */
sealed class InlineQueryReplacement(@get:JvmName("isKeywordSearch") val keywordSearch: Boolean = false) {
  abstract fun toCharSequence(context: Context): CharSequence
}
