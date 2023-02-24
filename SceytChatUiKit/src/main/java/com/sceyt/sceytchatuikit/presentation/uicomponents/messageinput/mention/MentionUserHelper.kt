package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

object MentionUserHelper {

    fun initMentionMetaData(body: String, mentionUsers: List<MentionUserData>): String {
        if (body.isEmpty() || mentionUsers.isEmpty()) return ""
        val items = hashMapOf<String, MentionUserMetaDataPayLoad>()
        mentionUsers.forEach {
            val name = it.toString()
            body.indexOf(name).let { index ->
                if (index != -1)
                    items[it.id] = MentionUserMetaDataPayLoad(index.toString(), name.length.toString())
            }
        }
        return Gson().toJson(items)
    }

    fun buildWithMentionedUsers(context: Context, body: String, metaData: String?, mentionUsers: Array<User>?): SpannableString {
        metaData ?: return SpannableString(body)
        try {
            val empMapType = object : TypeToken<Map<String, MentionUserMetaDataPayLoad>>() {}.type
            val data: Map<String, MentionUserMetaDataPayLoad> = Gson().fromJson(metaData, empMapType)

            val newBody = SpannableString(body)
            data.entries.reversed().forEach {
                val mentionUser = mentionUsers?.find { mentionUser -> mentionUser.id == it.key }
                var name = mentionUser?.let { user ->
                    SceytKitConfig.userNameBuilder?.invoke(user) ?: it.key
                } ?: it.key
                name = "@$name"
                newBody.replaceRange(it.value.loc.toInt(), it.value.loc.toInt() + it.value.len.toInt(), name)
                newBody.setSpan(ForegroundColorSpan(context.getCompatColor(SceytKitConfig.sceytColorAccent)), it.value.loc.toInt(), it.value.loc.toInt() + name.length, 0)
            }

            return newBody

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return SpannableString(body)
    }

    data class MentionUserMetaDataPayLoad(
            val loc: String,
            val len: String
    )
}