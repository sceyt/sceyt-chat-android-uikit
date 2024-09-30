package com.sceyt.chatuikit.presentation.components.channel.input.mention

import android.content.Context
import android.text.Annotation
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.google.gson.Gson
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.notAutoCorrectable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.TextStyle

object MentionUserHelper {
    const val MENTION = "mention"
    private val userNameFormatter get() = SceytChatUIKit.formatters.mentionUserNameFormatter

    fun initMentionAttributes(mentionUsers: List<Mention>): List<BodyAttribute>? {
        if (mentionUsers.isEmpty()) return null
        val items = mutableListOf<BodyAttribute>()
        mentionUsers.forEach {
            items.add(BodyAttribute(MENTION, it.start, it.length, it.recipientId))
        }
        return items
    }

    fun buildWithMentionedUsers(
            context: Context,
            body: CharSequence,
            mentionAttributes: List<BodyAttribute>?,
            mentionUsers: List<SceytUser>?,
            mentionTextStyle: TextStyle,
            mentionUserNameFormatter: Formatter<SceytUser>,
            mentionClickListener: ((String) -> Unit)? = null,
    ): CharSequence {
        return try {
            mentionAttributes ?: return body
            val newBody = SpannableStringBuilder(body)
            mentionAttributes.sortedByDescending { it.offset }.forEach {
                val name = setNewBodyWithName(context, mentionUsers, newBody, it, mentionUserNameFormatter)
                mentionTextStyle.apply(context, newBody, it.offset, it.offset + name.length)
                if (mentionClickListener != null) {
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(textView: View) {
                            mentionClickListener(it.metadata ?: return)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.isUnderlineText = false
                        }
                    }
                    newBody.setSpan(clickableSpan, it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            newBody
        } catch (e: Exception) {
            e.printStackTrace()
            body
        }
    }

    fun getMentionsIndexed(attributes: List<BodyAttribute>?, mentionUsers: List<SceytUser>?): List<Mention> {
        val list = arrayListOf<Mention>()
        val data = attributes?.filter { it.type == MENTION } ?: return list

        data.forEach { entry ->
            val userId = entry.metadata ?: return@forEach
            val user = mentionUsers?.find { it.id == entry.metadata } ?: SceytUser(userId)
            val name = userNameFormatter?.format(user) ?: user.getPresentableName()
            list.add(Mention(userId, name, entry.offset, entry.length))
        }
        return list
    }

    private fun setNewBodyWithName(
            context: Context,
            mentionUsers: List<SceytUser>?,
            newBody: SpannableStringBuilder,
            item: BodyAttribute,
            mentionUserNameFormatter: Formatter<SceytUser>
    ): CharSequence {
        val mentionUser = mentionUsers?.find { mentionUser -> mentionUser.id == item.metadata }
        var name = mentionUser?.let { user ->
            mentionUserNameFormatter.format(context, user)
        } ?: item.metadata
        name = "@$name".notAutoCorrectable()

        val end = item.offset + item.length
        if (end > newBody.length)
            for (i in 0..end - newBody.length)
                newBody.append(" ")

        newBody.replace(item.offset, end, name)
        return name
    }

    fun Annotation.getValueData(): MentionAnnotationValue? {
        return try {
            Gson().fromJson(value, MentionAnnotationValue::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}