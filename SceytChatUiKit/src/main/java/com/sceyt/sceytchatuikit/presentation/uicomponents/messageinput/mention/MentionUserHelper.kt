package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.graphics.Typeface
import android.text.Annotation
import android.text.SpannableString
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
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.notAutoCorrectable
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

object MentionUserHelper {
    const val MENTION = "mention"
    var userNameBuilder = SceytKitConfig.userNameBuilder
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
                                mentionUsers: Array<User>?, @ColorRes colorId: Int = SceytKitConfig.sceytColorAccent,
                                mentionClickListener: ((String) -> Unit)? = null): SpannableString {
        val data = attributes?.filter { it.type == MENTION }
                ?: return SpannableString.valueOf(body)

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
            SpannableString.valueOf(newBody)
        } catch (e: Exception) {
            e.printStackTrace()
            SpannableString.valueOf(body)
        }
    }

    fun buildOnlyBoldNamesWithMentionedUsers(spannableBody: SpannableString, attributes: List<BodyAttribute>?,
                                             mentionUsers: Array<User>?): SpannableString {
        return try {
            val data = attributes?.filter { it.type == MENTION } ?: return spannableBody
            val newBody = SpannableStringBuilder(spannableBody)
            data.sortedByDescending { it.offset }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)
                newBody.setSpan(StyleSpan(Typeface.BOLD),
                    it.offset, it.offset + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            SpannableString.valueOf(newBody)
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
            val name = userNameBuilder?.invoke(user) ?: user.getPresentableName()
            list.add(Mention(userId, name, entry.offset, entry.length))
        }
        return list
    }

    private fun setNewBodyWithName(mentionUsers: Array<User>?, newBody: SpannableStringBuilder,
                                   item: BodyAttribute): String {
        val mentionUser = mentionUsers?.find { mentionUser -> mentionUser.id == item.metadata }
        var name = mentionUser?.let { user ->
            userNameBuilder?.invoke(user) ?: user.getPresentableName()
        } ?: item.metadata
        name = "@$name".notAutoCorrectable()

        val end = item.offset + item.length
        if (end > newBody.length)
            for (i in 0..end - newBody.length)
                newBody.append(" ")

        newBody.replace(item.offset, end, name)
        return name
    }

    fun setCustomUserNameBuilder(userNameBuilder: (User) -> String) {
        this.userNameBuilder = userNameBuilder
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