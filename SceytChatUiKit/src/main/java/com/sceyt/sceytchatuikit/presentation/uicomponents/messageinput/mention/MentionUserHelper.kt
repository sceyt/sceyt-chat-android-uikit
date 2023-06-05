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
import com.google.gson.reflect.TypeToken
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.notAutoCorrectable
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig


object MentionUserHelper {
    private var userNameBuilder = SceytKitConfig.userNameBuilder

    fun initMentionMetaData(body: String, mentionUsers: List<Mention>): String {
        if (body.isEmpty() || mentionUsers.isEmpty()) return ""
        val items = mutableListOf<MentionUserMetaDataPayLoad>()
        mentionUsers.forEach {
            val userId = it.recipientId
            items.add(MentionUserMetaDataPayLoad(userId, it.start, it.length))
        }
        return Gson().toJson(items)
    }

    fun buildWithMentionedUsers(context: Context, body: String, metaData: String?,
                                mentionUsers: Array<User>?, @ColorRes colorId: Int = SceytKitConfig.sceytColorAccent,
                                enableClick: Boolean): SpannableString {
        val data = getMentionData(metaData) ?: return SpannableString(body)
        val newBody = SpannableStringBuilder(body)

        return try {
            data.sortedByDescending { it.loc }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)
                newBody.setSpan(ForegroundColorSpan(context.getCompatColor(colorId)),
                    it.loc, it.loc + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                if (enableClick) {
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(textView: View) {
                            //todo: implement click action
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                        }
                    }
                    newBody.setSpan(clickableSpan, it.loc, it.loc + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            SpannableString.valueOf(newBody)
        } catch (e: Exception) {
            e.printStackTrace()
            SpannableString.valueOf(body)
        }
    }

    fun buildOnlyNamesWithMentionedUsers(body: String, metaData: String?,
                                         mentionUsers: Array<User>?): SpannableString {
        if (metaData.isNullOrBlank()) return SpannableString(body)

        return try {
            val data = getMentionData(metaData) ?: return SpannableString(body)
            val newBody = SpannableStringBuilder(body)
            data.sortedByDescending { it.loc }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)
                newBody.setSpan(StyleSpan(Typeface.BOLD),
                    it.loc, it.loc + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            SpannableString.valueOf(newBody)
        } catch (e: Exception) {
            e.printStackTrace()
            SpannableString.valueOf(body)
        }
    }

    fun updateBodyWithUsers(body: String, metaData: String?, mentionUsers: Array<User>?): String {
        if (metaData.isNullOrBlank()) return body

        val data = getMentionData(metaData) ?: return body
        val newBody = SpannableStringBuilder(body)
        data.sortedByDescending { it.loc }.forEach {
            setNewBodyWithName(mentionUsers, newBody, it)
        }
        return newBody.toString()
    }

    fun containsMentionsUsers(message: SceytMessage): Boolean {
        if (message.mentionedUsers?.isNotEmpty() == true) return true
        if (message.metadata.isNullOrBlank()) return false

        return try {
            getMentionData(message.metadata)?.isNotEmpty() == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getMentionsIndexed(metaData: String?, mentionUsers: Array<User>?): List<Mention> {
        val list = arrayListOf<Mention>()
        val data = getMentionData(metaData) ?: return list

        data.forEach { entry ->
            val user = mentionUsers?.find { it.id == entry.id } ?: User(entry.id)
            val name = userNameBuilder?.invoke(user) ?: user.getPresentableName()
            list.add(Mention(entry.id, name, entry.loc, entry.len))
        }
        return list
    }

    private fun setNewBodyWithName(mentionUsers: Array<User>?, newBody: SpannableStringBuilder,
                                   item: MentionUserMetaDataPayLoad): String {
        val mentionUser = mentionUsers?.find { mentionUser -> mentionUser.id == item.id }
        var name = mentionUser?.let { user ->
            userNameBuilder?.invoke(user) ?: user.getPresentableName()
        } ?: item.id
        name = "@$name".notAutoCorrectable()

        val end = item.loc + item.len
        if (end > newBody.length)
            for (i in 0..end - newBody.length)
                newBody.append(" ")

        newBody.replace(item.loc, end, name)
        return name
    }

    fun getMentionData(metadata: String?): List<MentionUserMetaDataPayLoad>? {
        if (metadata.isNullOrBlank()) return null

        return try {
            val empMapType = object : TypeToken<List<MentionUserMetaDataPayLoad>>() {}.type
            return Gson().fromJson(metadata, empMapType)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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