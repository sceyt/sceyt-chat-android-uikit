package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.text.SpannableString
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toBodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyler

object MessageBodyStyleHelper {

    fun buildWithAttributes(body: String, attribute: List<BodyAttribute>?): SpannableString {
        return BodyStyler.appendStyle(body, attribute?.mapNotNull { it.toBodyStyleRange() })
    }

    fun buildWithMentionsAndAttributes(message: SceytMessage): SpannableString {
        return buildWithMentionsAndAttributes(message.body, message.metadata, message.mentionedUsers, message.bodyAttributes)
    }

    fun buildWithMentionsAndAttributes(body: String, metaData: String?,
                                       mentionUsers: Array<User>?, bodyAttributes: List<BodyAttribute>?): SpannableString {

        val spannableBody = buildWithAttributes(body.trim(), bodyAttributes)
        return MentionUserHelper.buildOnlyNamesWithMentionedUsers(spannableBody, metaData, mentionUsers)
    }
}