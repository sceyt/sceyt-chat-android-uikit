package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.graphics.Typeface
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
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlin.collections.set


object MentionUserHelper {

    fun initMentionMetaData(body: String, mentionUsers: List<MentionUserData>): String {
        if (body.isEmpty() || mentionUsers.isEmpty()) return ""
        val items = mutableMapOf<String, MentionUserMetaDataPayLoad>()
        mentionUsers.forEach {
            val name = it.toString()
            body.indexOf(name, ignoreCase = true).let { index ->
                if (index != -1)
                    items[it.id] = MentionUserMetaDataPayLoad(index, name.length)
            }
        }
        return Gson().toJson(items)
    }

    fun buildWithMentionedUsers(context: Context, body: String, metaData: String?,
                                mentionUsers: Array<User>?, @ColorRes colorId: Int = SceytKitConfig.sceytColorAccent,
                                enableClick: Boolean): SpannableString {
        metaData ?: return SpannableString(body)
        return try {
            val empMapType = object : TypeToken<Map<String, MentionUserMetaDataPayLoad>>() {}.type
            val data: Map<String, MentionUserMetaDataPayLoad> = Gson().fromJson(metaData, empMapType)

            val newBody = SpannableStringBuilder(body)
            data.entries.sortedByDescending { it.value.loc }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)
                newBody.setSpan(ForegroundColorSpan(context.getCompatColor(colorId)),
                    it.value.loc, it.value.loc + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

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
                    newBody.setSpan(clickableSpan, it.value.loc, it.value.loc + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            SpannableString.valueOf(newBody)
        } catch (ex: Exception) {
            ex.printStackTrace()
            SpannableString(body)
        }
    }

    fun buildOnlyNamesWithMentionedUsers(body: String, metaData: String?,
                                         mentionUsers: Array<User>?): SpannableString {
        if (metaData.isNullOrBlank()) return SpannableString(body)
        return try {
            val empMapType = object : TypeToken<Map<String, MentionUserMetaDataPayLoad>>() {}.type
            val data: Map<String, MentionUserMetaDataPayLoad> = Gson().fromJson(metaData, empMapType)

            val newBody = SpannableStringBuilder(body)
            data.entries.sortedByDescending { it.value.loc }.forEach {
                val name = setNewBodyWithName(mentionUsers, newBody, it)
                newBody.setSpan(StyleSpan(Typeface.BOLD),
                    it.value.loc, it.value.loc + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            SpannableString.valueOf(newBody)
        } catch (ex: Exception) {
            ex.printStackTrace()
            SpannableString(body)
        }
    }

    fun containsMentionsUsers(message: SceytMessage): Boolean {
        if (message.mentionedUsers?.isNotEmpty() == true) return true
        if (message.metadata.isNullOrBlank()) return false

        return try {
            val empMapType = object : TypeToken<Map<String, MentionUserMetaDataPayLoad>>() {}.type
            val data: Map<String, MentionUserMetaDataPayLoad> = Gson().fromJson(message.metadata, empMapType)
            data.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getMentionData(message: SceytMessage): Map<String, MentionUserMetaDataPayLoad>? {
        if (message.metadata.isNullOrBlank()) return null

        return try {
            val empMapType = object : TypeToken<Map<String, MentionUserMetaDataPayLoad>>() {}.type
            return Gson().fromJson(message.metadata, empMapType)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun setNewBodyWithName(mentionUsers: Array<User>?, newBody: SpannableStringBuilder,
                                   item: Map.Entry<String, MentionUserMetaDataPayLoad>): String {
        val mentionUser = mentionUsers?.find { mentionUser -> mentionUser.id == item.key }
        var name = mentionUser?.let { user ->
            SceytKitConfig.userNameBuilder?.invoke(user) ?: user.getPresentableName()
        } ?: item.key
        name = "@$name"

        val end = item.value.loc + item.value.len
        if (end > newBody.length)
            for (i in 0..end - newBody.length)
                newBody.append(" ")

        newBody.replace(item.value.loc, end, name)
        return name
    }

    data class MentionUserMetaDataPayLoad(
            val loc: Int,
            val len: Int
    )
}