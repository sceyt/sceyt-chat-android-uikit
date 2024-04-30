package com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.graphics.Typeface
import android.text.Annotation
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.annotation.ColorRes
import com.google.gson.Gson
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.notAutoCorrectable
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter

object MentionUserHelper {
    const val MENTION = "mention"
    var userNameFormatter = SceytKitConfig.userNameFormatter
        private set

    fun initMentionAttributes(mentionUsers: List<Mention>): List<BodyAttribute>? {
        if (mentionUsers.isEmpty()) return null
        val items = mutableListOf<BodyAttribute>()
        mentionUsers.forEach {
            items.add(BodyAttribute(MENTION, it.start, it.length, it.recipientId))
        }
        return items
    }

    fun buildWithMentionedUsers(context: Context, body: CharSequence, attributes: List<BodyAttribute>?,
                                mentionUsers: Array<User>?, @ColorRes colorId: Int = SceytChatUIKit.theme.accentColor,
                                mentionClickListener: ((String) -> Unit)? = null): CharSequence {
        val data = attributes?.filter { it.type == MENTION }
                ?: return body

        val newBody = SpannableStringBuilder(body)

        return try {
            data.sortedByDescending { it.offset }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)

                if (mentionClickListener != null) {
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(textView: View) {
                            mentionClickListener(it.metadata ?: return)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = context.getCompatColor(colorId)
                            ds.isUnderlineText = false
                        }
                    }
                    newBody.setSpan(clickableSpan, it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else
                    newBody.setSpan(ForegroundColorSpan(context.getCompatColor(colorId)),
                        it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            newBody
        } catch (e: Exception) {
            e.printStackTrace()
            body
        }
    }

    fun buildOnlyBoldNamesWithMentionedUsers(spannableBody: CharSequence, attributes: List<BodyAttribute>?,
                                             mentionUsers: Array<User>?): CharSequence {
        return try {
            val data = attributes?.filter { it.type == MENTION } ?: return spannableBody
            val newBody = SpannableStringBuilder(spannableBody)
            data.sortedByDescending { it.offset }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)
                newBody.setSpan(StyleSpan(Typeface.BOLD),
                    it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            newBody
        } catch (e: Exception) {
            e.printStackTrace()
            spannableBody
        }
    }

    fun updateBodyWithUsers(body: String, attributes: List<BodyAttribute>?, mentionUsers: Array<User>?): String {
        val data = attributes?.filter { it.type == MENTION } ?: return body
        val newBody = SpannableStringBuilder(body)
        data.sortedByDescending { it.offset }.forEach {
            setNewBodyWithName(mentionUsers, newBody, it)
        }
        return newBody.toString()
    }

    fun getMentionsIndexed(attributes: List<BodyAttribute>?, mentionUsers: Array<User>?): List<Mention> {
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

    private fun setNewBodyWithName(mentionUsers: Array<User>?, newBody: SpannableStringBuilder,
                                   item: BodyAttribute): String {
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

    fun setCustomUserNameFormatter(userNameFormatter: UserNameFormatter) {
        this.userNameFormatter = userNameFormatter
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