package com.sceyt.chatuikit.presentation.components.channel.input.mention.query

/**
 * Represents an inline query via compose text.
 */
sealed class InlineQuery(val query: String) {
  data object NoQuery : InlineQuery("")
  class Mention(query: String) : InlineQuery(query)
}
