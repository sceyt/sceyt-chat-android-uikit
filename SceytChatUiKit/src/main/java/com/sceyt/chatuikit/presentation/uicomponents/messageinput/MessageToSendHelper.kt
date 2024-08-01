package com.sceyt.chatuikit.presentation.uicomponents.messageinput

import android.content.Context
import android.text.SpannableStringBuilder
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.extractLinks
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.isValidUrl
import com.sceyt.chatuikit.extensions.notAutoCorrectable
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import com.sceyt.chatuikit.persistence.mappers.getAttachmentType
import com.sceyt.chatuikit.persistence.mappers.toBodyAttribute
import com.sceyt.chatuikit.persistence.mappers.toMessage
import com.sceyt.chatuikit.persistence.mappers.toMetadata
import com.sceyt.chatuikit.persistence.mappers.toSceytAttachment
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.MentionAnnotation
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.style.BodyStyler
import java.io.File

class MessageToSendHelper(private val context: Context) {
    private var messageInputActionCallback: MessageInputView.MessageInputActionCallback? = null

    fun sendMessage(allAttachments: List<Attachment>, body: CharSequence?, editMessage: SceytMessage?,
                    replyMessage: SceytMessage?, replyThreadMessageId: Long?, linkDetails: LinkPreviewDetails?) {
        val replacedBody = replaceBodyMentions(body)

        if (!checkIsEditingMessage(replacedBody, editMessage, linkDetails)) {
            val link = getLinkAttachmentFromBody(body, linkDetails)
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
                messageInputActionCallback?.sendMessages(messages, linkDetails)
            } else {
                val attachment = if (link != null) arrayOf(link) else arrayOf()
                messageInputActionCallback?.sendMessage(buildMessage(replacedBody, attachment, true,
                    replyMessage, replyThreadMessageId), linkDetails)
            }
        }
    }

    private fun buildMessage(body: CharSequence, attachments: Array<Attachment>,
                             withMentionedUsers: Boolean, replyMessage: SceytMessage?,
                             replyThreadMessageId: Long?, type: String = "text"): Message {
        val message = Message.MessageBuilder()
            .setTid(ClientWrapper.generateTid())
            .setAttachments(attachments)
            .setType(type)
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

    private fun checkIsEditingMessage(body: CharSequence, message: SceytMessage?,
                                      linkDetails: LinkPreviewDetails?): Boolean {
        if (message == null) return false
        val linkAttachment = getLinkAttachmentFromBody(body, linkDetails)
            ?.toSceytAttachment(message.tid, TransferState.Uploaded, linkPreviewDetails = linkDetails)

        val attachments = if (linkAttachment != null) {
            if (message.attachments.isNullOrEmpty())
                arrayOf(linkAttachment)
            else {
                val existLinkIndex = message.attachments.indexOfFirst {
                    it.type == AttachmentTypeEnum.Link.value()
                }

                if (existLinkIndex == -1) {
                    message.attachments.plus(linkAttachment)
                } else {
                    val oldLinkAttachments = message.attachments
                    oldLinkAttachments[existLinkIndex] = linkAttachment
                    oldLinkAttachments
                }
            }
        } else // remove link attachment if exist, because message should contain only one link attachment
            message.attachments?.filter {
                it.type != AttachmentTypeEnum.Link.value()
            }?.toTypedArray()

        val (bodyAttributes, mentionedUsers) = getMentionUsersAndAttributes(body)
        val editedMessage = message.copy(
            body = body.toString(),
            attachments = attachments,
            bodyAttributes = bodyAttributes,
            mentionedUsers = mentionedUsers)

        messageInputActionCallback?.sendEditMessage(editedMessage, linkDetails)
        return true
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

    private fun getLinkAttachmentFromBody(body: CharSequence?, linkDetails: LinkPreviewDetails?): Attachment? {
        val validLink = linkDetails?.link
                ?: body.extractLinks().firstOrNull { it.isValidUrl(context) }
        if (validLink != null) {
            val metadata = linkDetails?.toMetadata() ?: ""
            return Attachment.Builder("", validLink, AttachmentTypeEnum.Link.value())
                .withTid(ClientWrapper.generateTid())
                .setName(linkDetails?.title ?: "")
                .setMetadata(metadata)
                .setCreatedAt(System.currentTimeMillis())
                .setUpload(false)
                .build()
        }
        return null
    }

    private fun Message.MessageBuilder.initRelyMessage(replyMessage: SceytMessage?,
                                                       replyThreadMessageId: Long?): Message.MessageBuilder {
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

    fun buildAttachment(path: String,
                        url: String = "",
                        metadata: String = "",
                        attachmentType: String? = null): Attachment? {
        val file = File(path)
        if (file.exists()) {
            val type = attachmentType ?: getAttachmentType(path).value()
            return Attachment.Builder(path, url, type)
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