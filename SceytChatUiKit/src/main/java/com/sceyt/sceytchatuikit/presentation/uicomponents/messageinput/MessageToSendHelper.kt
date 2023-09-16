package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import android.content.Context
import android.text.SpannableStringBuilder
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toSceytAttachment
import com.sceyt.sceytchatuikit.extensions.extractLinks
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.isValidUrl
import com.sceyt.sceytchatuikit.extensions.notAutoCorrectable
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.mappers.createEmptyUser
import com.sceyt.sceytchatuikit.persistence.mappers.getAttachmentType
import com.sceyt.sceytchatuikit.persistence.mappers.toBodyAttribute
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionAnnotation
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyler
import java.io.File

class MessageToSendHelper(private val context: Context) {
    private var messageInputActionCallback: MessageInputView.MessageInputActionCallback? = null

    fun sendMessage(allAttachments: List<Attachment>, body: CharSequence?, editMessage: SceytMessage?,
                    replyMessage: SceytMessage?, replyThreadMessageId: Long?) {
        val replacedBody = replaceBodyMentions(body)

        if (!checkIsEditingMessage(replacedBody, editMessage)) {
            val link = getLinkAttachmentFromBody(body)
            if (allAttachments.isNotEmpty()) {
                val messages = arrayListOf<Message>()
                allAttachments.forEachIndexed { index, attachment ->
                    var attachments = arrayOf(attachment)
                    val message = if (index == 0) {
                        if (link != null)
                            attachments = attachments.plus(link)
                        buildMessage(replacedBody, attachments, true, replyMessage, replyThreadMessageId)
                    } else buildMessage("", attachments, false, replyMessage, replyThreadMessageId)

                    messages.add(message)
                }
                messageInputActionCallback?.sendMessages(messages)
            } else {
                val attachment = if (link != null) arrayOf(link) else arrayOf()
                messageInputActionCallback?.sendMessage(buildMessage(replacedBody, attachment, true,
                    replyMessage, replyThreadMessageId))
            }
        }
    }

    private fun buildMessage(body: CharSequence, attachments: Array<Attachment>,
                             withMentionedUsers: Boolean, replyMessage: SceytMessage?, replyThreadMessageId: Long?): Message {
        val message = Message.MessageBuilder()
            .setTid(ClientWrapper.generateTid())
            .setAttachments(attachments)
            .setType("text")
            .setBody(body.toString())
            .setCreatedAt(System.currentTimeMillis())
            .initRelyMessage(replyMessage, replyThreadMessageId)

        if (withMentionedUsers) {
            val data = getMentionUsersAndAttributes(body)
            message.setMentionedUsers(data.second)
            message.setBodyAttributes(data.first.toTypedArray())
        }

        return message.build()
    }

    private fun replaceBodyMentions(body: CharSequence?): SpannableStringBuilder {
        val newBody = SpannableStringBuilder(body?.trim() ?: return SpannableStringBuilder())
        val mentions = MentionAnnotation.getMentionsFromAnnotations(body.trim())
        if (mentions.isEmpty()) return newBody
        mentions.sortedByDescending { it.start }.forEach { mention ->
            val replaceText = "@${mention.recipientId.notAutoCorrectable()}"
            newBody.replace(mention.start, mention.start + mention.length, replaceText)
        }

        return newBody
    }

    private fun initBodyAttributes(styling: List<BodyStyleRange>?, mentions: List<Mention>?): List<BodyAttribute> {
        val attributes = styling?.map { it.toBodyAttribute() }?.toArrayList() ?: arrayListOf()

        if (!mentions.isNullOrEmpty()) {
            MentionUserHelper.initMentionAttributes(mentions)?.let {
                attributes.addAll(it)
            }
        }
        return attributes
    }

    private fun checkIsEditingMessage(body: CharSequence, editMessage: SceytMessage?): Boolean {
        editMessage?.let { message ->
            val linkAttachment = getLinkAttachmentFromBody(body)?.toSceytAttachment(message.tid, TransferState.Uploaded)
            message.body = body.toString()

            if (linkAttachment != null)
                if (message.attachments.isNullOrEmpty())
                    message.attachments = arrayOf(linkAttachment)
                else message.attachments = (message.attachments ?: arrayOf()).plus(linkAttachment)

            val data = getMentionUsersAndAttributes(body)
            message.mentionedUsers = data.second
            message.bodyAttributes = data.first

            messageInputActionCallback?.sendEditMessage(message)
            return true
        }
        return false
    }

    private fun getMentionUsersAndAttributes(body: CharSequence): Pair<List<BodyAttribute>, Array<User>> {
        val bodyAttributes = arrayListOf<BodyAttribute>()
        var mentionUsers = arrayOf<User>()
        val mentions = MentionAnnotation.getMentionsFromAnnotations(body)
        val styling = BodyStyler.getStyling(body)
        val attributes = initBodyAttributes(styling, mentions)
        if (attributes.isNotEmpty()) {
            bodyAttributes.addAll(attributes)
            mentionUsers = mentions.map { createEmptyUser(it.recipientId, it.name) }.toTypedArray()
        }
        return Pair(bodyAttributes, mentionUsers)
    }

    private fun getLinkAttachmentFromBody(body: CharSequence?): Attachment? {
        val links = body.extractLinks()
        val isContainsLink = links.isNotEmpty() && links[0].isValidUrl(context)
        if (isContainsLink) {
            return Attachment.Builder("", links[0], AttachmentTypeEnum.Link.value())
                .withTid(ClientWrapper.generateTid())
                .setName("")
                .setMetadata("")
                .setCreatedAt(System.currentTimeMillis())
                .setUpload(false)
                .build()
        }
        return null
    }

    private fun Message.MessageBuilder.initRelyMessage(replyMessage: SceytMessage?, replyThreadMessageId: Long?): Message.MessageBuilder {
        replyMessage?.let {
            setParentMessageId(it.id)
            setParentMessage(it.toMessage())
            // setReplyInThread(replyThreadMessageId != null)
        } ?: replyThreadMessageId?.let {
            setParentMessageId(it)
            //setReplyInThread(true)
        }
        return this
    }

    fun buildAttachment(path: String, metadata: String = "", attachmentType: String? = null): Attachment? {
        val file = File(path)
        if (file.exists()) {
            val type = attachmentType ?: getAttachmentType(path).value()
            return Attachment.Builder(path, null, type)
                .setName(File(path).name)
                .withTid(ClientWrapper.generateTid())
                .setMetadata(metadata)
                .setCreatedAt(System.currentTimeMillis())
                .setFileSize(getFileSize(path))
                .setUpload(false)
                .build()
        }
        return null
    }

    fun setInputActionCallback(callback: MessageInputView.MessageInputActionCallback) {
        messageInputActionCallback = callback
    }
}