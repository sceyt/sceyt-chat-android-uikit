package com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.inlinequery

/**
 * Represents an inline query via compose text.
 */
sealed class InlineQuery(val query: String) {
  object NoQuery : InlineQuery("")
  class Mention(query: String) : InlineQuery(query)
}
