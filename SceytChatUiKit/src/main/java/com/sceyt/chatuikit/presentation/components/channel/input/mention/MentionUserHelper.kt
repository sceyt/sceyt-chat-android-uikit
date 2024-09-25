package com.sceyt.chatuikit.presentation.components.channel.input.mention

import android.graphics.Typeface
import android.text.Annotation
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.annotation.ColorInt
import com.google.gson.Gson
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.notAutoCorrectable
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_STYLE

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
            body: CharSequence,
            mentionAttributes: List<BodyAttribute>?,
            mentionUsers: List<User>?,
            @ColorInt color: Int = 0,
            style: Int = UNSET_STYLE,
            mentionClickListener: ((String) -> Unit)? = null
    ): CharSequence {
        return try {
            mentionAttributes ?: return body
            val newBody = SpannableStringBuilder(body)
            mentionAttributes.sortedByDescending { it.offset }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)

                if (style != UNSET_STYLE)
                    newBody.setSpan(StyleSpan(style),
                        it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                if (mentionClickListener != null) {
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(textView: View) {
                            mentionClickListener(it.metadata ?: return)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            if (color != 0)
                                ds.color = color
                            ds.isUnderlineText = false
                        }
                    }
                    newBody.setSpan(clickableSpan, it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (color != 0)
                    newBody.setSpan(ForegroundColorSpan(color),
                        it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            newBody
        } catch (e: Exception) {
            e.printStackTrace()
            body
        }
    }

    fun buildOnlyBoldNamesWithMentionedUsers(
            spannableBody: CharSequence,
            attributes: List<BodyAttribute>?,
            mentionUsers: List<User>?,
            style: Int = Typeface.BOLD
    ): CharSequence {
        return try {
            val data = attributes?.filter { it.type == MENTION } ?: return spannableBody
            val newBody = SpannableStringBuilder(spannableBody)
            data.sortedByDescending { it.offset }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)
                if (style != UNSET_STYLE)
                    newBody.setSpan(StyleSpan(style),
                        it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            newBody
        } catch (e: Exception) {
            e.printStackTrace()
            spannableBody
        }
    }

    fun updateBodyWithUsers(body: String, attributes: List<BodyAttribute>?, mentionUsers: List<User>?): String {
        val data = attributes?.filter { it.type == MENTION } ?: return body
        val newBody = SpannableStringBuilder(body)
        data.sortedByDescending { it.offset }.forEach {
            setNewBodyWithName(mentionUsers, newBody, it)
        }
        return newBody.toString()
    }

    fun getMentionsIndexed(attributes: List<BodyAttribute>?, mentionUsers: List<User>?): List<Mention> {
        val list = arrayListOf<Mention>()
        val data = attributes?.filter { it.type == MENTION } ?: return list

        data.forEach { entry ->
            val userId = entry.metadata ?: return@forEach
            val user = mentionUsers?.find { it.id == entry.metadata } ?: User(userId)
            val name = userNameFormatter?.format(user) ?: user.getPresentableName()
            list.add(Mention(userId, name, entry.offset, entry.length))
        }
        return list
    }

    private fun setNewBodyWithName(
            mentionUsers: List<User>?,
            newBody: SpannableStringBuilder,
            item: BodyAttribute
    ): String {
        val mentionUser = mentionUsers?.find { mentionUser -> mentionUser.id == item.metadata }
        var name = mentionUser?.let { user ->
            userNameFormatter?.format(user) ?: user.getPresentableName()
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